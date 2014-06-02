package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsError
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick.DBAction
import play.api.libs.concurrent.Akka

import scalaz.{\/,-\/,\/-}

import models._
import models.JsonHelper._
import FormToV._	



object Games extends Controller with Security {
  
  def all() = DBAction { implicit rs =>
      implicit val session = rs.dbSession
      val json = Json.toJson(BetterDb.allGamesWithTeams())  
      Ok(json)
  }
    
  def get(gameNr: Int) = DBAction { implicit rs =>
      implicit val session = rs.dbSession
      BetterDb.getGameByNr(gameNr).fold(
        err => NotFound(Json.obj("err" -> err)),
        game => {
	      val betsWithUsers = BetterDb.betsWitUsersForGame(game.game)
          val json = Json.obj("game" -> game, "betsUsers" -> betsWithUsers)
          Ok(json)
        }  
      )
  }

 
  def submitResult() = withAdmin(parse.json){ userid => admin => implicit request =>
	  request.body.validate[Game].fold(
		err => BadRequest(Json.obj("error" -> JsError.toFlatJson(err))),
	    game => {
	       implicit val session = request.dbSession
	       BetterDb.updateGameResults(game, admin, BetterSettings.now, BetterSettings.closingMinutesToGame).fold(
		      err => UnprocessableEntity(Json.obj("error" -> err)),
			  success => {
			      WorkQueue(Akka.system).instance ! UpdatePoints(session) 
				  Ok(s"updated result for game $success")
			  }
		   )
		}
	  )
  }
     

}