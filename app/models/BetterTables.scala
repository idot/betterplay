package models

import javax.inject.{ Inject, Singleton }


import javax.inject.{ Inject, Singleton }
import slick.jdbc.JdbcProfile
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ Future, ExecutionContext }


import java.time.OffsetDateTime


import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Try,Success,Failure}
import play.api.Logger
import slick.jdbc.meta.MTable
import slick.jdbc.JdbcProfile


  


trait BetterTables { 
  val dbLogger = Logger("db")
  
  protected def dbConfigProvider: DatabaseConfigProvider
 
  protected val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._


  implicit def StringColumnType = MappedColumnType.base[java.time.OffsetDateTime, String](
     { dt => dt.toString()  },
     { ts => OffsetDateTime.parse(ts) }
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
  val messages = TableQuery[Messages]
  val usersmessages = TableQuery[UsersMessages]
  val messageserrors = TableQuery[MessagesErrors]
  
  def schema() = { 
               users.schema ++
               teams.schema ++
               players.schema ++
               levels.schema ++
               games.schema ++
               bets.schema ++
               specialbetstore.schema ++
               specialbetsuser.schema ++
               betlogs.schema ++
               messages.schema ++
               usersmessages.schema ++
               messageserrors.schema
  }

  def createTables(): Unit = {
     dbLogger.info("creating tables") 
     Await.result(db.run(DBIO.seq(schema().create)), 1.second)
  }
  
  def drop(): Unit = {
     dbLogger.info("dropping tables") 
     Await.result(db.run(DBIO.seq(schema().drop)), 1.second)
  }

  def dropCreate(): Unit = {
      import scala.concurrent.ExecutionContext.Implicits.global
      dbLogger.info("starting to drop or create tables") 

      val f = db.run(MTable.getTables(namePattern = "users").headOption)
      val r = Await.result(f, 1.seconds)
      r match {
        case Some(t) => {
          drop()
          createTables()
        }
        case None => {
          createTables()
        }
      }
  }
  

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
    def localStart = column[OffsetDateTime]("localstart")
    def localtz = column[String]("localtz")
    def serverStart = column[OffsetDateTime]("serverstart")
    def servertz = column[String]("servertz")
    def venue = column[String]("venue")
    def group = column[String]("group")
    def nr = column[Int]("nr")
    def viewMinutesToGame = column[Int]("viewminutestogame")
    def closingMinutesToGame = column[Int]("closingminutestogame")
    def gameClosed = column[Boolean]("gameclosed")
    def nextGame = column[Boolean]("nextgame")
    
    def team1 = foreignKey("GAME_TEAM1_FK", team1Id, teams)(_.id) 
    def team2 = foreignKey("GAME_TEAM2_FK", team2Id, teams)(_.id) 
    def level = foreignKey("GAME_LEVEL_FK", levelId, levels)(_.id) 
    
    def * = (id.?, result, team1Id, team2Id, levelId, localStart, localtz, serverStart, servertz, venue, group, nr, viewMinutesToGame, closingMinutesToGame, gameClosed, nextGame) <> (Game.tupled, Game.unapply)
    def result = (goalsTeam1, goalsTeam2, isSet) <> (GameResult.tupled, GameResult.unapply)

  }

  
  class SpecialBetsTs(tag: Tag) extends Table[SpecialBetT](tag, "specialbetsstore"){
     def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
     def name = column[String]("name")
     def description = column[String]("description")
     def points = column[Int]("points")
	   def closeDate = column[OffsetDateTime]("closedate")
	   def betGroup = column[String]("betgroup")
     def itemtype = column[String]("itemtype")
     def result = column[String]("result") 
     def nr = column[Int]("number")
     def * = (id.?, name, description, points, closeDate, betGroup, itemtype, result, nr) <> (SpecialBetT.tupled, SpecialBetT.unapply)  
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
    def institute = column[String]("institute")
    def showName = column[Boolean]("showname")
    def email = column[String]("email")
    def passwordhash = column[String]("password")
    def isRegistrant = column[Boolean]("isregistrant")
    def isAdmin = column[Boolean]("isadmin")
    def hadInstructions = column[Boolean]("instructions")
    def sendEmail = column[Boolean]("sendemail")
    def canBet = column[Boolean]("canbet")
	  def points = column[Int]("points")
    def iconurl = column[String]("iconurl")
    def icontype = column[String]("icontype")
	  def registerby = column[Option[Long]]("registerby")
    def pointsSpecial = column[Int]("pointsspecial")
    def filterBet = column[String]("filterbet")
    def filterGame = column[String]("filtergame")
    def filterLevel = column[String]("filterlevel")
    def haddsgvo = column[Boolean]("haddsgvo")
    def usernameidx = index("USER_USERNAME_INDEX", (username), unique = true)
	  def emailidx = index("USER_EMAIL_INDEX", (email), unique = true)

       
    def registerfk = foreignKey("USER_USER_FK", registerby, users)(_.id.?) 
    
    def * = (id.?, username, firstname, lastname, institute, showName, email, passwordhash, isAdmin, isRegistrant, sendEmail, hadInstructions, canBet, points, pointsSpecial, iconurl, icontype, registerby, haddsgvo, filterSettings) <> (User.tupled, User.unapply)
    def filterSettings = (filterBet, filterGame, filterLevel) <> (FilterSettings.tupled, FilterSettings.unapply)
    
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
    def sortname = column[String]("sortname")   
    def imageFormat = column[String]("format")
    def image = column[String]("image")
     
    def foto = (imageFormat, image) <> (DBImage.tupled, DBImage.unapply)
    
    def team = foreignKey("PLAYER_COUNTRY_FK", teamId, teams)(_.id)    
    def * = (id.?, name, role, club, teamId, foto, sortname) <> (Player.tupled, Player.unapply _)

  }

  //no constraints in id columns so its possible to see what went wrong
  class BetLogs(tag: Tag) extends Table[BetLog](tag, "betlogs") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
	  def userId = column[Long]("user_id")
	  def gameId = column[Long]("game_id")
	  def gameStart = column[OffsetDateTime]("gameStart")
	  def betId = column[Long]("bet_id")
    def t1old = column[Int]("t1old")
    def t1new = column[Int]("t1new")
    def t2old = column[Int]("t2old")
    def t2new = column[Int]("t2new")
	  def created = column[OffsetDateTime]("change")
	  def comment = column[String]("comment")
	  
	  def * = (id.?, userId, gameId, gameStart, betId, t1old, t1new, t2old, t2new, created, comment) <> (BetLog.tupled, BetLog.unapply _)
  }

  class Messages(tag: Tag) extends Table[Message](tag, "messages"){
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def messageType = column[String]("messagetype")
    def subject = column[String]("subject")
    def body = column[String]("body")
    def sendingUser = column[Long]("sendinguser")
    
    def * = (id.?, messageType, subject, body, sendingUser) <> (Message.tupled, Message.unapply _)
    
  }
  
  class UsersMessages(tag: Tag) extends Table[UserMessage](tag, "usersmessages"){
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("userid")
    def messageId = column[Long]("messageid")
    def send = column[Boolean]("send")
    def sent = column[Option[OffsetDateTime]]("sent")
    def display = column[Boolean]("display")
    def seen = column[Option[OffsetDateTime]]("seen")
    def token = column[String]("token")
    def sendingUser = column[Long]("sendinguser")
    
    def user = foreignKey("MESSAGE_USER_FK", userId, users)(_.id)
    def message = foreignKey("MESSAGE_MESSAGE_FK", messageId, messages)(_.id)
    
    def * = (id.?, userId, messageId, token, send, sent, display, seen, sendingUser) <> (UserMessage.tupled, UserMessage.unapply _)
  
  }
  
  
  class MessagesErrors(tag: Tag) extends Table[MessageError](tag, "messageserrors"){
     def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
     def userMessageId = column[Long]("usermessageid")
     def error = column[String]("error")
     def time = column[OffsetDateTime]("time")
    
     def message = foreignKey("ERROR_USERMESSAGE_FK", userMessageId, usersmessages)(_.id)
    
     def * = (id.?, userMessageId, error, time) <> (MessageError.tupled, MessageError.unapply _)
  
  }
      
     
  
}


