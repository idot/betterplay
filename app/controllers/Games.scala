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


}