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
  		request.body.validate[Bet].fold(
  			err => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(err)))),
  			bet => {
   		    val now = BetterSettings.now
  				  val mtg = BetterSettings.closingMinutesToGame
  				  betterException{
  				   betterDb.updateBetResult(bet, request.user, now, mtg).map{
  				     succ => succ match {  case(game,betold,betnew, log, errs) =>
  					   //TODO: add broadcast succ is (game,betold, betnew)
  					    Ok(Json.obj("game" -> game, "betold" -> betold, "betnew" -> betnew))
  				   }
  				 }}
  			}
		  )
    }	  	 
   
    def log() = withUser.async { request =>
      betterDb.allBetLogs().map{ log =>
        val txt = log.map(_.toText).mkString("\n") 
        Ok(txt)
      }
    }
	
 	 

}