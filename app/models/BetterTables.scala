package models

import org.joda.time.DateTime
import play.api.db.slick.HasDatabaseConfigProvider

//import slick.jdbc.meta.MTable
import slick.driver.JdbcProfile
import org.joda.time.Period


object JodaHelper { //TODO: check timezone, might have to use calendar

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

trait BetterTables { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import driver.api._

  implicit val dateTimeColumnType = MappedColumnType.base[org.joda.time.DateTime, java.sql.Timestamp](
     { dt => new java.sql.Timestamp(dt.getMillis) },
     { ts => new org.joda.time.DateTime(ts) }
  )

  val users = TableQuery[Users]
  val teams = TableQuery[Teams]
  val players = TableQuery[Players]
  val levels = TableQuery[GameLevels]
  val games = TableQuery[Games]
  val bets = TableQuery[Bets]
  val specialbetstore = TableQuery[SpecialBetsTs]
  val specialbetsuser = TableQuery[SpecialBetByUsers]
  val betlogs = TableQuery[BetLogs]

/*  val schema = users.schema ++
               teams.schema ++
               players.schema ++
               levels.schema ++
               games.schema ++
               bets.schema ++
               specialbetstore.schema ++
               specialbetsuser.schema ++
               betlogs.schema

  def createTables(){
     DBIO.seq(schema.create)
  }
  
  def drop(){
     DBIO.seq(schema.drop)
  }

  def dropCreate(){
      if(MTable.getTables("users").list().isEmpty) {
           createTables()
       }else{
           drop()
           createTables()
       }
  }*/

  class Teams(tag: Tag) extends Table[Team](tag, "teams") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def short3 = column[String]("short3")
    def short2 = column[String]("short2")

    def * = (id.?, name, short3, short2) <> (Team.tupled, Team.unapply _)
  }

  class GameLevels(tag: Tag) extends Table[GameLevel](tag, "gamelevel") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def pointsExact = column[Int]("pointsexact")
    def pointsTendency = column[Int]("pointstendency")
    def level = column[Int]("level")

    def * = (id.?, name, pointsExact, pointsTendency, level) <> (GameLevel.tupled, GameLevel.unapply)

  }

  class Games(tag: Tag) extends Table[Game](tag, "games") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def goalsTeam1 = column[Int]("goalsteam1")
    def goalsTeam2 = column[Int]("goalsteam2")
    def isSet = column[Boolean]("isset")
    def team1Id = column[Long]("team1_id")
    def team2Id = column[Long]("team2_id")
    def levelId = column[Long]("level_id")
    def localStart = column[DateTime]("localstart")
    def localtz = column[String]("localtz")
    def serverStart = column[DateTime]("serverstart")
    def servertz = column[String]("servertz")
    def venue = column[String]("venue")
    def group = column[String]("group")
    def nr = column[Int]("nr")

    
    def team1 = foreignKey("GAME_TEAM1_FK", team1Id, teams)(_.id) 
    def team2 = foreignKey("GAME_TEAM2_FK", team2Id, teams)(_.id) 
    def level = foreignKey("GAME_LEVEL_FK", levelId, levels)(_.id) 
    
    def * = (id.?, result, team1Id, team2Id, levelId, localStart, localtz, serverStart, servertz, venue, group, nr) <> (Game.tupled, Game.unapply)
    def result = (goalsTeam1, goalsTeam2, isSet) <> (GameResult.tupled, GameResult.unapply)

  }

  
  class SpecialBetsTs(tag: Tag) extends Table[SpecialBetT](tag, "specialbetsstore"){
     def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
     def name = column[String]("name")
     def description = column[String]("description")
     def points = column[Int]("points")
	   def closeDate = column[DateTime]("closedate")
	   def betGroup = column[String]("betgroup")
     def itemtype = column[String]("itemtype")
     def result = column[String]("result") 
     def * = (id.?, name, description, points, closeDate, betGroup, itemtype, result) <> (SpecialBetT.tupled, SpecialBetT.unapply)  
  }
  
  
  class SpecialBetByUsers(tag: Tag) extends Table[SpecialBetByUser](tag, "specialbetbyusers"){
     def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
     def userId = column[Long]("userid")
     def spId = column[Long]("specialbetid")
     def prediction = column[String]("prediction")
     def points = column[Int]("points")
    
     def * = (id.?, userId, spId, prediction, points) <> (SpecialBetByUser.tupled,SpecialBetByUser.unapply)     
    
     def userfk = foreignKey("SPECIALBET_USER_FK", userId, users)(_.id) 
     def spfk = foreignKey("SPECIALBET_SPECIALBETT_FK", spId, specialbetstore)(_.id) 
     
  }
  

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def firstname = column[String]("firstname")
    def lastname = column[String]("lastname")
    def email = column[String]("email")
    def passwordhash = column[String]("password")
    def isRegistrant = column[Boolean]("isregistrant")
    def isAdmin = column[Boolean]("isadmin")
    def hadInstructions = column[Boolean]("instructions")
    def canBet = column[Boolean]("canbet")
    def isRegistered = column[Boolean]("isregistered")
	  def points = column[Int]("points")
    def iconurl = column[String]("iconurl")
    def icontype = column[String]("icontype")
	
	  def usernameidx = index("USER_USERNAME_INDEX", (username))
    def registerby = column[Option[Long]]("registerby")
    def pointsSpecial = column[Int]("pointsspecial")
    
    def registerfk = foreignKey("USER_USER_FK", registerby, users)(_.id) 
    
    def * = (id.?, username, firstname, lastname, email, passwordhash, isAdmin, isRegistrant, hadInstructions, canBet, isRegistered, points, pointsSpecial, iconurl, icontype, registerby) <> (User.tupled, User.unapply)

  }

  class Bets(tag: Tag) extends Table[Bet](tag, "bets") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def points = column[Int]("points")
    def goalsTeam1 = column[Int]("goalsteam1")
    def goalsTeam2 = column[Int]("goalsteam2")
    def isSet = column[Boolean]("isset")
    def gameId = column[Long]("game_id")
    def userId = column[Long]("user_id")  
    
    def game = foreignKey("BET_GAME_FK", gameId, games)(_.id)
    def user = foreignKey("BET_USER_FK", userId, users)(_.id)
    
    def * = (id.?, points, result, gameId, userId) <> (Bet.tupled, Bet.unapply)
    def result = (goalsTeam1, goalsTeam2, isSet) <> (GameResult.tupled, GameResult.unapply)

  }

  class Players(tag: Tag) extends Table[Player](tag, "players") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def role = column[String]("role")
	  def club = column[String]("club")
    def teamId = column[Long]("team_id")
       
    def imageFormat = column[String]("format")
    def image = column[String]("image")
     
    def foto = (imageFormat, image) <> (DBImage.tupled, DBImage.unapply)
    
    def team = foreignKey("PLAYER_COUNTRY_FK", teamId, teams)(_.id)    
    def * = (id.?, name, role, club, teamId, foto) <> (Player.tupled, Player.unapply _)

  }




  class UserTokens(tag: Tag) extends Table[UserToken](tag, "usertokens") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
	  def userId = column[Long]("user_id")
	  def token = column[String]("token")
	  def created = column[DateTime]("created")
	  def used = column[Option[DateTime]]("used")
	  def tokentype = column[String]("type")
	  
	  def user = foreignKey("TOKEN_USER_FK", userId, users)(_.id)
	  
	  def * = (id.?, userId, token, created, used, tokentype) <> (UserToken.tupled, UserToken.unapply _)
  }

  //no constraints in id columns so its possible to see what went wrong
  class BetLogs(tag: Tag) extends Table[BetLog](tag, "betlogs") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
	  def userId = column[Long]("user_id")
	  def gameId = column[Long]("game_id")
	  def betId = column[Long]("bet_id")
    def t1old = column[Int]("t1old")
    def t1new = column[Int]("t1new")
    def t2old = column[Int]("t2old")
    def t2new = column[Int]("t2new")
	  def created = column[DateTime]("change")
	  
	  def * = (id.?, userId, gameId, betId, t1old, t1new, t2old, t2new, created) <> (BetLog.tupled, BetLog.unapply _)
  }


  
}


