package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.cache.SyncCacheApi
import models._
import models.JsonHelper._

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class Players @Inject()(cc: ControllerComponents, override val betterDb: BetterDb, override val cache: SyncCacheApi) extends AbstractController(cc) with Security {
 
  def all() = withUser.async { request =>
      betterDb.allPlayersWithTeam().map{ all => Ok(Json.toJson(all)) }
  }

  
}