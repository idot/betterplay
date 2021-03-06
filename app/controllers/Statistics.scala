package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.cache.SyncCacheApi
import models._
import models.JsonHelper._
import play.api.i18n.MessagesApi
import scala.concurrent.{Future,blocking}

import javax.inject.{Inject, Provider, Singleton}


@Singleton
class Statistics @Inject()(cc: ControllerComponents, override val betterDb: BetterDb, override val cache: SyncCacheApi, configuration: Configuration) extends AbstractController(cc) with Security {
  val logger: Logger = Logger(this.getClass())
  val excelSecret = configuration.getOptional[String]("betterplay.excelSecret").getOrElse("BAD")  

   
  def createExcel(userId: Long): Future[Result] = {
   
    Future{
        blocking{
	        val excel = ExcelData.generateExcel(betterDb, BetterSettings.now(), userId)
	        val mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	        val name = CryptoHelper.fileName(excel, excelSecret)
	        val headers = ("Content-Disposition",s"attachment; filename=${name}")
	        Ok(excel).as(mime).withHeaders(headers)
        }
     }
  }
  
  def excel() = withUser.async { request =>
      createExcel(request.user.id.get)
  }
   
  def excelAnon() = Action.async { request =>
      createExcel(-1)
  }
  
  def game(id: Long) = Action.async { request =>
     betterException{
        betterDb.betsForGame(id).map{ result =>
          Ok(Json.toJson(result)) 
        }
     }
  }
  
  //semifinals all have same name
  def specialBets(name: String) = Action.async { request =>
     betterException {
        betterDb.specialBetsPredictions(name).map{ result =>
          result.headOption.map{ t =>
             val r = (t._1, result.unzip._2)
             Ok(Json.toJson(r))
          }.getOrElse(NotFound(s"could not find special bets $name"))
        }
     }
  }
  

  
  
  def uploadExcel = Action(parse.multipartFormData) { request => 
     logger.debug("receiving upload request ")
     request.body.file("file").map { xls =>
        import java.io.File
        val filename = xls.filename
        logger.debug(s"received upload file $filename")
        val contentType = xls.contentType
        val randomFolder = java.util.UUID.randomUUID().toString.replaceAll("-","")
        val outdir = s"/tmp/better/${randomFolder}"
        val created = new File(outdir).mkdirs()
        if(created == false){
          logger.error(s"could not create folder $outdir")
          InternalServerError("could not create folder")
        }else{
           val outf = new File(s"${outdir}/$filename")
           logger.debug(s"creating: $outf")
           xls.ref.moveTo(outf)
           val result = CryptoHelper.validate(outf.getAbsolutePath, filename, excelSecret).fold(
               err => NotAcceptable(err),
               succ => Ok("valid file")
           )
           outf.delete()
           new java.io.File(outdir).delete()
           result
        }
    }.getOrElse {
       NotAcceptable("file not found")
    }
  }
  
 
}
