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
import play.api.cache.SyncCacheApi
import play.api.i18n.MessagesApi

import scala.concurrent.Future


@Singleton
class SpecialBets @Inject()(cc: ControllerComponents, override val betterDb: BetterDb, override val cache: SyncCacheApi) extends AbstractController(cc) with Security {
  

  def all() = withUser.async { implicit rs =>
      betterDb.allSpecialBetTemplates().map{ r=>
        Ok(Json.toJson(r))
      }
  }
   
  def specialBetsForUser(username: String) = withUser.async { request => 
      betterException{
	     	betterDb.userWithSpecialBets(username).map{  case(u, tb) =>
           Ok(Json.obj("user" -> UserNoPwC(u, request.user), "templateBets" -> tb)) 
		   }
     }
  }
  
  //nicer would be post to id resource. But I have all in the body
  //
  def updateSpecialBet() = withUser.async(parse.json) { request =>
	  request.body.validate[SpecialBetByUser].fold(
		  err => Future.successful(UnprocessableEntity(Json.obj("error" -> JsError.toJson(err)))),
		  succ => {
		      val now = BetterSettings.now
			    val mtg = BetterSettings.closingMinutesToGame	  
			    betterException{
			      betterDb.updateSpecialBetForUser(succ, now, mtg, request.user)
			         .map{ s => Ok(Json.toJson(s)) }
		      }
		  }
	  )
  }
  
  def resultSpecialBet() = withAdmin.async(parse.json) { request =>
	  request.body.validate[SpecialBetByUser].fold(
		  err => Future.successful(UnprocessableEntity(Json.obj("error" -> JsError.toJson(err)))),
		  succ => {
				    betterException{
			      betterDb.setSpecialBetResult(succ.specialbetId, succ.prediction, request.admin)
			         .map{ s => Ok(Json.toJson(s)) }
		      }
		  }
	  )
  }
  
  
  def specialBetsByTemplate(templateId: Long) = withUser.async(parse.json) { request =>
      betterException{
        betterDb.specialBetsByTemplate(templateId)
           .map{ case(t,bs) =>
			       Ok(Json.obj("template" -> t, "bets" -> bs))
        }
      }
  }
  
  

}

