package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.db.slick.DBAction
import play.api.libs.json.JsObject
import models._
import models.JsonHelper._
import play.api.libs.json.JsError

object SpecialBets extends Controller with Security {
  
  def all() = DBAction { implicit rs =>
      Ok("") 
  }
   
  def specialBetsForUser(username: String) = DBAction { implicit rs =>
	  implicit val session = rs.dbSession
	  BetterDb.userWithSpecialBet(username).fold(
	     err => NotFound(Json.obj("error" -> err)),
		 succ => succ match { case(u, tb) =>
	         Ok(Json.obj("user" -> UserNoPwC(u), "templatebets" -> tb)) 
		 }
	  )
  }
  //nicer would be post to id resource. But I have all in the body
  //
  def updateSpecialBet() = withUser(parse.json) { userId => user => implicit request =>
	  request.body.validate[SpecialBetByUser].fold(
		  err => UnprocessableEntity(Json.obj("error" -> JsError.toFlatJson(err))),
		  succ => {
			  implicit val session = request.dbSession
		      val now = BetterSettings.now
			  val mtg = BetterSettings.closingMinutesToGame	  
			  BetterDb.updateSpecialBetForUser(succ, now, mtg, user).fold(
			      err => UnprocessableEntity(Json.obj("error" -> err)),
				  succ => Ok(Json.toJson(succ))	  
			  )
	      }
	  )
  }
  


}