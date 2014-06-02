package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsError

import play.api.libs.json.JsObject
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick.DBAction

import scalaz.{\/,-\/,\/-}

import models._
import models.JsonHelper._
import FormToV._	


object Bets extends Controller with Security {  
  
    def update(id: Long) = withUser(parse.json){ userId => user => implicit request =>
		request.body.validate[Bet].fold(
			err => BadRequest(Json.obj("error" -> JsError.toFlatJson(err))),
			bet => {
				implicit val session = request.dbSession
			    val now = BetterSettings.now
				val mtg = BetterSettings.closingMinutesToGame
				BetterDb.updateBetResult(bet, user, now, mtg).fold(
				   err => UnprocessableEntity(Json.obj("error" -> err)),
				   succ => {
					   //TODO: add broadcast succ is (game,betold, betnew)
					   Ok(s"updated bet: bet")
				   }
				)
			}
		)
		
		
  	    val nop = UserNoPwC(user)
        val jnop = Json.toJson(nop)  

  	    val jnope = jnop.as[JsObject].deepMerge(Json.obj( "email" -> user.email ))
        Ok(jnope)	  
    }	  	 
    
  

}