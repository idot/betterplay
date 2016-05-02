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

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class Games @Inject()(override val betterDb: BetterDb, override val cache: CacheApi) extends Controller with Security {
  
  def all() = withUser.async { request =>
      betterDb.allGamesWithTeams().map{ teams =>  
         Ok(Json.toJson(teams)) 
      }
  }
    
  def get(gameNr: Int) = withUser.async { request =>
       betterDb.getGameByNr(gameNr).flatMap{ game => 
         betterException{
          betterDb.betsWitUsersForGame(game.game).map{ betsWithUsers =>
            val now = BetterSettings.now
  				  val vtg = game.level.viewMinutesToGame
            val vbWithUsers = betsWithUsers.map{ case(b,u) => (b.viewableBet(request.request.userId, game.game.serverStart, now, vtg), u) }
            val json = Json.obj("game" -> game, "betsUsers" -> vbWithUsers)
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
	         betterDb.updateGameResults(game, request.admin, BetterSettings.now, BetterSettings.closingMinutesToGame)
	             .flatMap{ case(g,gu) => betterDb.calculateAndUpdatePoints(request.admin)
	             .map{ succ => Ok("updated all points") }
	         }} 
	      }
  )}
 
  
  case class CreatedGame(serverStart: DateTime, localStart: DateTime, team1: String, team2: String, level: Int)
  implicit val createdGameFormat = Json.format[CreatedGame] 
	 
  def createGame() = withAdmin.async(parse.json){ request =>
      request.body.validate[CreatedGame].fold(
		    err =>  Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(err)))),
		  cg => {
		    betterException{
		     val game = Game(None, DomainHelper.gameResultInit, 0, 0, 0, cg.localStart, "unkown", cg.serverStart, "unknown", "unknown", "unknown", 0)
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
