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

import models._
import models.JsonHelper._


import javax.inject.{Inject, Provider, Singleton,Named}
import play.api.libs.json.JsSuccess


@Singleton
class Mail @Inject()(cc: ControllerComponents, override val betterDb: BetterDb, override val cache: SyncCacheApi) extends AbstractController(cc) with Security {
 
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
    

   
}
