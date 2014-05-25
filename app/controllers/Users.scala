package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.db.slick.DBAction

//import models._
//import models.JsonHelper._

trait Users extends Controller with Security {
  
//  def all() = DBAction { implicit rs =>
//      implicit val session = rs.dbSession
//      val users = BetterDb.allUsers()
//      val json =  Json.toJson(users.map(UserNoPwC(_)))
//      Ok(json)
//  } 
    
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

}

object Users extends Users