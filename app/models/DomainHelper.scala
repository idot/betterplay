package models

import java.time.OffsetDateTime
import play.api.Logger

object DomainHelper {
  import org.jasypt.util.password.StrongPasswordEncryptor
  import scravatar._
  
  val domainhelperLogger = Logger("domainhelper")
  
  type SpecialBets = Seq[(SpecialBetT,SpecialBetByUser)]
  
  val gts = Seq("monsterid", "identicon", "wavatar", "retro") 
  
  def encrypt(password: String): String = {
      new StrongPasswordEncryptor().encryptPassword(password)
  }
  
  def gravatarUrl(email: String, gravatartype: String): (String,String) = {
	  val gt = DefaultImage(gravatartype)
    val url = Gravatar(email).ssl(true).default(gt).maxRatedAs(G).avatarUrl  
    (url, gravatartype)
  }
  
  def randomGravatarUrl(email: String): (String,String) = {
      val ri = new scala.util.Random().nextInt(gts.length)
      gravatarUrl(email, gts(ri))
  }
  
  def gameResultInit(): GameResult = GameResult(0,0,false)  
  def betInit(user: User, game: Game): Bet = Bet(None, 0, gameResultInit, game.id.getOrElse(-1), user.id.getOrElse(-1)) 
  
  def filterSettings() = FilterSettings("all","all","all")
  
  /**
   * admins had instructions!
   */
  def userInit(user: User, isAdmin: Boolean, isRegistrant: Boolean, registeringUser: Option[Long]): User = {
	    val (u,t) = randomGravatarUrl(user.email)
	    val sendEmail = true
      User(None, user.username, user.firstName, user.lastName, user.institute, user.showName, user.email, user.passwordHash, isAdmin,  isRegistrant, sendEmail, isAdmin, true, 0, 0, u, t, registeringUser, false, filterSettings() )
  }

  def userFromUPE(username: String, password: String, firstName: String, lastName: String, email: String, registeringUser: Option[Long]): User = {
	    val (u,t) = randomGravatarUrl(email)
	    val sendEmail = true
      User(None, username, firstName, lastName, "", false, email, encrypt(password), false, false, sendEmail, false, true, 0, 0, u, t, registeringUser, false, filterSettings() )
  }
 
  def toBetLog(user: User, game: Game, betOld: Bet, betNew: Bet, time: OffsetDateTime, comment: String): BetLog = {
      BetLog(None, user.id.getOrElse(-1), game.id.getOrElse(-1), game.serverStart, betOld.id.getOrElse(-1), betOld.result.goalsTeam1, betNew.result.goalsTeam1, betOld.result.goalsTeam2, betNew.result.goalsTeam2, time, comment)
  }
 
  /**
   * @param viewTimeToStart: how much time before gamestart the play is viewable
   * 
   * its viewable AFTER the time!!!
   */
  def viewableTime(gameStart: OffsetDateTime, currentTime: OffsetDateTime, viewTimeToStart: Int): Boolean = {
       val viewOpen = gameStart.minusMinutes(viewTimeToStart)
       val open = currentTime.isAfter(viewOpen)
       open
  }
  
  
  /***
   * @param closingMinutesToGame: how much time before game start the bet is changeeable 
   * 
   */
   def gameOpen(gameStart: OffsetDateTime, currentTime: OffsetDateTime, closingMinutesToGame: Int): Boolean = {
       currentTime.isBefore(gameClosingTime(gameStart, closingMinutesToGame))
  }
  
  def gameClosingTime(gameStart: OffsetDateTime, closingMinutesToGame: Int): OffsetDateTime = {
      gameStart.minusMinutes(closingMinutesToGame)
  }
  
  def viewable(viewingUserId: Long, betUserId: Long, gameStart: OffsetDateTime, currentTime: OffsetDateTime, viewTimeToStart: Int): Boolean = {
       val result = if(viewingUserId == betUserId){
         true
       }else{
         viewableTime(gameStart, currentTime, viewTimeToStart)
       }
       domainhelperLogger.trace(s"viewable: $result $viewingUserId $betUserId ${TimeHelper.log(gameStart)} ${TimeHelper.log(currentTime)} $viewTimeToStart")
       result
  }
  
}
