package models

import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime
import scala.slick.jdbc.meta.MTable

import JodaHelper._
import org.joda.time.Period

object JodaHelper { //TODO: check timezone, might have to use calendar 
  implicit val dateTimeColumnType = MappedColumnType.base[org.joda.time.DateTime, java.sql.Timestamp](
    { dt => new java.sql.Timestamp(dt.getMillis) },
    { ts => new org.joda.time.DateTime(ts) })
    
  implicit object DateTimeOrdering extends Ordering[DateTime] { def compare(o1: DateTime, o2: DateTime) = o1.compareTo(o2)}
  
  def compareTimeHuman(firstTime: DateTime, lastTime: DateTime): String = {
      val period = new Period(firstTime, lastTime)
      val days = period.getDays()
      val hours = period.getHours()
      val minutes = period.getMinutes()
      val seconds = period.getSeconds()
      val result = s"$days days, $hours hours, $minutes minutes, $seconds seconds"
      result  
  } 
  
}

object BetterTables {
  val users = TableQuery[Users]
  val teams = TableQuery[Teams]
  val players = TableQuery[Players]
  val levels = TableQuery[GameLevels]
  val games = TableQuery[Games]
  val bets = TableQuery[Bets]
  val specialbetstore = TableQuery[SpecialBetsTs] 
  val specialbetsuser = TableQuery[SpecialBetByUsers]
  val betlogs = TableQuery[BetLogs]
 
  def createTables()(implicit s: Session) {
    users.ddl.create
    teams.ddl.create
    players.ddl.create
    levels.ddl.create
    games.ddl.create
    bets.ddl.create
    specialbetstore.ddl.create
	specialbetsuser.ddl.create
	betlogs.ddl.create
	
  }
  
  def drop()(implicit s: Session){
    val ddl = users.ddl ++ teams.ddl ++ players.ddl ++ levels.ddl ++ 
	games.ddl ++ bets.ddl ++
	specialbetstore.ddl ++ specialbetsuser.ddl ++ betlogs.ddl
    //ddl.createStatements.foreach(println)
    ddl.drop
  }

  def dropCreate()(implicit s: Session){
      if(MTable.getTables("users").list().isEmpty) {
           createTables()
       }else{
           drop()
           createTables()
       }
  }
  
  class GameLevels(tag: Tag) extends Table[GameLevel](tag, "gamelevel") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.NotNull)
    def pointsExact = column[Int]("pointsexact", O.NotNull)
    def pointsTendency = column[Int]("pointstendency", O.NotNull)
    def level = column[Int]("level", O.NotNull)

    def * = (id.?, name, pointsExact, pointsTendency, level) <> (GameLevel.tupled, GameLevel.unapply)

  }

  class Games(tag: Tag) extends Table[Game](tag, "games") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def goalsTeam1 = column[Int]("goalsteam1", O.NotNull)
    def goalsTeam2 = column[Int]("goalsteam2", O.NotNull)
    def isSet = column[Boolean]("isset", O.NotNull)
    def team1Id = column[Long]("team1_id", O.NotNull)
    def team2Id = column[Long]("team2_id", O.NotNull)
    def levelId = column[Long]("level_id", O.NotNull)
    def localStart = column[DateTime]("localstart", O.NotNull)
    def localtz = column[String]("localtz", O.NotNull)
    def serverStart = column[DateTime]("serverstart", O.NotNull)
    def servertz = column[String]("servertz", O.NotNull)
    def venue = column[String]("venue", O.NotNull)
    def group = column[String]("group", O.NotNull)
    def nr = column[Int]("nr", O.NotNull)

    
    def team1 = foreignKey("GAME_TEAM1_FK", team1Id, teams)(_.id) 
    def team2 = foreignKey("GAME_TEAM2_FK", team2Id, teams)(_.id) 
    def level = foreignKey("GAME_LEVEL_FK", levelId, levels)(_.id) 
    
    def * = (id.?, result, team1Id, team2Id, levelId, localStart, localtz, serverStart, servertz, venue, group, nr) <> (Game.tupled, Game.unapply)
    def result = (goalsTeam1, goalsTeam2, isSet) <> (GameResult.tupled, GameResult.unapply)

  }

  
  class SpecialBetsTs(tag: Tag) extends Table[SpecialBetT](tag, "specialbetsstore"){
     def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
     def name = column[String]("name", O.NotNull)
     def description = column[String]("description", O.NotNull)
     def points = column[Int]("points", O.NotNull)
	 def closeDate = column[DateTime]("closedate", O.NotNull)
	 def betGroup = column[String]("betgroup", O.NotNull)
     def itemtype = column[String]("itemtype", O.NotNull)
     def result = column[String]("result", O.NotNull) 
     def * = (id.?, name, description, points, closeDate, betGroup, itemtype, result) <> (SpecialBetT.tupled, SpecialBetT.unapply)  
  }
  
  
  class SpecialBetByUsers(tag: Tag) extends Table[SpecialBetByUser](tag, "specialbetbyusers"){
     def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
     def userId = column[Long]("userid", O.NotNull)
     def spId = column[Long]("specialbetid", O.NotNull)
     def prediction = column[String]("prediction", O.NotNull)
     def points = column[Int]("points", O.NotNull)
    
     def * = (id.?, userId, spId, prediction, points) <> (SpecialBetByUser.tupled,SpecialBetByUser.unapply)     
    
  }
  

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username", O.NotNull)
    def firstname = column[String]("firstname", O.NotNull)
    def lastname = column[String]("lastname", O.NotNull)
    def email = column[String]("email", O.NotNull)
    def passwordhash = column[String]("password", O.NotNull)
    def isRegistrant = column[Boolean]("isregistrant", O.NotNull)
    def isAdmin = column[Boolean]("isadmin", O.NotNull)
    def hadInstructions = column[Boolean]("instructions", O.NotNull)
    def canBet = column[Boolean]("canbet", O.NotNull)
    def isRegistered = column[Boolean]("isregistered", O.NotNull)
	def points = column[Int]("points", O.NotNull)
    def iconurl = column[String]("iconurl", O.NotNull)
    def icontype = column[String]("icontype", O.NotNull)
	
	def usernameidx = index("USER_USERNAME_INDEX", (username))
    def registerby = column[Option[Long]]("registerby", O.Nullable)
    def pointsSpecial = column[Int]("pointsspecial", O.NotNull)
    
    def registerfk = foreignKey("USER_USER_FK", registerby, users)(_.id) 
    
    def * = (id.?, username, firstname, lastname, email, passwordhash, isAdmin, isRegistrant, hadInstructions, canBet, isRegistered, points, pointsSpecial, iconurl, icontype, registerby) <> (User.tupled, User.unapply)

  }

  class Bets(tag: Tag) extends Table[Bet](tag, "bets") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def points = column[Int]("points", O.NotNull)
    def goalsTeam1 = column[Int]("goalsteam1", O.NotNull)
    def goalsTeam2 = column[Int]("goalsteam2", O.NotNull)
    def isSet = column[Boolean]("isset", O.NotNull)
    def gameId = column[Long]("game_id", O.NotNull)
    def userId = column[Long]("user_id", O.NotNull)  
    
    def game = foreignKey("BET_GAME_FK", gameId, games)(_.id)
    def user = foreignKey("BET_USER_FK", userId, users)(_.id)
    
    def * = (id.?, points, result, gameId, userId) <> (Bet.tupled, Bet.unapply)
    def result = (goalsTeam1, goalsTeam2, isSet) <> (GameResult.tupled, GameResult.unapply)

  }

  class Players(tag: Tag) extends Table[Player](tag, "players") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.NotNull)
    def role = column[String]("role", O.NotNull)
	def club = column[String]("club", O.NotNull)
    def teamId = column[Long]("team_id", O.NotNull)
       
    def imageFormat = column[String]("format", O.NotNull)
    def image = column[String]("image", O.NotNull)
     
    def foto = (imageFormat, image) <> (DBImage.tupled, DBImage.unapply)
    
    def team = foreignKey("PLAYER_COUNTRY_FK", teamId, teams)(_.id)    
    def * = (id.?, name, role, club, teamId, foto) <> (Player.tupled, Player.unapply _)

  }

  class Teams(tag: Tag) extends Table[Team](tag, "teams") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.NotNull)
    def short3 = column[String]("short3", O.NotNull)
	def short2 = column[String]("short2", O.NotNull)
	     
    def * = (id.?, name, short3, short2) <> (Team.tupled, Team.unapply _)
  }


  class UserTokens(tag: Tag) extends Table[UserToken](tag, "usertokens") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
	  def userId = column[Long]("user_id", O.NotNull)
	  def token = column[String]("token", O.NotNull)
	  def created = column[DateTime]("created", O.NotNull)
	  def used = column[Option[DateTime]]("used", O.NotNull)
	  def tokentype = column[String]("type", O.NotNull)
	  
	  def user = foreignKey("TOKEN_USER_FK", userId, users)(_.id)
	  
	  def * = (id.?, userId, token, created, used, tokentype) <> (UserToken.tupled, UserToken.unapply _)
  }

  class BetLogs(tag: Tag) extends Table[BetLog](tag, "betlogs") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
	  def userId = column[Long]("user_id", O.NotNull)
	  def gameId = column[Long]("game_id", O.NotNull)
	  def betId = column[Long]("bet_id", O.NotNull)
      def t1old = column[Int]("t1old", O.NotNull)
      def t1new = column[Int]("t1new", O.NotNull)
      def t2old = column[Int]("t2old", O.NotNull)
      def t2new = column[Int]("t2new", O.NotNull)
	  def created = column[DateTime]("change", O.NotNull)
	  
	  def user = foreignKey("LOG_USER_FK", userId, users)(_.id)
	  def game = foreignKey("LOG_GAME_FK", gameId, games)(_.id)
	  def bet = foreignKey("LOG_BET_FK",betId, bets)(_.id)

	  def * = (id.?, userId, gameId, betId, t1old, t1new, t2old, t2new, created) <> (BetLog.tupled, BetLog.unapply _)
  }
  

  
}


