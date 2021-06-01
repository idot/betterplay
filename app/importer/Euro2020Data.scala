package importer

import play.api._
import play.api.Logger
import play.api.db.slick._
import models._
import au.com.bytecode.opencsv.CSVParser
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._
import java.time.OffsetDateTime
import play.api.Environment
import scala.collection.Seq

/**
 * DATA FROM:
 * https://raw.githubusercontent.com/openfootball/euro/master/2020--europe/euro.txt
 * look at scripts folder
 * https://raw.githubusercontent.com/datasets/country-codes/master/data/country-codes.csv 
 * for conversion from FIFA to 2 letter country; of course data is sh*t ENG missing
 * 
 * This data gets inserted in debug mode for testing
 * 
 **/


class Euro2020Data(betterDb: BetterDb, environment: Environment) (implicit ec: ExecutionContext) {
  val logger: Logger = Logger(this.getClass())
  val long2cc = InitialDataX.longNameToCC(environment)
  
  val csv = new CSVParser()
  
  def parsePlayer(line: String): (Player,String) = {
      val items = line.split("\t")
      val name = items(0)
      val role = items(1)
      val team = items(2)
      val club = items(3)	    
	    val sortname = name
 	    (Player(None, name, role, club, -1, DBImage("",""), sortname), team)
  }
    
     
   def teamsGames(levelId: Long): (Set[Team], Seq[(String,String,Game)]) = {
      logger.info("parsing games")
      val lines = InitialDataX.toLines("uefa-euro-2020-CentralEuropeStandardTime.csv", environment)    
      val ttg = lines.zipWithIndex.map{ case(line,index) => parseGame(line,levelId, index) }
      val teams = ttg.map{case(t1,t2,g) => Seq(t1,t2)}.flatten.toSet
      val ssg = ttg.map{case(t1,t2,g) => (t1.name, t2.name, g)}
      (teams, ssg)     
  }
   
   //Match Number,Round Number,Date,Location,Home Team,Away Team,Group,Result
   def parseGame(line: String, levelId: Long, pos: Int): (Team,Team, Game) = {
      logger.trace(line)
      val items = line.split("\\,")
      val venue = items(3)
      val serverStart = parseDate(items(2))
      val t1n = items(4)
      val t2n = items(5)
      val group = items(6).reverseIterator.next().toString()
      def toTeam(long: String): Team = {
         long2cc.get(long).map{ case(two,three) =>
            Team(None, long, three.toLowerCase(), two.toLowerCase())
         }.getOrElse{
           val minimap = Map(
               "Wales" -> Team(None, long, "gb-wls", "gb-wls"),
               "England" -> Team(None, long, "gb-eng", "gb-eng"),
               "Russia" -> Team(None, long, "ru", "ru"), //TODO: is wrong
               "Northern Ireland" -> Team(None, long, "gb-nir", "gb-nir"),
               "Republic of Ireland" -> Team(None, long, "ir", "ir"),
               "North Macedonia" -> Team(None, long, "mk", "mk"),
               "Scotland" -> Team(None, long, "gb-sct","gb-sct")
               )
           minimap.get(long).getOrElse(throw new RuntimeException(s"could not find team in map: $t1n x $t2n x $group :"+long+"\n"+line))
         }
      }
      val t1 = toTeam(t1n)
      val t2 = toTeam(t2n)
      val g = Game(None, DomainHelper.gameResultInit(), 0, 0, levelId, serverStart, "UNK", serverStart, "UNK", venue, group, pos+1, BetterSettings.viewMinutesToGame(), BetterSettings.closingMinutesToGame(), false, false)
      (t1,t2,g)
  }
  
  
  // 11/06/2021 21:00
  //TODO: time difference is 5 hours or 6 hours  in Manaus und Cuiab� !!!!
  def parseDate(times: String): OffsetDateTime = {
      try{
         val time = TimeHelper.fromString(times,"dd/MM/yyyy HH:mm")
         time
      }catch{
        case e: Exception => {
          logger.error(s"error on date: ${times} ${e.getMessage}\n")
          throw(e)
        }
      }
  }
  

  def players(): Seq[(Player,String)] = {
      logger.info("parsing players")
	    val lines = InitialDataX.toLines("euro2020_players.tab", environment)
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
         val users = (1 to 100).map(n => uf(s"n$n", s"f$n", s"l$n", s"f${n}.l${n}@betting.com", s"p$n", false))
         admin +: users
	  }else{
	      val names = Seq("ido")
        names.map(n => uf(n, "","", "", n, true))		  		    
  	  }
  }
 
//  def updateChampion(){
//	     betterDb.specialbetsuser.filter(s => s.spId === 3l).map(_.prediction).update("Argentinia")  
 // }
  
 def insert(debug: Boolean): Unit = { 
    val ls = InitialDataX.levels(environment)
    val us = users(debug)
    val ps = players()
    val sp = InitialDataX.specialBets(OffsetDateTime.of(2020, 6, 11, 20, 0,0,0,BetterSettings.offset())) //here insert start of first game -1hr

    betterDb.dropCreate()

    logger.info("inserting data in db")
    Await.result(Future.sequence(sp.map(t => betterDb.insertSpecialBetInStore(t))), 3.seconds)
    logger.info("inserted special bets")
    Await.result(Future.sequence(us.map{ u => betterDb.insertUser(u, u.isAdmin, u.isRegistrant, None) } ), 5.seconds)
    logger.info("inserted users")
    val admin = Await.result(betterDb.allUsers(), 1.seconds).filter(_.isAdmin).sortBy(_.id).head
    Await.result(Future.sequence(ls.map(l => betterDb.insertLevel(l, admin))), 3.seconds)  
    val level = Await.result(betterDb.allLevels(), 1.second)(0)
    val (teams, ttg) = teamsGames(level.id.get)
    Await.result(Future.sequence(teams.map(t => betterDb.insertTeam(t, admin))), 3.seconds)
    Await.result(Future.sequence(ttg.map{ case(t1,t2,g) => betterDb.insertGame(g, t1, t2, level.level, admin)}), 5.seconds)
    Await.result(betterDb.createBetsForGamesForAllUsers(admin), 10.seconds)
    ps.map{ case(p,t) => Await.result(betterDb.insertPlayer(p, t, admin), 3.seconds) }

	   
	  

    logger.info("done inserting data in db")
    
   
    
  }

}
