package controllers

import play.api._
import play.api.Play.current
import play.api.Logger
import play.api.db.slick._
import org.apache.commons.io.IOUtils
import models._
import au.com.bytecode.opencsv.CSVParser
import scala.concurrent.{Future,Await}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.Singleton
import org.joda.time.DateTime
import play.api.Environment

/**
 * DATA FROM:
 * https://github.com/openfootball/world-cup/tree/master/2014--brazil
 * look at scripts folder
 * https://raw.githubusercontent.com/datasets/country-codes/master/data/country-codes.csv 
 * for conversion from FIFA to 2 letter country; of course data is sh*t ENG missing
 * 
 * This data gets inserted in debug mode for testing
 * 
 **/


class Euro2016Data(betterDb: BetterDb, environment: Environment) {
  import org.joda.time.format.DateTimeFormat
  import org.joda.time.DateTime
  
  val csv = new CSVParser()
  
  def parsePlayer(line: String): (Player,String) = {
      val items = line.split("\t")
	  val country = items(2)
 	  (Player(None, items(0), items(1), items(3), -1, DBImage("","")), country)  
  }  
    
 
  def toLines(file: String): Seq[String] = {
     val is = environment.classLoader.getResourceAsStream("data/"+file)
     val string = IOUtils.toString(is, "UTF-8")
     val li = string.split("\n").drop(1)
     is.close
     li     
  }
  
  //2014-06-12 17:00:00.000000
  //TODO: time difference is 5 hours or 6 hours  in Manaus und Cuiab� !!!!
  def parseDate(str: String, venue: String): (DateTime,DateTime) = {
      try{
      val short = str.substring(0, str.lastIndexOf("."))
      val df = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
      val dt = df.parseDateTime(short)
	  val shift = if(venue == "+") 1 else 0
      (dt,dt.plusHours(5+shift)) 
      }catch{
        case e: Exception => {
          Logger.error("error on date: "+str+" "+venue+" "+e.getMessage)
          throw(e)
        }
      }
  }
  

  
  //level   exact   tendency        nr #must be tab delimited
  //group   3       1       0
  def parseLevel(line: String): GameLevel = {
      val items = line.split("\t")
      GameLevel(None, items(0), items(1).toInt, items(2).toInt, items(3).toInt, 30)
  }
  
  def levels(): Seq[GameLevel] = {
      Logger.info("parsing levels")
      val lines = toLines("levels.tab")
      lines.map(parseLevel)    
  }
  
  def players(): Seq[(Player,String)] = {
      Logger.info("parsing teams")
	  val lines = toLines("players.tab")
	  lines.map(parsePlayer)
  }
  
  def users(debug: Boolean): Seq[User] = {
      def uf(name: String, first: String, last: String, email: String, pw: String, admin: Boolean): User = {
          val encrypted = DomainHelper.encrypt(pw)
		  val (u,t) = DomainHelper.randomGravatarUrl(email)
          User(None, name, first, last, "", true, email, encrypted, admin, admin, admin, true, 0, 0, u, t, None, DomainHelper.filterSettings())
      }
	  if(debug){
         val admin = uf("admin", "admin" ,"admin", "admin@admin.com", "admin", true)
         val users = (1 to 10).map(n => uf(s"n$n", s"f$n", s"l$n", s"f${n}.l${n}@betting.com", "p$n", false))
         admin +: users
	  }else{
	      val names = Seq("anjae","thomasd", "andreas")
          names.map(n => uf(n, "","", "", n, true))		  		    
  	  }
  }
 
//  def updateChampion(){
//	     betterDb.specialbetsuser.filter(s => s.spId === 3l).map(_.prediction).update("Argentinia")  
 // }
  


}
