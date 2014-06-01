package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick.DBAction

import scalaz.{\/,-\/,\/-}

import models._
import models.JsonHelper._
import FormToV._	

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
    
//  /** Example for token protected access */
//  def myUserInfo() = HasToken() { _ => currentId => implicit request =>
//    US.get(currentId.toInt) map { user =>
//      Ok(Json.toJson(user))
//    } getOrElse NotFound (Json.obj("err" -> "User Not Found"))
//  }
//
//  /** Just an example of a composed function that checks privileges to access given user */
//  def CanEditUser[A](userId: Long, p: BodyParser[A] = parse.anyContent)(f: U => Request[A] => Result) =
//    HasToken(p) { _ => currentId => request =>
//      if (userId == currentId) { // Imagine role-based checks here
//        US.get(currentId.toInt) map { user =>
//          f(user)(request)
//        } getOrElse NotFound (Json.obj("err" -> "User Not Found"))
//      } else {
//        Forbidden (Json.obj("err" -> "You don't have sufficient privileges to access this user"))
//      }
//    }

//  def getUser(id: Long) = CanEditUser(id) { user => _ =>
//    Ok(Json.toJson(user))
//  }
  
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
  
   def update(username: String) = HasToken(parse.json){ token => userId => implicit request =>
       implicit val session = request.dbSession
       FormUserCreate.bind(request.body).map{ sub => 
		   BetterDb.userById(userId).flatMap{ user =>
             if(user.isAdmin){
			      val created = DomainHelper.userFromUPE(sub.username, sub.password, sub.email, user.id)
			      BetterDb.insertUser(created, false, false, user.id)		   
		     } else -\/("must be admin")
		  }
       }.fold(
          err => Forbidden(Json.obj("error" -> err)),
          succ => Ok("created bets for users")      
       )
   }
  
  
  /**
   * todo: should delegate to actor so that only one is active
   * 
   */
   def createBetsForUsers() = HasToken() { token => userId => implicit request =>
       implicit val session = request.dbSession
       BetterDb.userById(userId).flatMap{ user =>
          if(user.isAdmin) BetterDb.createBetsForGamesForAllUsers(user) else -\/("must be admin")
       }.fold(
          err => Forbidden(Json.obj("error" -> err)),
          succ => Ok("created bets for users")      
       )
   }

}

object Users extends Users