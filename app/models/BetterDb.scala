package models

import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime
import play.api.db.DB

import scalaz.{\/,-\/,\/-,Validation,ValidationNel,Success,Failure}
import scalaz.syntax.apply._ 



/**
 * message types for games
 */
sealed trait GameUpdate
case object SetResult extends GameUpdate
case object ChangeDetails extends GameUpdate
case object NewGame extends GameUpdate

case class UpdatePoints(session: Session)


object BetterDb {
   import BetterTables._
  
   def withT[A,B](f: => String \/ B)(implicit s: Session): String \/ B = {
       s.withTransaction{
		    try{
			   f
            }catch{
            case e: Exception => {
              s.rollback()
              -\/(e.getMessage)
            }
          }
	   }
   } 
  
   def allTeams()(implicit s: Session): Seq[Team] = {
       teams.list
   }
    
   def allPlayersWithTeam()(implicit s: Session): Seq[(Player,Team)] = {
       val pt = for{
         (p,t) <- players.innerJoin(teams).on(_.teamId === _.id)
       } yield (p,t)
       pt.list
   }
     
   def joinGamesTeamsLevels() = {
       games.innerJoin(teams).on(_.team1Id === _.id).innerJoin(teams).on(_._1.team2Id === _.id).innerJoin(levels).on(_._1._1.levelId === _.id) 
   }
   
   def allLevels()(implicit s: Session): Seq[GameLevel] = {
       levels.list
   }
   
   def allUsers()(implicit s: Session): Seq[User] = {
       users.list
   }
   
   def usersWithSpecialBetsAndRank()(implicit s: Session): Seq[(User,SpecialBets,Int)] = {
       val ust = for{
          ((u,s),t) <- users.join(specialbetsuser).on(_.id === _.userId).join(specialbetstore).on(_._2.spId === _.id)
       } yield (u,s,t)
	   val perUser = ust.list.groupBy{ case(u,s,t) => u }.mapValues{ usts => SpecialBets(usts.map{ case(u,s,t) => (t,s)})}
       val sorted = perUser.toList.sortBy(_._1.totalPoints).reverse
       val points = sorted.map(_._1.totalPoints) 
       val ranks = PointsCalculator.pointsToRanks(points)
       sorted.zip(ranks).map{ case ((u,sp),r) => (u,sp,r) }
   }
 
   def allGamesWithTeams()(implicit s: Session): Seq[GameWithTeams] = {
       val gtt = (for{
         (((g, t1), t2),l) <- joinGamesTeamsLevels() 
       } yield {
         (g, t1, t2,l)
       })   
       gtt.list.map{ case(g,t1,t2,l) => GameWithTeams(g,t1,t2,l) }
   }
  
   def insertOrUpdateLevelByNr(level: GameLevel, submittingUser: User)(implicit s: Session): String \/ GameLevel = {
       if(!submittingUser.isAdmin){
          return -\/("only admin user can change levels")
       }
       val l = levelByNr(level.level).map{ l =>
           levels.filter(_.id === level.id).update(l)
           l
       }.getOrElse{
          val levelId = (levels returning levels.map(_.id)) += level
          level.copy(id=Some(levelId))
       }
       \/-(l)
   }    
   
   def levelByNr(levelNr: Int)(implicit s: Session): String \/ GameLevel = {
       levels.filter(_.level === levelNr).firstOption.map{ \/-(_) }.getOrElse{ -\/(s"level not found by nr: $levelNr") }
   }
   
   def insertOrUpdateTeamByName(team: Team, submittingUser: User)(implicit s: Session): String \/ Team = {
       if(!submittingUser.isAdmin){
          return -\/("only admin users can change teams")
       }
       val t = getTeamByName(team.name).map{ t =>
          teams.filter(_.id === team.id).update(t)
          t
       }.getOrElse{
          val teamId = (teams returning teams.map(_.id)) += team
          team.copy(id=Some(teamId))
       }
       \/-(t)
   }
   
   def getGameByNr(gameNr: Int)(implicit s: Session): String \/ GameWithTeams = {
       val gtt = (for{
         (((g, t1), t2),l) <- joinGamesTeamsLevels() if g.nr === gameNr
       } yield {
         (g, t1, t2,l)
       })   
       gtt.firstOption.map{ case(g,t1,t2,l) => \/-(GameWithTeams(g,t1,t2,l)) }.getOrElse( -\/(s"game nr $gameNr not found"))
   }
   
   def getTeamByName(teamName: String)(implicit s: Session): String \/ Team = {
       teams.filter(_.name === teamName).firstOption.map{ \/-(_) }.getOrElse{ -\/(s"team not found by name: $teamName") }    
   }
   
   def isAdmin(settingUser: User): Validation[String,User] = {
       if(settingUser.isAdmin) Success(settingUser) else Failure(s"only admin can make these changes")
   }

   def insertSpecialBetInStore(specialBetT: SpecialBetT)(implicit s: Session): String \/ SpecialBetT = {
       val spid = (specialbetstore returning specialbetstore.map(_.id)) += specialBetT
       val sp = specialBetT.copy(id=Some(spid))
       \/-(sp)
   }
   
   def validSPU(sp: SpecialBetByUser, currentTime: DateTime, closingMinutesToGame: Int, submittingUser: User)(implicit s: Session): ValidationNel[String,String] = {
       val v = startOfGames().map{ startTime => 
           isGameOpen(startTime, currentTime, closingMinutesToGame)
       }.getOrElse( Failure("no games yet") ).toValidationNel |@| compareIds(submittingUser.id.getOrElse(-1), sp.userId, "user ids").toValidationNel

       v{ case(time, ids) => Seq(time, ids).mkString("\n") }
   }

   /**
   * API
   */
   def updateSpecialBetForUser(sp: SpecialBetByUser, currentTime: DateTime, closingMinutesToGame: Int, submittingUser: User)(implicit s: Session): String \/ SpecialBetByUser = {
       validSPU(sp, currentTime, closingMinutesToGame, submittingUser).fold(
         err => -\/(err.list.mkString("\n")),
         succ => updateSPU(sp)
       )
   }  
  
   //TODO: maybe update can be done in one query?
   //internal
   def updateSPU(sp: SpecialBetByUser)(implicit s: Session): String \/ SpecialBetByUser = {
       withT{
	      specialbetsuser.filter(_.id === sp.id).firstOption.map{ spdb =>
             val updated = sp.copy(points=spdb.points)
             specialbetsuser.filter(_.id === sp.id).update(updated)
             \/-(updated)
          }.getOrElse(-\/(s"no special bet found with ${sp.id}"))
	   }
   }
 
   def getSpecialBetsSPUForUser(user: User)(implicit s: Session): Seq[SpecialBetByUser] = {
       specialbetsuser.filter(_.userId === user.id).list
   }

   /**
    * This is for text import   game details | team1name | team2name | levelNr (| == tab)
    * ids for teams and levels and results are ignored, taken from db and amended in game object
    * todo: add updatingUser
    *
    */
   def insertGame(game: Game, team1Name: String, team2Name: String, levelNr: Int, settingUser: User)(implicit s: Session): String \/ GameWithTeams = {

       val result = ( getTeamByName(team1Name).validation.toValidationNel |@| getTeamByName(team2Name).validation.toValidationNel |@| 
                      levelByNr(levelNr).validation.toValidationNel |@|
                      isAdmin(settingUser).toValidationNel
             ){
           case (team1, team2, level, u) => (team1, team2, level, u)
        }
        result.fold(
            err => -\/(err.list.mkString("\n")),
            succ => succ match {
              case (t1@Team(Some(t1id),_,_,_), t2@Team(Some(t2id),_,_,_), l@GameLevel(Some(lid),_,_,_,_), _) => {
                 val gameWithTeamsAndLevel = game.copy(team1id=t1id, team2id=t2id, levelId=lid, result=DomainHelper.gameResultInit)
                 val gameId = (games returning games.map(_.id)) += gameWithTeamsAndLevel
                 val dbgame = gameWithTeamsAndLevel.copy(id=Some(gameId))
                 val gwt = GameWithTeams(dbgame, t1, t2, l) 
                 \/-(gwt)
              }
              case _ => -\/("problem with ids of team1, team2 or level")
            }
        )   
   }
    
  // def userWithSpecialBet(condition: User => Boolean): Boolean = {
  // 
  // 
  // }	
	
   def userWithSpecialBet(userId: Long)(implicit s: Session):  String \/ (User, Seq[SpecialBetByUser]) = {
       val us = for{
         (u,s) <- users.join(specialbetsuser).on(_.id === _.userId) if u.id === userId
       }yield (u,s)
	   val usList = us.list
	   if(usList.size > 0){
		   val user = usList(0)._1
		   val bets = usList.map{ case(u,b) => b}
	       \/-((user,bets))
		}else{
		   -\/(s"could not find user for id $userId")  
		}
   }  
   
   /**
    * todo: test
    */
   def userWithSpecialBet(username: String)(implicit s: Session):  String \/ (User, Seq[(SpecialBetT,SpecialBetByUser)]) = {
       val us = for{
         ((u,b),t) <- users.join(specialbetsuser).on(_.id === _.userId).join(specialbetstore).on(_._2.spId === _.id ) if u.username === username
       }yield (u,b,t)
	   val usList = us.list
	   if(usList.size > 0){
		   val user = usList(0)._1
		   val bets = usList.map{ case(u,b,t) => (t,b)}.sortBy(_._1.id)
	       \/-((user,bets))
		}else{
		   -\/(s"could not find user with $username")  
		}
   }  
   
   
   def userById(userId: Long)(implicit s: Session):  String \/ User = {
       users.filter(u => u.id === userId).firstOption.map(\/-(_)).getOrElse(-\/(s"could not find user with id $userId"))     
   }
   
   def authenticate(username: String, inputPassword: String)(implicit s: Session):  String \/ User = {
       users.filter(u => u.username === username && u.isRegistered).firstOption.map{ u => 
         if(new org.jasypt.util.password.StrongPasswordEncryptor().checkPassword(inputPassword, u.passwordHash)){
            \/-(u)
         }else{
            -\/(s"wrong password or user not found")
         }       
       }.getOrElse(-\/(s"wrong password or user not found"))
   }  
     
   /**
    * The ids are declared as Option[Long]
    * this creates problems in filter clauses for joins (i.e WHERE)
    * 
    */
   def opId(id: Option[Long]): Long = {
       id.map(i => i).getOrElse(-1)     
   }
   
   /***
    * UI 1
    * 
    * 
    */   
   def betsWitUsersForGame(game: Game)(implicit s: Session): Seq[(Bet,User)] = {
       val bu = (for{
         (b, u) <- bets.innerJoin(users).on(_.userId === _.id) if b.gameId === game.id
       } yield {
         (b,u)
       }) 
       bu.list
   }
   
   /**
    * UI 2
    * 
    */
   def gamesWithBetForUser(user: User)(implicit s: Session): Seq[(GameWithTeams,Bet)] = {
       val gtt = (for{
         ((((g, t1), t2),l),b) <- joinGamesTeamsLevels().innerJoin(bets).on(_._1._1._1.id === _.gameId) if b.userId === user.id
       } yield {
         (g, t1, t2,l,b)
       })   
       gtt.list.map{ case(g,t1,t2,l,b) => (GameWithTeams(g,t1,t2,l),b) }
   }
   
   /***
    * all times are stored in local time
    * This happens at import where the timezone has to be specified
    */
   def isGameOpen(gameTimeStart: DateTime, currentTime: DateTime, closingMinutesToGame: Int): Validation[String,String] = {
       val gameClosing = gameTimeStart.minusMinutes(closingMinutesToGame)  
       val open = currentTime.isBefore(gameClosing)
       if(open) Success("valid time") else Failure(s"game closed since ${JodaHelper.compareTimeHuman(gameClosing, currentTime)}")
   }
   

   /***
    * 
    * 
    */
   def startOfGames()(implicit s: Session): Option[DateTime] = {
        games.sortBy(_.serverStart.asc).firstOption.map{ firstGame =>
          firstGame.serverStart
       }
   }
   
   def closingTimeSpecialBet(closingMinutesToGame: Int)(implicit s: Session): Option[DateTime] = {
      startOfGames.map{ s => s.minusMinutes(closingMinutesToGame) }
   }
   

   def betWithGameWithTeamsAndUser(bet: Bet)(implicit s: Session): String \/ (Bet,GameWithTeams,User) = {
       val bg = for{
        (((((g,t1),t2),l),b),u) <- joinGamesTeamsLevels().join(bets).on(_._1._1._1.id === _.gameId).join(users).on(_._2.userId === _.id) if b.id === bet.id
       } yield (g, t1, t2, l, b, u)
       bg.firstOption.map{ case(g,t1,t2,l,b,u) =>
          \/-(b, GameWithTeams(g,t1,t2,l),u)  
       }.getOrElse( -\/(s"could not find bet in database $bet"))
   }
   
   /**
    * current time should come from date time provider
    * need DI?
    * I reload the bet to make sure its not tampered with gameid or userid
    * 
    * The successfull return value is for the messageing functionality (log, ticker, facebook etc...)
    * 
    */
   def updateBetResult(bet: Bet, submittingUser: User, currentTime: DateTime, closingMinutesToGame: Int)(implicit s: Session): String \/ (GameWithTeams,Bet,Bet) = {
       betWithGameWithTeamsAndUser(bet).flatMap{ case(dbBet, dbgame, dbuser) =>
             compareBet(dbuser.canBet, dbuser.id.getOrElse(-1), submittingUser.id.getOrElse(-1), dbBet.gameId, bet.gameId, dbgame.game.serverStart, currentTime, closingMinutesToGame).fold(
                  err => {
		 		     val invalid = GameResult(-1,-1,true)
		 		     val log = DomainHelper.toBetLog(dbuser, dbgame.game, dbBet, dbBet.copy(result=invalid), currentTime)
		 			 betlogs.insert(log)
					  -\/(err.list.mkString("\n"))
				  },
                  succ => {
                    val result = bet.result.copy(isSet=true)
  	                val updatedBet = dbBet.copy(result=result)
	                bets.filter(_.id === updatedBet.id).update(updatedBet)
					val log = DomainHelper.toBetLog(dbuser, dbgame.game, dbBet, updatedBet, currentTime)
					betlogs.insert(log)
                    \/-(dbgame, dbBet, updatedBet)
             })
       }
   }
   
   
   def allBetLogs()(implicit s: Session): Seq[BetLog] = {
       betlogs.list
   }
   
   def compareIds(original: Long, proposed: Long, idName: String): Validation[String,String] = {
       if(original == proposed) Success("valid id") else Failure(s"$idName differ $original $proposed")
   }     
   
   def canBet(cb: Boolean): Validation[String,String] = {
       if(cb) Success("can bet") else Failure(s"user has not paid, no dice")     
   }
   
   def compareBet(cb: Boolean, userId: Long, betUserId: Long, gameId: Long, betGameId: Long, gameTimeStart: DateTime, currentTime: DateTime, closingMinutesToGame: Int): ValidationNel[String,String] = {
       (compareIds(userId, betUserId, "user ids").toValidationNel |@| 
           compareIds(gameId, betGameId, "game ids").toValidationNel |@| 
           isGameOpen(gameTimeStart, currentTime, closingMinutesToGame).toValidationNel |@|
           canBet(cb).toValidationNel ){
         case(u,g,t,c) => Seq(u,g,t,c).mkString("\n")
       }
   }
   
   
   def checkSpecial(cb: Boolean, userId: Long, betUserId: Long): ValidationNel[String,String] = {
       (canBet(cb).toValidationNel |@| compareIds(userId, betUserId, "user ids").toValidationNel){
         case(c,u) => Seq(c,u).mkString("\n")
       }    
   }
   

   /**
    * this should only be called immediately after game creation if there has been an error!
    * not good for users to have team changes!
    * 
    */
   def updateGameDetails(game: Game, submittingUser: User, currentTime: DateTime, gameDuration: Int)(implicit s: Session): String \/ (Game,GameUpdate) = {
       if(! submittingUser.isAdmin){
         return -\/("must be admin to change game details")
       }
       games.filter(_.id === game.id).firstOption.map{ dbGame =>
          isGameOpen(dbGame.serverStart, currentTime: DateTime, gameDuration*5).fold( 
            err => -\/("game will start in 5x90 minutes no more changes! "+err),
            succ =>  {//game open can not set points but can change teams and start time
                     val gameWithTeams = dbGame.copy(team1id=game.team1id, team2id=game.team2id, localStart=game.localStart, localtz=game.localtz, serverStart=game.serverStart, servertz=game.servertz, venue=game.venue)
                     games.filter(_.id === gameWithTeams.id).update(gameWithTeams)
                     \/-(gameWithTeams, ChangeDetails)
            }         
          )
       }.getOrElse(-\/(s"could not find game in database $game"))      
   }  
     
   /**
    * 
    * from ui with correct foreign keys set by ui
    * contoller should initiate points calculation
    * 
    */
   def updateGameResults(game: Game, submittingUser: User, currentTime: DateTime, gameDuration: Int)(implicit s: Session): String \/ (Game,GameUpdate) = {
       if(! submittingUser.isAdmin){
         return -\/("must be admin to change game results")
       }
       games.filter(_.id === game.id).firstOption.map{ dbGame =>
            isGameOpen(dbGame.serverStart, currentTime: DateTime, -gameDuration).fold( //game is open until start + duration => points not settable yet
                err => {//game closed can set points but not change teams anymore
	                     val result = game.result.copy(isSet=true)
	                     val gameWithResult = dbGame.copy(result=result)
	                     games.filter(_.id === gameWithResult.id).update(gameWithResult) 
	                     \/-(gameWithResult, SetResult)
                },
                succ => -\/("game is still not finished")            
            )
       }.getOrElse(-\/(s"could not find game in database $game"))
   }
   
   /**
    * one of 2 methods for creating users:
    * this one creates the user directly
    * sets up all bets including special bets
    * TODO: only isResitserging users have to check registeringUser!!
    * 
    *  the other one is token based, but I don't know if i manage to create this workflow:
    * 
    *  user registers with e-mail, token is generated for his id
    *  user klicks link with token e-mail, opens web, => user signs on..
    *  
    */
   def insertUser(taintedUser: User, isAdmin: Boolean, isRegistering: Boolean, registeringUser: Option[Long])(implicit s: Session): String \/ User = {
       withT{ 
           val initUser = DomainHelper.userInit(taintedUser, isAdmin, isRegistering, registeringUser)
           val userId = (users returning users.map(_.id)) += initUser
           val userWithId = initUser.copy(id=Some(userId))
           users.filter(_.id === userId).firstOption.map{ user =>
			  createSpecialBetsForUser(userWithId)
              createBetsForGamesForUser(userWithId)
           }   
		   \/-(userWithId)
		}       
   }
   
   def updateUserPassword(userId: Long, passwordHash: String)(implicit s: Session): String \/ User = {
	   withT{
          users.filter(u => u.id === userId).firstOption.map{ user => 
		      val updatedUser = user.copy(passwordHash=passwordHash)   
			  users.filter(u => u.id === userId).update(updatedUser)
			  \/-(updatedUser)		  
		  }.getOrElse(-\/(s"user not found $userId"))
       }
   }
   
   def updateUserDetails(userId: Long, firstName: String, lastName: String, email: String, icontype: String)(implicit s: Session): String \/ User = {
	   withT{
          users.filter(u => u.id === userId).firstOption.map{ user => 
			  val (u,t) = DomainHelper.gravatarUrl(email, icontype)
		      val updatedUser = user.copy(firstName=firstName, lastName=lastName, email=email, iconurl=u, icontype=t)   
			  users.filter(u => u.id === userId).update(updatedUser)
			  \/-(updatedUser)		  
		  }.getOrElse(-\/(s"user not found $userId"))
       }
   }
   
   def updateUserHadInstructions(user: User)(implicit s: Session): String \/ String = {
       withT{
		   val withInstructions = user.copy(hadInstructions = true)
		   users.filter(u => u.id === user.id).update(withInstructions)
		   \/-("first special bet! Excellent")
	   }   
   }   
  
// not neccessary? using getUser(usernmae)   
//   def usernameExists(username: String): Boolean = {
//       users.list.filter(u.username === username).headOption.map(b => true).getOrElse(false)
//   }
   
   def createBetsForGamesForAllUsers(submittingUser: User)(implicit s: Session): String \/ String = {
       if(submittingUser.isAdmin){ 
          users.list.foreach{ u =>
             createBetsForGamesForUser(u)
          }
          \/-("created bets for all users")
       }else{
         -\/("only admin users can create bets")
       }
   }
   
   def createBetsForGamesForUser(user: User)(implicit s: Session){
       s.withTransaction { //this is something slick is misssing: nested transactions and joining open transactions 
          user.id.map{ uid => 
	          val gamesWithoutBets = gamesWithoutBetsForUser(user)
	          gamesWithoutBets.flatMap{ g => g.id }.foreach{ gid =>
	             createBetForGameAndUser(gid, uid)
	          }
          }
       }
   }
   
   def createBetForGameAndUser(gameId: Long, userId: Long)(implicit s: Session){
       val bet = Bet(None, 0, GameResult(0,0,false), gameId, userId)
       bets.insert(bet)
   }
   
   /**
    * I need only to filter for games without bets, therfore the
    * row hack with ? is not necessary, but if it is:
    * http://stackoverflow.com/questions/14990365/slick-left-outer-join-fetching-whole-joined-row-as-option
    * 
    * 
    */
   def gamesWithoutBetsForUser(user: User)(implicit s: Session): Seq[Game] = {
       val allGamesWithBetsForUser = for{
         (g, b) <- games.join(bets).on(_.id === _.gameId) if b.userId === user.id
       } yield (g.id)
       
       val allGamesWithoutBetsForUser = for{
         g <- games if g.id notIn allGamesWithBetsForUser         
       } yield g
       
       //does not compile
        //  val allGamesWithoutBetsForUser = games.filterNot(_.id inSet(allGamesWithBetsForUser) )
       
       allGamesWithoutBetsForUser.list
       
   }

   /**
    * if something was wrong with the game. we set the results for this game to isSet = false
    * This does not delete the set result, but excludes them from accruing points for the user
    * 
    */
   def invalidateGame(game: Game, submittingUser: User)(implicit s: Session): String \/ String = {
       if(submittingUser.isAdmin){
          s.withTransaction{
             games.filter(_.id === game.id).map(_.isSet).update(false)
             \/-(s"invalidated $game")
          }    
       }else -\/(s"only admin user can invalidate games")
   }
   
   /**
    * 
    * There should be one actor executing this function to prevent race conditions
    * 
    * TODO://add special bet result somewhere
    * specialBetResult is set at runtime at end of game by admin.
    * Has to recalculate everything
    * 
    * has return value for return result and free actor
    * 
    **/
   def calculatePoints(submittingUser: User)(implicit s: Session): String \/ Boolean = {
        if(submittingUser.isAdmin){
           s.withTransaction {
              updateBetsWithPoints()
              updateUsersPoints()
              \/-(true)
           }
        }else{
          -\/("only admin can calculate points")
        }
   }
   
   /***
    * updates all bets which have a valid result with points depending on game results and level
    * also using invalid games (i.e. result not set because after invalidating the game I want to keep the 
    * bet result still set and valid (just in case somebody messed up). But the points in the bet will be set to 0 if
    * the game is invalid
    *
    */
   def updateBetsWithPoints()(implicit s: Session): Boolean = {
       val gamesLevelBets = for{
           ((g,l),b) <- games.join(levels).on(_.levelId === _.id).join(bets).on(_._1.id === _.gameId) if b.isSet
        } yield {
           (g,l,b)
        }
        gamesLevelBets.list.foreach{ case (g,l,b) =>
              val points = PointsCalculator.calculatePoints(b, g, l) 
              val betWithPoints = b.copy(points=points)
              bets.filter(_.id === betWithPoints.id).update(betWithPoints)
        }
        true
   }
   
   /***
    * updates the tally of the bet points in the user 
    * 
    */
   def updateUsersPoints()(implicit s: Session): Boolean = {
	        users.list.foreach{ user =>
	            val points = for{
	              b <- bets if(b.userId === user.id)
	            } yield {
	              b.points
	            }
	            val p = points.list.sum 
	      //      val specialPoints = calculateSpecialPointsForUser(user).getOrElse(0)
	            val userWithPoints = user.copy(points=p, pointsSpecialBet=0)      
	            users.filter(_.id === userWithPoints.id).update(userWithPoints)
	        }
            true       
   }
   
   
   
   /**
    * calculates but does not set special points for user
    * 
    */
   def calculateSpecialPointsForUser(user: User)(implicit s: Session): Option[Int] = {
       ???
   }
   
   def insertPlayer(player: Player, teamName: String, submittingUser: User)(implicit s: Session): String \/ Player = {
       if(! submittingUser.isAdmin){
         return -\/("must be admin to insert players")
       }
       getTeamByName(teamName).map{ team =>
           val playerWithTeamId = player.copy(teamId=team.id.get)
           players.insert(playerWithTeamId)
           playerWithTeamId
       }
   }
   
   def specialBetTemplates()(implicit s: Session): Seq[SpecialBetT] = {
       specialbetstore.list
   }
   
   def createSpecialBetsForUser(user: User)(implicit s: Session){
       specialbetstore.list.foreach{ sp => 
	      val us = SpecialBetByUser(None, user.id.get, sp.id.get, "", 0)
	      specialbetsuser.insert(us)
	   }
   }
   
   //the optimum would be a left outer join with an and within the on clause!, but this is too complicated becasue there is
   //no option[row].? as return type possible for slick yet
   //(t, b) <- specialbetstore.join(specialbetsuser).on( (temp,bet) => temp.id === bet.spId  && bet.userId === user.id
   //
   //we generate for each user the correct bets when he signs in
   //http://stackoverflow.com/questions/20386593/slick-left-right-outer-joins-with-option
   def getSpecialBetsForUser(user: User)(implicit s: Session): Seq[(SpecialBetT,SpecialBetByUser)] = {
	   val tbs = for {
		   (t,b) <- specialbetstore.join(specialbetsuser).on( (temp,bet) => temp.id === bet.spId ) if b.userId === user.id
	   } yield (t,b)	
	   tbs.list
	}
    
    def specialBetsByTemplate(id: Long)(implicit s: Session): String \/ (SpecialBetT,Seq[SpecialBetByUser]) = {
		specialbetstore.filter(_.id === id).firstOption.map{ template => 
		    val sp = specialbetsuser.filter(s => s.spId === template.id).list
		    \/-((template,sp))
		}.getOrElse( -\/(s"special bet $id not found"))
	}
   
   
}

