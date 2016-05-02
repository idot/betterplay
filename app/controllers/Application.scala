package controllers


import play.api._
import play.api.db.DatabaseConfig
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import play.api.cache.CacheApi
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.data.Forms._
import play.api.data.Form
import models.DomainHelper
import models.BetterDb
import models.User
import models.UserNoPw
import models.UserNoPwC
import models.BetterSettings

import play.api.i18n.I18nSupport
import org.joda.time.DateTime
import javax.inject.{Inject, Provider, Singleton}
import play.api.mvc.{Action, Controller}
import play.api.routing.{JavaScriptReverseRoute, JavaScriptReverseRouter, Router}
import play.api.inject.ApplicationLifecycle
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.driver.JdbcProfile
import scala.concurrent.Future
import play.api.i18n.MessagesApi
import scala.concurrent.duration._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models.{AccessViolationException,ItemNotFoundException,ValidationException}

import com.github.mmizutani.playgulp.GulpAssets

object FormToV {
  import play.api.i18n.Messages.Implicits._
  import scalaz.{\/,-\/,\/-}
  implicit def toV[T](form: Form[T])(implicit messages: play.api.i18n.Messages): JsValue \/ T = {
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
class TokenRequest[A](val token: String, val userId: Long, request: Request[A]) extends WrappedRequest[A](request)
class UserRequest[A](val user: User, val request: TokenRequest[A]) extends WrappedRequest[A](request)
class AdminRequest[A](val admin: User, val request: TokenRequest[A]) extends WrappedRequest[A](request)

trait Security{ self: Controller =>
  val betterDb: BetterDb
  val cache: CacheApi
 
  val AuthTokenHeader = "X-AUTH-TOKEN"   //TODO: change to X-XSRF-TOKEN
  val AuthTokenCookieKey = "AUTH-TOKEN"   //TODO: change to XSRF-TOKEN
  val AuthTokenUrlKey = "auth"

  
  def hasToken[A] = new ActionRefiner[Request,TokenRequest] with ActionBuilder[TokenRequest]{
      def refine[A](input: Request[A]) = Future.successful{
          val maybeToken = input.headers.get(AuthTokenHeader)
          maybeToken.flatMap{ token =>
            cache.get[Long](token) map { userId => new TokenRequest(token, userId, input) }
     }.toRight(Unauthorized)
  }}
   
  def withUserA = new ActionRefiner[TokenRequest,UserRequest]{
      def refine[A](input: TokenRequest[A]) = {
         betterDb.userById(input.userId)
            .map{ user => Right(new UserRequest(user, input)) }
            .recoverWith{ case e: AccessViolationException => Future.successful(Left(Unauthorized(e.getMessage))) }
      }
  }
  
  def withAdminA = new ActionRefiner[TokenRequest,AdminRequest]{
      def refine[A](input: TokenRequest[A]) = {
         betterDb.userById(input.userId)
            .map{ user => if(user.isAdmin) Right(new AdminRequest(user, input)) else Left(Unauthorized("you must be admin")) } //todo: unauthorized
            .recoverWith{ case e: AccessViolationException => Future.successful(Left(Unauthorized(e.getMessage))) }
      }
  }
  
  def withUser = { hasToken andThen withUserA }
    
  def withAdmin = { hasToken andThen withAdminA }
 
  def betterException(future: Future[Result]): Future[Result] = {
      future.recoverWith{
        case e: AccessViolationException => Future.successful(Unauthorized(e.getMessage))
        case e: ItemNotFoundException => Future.successful(NotFound(e.getMessage))
        case e: ValidationException => Future.successful(NotAcceptable(e.getMessage))
      }
  }
  
  
}



@Singleton
class Application(env: Environment,
                  gulpAssets: GulpAssets,
                  lifecycle: ApplicationLifecycle,
                  dbConfigProvider: DatabaseConfigProvider,
                  override val betterDb: BetterDb,
                  val messagesApi: MessagesApi,
                  val cache: CacheApi,
                  configuration: Configuration,
                  router: => Option[Router] = None) extends Controller with Security with I18nSupport {

  // Router needs to be wrapped by Provider to avoid circular dependency when doing DI
  @Inject
  def this(env: Environment, gulpAssets: GulpAssets,
            lifecycle: ApplicationLifecycle,
            dbConfigProvider: DatabaseConfigProvider,
            betterDb: BetterDb,
            messagesApi: MessagesApi,
            cache: CacheApi,
            configuration: Configuration,
            router: Provider[Router]) =
    this(env, gulpAssets, lifecycle, 
           dbConfigProvider, betterDb, 
          messagesApi, cache, configuration, Some(router.get))


   val debug = configuration.getBoolean("betterplay.debug").getOrElse(false)  
   val superpassword = configuration.getString("betterplay.superpassword").getOrElse(java.util.UUID.randomUUID().toString)  
   val cacheExpiration = configuration.getInt("cache.expiration").getOrElse(60 /*seconds*/ * 180 /* minutes */)
   
  /**
   * Returns ui/src/index.html in dev/test mode and ui/dist/index.html in production mode
   */
  //def index = gulpAssets.index

  def index = gulpAssets.redirectRoot("/ui/")

  def toPrefix() = Action {
 	  //Redirect(routes.Application.index)
    Ok("TODO: redirect")
  }
  
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

  /**
   * Returns a list of all the HTTP action routes for easier debugging
   */
  def routes = Action { request =>
    if(env.mode == Mode.Prod){
      NotFound
    } else {
      Ok(views.html.devRoutes(request.method, request.uri, Some(router.get)))
    }
  }

  import models.JsonHelper._

  /**
  * caches the token with the userid
  *
  */
  implicit class ResultWithToken(result: Result) {
    def withToken(token: (String, Long)): Result = {
      cache.set(token._1, token._2, cacheExpiration minutes)
      result.withCookies(Cookie(AuthTokenCookieKey, token._1, None, httpOnly = false))
    }

    def discardingToken(token: String): Result = {
      cache.remove(token)
      result.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey))
    }
  }
 
  def time() = Action {
	  val now = BetterSettings.now
	  val j = Json.obj("serverTime" -> now)
	  Ok(j)
  }
  

  def setDebugTime() = withAdmin(parse.json) { request =>
	  if(debug){
         (request.body \ "serverTime").validate[DateTime].fold(
		   err => BadRequest(Json.obj("error" -> "could not parse json")),
		   succ => {
			    BetterSettings.setDebugTime(succ)	
				  Ok("set time to debug")
		   })
		}else{
	     Unauthorized(Json.obj("error" -> "setting of time only in debug modus! No cheating!!!"))
	  }
  }
  
  def resetTime() = withAdmin(parse.json) { request =>
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
  def login = Action.async(parse.json) { implicit request => 
     import play.api.i18n.Messages.Implicits._
    
     LoginForm.bind(request.body).fold(
      formErrors => Future.successful(BadRequest(formErrors.errorsAsJson)),
      loginData => {
	      if(debug && loginData.password == superpassword){
			   val token = java.util.UUID.randomUUID().toString
			   betterDb.userByName(loginData.username).map{ user => 
		       Ok(Json.obj(AuthTokenCookieKey -> token,"user" -> UserNoPwC(user))).withToken(token -> user.id.get)
			   }.recoverWith{ case ex: Exception =>
			       val error = s"user not found by name ${loginData.username}"
			       Logger.error(error+" "+ex.getMessage)
			       Future.successful(Unauthorized(Json.obj("error" -> error)))
			   }
		  } else {		
	           betterDb.authenticate(loginData.username, loginData.password).map{ user =>
	                val token = java.util.UUID.randomUUID().toString
	                Ok(Json.obj(AuthTokenCookieKey -> token,"user" -> UserNoPwC(user))).withToken(token -> user.id.get)
		       }.recoverWith{ case ex: Exception => Future.successful(NotFound(Json.obj("error" -> "user not found or password invalid")))}
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
  def ping = hasToken.async { tokenRequest => 
      val userId = tokenRequest.userId.toLong
      betterDb.userById(userId).map{ user =>
        Ok(Json.obj("userId" -> userId)).withToken(tokenRequest.token -> userId)
    } .recoverWith{ case ex: Exception => Future.successful(NotFound(Json.obj("error" -> ex.getMessage))) }
  }

  onStart()
  
  def onStart() {
    val insertdata = configuration.getBoolean("betterplay.insertdata").getOrElse(false)
    val debugString = if(debug){ "\nXXXXXXXXX debug mode XXXXXXXXX"}else{ "production" }
    Logger.info("starting up "+debugString)
    if(debug && insertdata){
      new InitialData(betterDb, env).insert(debug)
    }
  }

}

