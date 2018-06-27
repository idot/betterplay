package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.JsError
import play.api.cache.SyncCacheApi
import play.api.libs.json.JsObject
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import scala.concurrent.duration._
import akka.actor._
import scala.concurrent.{Future,Await}
import play.api.i18n.I18nSupport
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout


import models._
import models.JsonHelper._


import javax.inject.{Inject, Provider, Singleton,Named}
import play.api.libs.json.JsSuccess


@Singleton
class Mail @Inject()(cc: ControllerComponents, override val betterDb: BetterDb, override val cache: SyncCacheApi, @Named("mailer") mailer: ActorRef) extends AbstractController(cc) with Security {
 
   //TODO move to database in 1 transaction
  
   def createMail() = withAdmin.async(parse.json) { request =>
       val vsubject = (request.body \ "subject").validate[String]
       val vbody = (request.body \ "body").validate[String]
       (vsubject, vbody) match {
         case (subject: JsSuccess[String], body: JsSuccess[String]) => {
            betterException{
                 val fs = betterDb.allUsers().flatMap{ users =>
                    val ums = users.map{ user => 
                       val message = MailGenerator.personalize(subject.value, body.value, user, request.admin.id.get)
                       betterDb.insertMessage(message, user.id.get, BetterSettings.randomToken, true, false)
                    }
                    Future.sequence(ums)
                 }
                 fs.map{ r => Ok("inserted messages") }
              }
         }
         case _ => Future.successful(UnprocessableEntity(Json.obj("error" -> "Could not parse id, firstname or lastname")))
       }
   }
    
   
 def testMail() = withAdmin.async { implicit request =>
      Logger.info(s"sending test email")
      betterException {
          mailer.ask(models.TestMail())(new Timeout(Duration.create(BetterSettings.MAILTIMEOUT, "seconds")))
                    .mapTo[String].map{ result => Ok(Json.obj("ok" -> s"sent test email $result"))}
      }
  }
   
   def mailPassword() = withAdmin.async(parse.json) { implicit request =>
      val jpass = (request.body \ "password").validate[String]
      jpass match {
        case (pass: JsSuccess[String]) => {
                      val settings = BetterSettings.getMailSettings()
                      BetterSettings.setMailSettings(settings.copy(password = pass.value))
                      Logger.info(s"set mail password")
                      betterException {
                          mailer.ask(models.TestMail())(new Timeout(Duration.create(BetterSettings.MAILTIMEOUT, "seconds")))
                                  .mapTo[String].map{ result => Ok(Json.obj("ok" -> s"set mail password $result"))}
                      }
                  }
       case _ => {
           Future.successful(NotAcceptable(Json.obj("error" -> "could not parse mail password setting")))
       }
      }
   }

  def sendUnsentMail() = withAdmin.async { request =>
       mailer ! SendUnsent()
       Future{ Ok(Json.obj("ok" -> "sending unsent mail")) }
  }
 
}
