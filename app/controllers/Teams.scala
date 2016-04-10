package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import models._
import models.JsonHelper._
import play.api.libs.json.JsError
import play.api.cache.CacheApi
import javax.inject.{Inject, Provider, Singleton}

@Singleton
class Teams @Inject()(override val betterDb: BetterDb, override val cache: CacheApi) extends Controller with Security {
  /*
  def all() = DBAction { implicit rs =>
      implicit val session = rs.dbSession
      val json = Json.toJson(BetterDb.allTeams())  
      Ok(json)
  }
   
  def get(name: String) = DBAction { implicit rs =>
//      implicit val session = rs.dbSession
//      BetterDb.getTeamByName(name).fold(e => NotFound(e), s => Ok(Json.toJson(s)) )
      Ok("")
  } 
  
  
  def insert(name: String) = DBAction(parse.json) { implicit rs =>
      rs.request.body.validate[Team].map{ team => 
          implicit val session = rs.dbSession
       //   val result = BetterDb.insertOrUpdateTeamByName(team)
      //    val jr = Json.obj(
      //  		 "result" -> result
      //    )
       //   Ok(jr)
          Ok("")
      }.recoverTotal{
          e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
      }
  }
  
*/
}
