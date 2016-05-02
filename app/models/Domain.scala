package models

import org.joda.time.DateTime

trait BetterException 

object BetterException {
  
  def apply(message: String): BetterException =  new RuntimeException(message) with BetterException 

  def apply(message: String, cause: Throwable = null): BetterException = new RuntimeException(message, cause) with BetterException 
  
  def unapply(exception: BetterException with RuntimeException): (Option[String],Option[Throwable],Option[Seq[StackTraceElement]]) = {
      val stack = if(exception.getStackTrace != null) Some(exception.getStackTrace.toSeq) else None
      val message = if(exception.getMessage != null) Some(exception.getMessage) else None
      val cause = if(exception.getCause != null) Some(exception.getCause) else None
      (message, cause, stack)
  }
  
}

case class AccessViolationException(val message: String) extends RuntimeException(message) with BetterException
case class ItemNotFoundException(message: String) extends RuntimeException(message) with BetterException
case class ValidationException(message: String) extends RuntimeException(message) with BetterException

object DomainHelper {
  import org.jasypt.util.password.StrongPasswordEncryptor
  import scravatar._
  
  type SpecialBets = Seq[(SpecialBetT,SpecialBetByUser)]
  
  val gts = Seq("monsterid", "identicon", "wavatar", "retro") 
  
  def encrypt(password: String): String = {
      new StrongPasswordEncryptor().encryptPassword(password)
  }
  
  def gravatarUrl(email: String, gravatartype: String): (String,String) = {
	  val gt = DefaultImage(gravatartype)
      val url = Gravatar(email).ssl(true).default(gt).maxRatedAs(G).forceDefault(true).avatarUrl  
      (url, gravatartype)
  }
  
  def randomGravatarUrl(email: String): (String,String) = {
      val ri = new scala.util.Random().nextInt(gts.length)
      gravatarUrl(email, gts(ri))
  }
  
  def gameResultInit(): GameResult = GameResult(0,0,false)  
  def betInit(user: User, game: Game): Bet = Bet(None, 0, gameResultInit, game.id.getOrElse(-1), user.id.getOrElse(-1)) 
  
  /**
   * admins had instructions!
   */
  def userInit(user: User, isAdmin: Boolean, isRegistrant: Boolean, registeringUser: Option[Long]): User = {
	  val (u,t) = randomGravatarUrl(user.email)
      User(None, user.username, user.firstName, user.lastName, user.institute, user.showName, user.email, user.passwordHash, isAdmin, isRegistrant, isAdmin, true, true, 0, 0, u, t, registeringUser)
  }

  def userFromUPE(username: String, password: String, firstName: String, lastName: String, email: String, registeringUser: Option[Long]): User = {
	    val (u,t) = randomGravatarUrl(email)
      User(None, username, firstName, lastName, "", false, email, encrypt(password), false, false, false, true, true, 0, 0, u, t, registeringUser)
  }
 
  def toBetLog(user: User, game: Game, betOld: Bet, betNew: Bet, time: DateTime, comment: String): BetLog = {
      BetLog(None, user.id.getOrElse(-1), game.id.getOrElse(-1), game.serverStart, betOld.id.getOrElse(-1), betOld.result.goalsTeam1, betNew.result.goalsTeam1, betOld.result.goalsTeam2, betNew.result.goalsTeam2, time, comment)
  }
 
  def viewableTime(gameStart: DateTime, currentTime: DateTime, viewTimeToStart: Int): Boolean = {
       val viewOpen = gameStart.minusMinutes(viewTimeToStart)
       val open = currentTime.isAfter(viewOpen)
       open
  }
  
  def viewable(viewingUserId: Long, betUserId: Long, gameStart: DateTime, currentTime: DateTime, viewTimeToStart: Int): Boolean = {
       viewingUserId == betUserId && viewableTime(gameStart, currentTime, viewTimeToStart)
  }
  
}

//embedaable
case class GameResult(goalsTeam1: Int, goalsTeam2: Int, isSet: Boolean){
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


//maybe size could be added
case class DBImage(format: String, image: String) //unsure of base64 string or array[Byte]

case class Team(id: Option[Long] = None, name: String, short3: String, short2: String)

case class Player(id: Option[Long] = None, name: String, role: String, club: String, teamId: Long,  dbimage: DBImage)

case class Bet(id: Option[Long] = None, points: Int, result: GameResult, gameId: Long, userId: Long){ 
//unique: user/bet game/bet one bet for each user per game 
  
  def viewableBet(viewingUserId: Long, gameStart: DateTime, currentTime: DateTime, viewTimeToStart: Int): ViewableBet = {
      if(DomainHelper.viewable(viewingUserId, userId, gameStart, currentTime, viewTimeToStart)){
        ViewableBet(id, points, Some(result), gameId, userId)
      }else{
        ViewableBet(id, points, None, gameId, userId)
     }    
  }
  
  
  
}

case class ViewableBet(id: Option[Long] = None, points: Int, result: Option[GameResult], gameId: Long, userId: Long){
    
     def toBet(): Bet = {
         Bet(id, points, result.getOrElse(DomainHelper.gameResultInit()), gameId, userId)  
     }
     
}

case class BetLog(id: Option[Long] = None, userId: Long, gameId: Long, gameStart: DateTime, betId: Long, t1old: Int, t1new: Int, t2old: Int, t2new: Int, time: DateTime, comment: String){

	def toText(viewingUserId: Long, gameStart: DateTime, currentTime: DateTime, viewTimeToStart: Int): String = {
	    val betchange = if(DomainHelper.viewable(viewingUserId, userId, gameStart, currentTime, viewTimeToStart)){
          Seq(GameResult(t1old, t2old, true).display, "->",  GameResult(t1old, t2old, true).display).mkString(" ")
      }else{
          Seq(GameResult(t1old, t2old, false).display, "->",  GameResult(t1old, t2old, false).display).mkString(" ")
     }    
	   val format = org.joda.time.format.DateTimeFormat.fullDateTime() 
	   Seq(id, userId, gameId, betId, betchange, format.print(time), comment).mkString("\t")
	}
	
}



/***
 * hadinstructions === special bet was set
 */
case class User(id: Option[Long] = None, username: String, firstName: String, lastName: String, institute: String, showName: Boolean, email: String, passwordHash: String,
	        isAdmin: Boolean, isRegistrant: Boolean, hadInstructions: Boolean, canBet: Boolean, isRegistered: Boolean,
			points: Int, pointsSpecialBet: Int, iconurl: String, icontype: String, registeredBy: Option[Long] ){
  
     def totalPoints(): Int = points + pointsSpecialBet
  
}


case class UserNoPw(id: Option[Long] = None, username: String, firstName: String, lastName: String, 
	      isAdmin: Boolean, isRegistrant: Boolean, hadInstructions: Boolean, canBet: Boolean, 
		  totalPoints: Int, pointsGames: Int, pointsSpecialBet: Int, iconurl: String, icontype: String, registeredBy: Option[Long], rank: Int){
}
   
object UserNoPwC {
   def apply(user: User, rank: Int = 0): UserNoPw = {
        UserNoPw(user.id, user.username, user.firstName, user.lastName, 
		user.isAdmin, user.isRegistrant, user.hadInstructions, user.canBet,
		user.totalPoints, user.points, user.pointsSpecialBet, user.iconurl, user.icontype, user.registeredBy, rank)  
   }     
}      
         

object SpecialBetType {
	val team = "team"
	val player = "player"
}


case class SpecialBetByUser(id: Option[Long], userId: Long,  specialbetId: Long, prediction: String, points: Int)
/**
* the betgroupID allows grouping for multiple results e.g. semifinal1 seimifinal2 .. semifinal4 should all have the same groupId
*
**/
case class SpecialBetT(id: Option[Long], name: String, description: String, points: Int, closeDate: DateTime, betGroup: String, itemType: String, result: String)

case class SpecialBets(bets: Seq[(SpecialBetT,SpecialBetByUser)]){
	
	def byTemplateId(id: Option[Long]): Option[SpecialBetByUser] = {
	    bets.filter{ case(t,b)  => t.id == id }.headOption.map(_._2)
	}

}

case class GameLevel(id: Option[Long] = None, name: String, pointsExact: Int, pointsTendency: Int, level: Int, viewMinutesToGame: Int)//name: groups, quarter final, semi final, final

/**
 * startLocal: start in local timezone 
 * startServer: start in server timezone
 * 
 **/
case class Game(id: Option[Long] = None, result: GameResult, team1id: Long, team2id: Long, levelId: Long, localStart: DateTime, localtz: String, serverStart: DateTime, servertz: String, venue: String, group: String, nr: Int){
  //     def GameResultPrettyPrint = if(calculated) GameResult.goalsTeam1+":"+GameResult.goalsTeam2 else "NA"
	    	 
 //	     def datePrettyPrint = sdf.format(date.getTime)

  //def closed(): Boolean = {
    //compare with time
  //}
  
}


case class GameWithTeams(game: Game, team1: Team, team2: Team, level: GameLevel)

case class UserToken(id: Option[Long] = None, userId: Long, token: String, created: DateTime, used: Option[DateTime], tokentype: String)
