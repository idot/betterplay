package models

import java.time.OffsetDateTime
import play.api.Logger

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

case class AccessViolationException(message: String) extends RuntimeException(message) with BetterException
case class ItemNotFoundException(message: String) extends RuntimeException(message) with BetterException
case class ValidationException(message: String) extends RuntimeException(message) with BetterException




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

case class Player(id: Option[Long] = None, name: String, role: String, club: String, teamId: Long,  dbimage: DBImage, sortname: String)

case class Bet(id: Option[Long] = None, points: Int, result: GameResult, gameId: Long, userId: Long){ 
//unique: user/bet game/bet one bet for each user per game 
  
  def viewableBet(viewingUserId: Long, gameStart: OffsetDateTime, currentTime: OffsetDateTime, viewTimeToStart: Int): ViewableBet = {
      if(DomainHelper.viewable(viewingUserId, userId, gameStart, currentTime, viewTimeToStart)){
        ViewableBet(id, points, Some(result), gameId, userId, true)
      }else{
        ViewableBet(id, points, None, gameId, userId, false)
     }    
  }
    
}

case class ViewableBet(id: Option[Long] = None, points: Int, result: Option[GameResult], gameId: Long, userId: Long, viewable: Boolean){
    
     def toBet(): Bet = {
         Bet(id, points, result.getOrElse(DomainHelper.gameResultInit()), gameId, userId)  
     }
     
}

case class BetLog(id: Option[Long] = None, userId: Long, gameId: Long, gameStart: OffsetDateTime, betId: Long, t1old: Int, t1new: Int, t2old: Int, t2new: Int, time: OffsetDateTime, comment: String){

 
	def toText(viewingUserId: Long, gameStart: OffsetDateTime, currentTime: OffsetDateTime, viewTimeToStart: Int): String = {
	    val betchange = if(DomainHelper.viewable(viewingUserId, userId, gameStart, currentTime, viewTimeToStart)){
          Seq(GameResult(t1old, t2old, true).display, "->",  GameResult(t1old, t2old, true).display).mkString(" ")
      }else{
          Seq(GameResult(t1old, t2old, false).display, "->",  GameResult(t1old, t2old, false).display).mkString(" ")
     }    
	   Seq(id, userId, gameId, betId, betchange, TimeHelper.standardFormatter.format(time), comment).mkString("\t")
	}
	
}




case class FilterSettings(bet: String, game: String, level: String)

/***
 * hadinstructions === special bet was set
 * 
 * canBet: if user wants money back before game starts canbet == false
 *  
 */
case class User(id: Option[Long] = None, username: String, firstName: String, lastName: String, institute: String, 
      showName: Boolean, email: String, passwordHash: String,
	    isAdmin: Boolean, isRegistrant: Boolean, sendEmail: Boolean, hadInstructions: Boolean, canBet: Boolean,
			points: Int, pointsSpecialBet: Int, iconurl: String, icontype: String, registeredBy: Option[Long], hadDSGVO: Boolean,
			filterSettings: FilterSettings

      ){
  
     def totalPoints(): Int = points + pointsSpecialBet
  
}


case class UserNoPw(id: Option[Long] = None, username: String, email:String, firstName: String, lastName: String, institute: String,
        showName: Boolean, 
	      isAdmin: Boolean, isRegistrant: Boolean, hadInstructions: Boolean, canBet: Boolean, 
        totalPoints: Int, pointsGames: Int, pointsSpecialBet: Int,
        iconurl: String, icontype: String, registeredBy: Option[Long], hadDSGVO: Boolean, rank: Int,
        filterSettings: FilterSettings, viewable: Boolean		  
     ){
}
   
object UserNoPwC {
   def apply(user: User, viewingUser: Option[User], rank: Int = 0): UserNoPw = {
        def forName(value: String): String = {
            viewingUser.map{ v =>
              if(user.showName || v.id == user.id){
                 value
              }else{
                 ""
              }
            }.getOrElse("")
        }
     
        UserNoPw(user.id, user.username, forName(user.email), forName(user.firstName), forName(user.lastName), user.institute,
           user.showName,
		       user.isAdmin, user.isRegistrant, user.hadInstructions, user.canBet,
		       user.totalPoints, user.points, user.pointsSpecialBet, user.iconurl, user.icontype, user.registeredBy, user.hadDSGVO: Boolean, rank,
		       user.filterSettings, viewingUser.map(v => v.id == user.id).getOrElse(false)
        )  
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
case class SpecialBetT(id: Option[Long], name: String, description: String, points: Int, closeDate: OffsetDateTime, betGroup: String, itemType: String, result: String)

case class SpecialBets(bets: Seq[(SpecialBetT,SpecialBetByUser)]){
	
	def byTemplateId(id: Option[Long]): Option[SpecialBetByUser] = {
	    bets.filter{ case(t,b)  => t.id == id }.headOption.map(_._2)
	}

}

//name: groups, quarter final, semi final, final
case class GameLevel(id: Option[Long] = None, name: String, pointsExact: Int, pointsTendency: Int, level: Int)

/**
 * startLocal: start in local timezone 
 * startServer: start in server timezone
 * 
 **/
case class Game(id: Option[Long] = None, result: GameResult, team1id: Long, team2id: Long, levelId: Long, localStart: OffsetDateTime, localtz: String, serverStart: OffsetDateTime, servertz: String, venue: String, group: String, nr: Int, viewMinutesToGame: Int, closingMinutesToGame: Int, gameClosed: Boolean, nextGame: Boolean){

  
}


case class GameWithTeams(game: Game, team1: Team, team2: Team, level: GameLevel)

case class Message(id: Option[Long] = None, messageType: String, subject: String, body: String, creatingUser: Long)
case class UserMessage(id: Option[Long], userId: Long, messageId: Long, token: String, send: Boolean, sent: Option[OffsetDateTime], display: Boolean, seen: Option[OffsetDateTime], sendingUser: Long)
case class MessageError(id: Option[Long], userMessageId: Long, error: String, time: OffsetDateTime)

