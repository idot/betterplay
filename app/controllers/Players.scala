package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._

import models._
import models.JsonHelper._

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class Players @Inject()(override val betterDb: BetterDb) extends Controller with Security {
 /* 
  def all() = DBAction { implicit rs =>
      implicit val session = rs.dbSession
      val json = Json.toJson(BetterDb.allPlayersWithTeam())  
      Ok(json)
  }
  */
 
}