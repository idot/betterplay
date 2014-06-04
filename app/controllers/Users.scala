package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsObject
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick.DBAction

import scalaz.{\/,-\/,\/-}

import models._
import models.JsonHelper._
import FormToV._	
import models.BetterSettings

trait Users extends Controller with Security {
  
  def all() = DBAction { implicit rs =>
      implicit val session = rs.dbSession
      val users = BetterDb.allUsers()
      val json =  Json.toJson(users.map(UserNoPwC(_)))
      Ok(json)
  } 
  
  def get(username: String) = DBAction { implicit rs =>
      implicit val session = rs.dbSession
      BetterDb.userWithSpecialBet(username).fold(
        err => NotFound(Json.obj("error" -> err)),
        succ => succ match { case(user, sp) =>
          val gamesWithBets = BetterDb.gamesWithBetForUser(user)
          val json = Json.obj("user" -> UserNoPwC(user), "specialBet" -> sp, "gameBets" -> gamesWithBets)
          Ok(json)
        }  
      )
  }
     
  def userWithEmail() = withUser(){ userId => user => implicit request =>
	  val nop = UserNoPwC(user)
      val jnop = Json.toJson(nop)  

	  val jnope = jnop.as[JsObject].deepMerge(Json.obj( "email" -> user.email ))
      Ok(jnope)	  
  }	  	 
	  
  /** 
  *
  **/
  case class UserCreate(username: String, password: String, email: String)
    val FormUserCreate = Form(
       mapping(
          "username" -> nonEmptyText(3,20),
          "password" -> nonEmptyText(6),
		  "email" -> email
       )(UserCreate.apply)(UserCreate.unapply)
   )
  
   def create(username: String) = withUser(parse.json){ userId => user => implicit request =>
       implicit val session = request.dbSession
       FormUserCreate.bind(request.body).map{ sub => 
             if(user.isAdmin){
			      val created = DomainHelper.userFromUPE(sub.username, sub.password, sub.email, user.id)
			      BetterDb.insertUser(created, false, false, user.id)		   
		     } else -\/("must be admin")
	   }.fold(
          err => Forbidden(Json.obj("error" -> err)),
          succ => Ok("created bets for users")      
       )
   }
   
   
   case class UserUpdateDetails(firstName: String, lastName: String, email: String, icontype: String)
   val FormUserUpdateDetails = Form(
      mapping(
         "firstName" -> text,
         "lastName" -> text,
	     "email" -> email,
		 "icontype" -> text
      )(UserUpdateDetails.apply)(UserUpdateDetails.unapply)
   )
   
   def updateDetails(username: String) = withUser(parse.json){ userId => user => implicit request =>
       implicit val session = request.dbSession
       FormUserUpdateDetails.bind(request.body).map{ sub => 
			   BetterDb.updateUserDetails(userId, sub.firstName, sub.lastName, sub.email, sub.icontype)		   
	   }.fold(
          err => Forbidden(Json.obj("error" -> err)),
          succ => Ok("updated user details")      
       )
   }
   
   def updatePassword(username: String) = withUser(parse.json){ userId => user => implicit request =>
       implicit val session = request.dbSession
       (request.body \ "password").validate[String].fold(
	      err => -\/("password not found"),
		  succ => {
 		      val encryptedPassword = DomainHelper.encrypt(succ)
 			  BetterDb.updateUserPassword(userId, encryptedPassword)		   	 
		  }
	   ).fold(
          err => Forbidden(Json.obj("error" -> err)),
          succ => Ok("updated user password")      
       )
   }
   
  
  /**
   * todo: should delegate to actor so that only one is active
   * 
   */
   def createBetsForUsers() = withUser(){ userId => user => implicit request =>
       implicit val session = request.dbSession
       if(user.isAdmin){ 
		   BetterDb.createBetsForGamesForAllUsers(user).fold(
			   err => Forbidden(Json.obj("error" -> err)),
			   succ => Ok("created bets for users")
		   )
	   } else Forbidden(Json.obj("error" -> "must be admin"))
   }

}

object Users extends Users