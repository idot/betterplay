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
import scalaz.{\/,-\/,\/-}

import models._
import models.JsonHelper._
import FormToV._	
import models.BetterSettings

import javax.inject.{Inject, Provider, Singleton}


import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class Users @Inject()(override val betterDb: BetterDb, override val cache: CacheApi) extends Controller with Security {
 
  def all() = withUser.async { request =>
      betterDb.allUsersWithRank().map{ all => 
        val allNoPw = all.map{ case(u,r) => UserNoPwC(u, r) }
        Ok(Json.toJson(allNoPw )) 
      }
  }
 
  
  def get(username: String) = withUser.async { request =>
     betterException{
      betterDb.userWithSpecialBets(username)
         .flatMap{ case(user, sp) =>
          betterDb.gamesWithBetForUser(user)
            .map{ gamesWithBets =>
              val json = Json.obj("user" -> UserNoPwC(user), "specialBets" -> sp, "gameBets" -> gamesWithBets)
              Ok(json)
          }
      }        
     }
  }
     
  def userWithEmail() = withUser.async { request =>
	  val nop = UserNoPwC(request.user)
    val jnop = Json.toJson(nop)  

	  val jnope = jnop.as[JsObject].deepMerge(Json.obj( "email" -> request.user.email ))
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
  
   def create(username: String) = withAdmin.async(parse.json) { request =>
       FormUserCreate.bind(request.body).fold(
           err => Future.successful(UnprocessableEntity(Json.obj("error" -> "TODO!"))),   //JsError.toFlatJson(err)))),
           succ =>  {
             betterException{
             val created = DomainHelper.userFromUPE(succ.username, succ.password, succ.firstname, succ.lastname, succ.email, request.admin.id)
             betterDb.insertUser(created, false, false, Some(request.admin)).map{ r =>
			         Ok(s"created bets for user $username")
			       }}
		   })
   }
   
   
   case class UserUpdateDetails(firstName: String, lastName: String, email: String, showname: Boolean, institute: String, icontype: String)
   val FormUserUpdateDetails = Form(
      mapping(
         "firstName" -> text,
         "lastName" -> text,
	       "email" -> email,
	       "showname" -> boolean,
	       "institute" -> text,
		     "icontype" -> text
      )(UserUpdateDetails.apply)(UserUpdateDetails.unapply)
   )
   
   def updateDetails(username: String) = withUser.async(parse.json){ request =>
       FormUserUpdateDetails.bind(request.body).fold(
           err => Future.successful(UnprocessableEntity(Json.obj("error" -> "TODO!"))),
           succ => {
             betterException{
             betterDb.updateUserDetails(request.user.id.get, succ.firstName, succ.lastName, succ.email, succ.icontype, succ.showname, succ.institute, request.user)
               .map{ u =>
                 Ok("updated user details")     
               }
           }}
       )
   }
   
   
   
   def updatePassword(username: String) = withUser.async(parse.json){ request =>
        (request.body \ "password").validate[String].fold(
	        err => Future.successful(UnprocessableEntity(Json.obj("error" -> "Password not found"))),
		     succ => {
		          betterException{
 		           val encryptedPassword = DomainHelper.encrypt(succ)
 			         betterDb.updateUserPassword(request.user.id.get, encryptedPassword, request.user).map{ r =>
 		             Ok("updated user password")      
 		           }
		          }
		    }
	   )}
   
  
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
       betterDb.updateUserHadInstructions(request.user.id.get, request.user)
         .map{ succ => Ok("user knows instructions")
       }}
   }
   
  
   
}

