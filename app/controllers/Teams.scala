package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import models._
import models.JsonHelper._
import play.api.libs.json.JsError
import play.api.cache.SyncCacheApi
import javax.inject.{Inject, Provider, Singleton}
import play.api.i18n.MessagesApi

import scala.concurrent.Future



@Singleton
class Teams @Inject()(cc: ControllerComponents, override val betterDb: BetterDb, override val cache: SyncCacheApi) extends AbstractController(cc) with Security {
  
  def all() = Action.async { request =>
      betterDb.allTeams().map{ all => Ok(Json.toJson(all)) }
  }
  
  
}
