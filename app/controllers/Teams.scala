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

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Singleton
class Teams @Inject()(override val betterDb: BetterDb, override val cache: CacheApi) extends Controller with Security {
  
  def all() = withUser.async { request =>
      betterDb.allTeams().map{ all => Ok(Json.toJson(all)) }
  }
  
  
}
