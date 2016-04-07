package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._

import models._
import models.JsonHelper._

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class Statistics @Inject()(override val betterDb: BetterDb) extends Controller with Security {

  val excelSecret = Play.current.configuration.getString("betterplay.excelSecret").getOrElse("BAD")  
  /*  
  def excel() = DBAction { implicit rs =>
       implicit val session = rs.dbSession
	   val helper = new StatsHelper()(session)     
	   val templates = BetterDb.specialBetTemplates()
	   val gwts = helper.getGwts()
       val excelD = new ExcelData(helper.createUserRows, gwts, templates)
	   val excel = excelD.createExcelSheetComplete()
	   val mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	   val name = BetterSettings.fileName(excel, excelSecret)
	   val headers = ("Content-disposition",s"attachment; filename=$name")
	   Ok(excel).as(mime).withHeaders(headers)   
  }
   
*/
  
 
}