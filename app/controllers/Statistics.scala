package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.cache.CacheApi
import models._
import models.JsonHelper._
import play.api.i18n.MessagesApi

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class Statistics @Inject()(override val betterDb: BetterDb, override val cache: CacheApi, configuration: Configuration) extends Controller with Security {

  val excelSecret = configuration.getString("betterplay.excelSecret").getOrElse("BAD")  
   
  def excel() = withUser { request =>
	   val helper = new StatsHelper(betterDb, BetterSettings.now(), request.request.userId)  
	   val templates = helper.specialBetsTemplates()
	   val gwts = helper.getGwts()
     val excelD = new ExcelData(helper.createUserRows, gwts, templates)
	   val excel = excelD.createExcelSheetComplete()
	   val mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	   val name = BetterSettings.fileName(excel, excelSecret)
	   val headers = ("Content-disposition",s"attachment; filename=$name")
	   Ok(excel).as(mime).withHeaders(headers)   
  }
   

  
 
}