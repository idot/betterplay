package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.db.slick.DBAction

import models._

object Teams extends Controller {
  
//  def all() = DBAction { implicit rs =>
//            
//        
//  }
   def t(){
     import scalaz._
     import Scalaz._
     
     val v = "a".success
     v.toValidationNel
     def x(): String \/ Throwable = {
        -\/("a")
     }
     
     case class Truck(b: String)
     case class Car(a: Int, b: String)
     val d: String\/Truck = -\/("a")
     val c: String\/Car = \/-(Car(1,"a"))
     
    val res = (d.validation.toValidationNel |@| c.validation.toValidationNel){//{ _ + _ }
        case (v1, v2) => (v1, v2)
     }
     
     res.fold(
        f => f.toList.mkString("\n"),    
       r => r
     )
     
     
   }
    
  

}