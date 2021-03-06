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
import play.api.cache.SyncCacheApi
import play.api.i18n.MessagesApi
import akka.actor._

import models._
import models.JsonHelper._


import java.time.OffsetDateTime


import javax.inject.{Inject, Provider, Singleton, Named}

import scala.concurrent.Future



@Singleton
class Games @Inject()(cc: ControllerComponents, override val betterDb: BetterDb, override val cache: SyncCacheApi) extends AbstractController(cc) with Security {
  
  def all() = Action.async { request =>
      betterDb.allGamesWithTeams().map{ teams =>  
         Ok(Json.toJson(teams)) 
      }
  }
    
  def get(gameNr: Int) = withOptUser.async { request =>
       betterDb.getGameByNr(gameNr).flatMap{ game => 
         betterException{
          val id = request.getIdOrN()
          betterDb.betsWitUsersForGame(game.game).map{ betsWithUsers =>
            val now = BetterSettings.now()
  				  val vtg = game.game.viewMinutesToGame
            val vbWithUsers = betsWithUsers.map{ case(b,u) => (b.viewableBet(id, game.game.serverStart, now, vtg), UserNoPwC(u, request.optUser)) }
            val vbs = vbWithUsers.sortBy{ case(b,u) => u.username }
            val json = Json.obj("game" -> game, "betsUsers" -> vbs)
            Ok(json)
          } 
         }
      }
  }

 
  def submitResult() = withAdmin.async(parse.json){ request =>
	  request.body.validate[Game].fold(
		err => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(err)))),
	    game => {
	       betterException{
	         betterDb.updateGameResults(game, request.admin, BetterSettings.now(), BetterSettings.closingMinutesToGame())
	             .flatMap{ case(g,gu) => betterDb.calculateAndUpdatePoints(request.admin)
	             .map{ succ => Ok(Json.obj("message" -> "updated all points"))}
	         }} 
	      }
  )}
 

  
  case class CreatedGame(serverStart: OffsetDateTime, localStart: OffsetDateTime, team1: String, team2: String, level: Int)
  implicit val createdGameFormat = Json.format[CreatedGame] 
	 
  def createGame() = withAdmin.async(parse.json){ request =>
      request.body.validate[CreatedGame].fold(
		    err =>  Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(err)))),
		  cg => {
		    betterException{
		     val game = Game(None, DomainHelper.gameResultInit(), 0, 0, 0, cg.localStart, "unkown", cg.serverStart, "unknown", "unknown", "unknown", 0, BetterSettings.viewMinutesToGame(), BetterSettings.closingMinutesToGame(), false, false)
			       betterDb.insertGame(game, cg.team1, cg.team2, cg.level, request.admin)
			        .flatMap{ gwt => 
				           betterDb.createBetsForGamesForAllUsers(request.admin)
				             .map( succ => Ok(Json.obj("gwt" -> gwt)) )
			       }   
		    }
			}
	  )
  }
     

}
