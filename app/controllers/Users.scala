package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsError
import play.api.data._
import play.api.data.Forms._
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport
import akka.actor._

import models._
import models.JsonHelper._
import models.BetterSettings

import javax.inject.{Inject, Provider, Singleton, Named}


import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsSuccess
import play.api.libs.ws._
import scala.concurrent.duration._

@Singleton
class Users @Inject()(override val betterDb: BetterDb, override val cache: CacheApi, val messagesApi: MessagesApi,
                  ws: WSClient, configuration: Configuration, @Named("mailer") mailer: ActorRef) extends Controller with Security with I18nSupport {
    
  
  def all() = withUser.async { request =>
      betterDb.allUsersWithRank().map{ all => 
        val allNoPw = all.map{ case(u,r) => UserNoPwC(u, request.user, r) }
        Ok(Json.toJson(allNoPw )) 
     }
  }
   
  def get(username: String) = withUser.async { request =>
     betterException{
      betterDb.userWithSpecialBets(username)
         .flatMap{ case(user, sp) =>
          betterDb.gamesWithBetForUser(user)
            .map{ gamesWithBets =>
              val now = BetterSettings.now
              val gamesWithVBets = gamesWithBets.map{ case(g, b) => 
                val vtg = g.level.viewMinutesToGame
                (g, b.viewableBet(request.request.userId, g.game.serverStart, now, vtg)) 
              }
              val json = Json.obj("user" -> UserNoPwC(user, request.user), "specialBets" -> sp, "gameBets" -> gamesWithVBets)
              Ok(json)
          }
      }        
     }
  }
     
  def userToUserNoPwC(user: User): JsObject = {
      val nop = UserNoPwC(user, user)
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
  case class UserCreate(username: String, password: String, firstname: String, lastname: String, email: String)
    val FormUserCreate = Form(
       mapping(
          "username" -> nonEmptyText(3,20),
          "password" -> nonEmptyText(6),
          "firstname" -> nonEmptyText(3,50),
          "lastname" -> nonEmptyText(3,50),
   	      "email" -> email
       )(UserCreate.apply)(UserCreate.unapply)
   )
  
   def create() = withAdmin.async(parse.json) { request =>
       FormUserCreate.bind(request.body).fold(
           err => Future.successful(UnprocessableEntity(Json.obj("error" -> err.errorsAsJson))),   
           succ =>  {
             betterException{
               val created = DomainHelper.userFromUPE(succ.username, succ.password, succ.firstname, succ.lastname, succ.email, request.admin.id)
               val token = BetterSettings.randomToken()
               for{
                 user <- betterDb.insertUser(created, false, false, Some(request.admin))
                 message = MailGenerator.createUserRegistrationMail(user, token, request.admin)
                 inserted <- betterDb.insertMessage(message, user.id.get, token, true, false)
               } yield {
                 Ok(s"created user ${user.username}")
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
   
   def updateDetails() = withUser.async(parse.json){ request =>
       FormUserUpdateDetails.bind(request.body).fold(
           err => Future.successful(UnprocessableEntity(Json.obj("error" -> err.errorsAsJson))),
           succ => {
             betterException{
             betterDb.updateUserDetails(succ.email, succ.icontype, succ.showname, succ.institute, request.user)
               .map{ u =>
                 Ok("updated user details")     
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
 			           //TODO: EMAIL
 		             Ok("updated user password")      
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
                Ok("updated user name for user ${r.username}")
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
                      Ok("updated filter")
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
	      .map{ succ =>  Ok("created bets for users") }
   }}

   def updateUserHadInstructions() = withUser.async { request =>
       betterException{
       betterDb.updateUserHadInstructions(request.user)
         .map{ succ => Ok("user knows instructions")
       }}
   }
   
   /**
   *
   * https://developers.google.com/recaptcha/docs/verify#api-request
   *
   **/
   def verifyRecaptcha() = Action.async(parse.json){ request =>
       val url = "https://www.google.com/recaptcha/api/siteverify"
       val jemail =  (request.body \ "email").validate[String]
       val jresponse =  (request.body \ "response").validate[String]
       (jemail, jresponse) match {
         case (email: JsSuccess[String], response: JsSuccess[String]) =>
           val postRequest = ws.url(url)
           val secret = configuration.getString("recaptchasecred")
           val pdata = Json.obj(
                  "secret" -> secret,
                  "response" -> response.value
           )
           
//           postRequest.withHeaders("Accept" -> "application/json")
//              .withRequestTimeout(10000.millis).post(pdata).map{ r =>
//                 ( r.json \ "success" ).validate[Boolean].fold(
//                     err => ValidationException("could not show humaneness"),
//                     succ => if(succ){
//                       //TODO: SEND EMAIL
//                     }else{
//                       //DENY
//                     }
//              }
//                  
//           )
         case _ => 
       }
       Future.successful(Ok("hi"))
     
   }
   
   
}

