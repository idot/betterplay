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
import models.UserNoPw
import models.UserNoPwC
import play.api.db.slick.DBSessionRequest

import org.joda.time.DateTime


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
 */
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
        Cache.getAs[Long](token) map { userid =>
          f(token)(userid)(request)
        }
      }.getOrElse( Unauthorized(Json.obj("err" -> "No Token")) )
    }
  }
}
  



trait Application extends Controller with Security {
  import models.JsonHelper._
  
  lazy val CacheExpiration =
    app.configuration.getInt("cache.expiration").getOrElse(60 /*seconds*/ * 2 /* minutes */)

  def index = Action {
    Ok(views.html.index())
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
	  val now = new DateTime()
	  val j = Json.obj("serverTime" -> now)
	  Ok(j)
  }
  
  def settings() = Action {
	  val debug = Play.current.configuration.getBoolean("betterplay.debug").getOrElse(false)
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
      formErrors => BadRequest(Json.obj("err" -> formErrors.errorsAsJson)),
      loginData => {
        implicit val session = request.dbSession
        BetterDb.authenticate(loginData.username, loginData.password).map{ user =>
          val token = java.util.UUID.randomUUID().toString
          Ok(Json.obj(
            AuthTokenCookieKey -> token,
            "user" -> UserNoPwC(user)
          )).withToken(token -> user.id.get)
        }.getOrElse(NotFound(Json.obj("err" -> "user not found or password invalid")))
      }
    )
  }

  /** Invalidate the token in the Cache and discard the cookie */
  def logout = Action { implicit request =>
    request.headers.get(AuthTokenHeader) map { token =>
      Redirect("/").discardingToken(token)
    } getOrElse BadRequest(Json.obj("err" -> "No Token"))
  }

  /**
   * Returns the current user's ID if a valid token is transmitted.
   * Also sets the cookie (useful in some edge cases).
   * This action can be used by the route service.
   */
  def ping() = HasToken() { token => userId => implicit request =>
    implicit val session = request.dbSession
    BetterDb.userById(userId.toInt).fold(
      err => NotFound(Json.obj("err" -> err)),
      user => Ok(Json.obj("userId" -> userId)).withToken(token -> userId)
    )
  }
  

}

object Application extends Application