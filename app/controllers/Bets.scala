package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsError
import play.api.cache.CacheApi
import play.api.libs.json.JsObject
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.Future

import models._
import models.JsonHelper._
import FormToV._	

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class Bets @Inject()(override val betterDb: BetterDb, override val cache: CacheApi) extends Controller with Security {
  
    def update(id: Long) = withUser.async(parse.json) { request =>
  		request.body.validate[ViewableBet].fold(
  			err => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(err)))),
  			bet => {
   		      val now = BetterSettings.now
   		      val mtg = BetterSettings.closingMinutesToGame
  				  betterException{
  				   betterDb.updateBetResult(bet.toBet, request.user, now, mtg).map{
  				     succ => succ match {  case(game, betold, betnew, log, errs) =>
  				      val vtg = game.level.viewMinutesToGame
  					   //TODO: add broadcast succ is (game,betold, betnew)
                                       if(errs.length > 0){
                                              NotAcceptable(Json.obj("error" -> errs))       
                                        }else{
  				              Ok(Json.obj("game" -> game, "betold" -> betold.viewableBet(request.request.userId, game.game.serverStart, now, vtg), "betnew" -> betnew.viewableBet(request.request.userId, game.game.serverStart, now, vtg)))
                                         }
                                     }
  				 }}
  			}
		  )
    }	  	 
   
    def log() = withUser.async { request =>
      betterDb.allBetLogs().map{ log =>
        val now = BetterSettings.now
  			val vtg = 30
  			//TODO:BetterSettings.viewMinutesToGame
  			//join betLogs with games/levels map game.level.viewMinutesToGame => hide vtg
        val txt = log.map(l => l.toText(request.request.userId, l.gameStart, now, vtg)).mkString("\n") 
        Ok(txt)
      }
    }
	
 	 

}