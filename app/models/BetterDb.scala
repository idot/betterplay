package models

import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime
import play.api.db.DB

import scalaz.{\/,-\/,\/-,Validation,ValidationNel,Success,Failure}
import scalaz.syntax.apply._ //|@|

/**
 * message types for games
 */
sealed trait GameUpdate
case object SetResult extends GameUpdate
case object ChangeDetails extends GameUpdate
case object NewGame extends GameUpdate


object BetterDb {
   import BetterTables._
  
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
   
   def allGamesWithTeams()(implicit s: Session): Seq[GameWithTeams] = {
       val gtt = (for{
         (((g, t1), t2),l) <- joinGamesTeamsLevels() 
       } yield {
         (g, t1, t2,l)
       })   
       gtt.list.map{ case(g,t1,t2,l) => GameWithTeams(g,t1,t2,l) }
   }
  
   def insertOrUpdateLevelByNr(level: GameLevel)(implicit s: Session): GameLevel = {
       levelByNr(level.level).map{ l =>
           levels.filter(_.id === level.id).update(l)
           l
       }.getOrElse{
          levels.insert(level)
          level
       }
   }    
   
   def levelByNr(levelNr: Int)(implicit s: Session): String \/ GameLevel = {
       levels.filter(_.level === levelNr).firstOption.map{ \/-(_) }.getOrElse{ -\/(s"level not found by nr: $levelNr") }
   }
   
   def insertOrUpdateTeamByName(team: Team)(implicit s: Session): String = {
       getTeamByName(team.name).map{ t =>
          teams.filter(_.id === team.id).update(t)
          s"team updated: $team"
       }.getOrElse{
         teams.insert(team)
          s"team inserted: $team"
       }
   }
   
   def getTeamByName(teamName: String)(implicit s: Session): String \/ Team = {
       teams.filter(_.name === teamName).firstOption.map{ \/-(_) }.getOrElse{ -\/(s"team not found by name: $teamName") }    
   }
   
      
   /**
    * This is for text import   game details | team1name | team2name | levelNr (| == tab)
    * ids for teams and levels and results are ignored, taken from db and amended in game object
    * 
    */
   def insertGame(game: Game, team1Name: String, team2Name: String, levelNr: Int)(implicit s: Session): String \/ String = {

       val result = ( getTeamByName(team1Name).validation.toValidationNel |@| getTeamByName(team2Name).validation.toValidationNel |@| levelByNr(levelNr).validation.toValidationNel ){
           case (team1, team2, level) => (team1.id, team2.id, level.id)
        }
        result.fold(
            err => -\/(err.list.mkString("\n")),
            succ => succ match {
              case (Some(t1), Some(t2), Some(l)) => {
                 val gamesWithTeamsAndLevel = game.copy(team1id=t1, team2id=t2, levelId=l, result=DomainHelper.resultInit)
                 games.insert(gamesWithTeamsAndLevel)
                 \/-(s"inserted game $gamesWithTeamsAndLevel")
              }
              case _ => -\/("problem with ids of team1, team2 or level")
            }
        )   
   }
   
   
   def userWithSpecialBet(userId: Long)(implicit s: Session):  String \/ (User,SpecialBet) = {
       val us = for{
         (u,s) <- users.join(specialbets).on(_.id === _.userId) if u.id === userId
       }yield (u,s)
       us.firstOption.map{ case(u,s) => \/-((u,s))}.getOrElse(-\/(s"could not find user for id $userId"))
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
         (b, u) <- bets.innerJoin(users).on(_.userId === _.id) if b.gameId === opId(game.id)
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
         ((((g, t1), t2),l),b) <- joinGamesTeamsLevels().innerJoin(bets).on(_._1._1._1.id === _.gameId) if b.userId === opId(user.id)
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
       games.sortBy(_.start.asc).firstOption.map{ firstGame =>
          firstGame.start
       }
   }
   
   def closingTimeSpecialBet(closingMinutesToGame: Int)(implicit s: Session): Option[DateTime] = {
      startOfGames.map{ s => s.minusMinutes(closingMinutesToGame) }
   }
   

   def betWithGameWithTeamsAndUser(bet: Bet)(implicit s: Session): String \/ (Bet,GameWithTeams,User) = {
       val bg = for{
        (((((g,t1),t2),l),b),u) <- joinGamesTeamsLevels().join(bets).on(_._1._1._1.id === _.gameId).join(users).on(_._2.userId === _.id) if b.id === opId(bet.id)
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
             compareBet(dbuser.canBet, dbuser.id.getOrElse(-1), submittingUser.id.getOrElse(-1), dbBet.gameId, bet.gameId, dbgame.game.start, currentTime, closingMinutesToGame).fold(
                  err => -\/(err.list.mkString("\n")),
                  succ => {
                    val result = bet.result.copy(isSet=true)
  	                val updatedBet = dbBet.copy(result=result)
	                bets.filter(_.id === updatedBet.id).update(updatedBet)
                    \/-(dbgame, dbBet, updatedBet)
             })
       }
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
    * 
    * checking if start of games, specialbet from user, user can bet
    * 
    * 
    */
   def updateSpecialBet(specialBet: SpecialBet, user: User, currentTime: DateTime, closingMinutesToGame: Int)(implicit s: Session): String \/ (SpecialBet,User) = {
       startOfGames().map{ startTime =>
          isGameOpen(startTime, currentTime, closingMinutesToGame).fold(
              err => -\/(err),
              succ => updateOpenSpecialbet(specialBet, user)      
          )}.getOrElse(-\/("no games yet"))
   }
   
   
   def updateOpenSpecialbet(specialBet: SpecialBet, submittingUser: User)(implicit s: Session): String \/ (SpecialBet,User) = {
        s.withTransaction{ 
                 userWithSpecialBet(specialBet.userId).flatMap{ case(udb, spdb) =>
                         checkSpecial(udb.canBet, opId(submittingUser.id), spdb.userId).fold(
                             err => -\/(err.list.mkString("\n")),
                             succ => {
                                  val nsb = specialBet.copy(isSet=true)
			                      specialbets.filter(_.id === nsb.id).update(nsb)
			                      val nu = udb.copy(hadInstructions=true)
			                      users.filter(_.id === nu.id).update(nu)
		                          \/-((nsb,nu))
                             }                         
                         )
                    }
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
          isGameOpen(game.start, currentTime: DateTime, gameDuration*5).fold( 
            err => -\/("game will start in 5x90 minutes no more changes!"),
            succ =>  {//game open can not set points but can change teams and start time
                     val gameWithTeams = dbGame.copy(team1id=game.team1id, team2id=game.team2id, start=game.start, venue=game.venue)
                     games.filter(_.id === gameWithTeams.id).update(gameWithTeams)
                     \/-(gameWithTeams, ChangeDetails)
            }         
          )
       }.getOrElse(-\/(s"could not find game in database $game"))      
   }  
     
   /**
    * 
    * from ui with correct foreign keys set by ui
    * ui should initiate points calculation
    * 
    */
   def updateGameResults(game: Game, submittingUser: User, currentTime: DateTime, gameDuration: Int)(implicit s: Session): String \/ (Game,GameUpdate) = {
       if(! submittingUser.isAdmin){
         return -\/("must be admin to change game results")
       }
       games.filter(_.id === game.id).firstOption.map{ dbGame =>
            isGameOpen(game.start, currentTime: DateTime, -gameDuration).fold( //game is open until start + duration => points not settable yet
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
    * sets up all bets including special bets
    * 
    */
   def insertUser(taintedUser: User, isAdmin: Boolean, isRegistering: Boolean, registeringUser: Option[Long])(implicit s: Session): String \/ User = {
      s.withTransaction{ 
           try{
              val initUser = DomainHelper.userInit(taintedUser, isAdmin, isRegistering, registeringUser)
              val userId = (users returning users.map(_.id)) += initUser
              val userWithId = initUser.copy(id=Some(userId))
              users.filter(_.id === userId).firstOption.map{ user =>
                 specialbets.insert(SpecialBet(None, None, None, None, None, None, None, None, false, userId ))
                 createBetsForGamesForUser(userWithId)
              }  
              \/-(userWithId)
           }catch{
            case e: Exception => {
              s.rollback()
              
              -\/(e.getMessage)
              
            }
          }
      }       
   }
   
   def createBetsForGamesForAllUsers()(implicit s: Session){
       users.list.foreach{ u =>
           createBetsForGamesForUser(u)
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
       val bet = Bet(None, 0, Result(0,0,false), gameId, userId)
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
   def invalidateBetsForGame(game: Game)(implicit s: Session){
       s.withTransaction{
          val invalidBets = for{
             b <- bets if b.gameId === game.id
          } yield b.isSet
          invalidBets.update(false)
       }    
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
   def calculatePoints(specialBetResult: Option[SpecialBet])(implicit s: Session): Boolean = {
      s.withTransaction {
        updateBetsWithPoints()
        updateUsersPoints(specialBetResult)
        true
      }
   }
   
   /***
    * updates all bets which have a valid result with points depending on game results and level
    * 
    */
   def updateBetsWithPoints()(implicit s: Session){
       val gamesLevelBets = for{
           ((g,l),b) <- games.join(levels).on(_.levelId === _.id).join(bets).on(_._1.id === _.gameId) if g.isSet && b.isSet
        } yield {
           (g,l,b)
        }
        gamesLevelBets.list.foreach{
          case (g,l,b) =>
            PointsCalculator.calculatePoints(b, g, l).foreach{ points => 
              val betWithPoints = b.copy(points=points)
              bets.filter(_.id === betWithPoints.id).update(betWithPoints) //correct long === Option[Long]???
            }
        }
   }
   
   /***
    * updates the tally of the bet points in the user 
    * 
    */
   def updateUsersPoints(specialBetResult: Option[SpecialBet])(implicit s: Session){
        users.list.foreach{ user =>
            val points = for{
              b <- bets if(b.userId === user.id)  //correct long === Option[Long]???
            } yield {
              b.points
            }
            val p = points.list.sum 
            val specialPoints = calculateSpecialPointsForUser(user, specialBetResult).getOrElse(0)
            val userWithPoints = user.copy(points=p, pointsSpecialBet=specialPoints)      
            users.filter(_.id === userWithPoints.id).update(userWithPoints)
        }
   }
   
   
   
   /**
    * calculates but does not set special points for user
    * 
    */
   def calculateSpecialPointsForUser(user: User, specialBetResult: Option[SpecialBet])(implicit s: Session): Option[Int] = {
       specialBetResult.map{ spr =>
            specialbets.filter(_.userId === user.id).firstOption.map{ spb =>   //correct long === Option[Long]???
                PointsCalculator.calculateSpecialBets(spb, spr)               
            }.getOrElse(0)
       }
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
   
}

