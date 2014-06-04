package models

import org.joda.time.DateTime


object DomainHelper {
  import org.jasypt.util.password.StrongPasswordEncryptor
  import scravatar._
  
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
      User(None, user.username, user.firstName, user.lastName, user.email, user.passwordHash, isAdmin, isRegistrant, isAdmin, true, true, 0, 0, u, t, registeringUser)
  }

  def userFromUPE(username: String, password: String, email: String, registeringUser: Option[Long]): User = {
	  val (u,t) = randomGravatarUrl(email)
      User(None, username, "", "", email, encrypt(password), false, false, false, true, true, 0, 0, u, t, registeringUser)
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
}



/***
 * hadinstructions === special bet was set
 */
case class User(id: Option[Long] = None, username: String, firstName: String, lastName: String, email: String, passwordHash: String,
	        isAdmin: Boolean, isRegistrant: Boolean, hadInstructions: Boolean, canBet: Boolean, isRegistered: Boolean,
			points: Int, pointsSpecialBet: Int, iconurl: String, icontype: String, registeredBy: Option[Long] ){
//  
//  	def password_=(in: String) = this.password_hash = encrypt(in)
//	
//	def authenticate(in: String): Boolean = new StrongPasswordEncryptor().checkPassword(in, password) 
//
//    private def encrypt(in: String): String = new StrongPasswordEncryptor().encryptPassword(in)
  
     def totalPoints(): Int = points + pointsSpecialBet
  
}


case class UserNoPw(id: Option[Long] = None, username: String, firstName: String, lastName: String, 
	      isAdmin: Boolean, isRegistrant: Boolean, hadInstructions: Boolean, canBet: Boolean, 
		  totalPoints: Int, pointsGames: Int, pointsSpecialBet: Int, iconurl: String, icontype: String, registeredBy: Option[Long]){
}
   
object UserNoPwC {
   def apply(user: User): UserNoPw = {
        UserNoPw(user.id, user.username, user.firstName, user.lastName, 
		user.isAdmin, user.isRegistrant, user.hadInstructions, user.canBet,
		user.totalPoints, user.points, user.pointsSpecialBet, user.iconurl, user.icontype, user.registeredBy)  
   }     
}      
         

case class SpecialBet(id: Option[Long], topScorer: Option[Long], mvp: Option[Long], winningTeam: Option[Long], semi1: Option[Long], semi2: Option[Long], semi3: Option[Long], semi4: Option[Long], isSet: Boolean, userId: Long){
  
  def semiIds(): Set[Long] = {
      Set(semi1, semi2, semi3, semi4).flatten 
  }
  
}

object SpecialBetType {
	val team = "team"
	val player = "player"
}

//TODO: convert specialbet 
case class SpecialBetByUser(id: Option[Long], userId: Long,  specialbetId: Long, targetId: Long, creationDate: DateTime, points: Int)
case class SpecialBetT(id: Option[Long], name: String, description: String, points: Int, closeDate: DateTime, itemType: String, resultId: Option[Long])




case class GameLevel(id: Option[Long] = None, name: String, pointsExact: Int, pointsTendency: Int, level: Int)//name: groups, quarter final, semi final, final

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
