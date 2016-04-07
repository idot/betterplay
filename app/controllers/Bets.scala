package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsError

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
class Bets @Inject()(override val betterDb: BetterDb) extends Controller with Security {
  /*
    def update(id: Long) = withUser(parse.json){ userId => user => implicit request =>
  		request.body.validate[Bet].fold(
  			err => BadRequest(Json.obj("error" -> JsError.toFlatJson(err))),
  			bet => {
   		    val now = BetterSettings.now
  				  val mtg = BetterSettings.closingMinutesToGame
  				  betterDb.updateBetResult(bet, user, now, mtg).map{
  				   succ => succ match {  case(game,betold,betnew) =>
  					   //TODO: add broadcast succ is (game,betold, betnew)
  					   Ok(Json.obj("game" -> game, "betold" -> betold, "betnew" -> betnew))
  				   }}.recoverWith{ case ex: Exception => 
  				      UnprocessableEntity(Json.obj("error" -> ex.getMessage))
  				   }
  			}
		  )
    }	  	 
 */   
    def log() = Action.async { implicit rs =>
      betterDb.allBetLogs().map{ log =>
        val txt = log.map(_.toText).mkString("\n") 
        Ok(txt)
      }
    }
	
 	 

}