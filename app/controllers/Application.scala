package controllers

import play.api._
import play.api.mvc._
import play.api.cache.Cache
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.data.Forms._
import play.api.data.Form

/***
 * security based on 
 * http://www.jamesward.com/2013/05/13/securing-single-page-apps-and-rest-services
 * and especially
 * http://www.mariussoutier.com/blog/2013/07/14/272/
 */
trait Security { self: Controller =>
  case class U(id: Option[Long], name: String, password: String)
  val US = (1 to 10).map{id => (id*10,U(Some(id*10), s"name${id*10}", s"pw${id*10}"))}.toMap
  
  implicit val userFormat = Json.format[U]    
  implicit val app: play.api.Application = play.api.Play.current

  val AuthTokenHeader = "X-AUTH-TOKEN"
  val AuthTokenCookieKey = "AUTH-TOKEN"
  val AuthTokenUrlKey = "auth"

  /** Checks that a token is either in the header or in the query string */
  def HasToken[A](p: BodyParser[A] = parse.anyContent)(f: String => Long => Request[A] => Result): Action[A] =
    Action(p) { implicit request =>
      val maybeToken = request.headers.get(AuthTokenHeader).orElse(request.getQueryString(AuthTokenUrlKey))
      maybeToken flatMap { token =>
        Cache.getAs[Long](token) map { userid =>
          f(token)(userid)(request)
        }
      } getOrElse Unauthorized(Json.obj("err" -> "No Token"))
    }

}


trait Application extends Controller with Security {
  
  
  lazy val CacheExpiration =
    app.configuration.getInt("cache.expiration").getOrElse(60 /*seconds*/ * 2 /* minutes */)
  

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  
  case class Login(email: String, password: String)

  val loginForm = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText
    )(Login.apply)(Login.unapply)
  )

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
  
  
  
  def getUser(username: String, password: String): Option[U] = {
      US.values.find{ u => u.name==username && u.password == password }
  }
  
  /** Check credentials, generate token and serve it back as auth token in a Cookie */
  def login = Action(parse.json) { implicit request =>
    loginForm.bind(request.body).fold( // Bind JSON body to form values
      formErrors => BadRequest(Json.obj("err" -> formErrors.errorsAsJson)),
      loginData => {
        getUser(loginData.email, loginData.password) map { user =>
          val token = java.util.UUID.randomUUID().toString
          Ok(Json.obj(
            "authToken" -> token,
            "userId" -> user.id.get
          )).withToken(token -> user.id.get)
        } getOrElse NotFound(Json.obj("err" -> "User Not Found or Password Invalid"))
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
    US.get(userId.toInt) map { user =>
      Ok(Json.obj("userId" -> userId)).withToken(token -> userId)
    } getOrElse NotFound (Json.obj("err" -> "User Not Found"))
  }
  
  
}

object Application extends Application