import java.text.SimpleDateFormat
import play.api._
import models._
import play.api.db.slick._
import play.api.Play.current
import org.apache.commons.io.IOUtils
import scala.slick.jdbc.meta.MTable


object Global extends GlobalSettings {

  override def onStart(app: Application) {
     InitialData.insert()
  }
 
}


/**
 * DATA FROM:
 * https://github.com/openfootball/world-cup/tree/master/2014--brazil
 * look at scripts folder
 */
object InitialData {
  import org.joda.time.format.DateTimeFormat
  import org.joda.time.DateTime
  
  
   
  def toLines(file: String): Seq[String] = {
     val is = Play.classloader.getResourceAsStream(file)
     val string = IOUtils.toString(is, "UTF-8")
     val li = string.split("\n").drop(1)
     is.close
     li     
  }
  
  //2014-06-12 17:00:00.000000
  //TODO: time difference is 5 hours or 6 hours  in Manaus und Cuiab� !!!!
  def parseDate(str: String, venue: String): DateTime = {
      try{
      val short = str.substring(0, str.lastIndexOf("."))
      val df = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
      val dt = df.parseDateTime(short)
      dt.plusHours(5) //TODO: make dependent on venue
      }catch{
        case e: Exception => {
          Logger.error("error on date: "+str+" "+venue+" "+e.getMessage)
          throw(e)
        }
      }
  }
  
  //play_at|pos|title|key|title|code|key|title|code
  //2014-06-12 17:00:00.000000|1|Group A|bra|Brazil|BRA|cro|Croatia|CRO
  def parseLine(line: String, levelId: Long): (Team,Team, Game) = {
      Logger.info(line)
      val items = line.split("\\|")
      val venue = ""
      val date = parseDate(items(0), venue)
      val pos = items(1).toInt
      val group = items(2)
      val t1 = Team(None, items(4), items(3), DBImage("",""))
      val t2 = Team(None, items(7), items(6), DBImage("",""))
      val g = Game(None, DomainHelper.gameResultInit, 0, 0, levelId, date, venue, group, pos)
      (t1,t2,g)
  }
  
  def teamsGames(levelId: Long): (Set[Team], Seq[(String,String,Game)]) = {
      Logger.info("parsing games")
      val lines = toLines("GAMES2014.txt")    
      val ttg = lines.map(parseLine(_,levelId))
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
      Logger.info("parsing levels")
      val lines = toLines("levels.tab")
      lines.map(parseLevel)    
  }
  
  def users(): Seq[User] = {
      def uf(name: String, first: String, last: String, email: String, pw: String, admin: Boolean): User = {
          User(None, name, first, last, email, DomainHelper.encrypt(pw), admin, admin, admin, true, 0, 0, None, None)
      }
      val admin = uf("admin", "admin" ,"admin", "admin@admin.com", "admin", true)
      val users = (1 to 10).map(n => uf(s"n$n", s"f$n", s"l$n", s"f${n}.l${n}@betting.com", "p$n", false))
      admin +: users
  }
  
  def insert(): Unit = { //again slick is missing nested transactions!
    val ls = levels()
    val us = users()
    
    Logger.info("inserting data in db")
    DB.withSession { implicit s: Session =>
       if(MTable.getTables("users").list().isEmpty) {
        Logger.info("creating tables")
        BetterTables.createTables()
       }else{
         Logger.info("dropping tables")
         BetterTables.drop()
         BetterTables.createTables()
       }
       Logger.info("inserting data")
       val admin = BetterDb.insertUser(us(0), true, true, None).toOption.get //admin
       val levels = ls.map(l => BetterDb.insertOrUpdateLevelByNr(l, admin)).map(_.toOption.get)
       val level = levels(0)
       val (teams, ttg) = teamsGames(level.id.get)
       teams.map(t => BetterDb.insertOrUpdateTeamByName(t, admin))
       ttg.map{ case(t1,t2,g) => BetterDb.insertGame(g, t1, t2, level.level, admin)}        
       us.drop(1).foreach(u => BetterDb.insertUser(u, false, false, admin.id))
       Logger.info("inserted data")
    }
    Logger.info("done inserting data in db")
  
  }


}