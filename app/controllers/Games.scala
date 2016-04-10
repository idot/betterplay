package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsError
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Akka
import play.api.cache.CacheApi

import scalaz.{\/,-\/,\/-}

import models._
import models.JsonHelper._
import FormToV._	

import org.joda.time.DateTime


import javax.inject.{Inject, Provider, Singleton}

@Singleton
class Games @Inject()(override val betterDb: BetterDb, override val cache: CacheApi) extends Controller with Security {
  /*
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
				  BetterDb.calculatePoints(admin).fold(
				     err => UnprocessableEntity(Json.obj("error" -> err)),
					 succ => Ok(s"updated all points")	  
				  )
			  }
		   )
		}
	  )
  }
  
  
  case class CreatedGame(serverStart: DateTime, localStart: DateTime, team1: String, team2: String, level: Int)
  implicit val createdGameFormat = Json.format[CreatedGame] 
	 
  def createGame() = withAdmin(parse.json){ userid => admin => implicit request => 
      request.body.validate[CreatedGame].fold(
		  err =>  BadRequest(Json.obj("error" -> JsError.toFlatJson(err))),
		  cg => {
		       implicit val session = request.dbSession
			   val game = Game(None, DomainHelper.gameResultInit, 0, 0, 0, cg.localStart, "unkown", cg.serverStart, "unknown", "unknown", "unknown", 0)
			   BetterDb.insertGame(game, cg.team1, cg.team2, cg.level, admin).toOption.flatMap{ gwt => 
				  BetterDb.createBetsForGamesForAllUsers(admin).toOption.map{ succ =>
					   Ok(Json.obj("gwt" -> gwt))
			      }   
			   }.getOrElse(UnprocessableEntity(Json.obj("error" -> "could not create game")))
		  }
	  )
  }
     
*/
}
