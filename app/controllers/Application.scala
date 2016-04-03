package controllers

import play.api._
import play.api.mvc._
import play.api.cache.Cache
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.data.Forms._
import play.api.data.Form
import play.api.db.slick.DBAction
import models.DomainHelper
import models.BetterDb
import models.User
import models.UserNoPw
import models.UserNoPwC
import models.BetterSettings
import play.api.db.slick.DBSessionRequest


import org.joda.time.DateTime
import javax.inject.{Inject, Provider, Singleton}
import play.api.mvc.{Action, Controller}
import play.api.routing.{JavaScriptReverseRoute, JavaScriptReverseRouter, Router}
import play.api.inject.ApplicationLifecycle

object PlayHelper {

   val debug = Play.current.configuration.getBoolean("betterplay.debug").getOrElse(false)  
   val superpassword = Play.current.configuration.getString("betterplay.superpassword").getOrElse(java.util.UUID.randomUUID().toString)  
     
}


object FormToV {
  import scalaz.{\/,-\/,\/-}
  implicit def toV[T](form: Form[T]): JsValue \/ T = {
	  form.fold(
	     err => -\/(err.errorsAsJson),
		 succ => \/-(succ)	  
      )
  }
}


/***
 * security based on 
 * http://www.jamesward.com/2013/05/13/securing-single-page-apps-and-rest-services
 * and especially
 * http://www.mariussoutier.com/blog/2013/07/14/272/
 *
 * It is important to not trust the client.
 * Every action that requires a certain user should first fetch the user from the database
 * The User object should not get to the UI there is the UserNoPw for that 
 * which does not have the fields passwordHash and e-mail
 * 
 * actions that reveal sensitive information only fetch from db the user that is logged in by token 
 *
 **/
trait Security { self: Controller =>

	
  implicit val app: play.api.Application = play.api.Play.current

  val AuthTokenHeader = "X-AUTH-TOKEN"
  val AuthTokenCookieKey = "AUTH-TOKEN"
  val AuthTokenUrlKey = "auth"

  /** Checks that a token is either in the header ***/ 
  def HasToken[A](p: BodyParser[A] = parse.anyContent)(f: String => Long => DBSessionRequest[A] => Result): Action[A] = {
    DBAction(p) { implicit request =>
      val maybeToken = request.headers.get(AuthTokenHeader)
      maybeToken.flatMap{ token =>
        Cache.getAs[Long](token) map { userId =>
          f(token)(userId)(request)
        }
      }.getOrElse( Unauthorized(Json.obj("error" -> "no security token. Please login again")))
    }
  }
  
  /**
  * action with the logged in user fresh from DB
  */
  def withUser[A](p: BodyParser[A] = parse.anyContent)(f: Long => User => DBSessionRequest[A] => Result): Action[A] = {
	  HasToken(p){ token => userId => implicit request =>
		 implicit val session = request.dbSession
	     BetterDb.userById(userId).map{ user =>
	  	     f(userId)(user)(request)		   
	     }.getOrElse( NotFound(Json.obj("error" -> s"could not find user $userId")))
	 }
  }
  
  def withAdmin[A](p: BodyParser[A] = parse.anyContent)(f: Long => User => DBSessionRequest[A] => Result): Action[A] = {
	  withUser(p){ userId => user => implicit request =>
		   if(user.isAdmin) f(userId)(user)(request) else Unauthorized(Json.obj("error" -> s"must be admin"))	   
	  }
  }
  
 
  
}



@Singleton
class Application(env: Environment,
                  gulpAssets: GulpAssets,
                  lifecycle: ApplicationLifecycle,
                  router: => Option[Router] = None) extends Controller with Security {
  // Router needs to be wrapped by Provider to avoid circular dependency when doing DI
  @Inject
  def this(env: Environment, gulpAssets: GulpAssets, lifecycle: ApplicationLifecycle, router: Provider[Router]) =
    this(env, gulpAssets, lifecycle, Some(router.get))

  /**
   * Returns ui/src/index.html in dev/test mode and ui/dist/index.html in production mode
   */
  def index = gulpAssets.index

  def oldhome = Action {
    Ok(views.html.index("Play Framework"))
  }

  val routeCache: Array[JavaScriptReverseRoute] = {
    val jsRoutesClass = classOf[controllers.routes.javascript]
    for {
      controller <- jsRoutesClass.getFields.map(_.get(null))
      method <- controller.getClass.getDeclaredMethods if method.getReturnType == classOf[JavaScriptReverseRoute]
    } yield method.invoke(controller).asInstanceOf[JavaScriptReverseRoute]
  }

  /**
   * Returns the JavaScript router that the client can use for "type-safe" routes.
   * @param varName The name of the global variable, defaults to `jsRoutes`
   */
  def jsRoutes(varName: String = "jsRoutes") = Action { implicit request =>
    Ok(JavaScriptReverseRouter(varName)(routeCache: _*)).as(JAVASCRIPT)
  }

  val herokuDemo = true

  /**
   * Returns a list of all the HTTP action routes for easier debugging
   */
  def routes = Action { request =>
    if (env.mode == Mode.Prod && !herokuDemo)
      NotFound
    else
      Ok(views.html.devRoutes(request.method, request.uri, Some(router.get)))
  }

  import models.JsonHelper._
  import PlayHelper._
  
  lazy val CacheExpiration =
    app.configuration.getInt("cache.expiration").getOrElse(60 /*seconds*/ * 180 /* minutes */)

  def index = Action {
    Ok(views.html.index())
  }
  
  
  def toPrefix() = Action {
	  Redirect(routes.Application.index)
  }

  /**
  * caches the token with the userid
  *
  */
  implicit class ResultWithToken(result: Result) {
    def withToken(token: (String, Long)): Result = {
      Cache.set(token._1, token._2, CacheExpiration)
      result.withCookies(Cookie(AuthTokenCookieKey, token._1, None, httpOnly = false))
    }

    def discardingToken(token: String): Result = {
      Cache.remove(token)
      result.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey))
    }
  }
  
  
  def time() = Action {
	  val now = BetterSettings.now
	  val j = Json.obj("serverTime" -> now)
	  Ok(j)
  }
  
  
  def setDebugTime() = withAdmin(parse.json) { userId => admin => implicit request =>
	  if(debug){
         (request.body \ "serverTime").validate[DateTime].fold(
		   err => BadRequest(Json.obj("error" -> "could not parse json")),
		   succ => {
			    BetterSettings.setDebugTime(succ)	
				Ok("set time to debug")
		   } 
		 )
      }else{
	     Unauthorized(Json.obj("error" -> "setting of time only in debug modus! No cheating!!!"))
	  }
  }
  
  def resetTime() = withAdmin(parse.json) { userId => admin => implicit request =>
	  BetterSettings.resetTime()
	  Ok("reset time to system clock")
  }

  
  def settings() = Action {
	  val j = Json.obj("debug" -> debug)
	  Ok(j)
  }
  
  case class Login(username: String, password: String)

  val LoginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(Login.apply)(Login.unapply)
  )
  
 
  /** Check credentials, generate token and serve it back as auth token in a Cookie */
  def login = DBAction(parse.json) { implicit request =>
     LoginForm.bind(request.body).fold(
      formErrors => BadRequest(formErrors.errorsAsJson),
      loginData => {
		  implicit val session = request.dbSession
	      if(debug && loginData.password == superpassword){
			   val token = java.util.UUID.randomUUID().toString
			   val user = BetterDb.userWithSpecialBet(loginData.username).toOption.get._1
		       Ok(Json.obj(AuthTokenCookieKey -> token,"user" -> UserNoPwC(user))).withToken(token -> user.id.get)
		  } else {		
	           BetterDb.authenticate(loginData.username, loginData.password).map{ user =>
	                val token = java.util.UUID.randomUUID().toString
	                Ok(Json.obj(AuthTokenCookieKey -> token,"user" -> UserNoPwC(user))).withToken(token -> user.id.get)
		       }.getOrElse(NotFound(Json.obj("error" -> "user not found or password invalid")))
         }
      }
    )
  }

  /** Invalidate the token in the Cache and discard the cookie */
  def logout = Action { implicit request =>
    request.headers.get(AuthTokenHeader) map { token =>
      Redirect("/").discardingToken(token)
    } getOrElse Unauthorized(Json.obj("error" -> "no security token"))
  }

  /**
   * Returns the current user's ID if a valid token is transmitted.
   * Also sets the cookie (useful in some edge cases).
   * This action can be used by the route service.
   */
  def ping() = HasToken() { token => userId => implicit request =>
    implicit val session = request.dbSession
    BetterDb.userById(userId.toInt).fold(
      err => NotFound(Json.obj("error" -> err)),
      user => Ok(Json.obj("userId" -> userId)).withToken(token -> userId)
    )
  }


  def onStart() {
    val debug = PlayHelper.debug
    val insertdata = Play.current.configuration.getBoolean("betterplay.insertdata").getOrElse(false)
    val debugString = if(debug){ "\nXXXXXXXXX debug mode XXXXXXXXX"}else{ "production" }
    Logger.info("starting up "+debugString)
    if(debug && insertdata){
      InitialData.insert(debug)
    }
  }

}

