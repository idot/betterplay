package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.db.slick.DBAction

import models._
import models.JsonHelper._

object Bets extends Controller {
  
  def all() = DBAction { implicit rs =>
      Ok("")      
        
  }
   
  

}