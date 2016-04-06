package models



import javax.inject.{Singleton, Inject}

import org.joda.time.DateTime
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.driver.JdbcProfile

import scalaz.{\/,-\/,\/-,Validation,ValidationNel,Success,Failure}
import scalaz.syntax.apply._ 
import scala.concurrent.Future

import play.api.Logger

/**
 * message types for games
 */
sealed trait GameUpdate
case object SetResult extends GameUpdate
case object ChangeDetails extends GameUpdate
case object NewGame extends GameUpdate

//case class UpdatePoints(session: Session)


@Singleton()
class BetterDb @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends BetterTables with HasDatabaseConfigProvider[JdbcProfile] {
  //TODO: check if this is efficient or different ExecutionContext necessary
  import scala.concurrent.ExecutionContext.Implicits.global

  import driver.api._
  // def withT[A,B](f: => String \/ B): String \/ B = {
  //     s.withTransaction{
	//	    try{
	//		   f
  //          } catch {
  //          case e: Exception => {
	//		  Logger.error(e.getMessage)
  //            s.rollback()
 //             -\/(e.getMessage)
  //          }
  //        }
//	   }
 //  }


   def allTeams(): Future[Seq[Team]] = {
       db.run(teams.result)
   }

    def allPlayersWithTeam(): Future[Seq[(Player,Team)]] = {
         val pt = for{
           (p,t) <- players.join(teams).on(_.teamId === _.id)
         } yield (p,t)
         db.run(pt.result)
    }

    def joinGamesTeamsLevels() = {
         games.join(teams).on(_.team1Id === _.id).join(teams).on(_._1.team2Id === _.id).join(levels).on(_._1._1.levelId === _.id)
    }

     def allLevels(): Future[Seq[GameLevel]] = {
         db.run(levels.result)
     }

     def allUsers(): Future[Seq[User]] = {
         db.run(users.result)
     }

     def allUsersWithRank(): Future[Seq[(User,Int)]] = {
         val res = users.sortBy(u => (u.points + u.pointsSpecial).desc ).result.map{ sorted =>
            val points = sorted.map(_.totalPoints)
            val ranks = PointsCalculator.pointsToRanks(points)
            sorted.zip(ranks)
         }
         db.run(res)
     }
     
     def usersWithSpecialBetsAndRank(): Future[Seq[(User,SpecialBets,Int)]] = {
            val ust = (for {
                ((u, sp),t) <- users.join(specialbetsuser)
                .on(_.id === _.userId)
                .join(specialbetstore)
                .on(_._2.spId === _.id)
             } yield { 
                (u,sp,t)
             })
             
          
             val sp = ust.result.map{ usts => 
                   val betsPerUser = usts.groupBy(_._1).map{ case(u, ustsPerUser) =>
                     val ustt = ustsPerUser.unzip3
                     val spT = ustt._3
                     val spU = ustt._2
                     (u, SpecialBets(spT.zip(spU)))
                   }
                   val sorted = betsPerUser.toList.sortBy{ case(u,b) => u.totalPoints() }.reverse
                   val points = sorted.map{ case(u,b) => u.totalPoints }
                   val ranks = PointsCalculator.pointsToRanks(points)
                   sorted.zip(ranks).map{ case((u,b),p) => (u,b,p) }
             }
             db.run(sp)
     }
     
     def allGamesWithTeams(): Future[Seq[GameWithTeams]] = {
         val gtt = (for{
           (((g, t1), t2),l) <- joinGamesTeamsLevels()
         } yield {
           (g, t1, t2,l)
         })
         val li = gtt.result.map{ list => list.map{ case(g,t1,t2,l) => GameWithTeams(g,t1,t2,l) }}
         db.run(li)
     }
/* not clear if needed besides initialdata:
     def insertOrUpdateLevelByNr(level: GameLevel, submittingUser: User): Future[GameLevel] = {
         if(!submittingUser.isAdmin){
            return Future.failed(AccessViolationException("only admin user can change levels"))
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

  
     def levelByNr(levelNr: Int): Future[GameLevel] = {
         levels.filter(_.level === levelNr).headOption.getOrElse{ ItemNotFoundException(s"level not found by nr: $levelNr") }
     }


     def insertOrUpdateTeamByName(team: Team, submittingUser: User): Future[Team] = {
         if(!submittingUser.isAdmin){
            return Future.failed(AccessViolationException("only admin user can change teams"))
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
     
     def getTeamByName(teamName: String): String \/ Team = {
         teams.filter(_.name === teamName).firstOption.map{ \/-(_) }.getOrElse{ -\/(s"team not found by name: $teamName") }
     }
     */

     def getGameByNr(gameNr: Int): Future[GameWithTeams] = {
         val gtt = (for{
           (((g, t1), t2),l) <- joinGamesTeamsLevels() if g.nr === gameNr
         } yield {
           (g, t1, t2,l)
         })
         db.run(gtt.result.headOption)
            .flatMap{ gs =>
                 gs match {
                    case Some((g,t1,t2,l)) => Future{ GameWithTeams(g,t1,t2,l) }
                    case _ => Future.failed(ItemNotFoundException(s"game not found by nr: $gameNr"))
                }
            }    
     }

      
     def insertSpecialBetInStore(specialBetT: SpecialBetT): Future[SpecialBetT] = {
         db.run((specialbetstore returning specialbetstore.map(_.id)) += specialBetT).map{ spid =>
            specialBetT.copy(id=Some(spid))
         }
     }

     def isAdmin(settingUser: User): Validation[String,User] = {
         if(settingUser.isAdmin) Success(settingUser) else Failure(s"only admin can make these changes")
     }
     
     def validSPU(sp: SpecialBetByUser, currentTime: DateTime, closingMinutesToGame: Int, submittingUser: User): Future[ValidationNel[String,String]] = {
         val startedF = startOfGames().map{ OstartTime =>
             OstartTime.map{ startTime => isGameOpen(startTime, currentTime, closingMinutesToGame) }.getOrElse( Failure("no games yet") )
         }
         val ids = compareIds(submittingUser.id.getOrElse(-1), sp.userId, "user ids").toValidationNel
         startedF.map{ s  => ( s.toValidationNel |@| ids ){
             case(time, ids) => Seq(time, ids).mkString("\n")         
         }}
     }

     /**
     * API
     */
     def updateSpecialBetForUser(sp: SpecialBetByUser, currentTime: DateTime, closingMinutesToGame: Int, submittingUser: User): Future[String] = {
         validSPU(sp, currentTime, closingMinutesToGame, submittingUser).flatMap{ v =>
           v.fold(
               err => Future.failed(ValidationException(err.list.mkString("\n"))),
               succ => updateSPU(sp)
           )
         }
     }


     //TODO: TEST for exception when specialbet does not exist!
     def updateSPU(sp: SpecialBetByUser): Future[String] = {     
         val action = (specialbetsuser.filter(_.id === sp.id).map( c => c.prediction ).update( sp.prediction ).flatMap{ rowCount =>
             rowCount match {
               case 0 => DBIO.failed(new ItemNotFoundException(s"could not find specialbet with id ${sp.id}")) 
               case _ => DBIO.failed(new ItemNotFoundException(s"found multiple special best with id ${sp.id}"))
               case 1 => DBIO.successful(s"updated special bet with prediction ${sp.prediction}")
             }
         }).transactionally
         db.run(action)
     }

     def getSpecialBetsSPUForUser(user: User): Future[Seq[SpecialBetByUser]] = {
         db.run(specialbetsuser.filter(_.userId === user.id).result)
     }

     /**
      * This is for text import  and creation of game game details | team1name | team2name | levelNr (| == tab)
      * ids for teams and levels and results are ignored, taken from db and amended in game object
      * todo: add updatingUser
      *
      */
     /*
     def insertGame(game: Game, team1Name: String, team2Name: String, levelNr: Int, settingUser: User): Future[GameWithTeams] = {

         val result = ( getTeamByName(team1Name).validation.toValidationNel |@| getTeamByName(team2Name).validation.toValidationNel |@|
                        levelByNr(levelNr).validation.toValidationNel |@|
                        isAdmin(settingUser).toValidationNel
               ){
             case (team1, team2, level, u) => (team1, team2, level, u)
          }
          result.fold(
              err => Future.failed(ValidationException(err.list.mkString("\n"))),
              succ => succ match {
                case (t1@Team(Some(t1id),_,_,_), t2@Team(Some(t2id),_,_,_), l@GameLevel(Some(lid),_,_,_,_), _) => {
                   val gameNr = if(game.nr == 0) db.run(games.map(_.nr).max).first.map(m => m+1).getOrElse(0) else game.nr
                   val gameWithTeamsAndLevel = game.copy(team1id=t1id, team2id=t2id, levelId=lid, result=DomainHelper.gameResultInit, nr=gameNr)
                   val gameId = (games returning games.map(_.id)) += gameWithTeamsAndLevel
                   val dbgame = gameWithTeamsAndLevel.copy(id=Some(gameId))
                   val gwt = GameWithTeams(dbgame, t1, t2, l)
                   \/-(gwt)
                }
                case _ => -\/("problem with ids of team1, team2 or level")
              }
          )
    }*/

     /**
      * TODO: test
      */
     def userWithSpecialBet(userId: Long):  Future[(User, Seq[SpecialBetByUser])] = {
         val us = for{
           (u,s) <- users.join(specialbetsuser).on(_.id === _.userId) if u.id === userId
         }yield (u,s)
         db.run(us.result).flatMap{ ubs =>
           if(ubs.size > 0){
             val user = ubs(0)._1
             val bets = ubs.map{ case(u,b) => b } //TODO:.sortBy(b => b,id)
             Future{ (user, bets) }
          }else{
             Future.failed(ItemNotFoundException(s"could not find user with id $userId"))
          }
        }
     }

     /**
      * TODO: test
      */
     def userWithSpecialBet(username: String):  Future[(User, Seq[(SpecialBetT,SpecialBetByUser)])] = {
         val us = for{
           ((u,b),t) <- users.join(specialbetsuser).on(_.id === _.userId).join(specialbetstore).on(_._2.spId === _.id ) if u.username === username
         }yield (u,b,t)
       db.run(us.result).flatMap{ ubs =>
          if(ubs.size > 0){
             val user = ubs(0)._1
             val bets = ubs.map{ case(u,b,t) => (t,b)}.sortBy(_._1.id)
             Future{ (user, bets) }
          }else{
             Future.failed(ItemNotFoundException(s"could not find user with username $username"))
          }
       }
     }


     def userById(userId: Long):  Future[User] = {
         db.run(users.filter(u => u.id === userId).result.head)
            .recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id $userId")) }
     }

     def authenticate(username: String, inputPassword: String):  Future[User] = {
         val err = Future.failed(ItemNotFoundException(s"wrong password or user not found or user not registered: $username"))
         db.run(users.filter(u => u.username === username && u.isRegistered).result).flatMap{ users =>
           if(users.size == 1){
                if(new org.jasypt.util.password.StrongPasswordEncryptor().checkPassword(inputPassword, users(0).passwordHash))
                  Future{ users(0) } else err
           }else{ err }
         }
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
     def betsWitUsersForGame(game: Game): Future[Seq[(Bet,User)]] = {
         val bu = (for{
           (b, u) <- bets.join(users).on(_.userId === _.id) if b.gameId === game.id
         } yield {
           (b,u)
         })
         db.run(bu.result)
     }

     /**
      * UI 2
      *
      */
     def gamesWithBetForUser(user: User): Future[Seq[(GameWithTeams,Bet)]] = {
         val gtt = (for{
           ((((g, t1), t2),l),b) <- joinGamesTeamsLevels().join(bets).on(_._1._1._1.id === _.gameId) if b.userId === user.id
         } yield {
           (g, t1, t2,l,b)
         })
         db.run(gtt.result).map{ games => games.map{ case(g,t1,t2,l,b) => (GameWithTeams(g,t1,t2,l),b) }}
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
     def startOfGames(): Future[Option[DateTime]] = {
          db.run(games.sortBy(_.serverStart ).result).map{ games => games.headOption.map(_.serverStart) }
     }

     def closingTimeSpecialBet(closingMinutesToGame: Int): Future[Option[DateTime]] = {
         startOfGames.map{ s => s.map(_.minusMinutes(closingMinutesToGame)) }
     }

     def betWithGameWithTeamsAndUser(bet: Bet): Future[(Bet,GameWithTeams,User)] = {
         val bg = for{
          (((((g,t1),t2),l),b),u) <- joinGamesTeamsLevels().join(bets).on(_._1._1._1.id === _.gameId).join(users).on(_._2.userId === _.id) if b.id === bet.id
         } yield (g, t1, t2, l, b, u)
         val bwg = bg.result.head.map{ case(g,t1,t2,l,b,u) => (b, GameWithTeams(g,t1,t2,l),u) }
         db.run(bwg).recoverWith{ case ex: NoSuchElementException => Future.failed(new ItemNotFoundException(s"could not find bet in database $bet")) }
     }

     /**
      * current time should come from date time provider
      * need DI?
      * I reload the bet to make sure its not tampered with gameid or userid
      *
      * The successfull return value is for the messageing functionality (log, ticker, facebook etc...)
      *
      */
     /* TODO!!!!!!
     def updateBetResult(bet: Bet, submittingUser: User, currentTime: DateTime, closingMinutesToGame: Int): Future[(GameWithTeams,Bet,Bet)] = {
         betWithGameWithTeamsAndUser(bet).flatMap{ case(dbBet, dbgame, dbuser) =>
               compareBet(dbuser.canBet, dbuser.id.getOrElse(-1), submittingUser.id.getOrElse(-1), dbBet.gameId, bet.gameId, dbgame.game.serverStart, currentTime, closingMinutesToGame).fold(
                    err => {
             withT{
                   val invalid = GameResult(-1,-1,true)
                   val log = DomainHelper.toBetLog(dbuser, dbgame.game, dbBet, dbBet.copy(result=invalid), currentTime)
                 betlogs.insert(log)
                 -\/(err.list.mkString("\n"))
             }
            },
                    succ => {
              withT{
                          val result = bet.result.copy(isSet=true)
                          val updatedBet = dbBet.copy(result=result)
                        bets.filter(_.id === updatedBet.id).update(updatedBet)
                val log = DomainHelper.toBetLog(dbuser, dbgame.game, dbBet, updatedBet, currentTime)
                betlogs.insert(log)
                          \/-(dbgame, dbBet, updatedBet)
             }
               })
         }
     }
 */

     def allBetLogs(): Future[Seq[BetLog]] = {
         db.run(betlogs.result)
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
     def updateGameDetails(game: Game, submittingUser: User, currentTime: DateTime, gameDuration: Int): Future[(Game,GameUpdate)] = {
         if(! submittingUser.isAdmin){
           return Future.failed(AccessViolationException("only admin user can change levels"))
         }
         games.filter(_.id === game.id).result.head.map{ dbGame =>
            isGameOpen(dbGame.serverStart, currentTime: DateTime, gameDuration*5).fold(
              err => -\/("game will start in 5x90 minutes no more changes! "+err),
              succ =>  {//game open can not set points but can change teams and start time
                       val gameWithTeams = dbGame.copy(team1id=game.team1id, team2id=game.team2id, localStart=game.localStart, localtz=game.localtz, serverStart=game.serverStart, servertz=game.servertz, venue=game.venue)
                       games.filter(_.id === gameWithTeams.id).update(gameWithTeams)
                       \/-(gameWithTeams, ChangeDetails)
              }
            )
         }
         //.getOrElse(-\/(s"could not find game in database $game"))
         null
     }

     /**
      *
      * from ui with correct foreign keys set by ui
      * contoller should initiate points calculation
      *
      */
     def updateGameResults(game: Game, submittingUser: User, currentTime: DateTime, gameDuration: Int): String \/ (Game,GameUpdate) = {
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
     def insertUser(taintedUser: User, isAdmin: Boolean, isRegistering: Boolean, registeringUser: Option[Long]): String \/ User = {
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

     def updateUserPassword(userId: Long, passwordHash: String): String \/ User = {
       withT{
            users.filter(u => u.id === userId).firstOption.map{ user =>
            val updatedUser = user.copy(passwordHash=passwordHash)
          users.filter(u => u.id === userId).update(updatedUser)
          \/-(updatedUser)
        }.getOrElse(-\/(s"user not found $userId"))
         }
     }

     def updateUserDetails(userId: Long, firstName: String, lastName: String, email: String, icontype: String): String \/ User = {
       withT{
            users.filter(u => u.id === userId).firstOption.map{ user =>
          val (u,t) = DomainHelper.gravatarUrl(email, icontype)
            val updatedUser = user.copy(firstName=firstName, lastName=lastName, email=email, iconurl=u, icontype=t)
          users.filter(u => u.id === userId).update(updatedUser)
          \/-(updatedUser)
        }.getOrElse(-\/(s"user not found $userId"))
         }
     }

     def updateUserHadInstructions(user: User): String \/ String = {
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

     def createBetsForGamesForAllUsers(submittingUser: User): String \/ String = {
         if(submittingUser.isAdmin){
            users.list.foreach{ u =>
               createBetsForGamesForUser(u)
            }
            \/-("created bets for all users")
         }else{
           -\/("only admin users can create bets")
         }
     }

     def createBetsForGamesForUser(user: User){
         s.withTransaction { //this is something slick is misssing: nested transactions and joining open transactions
            user.id.map{ uid =>
              val gamesWithoutBets = gamesWithoutBetsForUser(user)
              gamesWithoutBets.flatMap{ g => g.id }.foreach{ gid =>
                 createBetForGameAndUser(gid, uid)
              }
            }
         }
     }

     def createBetForGameAndUser(gameId: Long, userId: Long){
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
     def gamesWithoutBetsForUser(user: User): Seq[Game] = {
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
     def invalidateGame(game: Game, submittingUser: User): String \/ String = {
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
     def calculatePoints(submittingUser: User): String \/ String = {
          if(submittingUser.isAdmin){
        withT{
                updateBetsWithPoints()
                updateUsersPoints()
                \/-("updated bets")
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
     def updateBetsWithPoints(): String \/ String = {
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
      \/-("updated")
     }

     /***
      * updates the tally of the bet points in the user
      *
      */
     def updateUsersPoints(): Boolean = {
            users.list.foreach{ user =>
                val points = for{
                  b <- bets if(b.userId === user.id)
                } yield {
                  b.points
                }
                val p = points.list.sum
                val specialPoints = calculateSpecialPointsForUser(user)
                val userWithPoints = user.copy(points=p, pointsSpecialBet=specialPoints)
                users.filter(_.id === userWithPoints.id).update(userWithPoints)
            }
              true
     }



     /**
      * calculates but does not set special points for user
      *
      */
     def calculateSpecialPointsForUser(user: User): Int = {
         val tbets = getSpecialBetsForUser(user)
       val updated = PointsCalculator.calculateSpecialBets(tbets)
       val bets = updated.map{ case(t,b) => b }
       val sum = bets.map(_.points).sum
       bets.foreach{ b =>
          specialbetsuser.filter(_.id === b.id).update(b)
         }
       sum
     }

     def insertPlayer(player: Player, teamName: String, submittingUser: User): String \/ Player = {
         if(! submittingUser.isAdmin){
           return -\/("must be admin to insert players")
         }
         getTeamByName(teamName).map{ team =>
             val playerWithTeamId = player.copy(teamId=team.id.get)
             players.insert(playerWithTeamId)
             playerWithTeamId
         }
     }

     def specialBetTemplates(): Seq[SpecialBetT] = {
         specialbetstore.list
     }

     def createSpecialBetsForUser(user: User){
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
     def getSpecialBetsForUser(user: User): Seq[(SpecialBetT,SpecialBetByUser)] = {
       val tbs = for {
         (t,b) <- specialbetstore.join(specialbetsuser).on( (temp,bet) => temp.id === bet.spId ) if b.userId === user.id
       } yield (t,b)
       tbs.list
    }

      def specialBetsByTemplate(id: Long): String \/ (SpecialBetT,Seq[SpecialBetByUser]) = {
      specialbetstore.filter(_.id === id).firstOption.map{ template =>
          val sp = specialbetsuser.filter(s => s.spId === template.id).list
          \/-((template,sp))
      }.getOrElse( -\/(s"special bet $id not found"))
    }
  */
   
}

