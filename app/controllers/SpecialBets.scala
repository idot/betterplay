package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsObject
import models._
import models.JsonHelper._
import play.api.libs.json.JsError
import javax.inject.{Inject, Provider, Singleton}
import play.api.cache.CacheApi

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext


@Singleton
class SpecialBets @Inject()(override val betterDb: BetterDb, override val cache: CacheApi) extends Controller with Security {
  
  //TODO: for statistics!
  def all() = withUser.async { implicit rs =>
      Future.successful(Ok("") )
  }
   
  def specialBetsForUser(username: String) = withUser.async { request => 
	 	 betterDb.userWithSpecialBets(username).map{  case(u, tb) =>
         Ok(Json.obj("user" -> UserNoPwC(u), "templateBets" -> tb)) 
		 }
  }
  
  //nicer would be post to id resource. But I have all in the body
  //
  def updateSpecialBet() = withUser.async(parse.json) { request =>
	  request.body.validate[SpecialBetByUser].fold(
		  err => Future.successful(UnprocessableEntity(Json.obj("error" -> JsError.toFlatJson(err)))),
		  succ => {
		      val now = BetterSettings.now
			    val mtg = BetterSettings.closingMinutesToGame	  
			    betterDb.updateSpecialBetForUser(succ, now, mtg, request.user)
			      .map{ s => Ok(Json.toJson(s)) }
		  }
	  )
  }
  
  def specialBetsByTemplate(templateId: Long) = withUser.async(parse.json) { request =>
      betterDb.specialBetsByTemplate(templateId)
        .map{ case(t,bs) =>
			    Ok(Json.obj("template" -> t, "bets" -> bs))
        }
     
  }

}