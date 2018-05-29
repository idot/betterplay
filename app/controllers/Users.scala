package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsError
import play.api.data._
import play.api.data.Forms._
import play.api.cache.SyncCacheApi
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import models._
import models.JsonHelper._
import models.BetterSettings

import javax.inject.{Inject, Provider, Singleton, Named}


import scala.concurrent.Future
import play.api.libs.json.JsSuccess
import play.api.libs.ws._
import scala.concurrent.duration._
import TimeHelper._


@Singleton
class Users @Inject()(cc: ControllerComponents, override val betterDb: BetterDb, override val cache: SyncCacheApi, override val messagesApi: MessagesApi,
                  ws: WSClient, configuration: Configuration, @Named("mailer") mailer: ActorRef) extends AbstractController(cc) with Security with I18nSupport {
    
  //TODO move to mail controller
  def sendUnsentMail() = withAdmin.async { request =>
       mailer ! SendUnsent()
       Future{ Ok(Json.obj("ok" -> "sending unsent mail")) }
  }
  
  def all() = Action.async { request =>
      betterDb.allUsersWithRank().map{ all => 
        val allNoPw = all.sortBy{ case(u,r) => (r, u.username.toLowerCase) }.map{ case(u,r) => UserNoPwC(u, None, r) }
        Ok(Json.toJson(allNoPw )) 
     }
  }
  

  def get(username: String) = withOptUser.async { request =>
     betterException{
      betterDb.userWithSpecialBets(username)
         .flatMap{ case(user, sp) =>
          betterDb.gamesWithBetForUser(user)
            .map{ gamesWithBets =>
              val now = BetterSettings.now
              val optUser = request.optUser
              val id = request.getIdOrN()
              val gamesWithVBets = gamesWithBets.map{ case(g, b) => 
                val vtg = g.game.viewMinutesToGame
                (g, b.viewableBet(id, g.game.serverStart, now, vtg)) 
              }
              val gwbvs = gamesWithVBets.sortBy{case (g,b) => g.game.serverStart }
              val json = Json.obj("user" -> UserNoPwC(user, optUser), "specialBets" -> sp, "gameBets" -> gwbvs)
              Ok(json)
          }
      }        
     }
  }
   
  /**
   * add e-mail manually to alwyas return userNoPwC
   */
  def userToUserNoPwC(user: User): JsObject = {
      val nop = UserNoPwC(user, Some(user))
      val jnop = Json.toJson(nop)  
	    val jnope = jnop.as[JsObject].deepMerge(Json.obj( "email" -> user.email ))
	    jnope
  }
  
  def userWithEmail() = withUser.async { request =>
	  val jnope = userToUserNoPwC(request.user)
    Future.successful(Ok(jnope))  
  }	  	 
	  
  /** 
  *
  **/
  case class UserCreate(username: String, firstname: String, lastname: String, email: String)
    val FormUserCreate = Form(
       mapping(
          "username" -> nonEmptyText(3,20),
          "firstname" -> nonEmptyText(3,50),
          "lastname" -> nonEmptyText(3,50),
   	      "email" -> email
       )(UserCreate.apply)(UserCreate.unapply)
   )
  
   def create() = withAdmin.async(parse.json) {implicit request =>
       FormUserCreate.bind(request.body).fold(
           err => Future.successful(UnprocessableEntity(Json.obj("error" -> err.errorsAsJson))),   
           succ =>  {
             betterException{
               val random = BetterSettings.randomToken()
               val created = DomainHelper.userFromUPE(succ.username, random, succ.firstname, succ.lastname, succ.email, request.admin.id)
               val token = BetterSettings.randomToken()
               for{
                 user <- betterDb.insertUser(created, false, false, Some(request.admin))
                 message = MailGenerator.createUserRegistrationMail(user, token, request.admin)
                 inserted <- betterDb.insertMessage(message, user.id.get, token, true, false)
                 mail <- mailer.ask(ImmediateMail(user))(new Timeout(Duration.create(BetterSettings.MAILTIMEOUT, "seconds"))).mapTo[String]
               } yield {
                 Ok(Json.obj("ok" -> s"created user ${user.username} $mail"))
               }
			       }
		       })
   }
   
   
   case class UserUpdateDetails(email: String, showname: Boolean, institute: String, icontype: String)
   val FormUserUpdateDetails = Form(
      mapping(
	       "email" -> email,
	       "showname" -> boolean,
	       "institute" -> text,
		     "icontype" -> text
      )(UserUpdateDetails.apply)(UserUpdateDetails.unapply)
   )
   
   def updateDetails() = withUser.async(parse.json){implicit request =>
       FormUserUpdateDetails.bind(request.body).fold(
           err => Future.successful(UnprocessableEntity(Json.obj("error" -> err.errorsAsJson))),
           succ => {
             betterException{
             betterDb.updateUserDetails(succ.email, succ.icontype, succ.showname, succ.institute, request.user)
               .map{ u =>
                 Ok(Json.obj("ok" -> s"updated ${u.username} details")) 
               }
           }}
       )
   }
   
   def updatePassword() = withUser.async(parse.json){ request =>
        (request.body \ "password").validate[String].fold(
	        err => Future.successful(UnprocessableEntity(Json.obj("error" -> "Password not found"))),
		     succ => {
		          betterException{
 		             val encryptedPassword = DomainHelper.encrypt(succ)
 			           betterDb.updateUserPassword(encryptedPassword, request.user).map{ r =>
 			           //TODO: send EMAIL to user
 		             Ok(Json.obj("ok" -> s"updated user password"))    
 		           }
		         }
		    }
	  )}
   
    def updateUserName() = withUser.async(parse.json){ request =>
       val un = (request.body \ "username").validate[String]
       val fn = (request.body \ "firstname").validate[String]
       val ln = (request.body \ "lastname").validate[String]
       (un, fn, ln) match {
         case (username: JsSuccess[String], firstName: JsSuccess[String], lastName: JsSuccess[String]) => {
            betterException{
              betterDb.updateUserName(username.value, firstName.value, lastName.value, request.user).map{ r =>
                Ok(Json.obj("ok" -> s"updated user name for user ${r.username}"))
              }
            }
         }
         case _ => Future.successful(UnprocessableEntity(Json.obj("error" -> "Could not parse id, firstname or lastname")))
       }
   }
   
   def updateFilter() = withUser.async(parse.json){ request =>
       request.body.validate[FilterSettings].fold(
         err => Future.successful(UnprocessableEntity(Json.obj("error" -> "Could not parse filter"))), //TODO: better error message   
         filterSettings => {
            betterException{
               betterDb.updateFilterSettings(filterSettings, request.user).map{ r =>
                  val jnope = userToUserNoPwC(r)
                  Ok(jnope)  
               }
            }
         }
       )
   }
   
  
   def updateCanBet() = withAdmin.async(parse.json){ request =>
       val unj = (request.body \ "username").validate[String]
       val canj = (request.body \ "canBet").validate[Boolean]
       (unj, canj) match {
         case (username: JsSuccess[String], canBet: JsSuccess[Boolean]) => {
                betterException{
                   betterDb.updateUserCanBet(username.value,  canBet.value, request.admin).map{ r =>
                      Ok(Json.obj("ok" -> s"updated filter"))
                   }
                }
               }
         case _ => Future.successful(UnprocessableEntity(Json.obj("error" -> "Could not find username or canBet")))
       }
   }
   
  
  /**
   * todo: should delegate to actor so that only one is active
   * 
   */
   def createBetsForUsers() = withAdmin.async{ request =>
      betterException{
	    betterDb.createBetsForGamesForAllUsers(request.admin)
	      .map{ succ =>  Ok(Json.obj("ok" -> s"created bets for users")) }
   }}

   
   
   /**** reCAPTCHA begin ****/
   def sendPasswordEmail(user: User): Future[Result] = {
       val token = BetterSettings.randomToken()
       val message = MailGenerator.createPasswordRequestMail(user, token)
       for{
          mail <- mailer.ask(ImmediateMail(user))(new Timeout(Duration.create(BetterSettings.MAILTIMEOUT, "seconds"))).mapTo[String]
          inserted <- betterDb.insertMessage(message, user.id.get, token, true, false)
       }yield{
          Ok(Json.obj("ok" -> s"sent new password request"))
       }
   }
   
   def checkCaptchaResponse(wsResponse: WSResponse, user: User): Future[Result] = {
       ( wsResponse.json \ "success" ).validate[Boolean].fold(
           err => Future.failed(ValidationException("could not show humaneness")),
           succ => if(succ){
                 sendPasswordEmail(user)       
           }else{
                ( wsResponse.json \ "error-codes" ).validate[Array[String]].fold(
                   err => Future.failed(ValidationException("could not check error codes")),
                   succ => Future.failed(ValidationException(s"error: ${succ.mkString(",")}"))
                )
           }
       )
   }
   
   def getUserAndVerify(secret: String, response: String, email: String): Future[Result] = {
      betterException {
         val url = "https://www.google.com/recaptcha/api/siteverify"   
         for{
           user <- betterDb.userByEmail(email)
           result <- ws.url(url)
                    .withQueryStringParameters(("secret", secret),("response", response))
                    .withRequestTimeout(10000.millis)
                    .withHttpHeaders("Accept" -> "application/json")
                    .post("")
           outcome <- checkCaptchaResponse(result, user)
         }yield(outcome)
       }  
   }
   
   /**
   *
   * https://developers.google.com/recaptcha/docs/verify#api-request
   * request to change password by recaptcha
   * 
   **/
   def changePasswordRequest() = Action.async(parse.json){ request =>
       val jemail =  (request.body \ "email").validate[String]
       val jresponse =  (request.body \ "response").validate[String]
       (jemail, jresponse) match {
         case (email: JsSuccess[String], response: JsSuccess[String]) =>
           configuration.getOptional[String]("betterplay.recaptchasecret")
               .fold(Future.successful(InternalServerError(Json.obj("error" -> "could not get recaptcha secret")))){ secret =>
                   getUserAndVerify(secret, response.value, email.value)
               }
         case _ => Future.successful(NotAcceptable(Json.obj("error" -> "could not parse recaptcha request")))
       }
   }
   /**** reCAPTCHA end ****/
   
       
  def mailPassword() = withAdmin.async(parse.json) { implicit request =>
      val jpass = (request.body \ "password").validate[String]
      jpass match {
        case (pass: JsSuccess[String]) => {
                      BetterSettings.setMailPassword(pass.value)
                      Logger.info(s"set mail password")
                      betterException {
                          mailer.ask(models.TestMail())(new Timeout(Duration.create(BetterSettings.MAILTIMEOUT, "seconds")))
                                  .mapTo[String].map{ result => Ok(Json.obj("ok" -> s"set mail password $result"))}
                      }
                  }
       case _ => {
           Future.successful(NotAcceptable(Json.obj("error" -> "could not parse mail password setting")))
       }
      }
   }
   
}

