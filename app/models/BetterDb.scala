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


case class UpdatePoints()

@Singleton()
class BetterDb @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends BetterTables with HasDatabaseConfigProvider[JdbcProfile] {
  //TODO: check if this is efficient or different ExecutionContext necessary
  import scala.concurrent.ExecutionContext.Implicits.global

  import driver.api._
  

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

     def allSpecialBetTemplates(): Future[Seq[SpecialBetT]] = {
         db.run(specialbetstore.result)
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
     
     /**
      * 
      * 
      **/  
     def insertLevel(level: GameLevel, submittingUser: User): Future[GameLevel] = {
        if(submittingUser.isAdmin){
           db.run((levels returning levels.map(_.id)) += level).map{ lid => level.copy(id=Some(lid))}
         } else {
           Future.failed(AccessViolationException("only admins can insert or update teams by name"))
        } 
     }


     def insertTeam(team: Team, submittingUser: User): Future[Team] = {
        if(submittingUser.isAdmin){
           db.run((teams returning teams.map(_.id)) += team).map{ tid => team.copy(id=Some(tid))}
         } else {
           Future.failed(AccessViolationException("only admins can insert or update teams by name"))
        }
     }
 
     def insertPlayer(player: Player, teamName: String, submittingUser: User): Future[Player] = {
         if(submittingUser.isAdmin){
           val ins = (for{
              team <- teams.filter(t => t.name === teamName).result.head
              playerWithTeamId = player.copy(teamId=team.id.get)
              playerId <- (players returning players.map(_.id)) += playerWithTeamId
           } yield {
              playerWithTeamId.copy(id=Some(playerId))
           }).transactionally
           db.run(ins).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not team name $teamName")) }
         } else {
           Future.failed(AccessViolationException("only admins can insert or update teams by name"))
         }
     }
     
 
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
     def insertGame(game: Game, team1Name: String, team2Name: String, levelNr: Int, submittingUser: User): Future[GameWithTeams] = {
         Logger.debug(s"inserting new game $game $team1Name $team2Name $levelNr")
         if(submittingUser.isAdmin){
             val gttl = (for{
                ((t1,t2),l) <- ((teams.filter(_.name === team1Name)).zip(teams.filter(_.name === team2Name)).zip(levels.filter(_.level === levelNr))).result.head
                 gameNr <- games.map(_.nr).max.result
                 gameWithTeamsAndLevel = game.copy(team1id=t1.id.get, team2id=t2.id.get, levelId=l.id.get, result=DomainHelper.gameResultInit, nr=if(game.nr == 0) gameNr.map(_ + 1).getOrElse(1) else game.nr)
                 gameId <- (games returning games.map(_.id)) += gameWithTeamsAndLevel
             }yield((gameWithTeamsAndLevel.copy(id=Some(gameId))), t1, t2, l)).transactionally
             db.run(gttl).map{ case(g,t1,t2,l) => GameWithTeams(g, t1, t2, l) }
        } else {
           Future.failed(AccessViolationException("only admins can create bets for all users"))
        }
    }

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
     
     def userByName(username: String): Future[User] = {
         db.run(users.filter(_.username === username).result.head)
             .recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with username $username")) }
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
      * //TODO
      */
     def updateBetResult(bet: Bet, submittingUser: User, currentTime: DateTime, closingMinutesToGame: Int): Future[(GameWithTeams,Bet,Bet)] = {
   //TODO
       /*
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
         */
         null
     }


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

     /*

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
*/
     
      /**
      * I need only to filter for games without bets, therfore the
      * row hack with ? is not necessary, but if it is:
      * http://stackoverflow.com/questions/14990365/slick-left-outer-join-fetching-whole-joined-row-as-option
      *
      * //was: Seq[Game]
      */
     def gamesWithoutBetsForUser(userId: Long): Query[BetterDb.this.Games,models.Game,Seq] = {
         val allGamesWithBetsForUser = for{
            (g,b) <- games.join(bets).on(_.id === _.gameId) if b.userId === userId
         } yield (g.id)
         val allGamesWithoutBetsForUser = games.filterNot(g => g.id in allGamesWithBetsForUser)
         allGamesWithoutBetsForUser
     }
     
   
     /**
      * one of 2 methods for creating users:
      * this one creates the user directly
      * sets up all bets including special bets
      * TODO: only isResitserging users have to check registeringUser!!
      *
      *  the other one is token based, but I don't know if i manage to create this workflow: (Not done now!!!)
      *  user registers with e-mail, token is generated for his id
      *  user klicks link with token e-mail, opens web, => user signs on..
      *
      */
     def insertUser(taintedUser: User, isAdmin: Boolean, isRegistering: Boolean, registeringUser: Option[Long]): Future[User] = {

       //TODO: check if registeringUser is registeringUser!!
       
          val initUser = DomainHelper.userInit(taintedUser, isAdmin, isRegistering, registeringUser)
          
          val userQ = (for{
             userId <- (users returning users.map(_.id)) += initUser
             specialBets <- specialbetstore.result
             // does not compile (would be interesting to get the spIds
             // spIds <- (specialbetsuser returning specialbetsuser.map(_id)) ++= specialBets.map(s => SpecialBetByUser(None, userId, s.id.get, "", 0))
             _ <- specialbetsuser ++= specialBets.map(s => SpecialBetByUser(None, userId, s.id.get, "", 0))
             _ <- createBetsForGamesForUser(userId)
          } yield userId).transactionally
           
          db.run(userQ).map(i => initUser.copy(id=Some(i)))
          
     }
     
      /**
      * only simple bets no special bets!
      */
     def createBetsForGamesForUser(userId: Long): DBIOAction[Option[Int],slick.dbio.NoStream,slick.dbio.Effect.Read with slick.dbio.Effect.Write]  = {
         Logger.debug(s"creating bets for games for user $userId")
         val r = for{
            games <- gamesWithoutBetsForUser(userId).result
            b <- bets ++= games.map(g => Bet(None, 0, GameResult(0,0,false), g.id.get, userId))
         } yield { b } 
         r
     }
     
     /**
      * only simple bets no special bets!
      * 
      * TODO: get userIds.map(u => createBetsForGamesForUser(u)) into the for loop
      * 
      */
     def createBetsForGamesForAllUsers(submittingUser: User): Future[String] = {
         Logger.debug(s"creating bets for games for all users")
         if(submittingUser.isAdmin){
           val create = (for{
             userIds <- users.map(_.id).result
           } yield { userIds.map(u => createBetsForGamesForUser(u))  }).transactionally
           db.run(create).map(r => "created bets for all users")
         }else{
           Future.failed(AccessViolationException("only admins can create bets for all users"))
         }
     }
  
     def updateUserPassword(userId: Long, passwordHash: String): Future[User] = {
           val upd = (for{
             user <- users.filter(u => u.id === userId).result.head
             updatedUser = user.copy(passwordHash=passwordHash)
             _ <- users.update(updatedUser)
           } yield( updatedUser )).transactionally
           db.run(upd).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id $userId")) }
     }

     def updateUserDetails(userId: Long, firstName: String, lastName: String, email: String, icontype: String): Future[User] = {
         val (u,t) = DomainHelper.gravatarUrl(email, icontype)
         val upd = (for{
             user <- users.filter(u => u.id === userId).result.head
             updatedUser = user.copy(firstName=firstName, lastName=lastName, email=email, iconurl=u, icontype=t)
             _ <- users.update(updatedUser)
         } yield( updatedUser )).transactionally
         db.run(upd).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id $userId")) }
     }
       
     def updateUserHadInstructions(userId: Long): Future[User] = {
         val upd = (for{
             user <- users.filter(u => u.id === userId).result.head
             updatedUser = user.copy(hadInstructions = true)
             _ <- users.update(updatedUser)
         } yield( updatedUser )).transactionally
         db.run(upd).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id $userId")) }
     }


    /**
     * if something was wrong with the game. we set the results for this game to isSet = false
     * This does not delete the set result, but excludes them from accruing points for the user
     *
     */
     def invalidateGame(game: Game, submittingUser: User): Future[String] = {
         if(submittingUser.isAdmin){
             val r = games.filter(_.id === game.id).map(_.isSet).update(false)
             Logger.debug("invalidating game $game")
             db.run(r).map(i => "invalidated game $game count: $i")
         } else {           
            Future.failed(AccessViolationException(s"only admin user can invalidate games"))
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
     def calculateAndUpdatePoints(submittingUser: User): Future[String] = {
          if(submittingUser.isAdmin){
             val r = (for{
               _ <- updateBetsWithPoints()
               _ <- updateUsersPoints()
             } yield()).transactionally
             db.run(r).map(r => "calculated all points for special bets and bets and updated users")
         } else {           
            Future.failed(AccessViolationException(s"only admin user can calculate points"))
         }
     }
        
     

     /***
      * updates all bets which have a valid result with points depending on game results and level
      * also using invalid games (i.e. result not set because after invalidating the game I want to keep the
      * bet result still set and valid (just in case somebody messed up). But the points in the bet will be set to 0 if
      * the game is invalid
      *
      */
     def updateBetsWithPoints(): DBIOAction[Seq[slick.profile.FixedSqlAction[Int,slick.dbio.NoStream,slick.dbio.Effect.Write]],slick.dbio.NoStream,slick.dbio.Effect.Read] = { //((g,l),b)
          Logger.info("updating bets with points") 
          val glbUpdate = for{
                glbs <- ((games.join(levels).on(_.levelId === _.id).join(bets.filter( b => b.isSet )).on(_._1.id === _.gameId))).result
             } yield {
               glbs.map{ case ((g,l),b) =>
                   val points = PointsCalculator.calculatePoints(b, g, l)
                   val betWithPoints = b.copy(points=points)
                   bets.filter(_.id === betWithPoints.id).update(betWithPoints)
             }
         }  
         glbUpdate    
     }
     
     /**
      * calculates but and updates special points for user
      * to be called from updateUserPoints
      *
      */
     def calculateAndUpdateSpecialPointsForUser(user: User) = {
         Logger.debug(s"calculating special points for user ${user.id}")
         val up = (for{
            s <- specialbetstore.join(specialbetsuser.filter(sp => sp.userId === user.id)).on( _.id === _.spId ).result   
            updated = PointsCalculator.calculateSpecialBets(s).map{ case(t,b) => b }
            sum = updated.map(_.points).sum
         } yield {
             updated.foreach{ b => specialbetsuser.filter(_.id === b.id).update(b) } //TODO: check on mailing list if this is actually executed transactionally!
             users.filter(_.id === user.id).update(user.copy(pointsSpecialBet=sum))
             sum
         })
         up
     }
     

     /***
      * updates the tally of the bet points in the user
      *
      */
     def updateUsersPoints() = {
         Logger.info("updating users with points")
         val userUpdate = for{
              suser <- users.join(bets).on(_.id === _.userId).result
         } yield {
           val r = suser.groupBy{ _._1 }.map{ case(user,ub) =>  
                 val betSum = ub.map{ _._2.points}.sum 
                 val userWithPoints = user.copy(points=betSum)
                 calculateAndUpdateSpecialPointsForUser(user) //stores both betSum and calculates special points
           }
           r
         }
         userUpdate
     }


     //the optimum would be a left outer join with an and within the on clause!, but this is too complicated becasue there is
     //no option[row].? as return type possible for slick yet
     //(t, b) <- specialbetstore.join(specialbetsuser).on( (temp,bet) => temp.id === bet.spId  && bet.userId === user.id
     //
     //we generate for each user the correct bets when he signs in
     //http://stackoverflow.com/questions/20386593/slick-left-right-outer-joins-with-option
     def getSpecialBetsForUser(user: User): Future[Seq[(SpecialBetT,SpecialBetByUser)]] = {
         val tbs = for {
           s <- specialbetstore.join(specialbetsuser.filter(sp => sp.userId === user.id)).on( _.id === _.spId ).result
           expectedCount = specialbetstore.length.result
         } yield (s, expectedCount)
         db.run(tbs).flatMap{ case(tbs,count) =>  
             if(tbs.size == count){
                Future{ tbs }
             }else{
               Future.failed(ItemNotFoundException(s"could not find all special bets for user ${user.id} with id $count")) 
             }
         }    
     }


    def specialBetsByTemplate(id: Long): Future[(SpecialBetT,Seq[SpecialBetByUser])] = {
       val res = specialbetstore.filter(_.id === id).join(specialbetsuser).on(_.id === _.spId).result
       db.run(res).map{ r => r.groupBy{ case(t, sp) => t }.mapValues{ v => v.unzip._2 }.head }
            .recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find special bet template with id $id")) }
    }
 
   
}

