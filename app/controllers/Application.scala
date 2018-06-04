package controllers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.Future

import play.api._
import play.api.db.DatabaseConfig
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._
import play.api.mvc.AnyContent

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.libs.json.{JsSuccess,JsError}
import play.api.data.Forms._
import play.api.data.Form
import play.api.cache.SyncCacheApi

import play.api.i18n.I18nSupport
import java.time.OffsetDateTime
import javax.inject.{Inject, Provider, Singleton, Named}
import play.api.mvc.Action
import play.api.routing.{JavaScriptReverseRoute, JavaScriptReverseRouter, Router}
import play.api.inject.ApplicationLifecycle
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}

import play.api.i18n.MessagesApi

import akka.pattern.AskTimeoutException
import akka.util.Timeout
import akka.actor._
import akka.pattern.ask

import models.DomainHelper
import models.BetterDb
import models.User
import models.UserNoPw
import models.UserNoPwC
import models.BetterSettings
import models.MailSettings

import models.{AccessViolationException,ItemNotFoundException,ValidationException}
import importer.InitialData
import importer.Euro2016Data



@Singleton
class Application(env: Environment,
                  lifecycle: ApplicationLifecycle,
                  dbConfigProvider: DatabaseConfigProvider,
                  cc: ControllerComponents,
                  override val betterDb: BetterDb,
                  override val messagesApi: MessagesApi,
                  val cache: SyncCacheApi,
                  configuration: Configuration,
                  system: ActorSystem,
          //        @Named("mailer") mailer: ActorRef,
                  router: => Option[Router] = None) extends AbstractController(cc) with Security with I18nSupport {

  // Router needs to be wrapped by Provider to avoid circular dependency when doing DI
  @Inject
  def this(env: Environment, 
            lifecycle: ApplicationLifecycle,
            dbConfigProvider: DatabaseConfigProvider,
            cc: ControllerComponents,
            betterDb: BetterDb,
            messagesApi: MessagesApi,
            cache: SyncCacheApi,
            configuration: Configuration,
       //     mailer: ActorRef,
            system: ActorSystem,
            router: Provider[Router]) =
    this(env, lifecycle, 
           dbConfigProvider,  cc, betterDb, 
          messagesApi, cache, configuration, system, Some(router.get))

   
   val debug = configuration.getOptional[Boolean]("betterplay.debug").getOrElse(false)  
   val superpassword = configuration.getOptional[String]("betterplay.superpassword").getOrElse(java.util.UUID.randomUUID().toString)  
   val cacheExpiration = configuration.getOptional[Int]("cache.expiration").getOrElse(60 /*seconds*/ * 180 /* minutes */)
   
  /**
   * Returns ui/src/index.html in dev/test mode and ui/dist/index.html in production mode
   */
  //def index = gulpAssets.index

  //def index = gulpAssets.redirectRoot("/em2016/")

  def toPrefix() = Action {
 	  //Redirect(routes.Application.index)
    Ok("TODO: redirect")
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
	  val j = Json.obj("serverTime" -> BetterSettings.now, "message" ->  "OK")
	  Ok(j)
  }
  

  def setDebugTime() = withAdmin(parse.json) { request =>
 	  if(debug){
       (request.body \ "serverTime").validate[OffsetDateTime].fold(
		     err => BadRequest(Json.obj("error" -> JsError.toJson(err))),
		     succ => {
			    BetterSettings.setDebugTime(succ)	
				  Ok(Json.obj("serverTime" -> BetterSettings.now, "message" -> "set time to debug"))
		   })
		}else{
	     Unauthorized(Json.obj("serverTime" -> BetterSettings.now, "message" -> "Error: setting of time only in debug modus! No cheating!!!"))
	  }
  }
  
  def resetTime() = withAdmin(parse.json) { request =>
	  BetterSettings.resetTime()
	  Ok(Json.obj("serverTime" -> BetterSettings.now, "message" -> "reset time to system clock"))
  }

  
  def settings() = Action.async {
    betterDb.startOfGames().map{ ot =>
      val start = ot.getOrElse( BetterSettings.now() )
      //default key no recaptcha
      val sitekey = configuration.getOptional[String]("betterplay.recaptchasite").getOrElse("6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI")
      val json = Json.obj("debug" -> debug, "gamesStarts" -> start, "recaptchasite" -> sitekey)
      Ok(json)
    } 
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
     LoginForm.bind(request.body).fold(
      formErrors => Future.successful(BadRequest(formErrors.errorsAsJson)),
      loginData => {
	      loginUser(loginData)
      }
    )
  }

  def loginUser(loginData: Login) = {
    if (loginData.password == superpassword) {
      betterDb.userByName(loginData.username).map { user =>
        val token = java.util.UUID.randomUUID().toString
        securityLogger.trace(s"login succesful ${user.username} $token using superpassword")
        Ok(Json.obj(AuthTokenCookieKey -> token, "user" -> UserNoPwC(user, Some(user)))).withToken(token -> user.id.get)
      }.recoverWith {
        case ex: Exception =>
          val error = s"user not found by name ${loginData.username}"
          securityLogger.trace(error + " " + ex.getMessage)
          Future.successful(Unauthorized(Json.obj("error" -> error)))
      }
    } else {
      betterDb.authenticate(loginData.username, loginData.password).map { user =>
        val token = java.util.UUID.randomUUID().toString
        securityLogger.trace(s"login succesful ${user.username} $token")
        Ok(Json.obj(AuthTokenCookieKey -> token, "user" -> UserNoPwC(user, Some(user)))).withToken(token -> user.id.get)
      }.recoverWith {
        case ex: Exception =>
          securityLogger.trace(s"could not find user in db or password invalid requested: ${loginData.username}")
          Future.successful(NotFound(Json.obj("error" -> "user not found or password invalid")))
      }
    }

  }
  
  def userByTokenPassword() = Action.async(parse.json) { request =>
      val tokenJ = (request.body \ "token").validate[String]
      val passwordJ = (request.body \ "password").validate[String]
      (tokenJ, passwordJ) match {
        case (token: JsSuccess[String], password: JsSuccess[String]) if token.value.length == BetterSettings.TOKENLENGTH && password.value.length >= 6 => {
           betterException {
               betterDb.userByTokenPassword(token.value, BetterSettings.now(), DomainHelper.encrypt(password.value)).flatMap{ user =>  
                 loginUser(Login(user.username, password.value)) 
               }
           }
        }
        case _ => {
             Future.successful(UnprocessableEntity(Json.obj("error" -> "Could not parse token or password")))
        }
     }  
  }
  
  
  /** Invalidate the token in the Cache and discard the cookie */
  def logout = Action { implicit request =>
    request.headers.get(AuthTokenHeader) map { token =>
              Logger.debug(s"logout called $token")
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
      securityLogger.trace(s"ping $userId")
      betterDb.userById(userId).map{ user =>
          securityLogger.trace(s"ping $userId found user ${user.username}")
         Ok(Json.obj("userId" -> userId)).withToken(tokenRequest.token -> userId)
      } .recoverWith{ case ex: Exception =>
         securityLogger.trace(s"ping $userId could not find user in db")
         Future.successful(NotFound(Json.obj("error" -> ex.getMessage))) 
      }
  }

  onStart()
  
  def onStart() {
    val insertdata = configuration.getOptional[String]("betterplay.insertdata").getOrElse("")
    val debugString = if(debug){ "\nXXXXXXXXX debug mode XXXXXXXXX"}else{ "production" }
    Logger.info(s"starting up: $debugString")
    val mailSettings = MailSettings.fromConfig(configuration)
    BetterSettings.setMailSettings(mailSettings)
    Logger.info(mailSettings.toString)
    if(debug){
      insertdata match {
        case "test" => new InitialData(betterDb, env).insert(debug)
        case "euro2016" => new Euro2016Data(betterDb, env).insert(false)
        case _ => Logger.info("not inserting any data!!!")//do nothing
      }
    }
    scheduleTasks()
  }
  
  
  
  def scheduleTasks(){
      scheduleGamesMaintenance()
  }
  
  def scheduleGamesMaintenance(){
    val maintenenceInterval = configuration.getOptional[Int]("betterplay.gamemaintenance.interval").getOrElse(0) 
    if(maintenenceInterval > 0){
      Logger.info(s"maintaining games each $maintenenceInterval minutes")
      class Maintain extends Runnable {
         def run {
            betterDb.maintainGames(BetterSettings.now)
         }
      }
      system.scheduler.schedule( 1 minutes, maintenenceInterval minutes, new Maintain()) 
    }else{
      Logger.info(s"not maintaining games")
    }
  }
  

}

