package importer

import play.api._
import play.api.Logger
import play.api.db.slick._
import org.apache.commons.io.IOUtils
import models._
import au.com.bytecode.opencsv.CSVParser
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._
import java.time.OffsetDateTime
import play.api.Environment
import scala.collection.Seq

/**
 * DATA FROM:
 * https://github.com/openfootball/world-cup/tree/master/2014--brazil
 * look at scripts folder
 * https://raw.githubusercontent.com/datasets/country-codes/master/data/country-codes.csv 
 * for conversion from FIFA to 2 letter country; of course data is sh*t ENG missing
 **/




class InitialData(betterDb: BetterDb, environment: Environment) (implicit ec: ExecutionContext) {
  val logger: Logger = Logger(this.getClass()) 
  val csv = new CSVParser()
  
  val fifa2iso2 = {
	  val lines = toLines("country-codes.csv").drop(1)
	  //FIFA = 10
	 // ISO3166.1.Alpha.2 = 2
	 def FIFA2ISO(line: String): (String,String) = {
	     val items = csv.parseLine(line)
	     (items(10).toLowerCase,items(2).toLowerCase)
	 }
	  lines.map(FIFA2ISO).toMap
  }  
  
  def specialBets(): Seq[SpecialBetT] = {
	  val start = OffsetDateTime.of(2014, 6, 12, 22, 0, 0, 0, BetterSettings.offset())
	  val s = Seq(
	   SpecialBetT(None, "topscorer", "highest scoring player", 8 , start, "topscorer" , SpecialBetType.player, "",1),
		 SpecialBetT(None, "mvp", "most valuable player", 8 , start, "mvp" , SpecialBetType.player, "", 2),
     SpecialBetT(None, "champion", "champion", 10 , start, "champion" , SpecialBetType.team, "", 3 ),
		 SpecialBetT(None, "semifinalist", "semifinalist", 5 , start, "semifinalist" , SpecialBetType.team, "", 4 ),
		 SpecialBetT(None, "semifinalist", "semifinalist", 5 , start, "semifinalist" , SpecialBetType.team, "", 5 ),
		 SpecialBetT(None, "semifinalist", "semifinalist", 5 , start, "semifinalist" , SpecialBetType.team, "", 6 ),
		 SpecialBetT(None, "semifinalist", "semifinalist", 5 , start, "semifinalist" , SpecialBetType.team, "", 7 )
	  )
	  s
  }
  
  
  def parsePlayer(line: String): (Player,String) = {
    val items = line.split("\t")
	  val country = items(2)
	  val sortname = items(0)
 	  (Player(None, items(0), items(1), items(3), -1, DBImage("",""), sortname), country)  
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
  def parseDate(str: String, venue: String): (OffsetDateTime,OffsetDateTime) = {
      try{
      val short = str.substring(0, str.lastIndexOf("."))
      val dt = TimeHelper.fromString(short, "yyyy-MM-dd HH:mm:ss")
    
	     val shift = if(venue == "+") 1 else 0
      (dt,dt.plusHours(5+shift)) 
      }catch{
        case e: Exception => {
          logger.error("error on date: "+str+" "+venue+" "+e.getMessage)
          throw(e)
        }
      }
  }
  
  //play_at|pos|title|key|title|code|key|title|code
  //2014-06-12 17:00:00.000000|1|Group A|bra|Brazil|BRA|cro|Croatia|CRO
  
  //TODO: fix time zone parsing
  def parseGame(line: String, levelId: Long): (Team,Team, Game) = {
      logger.trace(line)
      val items = line.split("\\|")
      val venue = if(items.length == 10) items(9) else ""
      val (localStart, serverStart) = parseDate(items(0), venue)
      val pos = items(1).toInt
      val group = items(2)
	    val s3_1 = items(3)
	    val s3_2 = items(6)
	    val s2_1 = fifa2iso2.get(s3_1.toLowerCase).getOrElse("XX")
	    val s2_2 = fifa2iso2.get(s3_2.toLowerCase).getOrElse("XX")
      val t1 = Team(None, items(4), s3_1, s2_1)
      val t2 = Team(None, items(7), s3_2, s2_2)
      val g = Game(None, DomainHelper.gameResultInit(), 0, 0, levelId, localStart, "UNK", serverStart, "UNK", venue, group, pos, BetterSettings.viewMinutesToGame(), BetterSettings.closingMinutesToGame(), false, false)
      (t1,t2,g)
  }
  
  def teamsGames(levelId: Long): (Set[Team], Seq[(String,String,Game)]) = {
      logger.info("parsing games")
      val lines = toLines("GAMES2014.txt")    
      val ttg = lines.map(parseGame(_,levelId))
      val teams = ttg.map{case(t1,t2,g) => Seq(t1,t2)}.flatten.toSet
      val ssg = ttg.map{case(t1,t2,g) => (t1.name, t2.name, g)}
      (teams, ssg)     
  }
  
  //level   exact   tendency        nr #must be tab delimited
  //group   3       1       0
  def parseLevel(line: String): GameLevel = {
      val items = line.split("\t")
      GameLevel(None, items(0), items(1).toInt, items(2).toInt, items(3).toInt)
  }
  
  def levels(): Seq[GameLevel] = {
      logger.info("parsing levels")
      val lines = toLines("levels.tab")
      lines.map(parseLevel)    
  }
  
  def players(): Seq[(Player,String)] = {
      logger.info("parsing teams")
	  val lines = toLines("players.tab")
	  lines.map(parsePlayer)
  }
  
  def users(debug: Boolean): Seq[User] = {
      def uf(name: String, first: String, last: String, email: String, pw: String, admin: Boolean): User = {
          val encrypted = DomainHelper.encrypt(pw)
		      val (u,t) = DomainHelper.randomGravatarUrl(email)
          User(None, name, first, last, "", true, email, encrypted, admin, admin, true, admin, true, 0, 0, u, t, None, false, DomainHelper.filterSettings())
      }
	  if(debug){
         val admin = uf("admin", "admin" ,"admin", "admin@admin.com", "admin", true)
         val users = (1 to 10).map(n => uf(s"n$n", s"f$n", s"l$n", s"f${n}.l${n}@betting.com", "p", false))
         admin +: users
	  }else{
	      val names = Seq("anjae","thomasd", "andreas")
          names.map(n => uf(n, "","", "", n, true))		  		    
  	  }
  }
 
//  def updateChampion(){
//	     betterDb.specialbetsuser.filter(s => s.spId === 3l).map(_.prediction).update("Argentinia")  
 // }
  
  def insert(debug: Boolean): Unit = { 
    val ls = levels()
    val us = users(debug)
    val ps = players()
    val sp = specialBets()

    betterDb.dropCreate()

    logger.info("inserting data in db")
    Await.result(Future.sequence(sp.map(t => betterDb.insertSpecialBetInStore(t))), 1.seconds)
    logger.info("inserted special bets")
    Await.result(Future.sequence(us.map{ u => betterDb.insertUser(u, u.isAdmin, u.isRegistrant, None) } ), 1.seconds)
    logger.info("inserted users")
    val admin = Await.result(betterDb.allUsers(), 1.seconds).sortBy(_.id).head
    Await.result(Future.sequence(ls.map(l => betterDb.insertLevel(l, admin))), 1.seconds)  
    val level = Await.result(betterDb.allLevels(), 1.second)(0)
    val (teams, ttg) = teamsGames(level.id.get)
    Await.result(Future.sequence(teams.map(t => betterDb.insertTeam(t, admin))), 1.seconds)
    Await.result(Future.sequence(ttg.map{ case(t1,t2,g) => betterDb.insertGame(g, t1, t2, level.level, admin)}), 1.seconds)
    Await.result(betterDb.createBetsForGamesForAllUsers(admin), 1.seconds)
    Await.result(Future.sequence(ps.map{ case(p,t) => betterDb.insertPlayer(p, t, admin) }), 1.seconds)
	   
	  
    //TODO: updateChampion()

    logger.info("done inserting data in db")
    
    BetterSettings.setDebugTime(OffsetDateTime.of(2014, 3, 9, 10, 0, 0, 0, BetterSettings.offset()))
    
  }


}
