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

   def usersWithSpecialBetsAndRank()(implicit s: Session): Seq[(User,SpecialBet,Int,Int)] = {
       val us = for{
          (u,s) <- users.innerJoin(specialbets).on(_.id === _.userId) 
       } yield (u,s)
       val sorted = us.list.sortBy(_._1.totalPoints).reverse
       val points = sorted.map(_._1.totalPoints) 
       val ranks = PointsCalculator.pointsToRanks(points)
       sorted.zip(ranks).map{ case ((u,s),r) =>
          (u,s,u.totalPoints,r)
       }
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
   
   def getTeamByName(teamName: String)(implicit s: Session): String \/ Team = {
       teams.filter(_.name === teamName).firstOption.map{ \/-(_) }.getOrElse{ -\/(s"team not found by name: $teamName") }    
   }
   
   def isAdmin(settingUser: User): Validation[String,User] = {
       if(settingUser.isAdmin) Success(settingUser) else Failure(s"only admin can make these changes")
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
              case (t1@Team(Some(t1id),_,_), t2@Team(Some(t2id),_,_), l@GameLevel(Some(lid),_,_,_,_), _) => {
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
          isGameOpen(dbGame.start, currentTime: DateTime, gameDuration*5).fold( 
            err => -\/("game will start in 5x90 minutes no more changes! "+err),
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
            isGameOpen(dbGame.start, currentTime: DateTime, -gameDuration).fold( //game is open until start + duration => points not settable yet
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
   def calculatePoints(specialBetResult: Option[SpecialBet], submittingUser: User)(implicit s: Session): String \/ Boolean = {
        if(submittingUser.isAdmin){
           s.withTransaction {
              updateBetsWithPoints()
              updateUsersPoints(specialBetResult)
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
   def updateUsersPoints(specialBetResult: Option[SpecialBet])(implicit s: Session): Boolean = {
	        users.list.foreach{ user =>
	            val points = for{
	              b <- bets if(b.userId === user.id)
	            } yield {
	              b.points
	            }
	            val p = points.list.sum 
	            val specialPoints = calculateSpecialPointsForUser(user, specialBetResult).getOrElse(0)
	            val userWithPoints = user.copy(points=p, pointsSpecialBet=specialPoints)      
	            users.filter(_.id === userWithPoints.id).update(userWithPoints)
	        }
            true       
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

