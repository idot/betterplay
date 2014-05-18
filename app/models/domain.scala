package models

import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime

object JodaHelper { //TODO: check timezone, might have to use calendar 
	implicit val dateTimeColumnType = MappedColumnType.base[org.joda.time.DateTime, java.sql.Timestamp](
	     { dt => new java.sql.Timestamp(dt.getMillis) },
	     { ts => new org.joda.time.DateTime(ts) }
	)
}

import JodaHelper._

//embedaable
case class Result(goalsTeam1: Int, goalsTeam2: Int, isSet: Boolean){
     def display = if(isSet) goalsTeam1+":"+goalsTeam2 else "-:-"
     def winner(): Int = {
         if(goalsTeam1 > goalsTeam2){
           1
         }else if(goalsTeam1 < goalsTeam2){
           2
         }else{
           0
         }
     } 
}


case class Country(id: Option[Long] = None, name: String, flag: Array[Byte])

class Countries(tag: Tag) extends Table[Country](tag, "countries") {
     def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
     def name = column[String]("name", O.NotNull)
     def flag = column[Array[Byte]]("photo", O.Nullable)
     
     def * = (id.?, name, flag) <> (Country.tupled, Country.unapply _)
}



case class Player(id: Option[Long] = None, firstName: String, lastName: String, role: String, countryId: Long, photo: Option[Array[Byte]])

class Players(tag: Tag) extends Table[Player](tag, "players") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def firstName = column[String]("firstname", O.NotNull)
  def lastName = column[String]("lastname", O.NotNull)
  def role = column[String]("role", O.NotNull)
  def countryId = column[Long]("countryid", O.NotNull)
  def photo = column[Option[Array[Byte]]]("photo", O.Nullable)
  
  def * = (id.?, firstName, lastName, role, countryId, photo) <> (Player.tupled, Player.unapply _)
  
}


case class Bet(id: Option[Long] = None, points: Int, result: Result, gameId: Long){ //GameID is missing!
//unique: user/bet game/bet one bet for each user per game
  
}

class Bets(tag: Tag) extends Table[Bet](tag, "bets") {
   def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
   def points = column[Int]("points", O.NotNull)
   def goalsTeam1 = column[Int]("goalsteam1", O.NotNull)
   def goalsTeam2 = column[Int]("goalsteam2", O.NotNull)
   def isSet = column[Boolean]("isset", O.NotNull)
   def gameId = column[Long]("gameid", O.NotNull)

   def * = (id.?, points, result, gameId) <> (Bet.tupled, Bet.unapply)
   def result = (goalsTeam1, goalsTeam2, isSet) <> (Result.tupled, Result.unapply)
   
}

case class User(id: Option[Long] = None, firstName: String, lastName: String, email: String, passwordHash: String, isAdmin: Boolean, hadInstructions: Boolean, canBet: Boolean, points: Int, iconurl: Option[String], registeredBy: Long ){
//  
//  	def password_=(in: String) = this.password_hash = encrypt(in)
//	
//	def authenticate(in: String): Boolean = new StrongPasswordEncryptor().checkPassword(in, password) 
//
//    private def encrypt(in: String): String = new StrongPasswordEncryptor().encryptPassword(in)
  
}

class Users(tag: Tag) extends Table[User](tag, "users") {
    def id =  column[Long]("id", O.PrimaryKey, O.AutoInc)
    def firstname = column[String]("firstname", O.NotNull)
    def lastname = column[String]("lastname", O.NotNull)
    def email = column[String]("email", O.NotNull)
    def passwordhash = column[String]("password", O.NotNull)
    def isAdmin = column[Boolean]("isadmin", O.NotNull)
    def hadInstructions = column[Boolean]("instructions", O.NotNull)
    def canBet = column[Boolean]("canbet", O.NotNull)
    def points = column[Int]("points", O.NotNull)
    def iconurl = column[Option[String]]("iconurl", O.NotNull)
    def registerby = column[Long]("registerby", O.NotNull)
    
    def * = (id.?, firstname, lastname, email, passwordhash, isAdmin, hadInstructions, canBet, points, iconurl, registerby) <> (User.tupled, User.unapply)
    
}


case class SpecialBet(id: Option[Long], topScorer: Long, mvp: Long, winningTeam: Long, semi1: Long, semi2: Long, semi3: Long, semi4: Long)

class SpecialBets(tag: Tag) extends Table[SpecialBet](tag, "specialbets"){ 
   def id =  column[Long]("id", O.PrimaryKey, O.AutoInc)
   def topScorer = column[Long]("topscorer", O.NotNull)
   def mvp = column[Long]("mvp", O.NotNull)
   def winningteam = column[Long]("winningteam", O.NotNull)
   def semi1 = column[Long]("semi1", O.NotNull)
   def semi2 = column[Long]("semi2", O.NotNull)
   def semi3 = column[Long]("semi3", O.NotNull)
   def semi4 = column[Long]("semi4", O.NotNull)
   
   def * = (id.?, topScorer, mvp, winningteam, semi1, semi2, semi3, semi4) <> (SpecialBet.tupled, SpecialBet.unapply)
  
}

case class GameLevel(name: String, pointsExact: Int, pointsTendency: Int, levelNr: Int)//groups, quarter final, semi final, final


case class Game(id: Option[Long] = None, result: Result, team1id: Long, team2id: Long, levelId: Long, start: DateTime, venue: String, group: String){
  //     def resultPrettyPrint = if(calculated) result.goalsTeam1+":"+result.goalsTeam2 else "NA"
	    	 
 //	     def datePrettyPrint = sdf.format(date.getTime)

  //def closed(): Boolean = {
    //compare with time
  //}
  
}

class Games(tag: Tag) extends Table[Game](tag, "games"){
   def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
   def goalsTeam1 = column[Int]("goalsteam1", O.NotNull)
   def goalsTeam2 = column[Int]("goalsteam2", O.NotNull)
   def isSet = column[Boolean]("isset", O.NotNull) 
   def team1 = column[Long]("team1id", O.NotNull)
   def team2 = column[Long]("team2id", O.NotNull)
   def levelId = column[Long]("levelid", O.NotNull)
   def start = column[DateTime]("start", O.NotNull)
   def venue = column[String]("venue", O.NotNull)
   def group = column[String]("group", O.NotNull)
   
   def * = (id.?, result, team1, team2, levelId, start, venue, group) <> (Game.tupled, Game.unapply)
   def result = (goalsTeam1, goalsTeam2, isSet) <> (Result.tupled, Result.unapply)
   
}






