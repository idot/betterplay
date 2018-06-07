package models



import javax.inject.{Singleton, Inject}

import java.time.OffsetDateTime
import play.api.db.slick.DatabaseConfigProvider


import scalaz.{\/,-\/,\/-,Validation,ValidationNel,Success,Failure}
import scalaz.syntax.apply._ 
import scala.concurrent.Future

import play.api.Logger
import scala.concurrent.ExecutionContext
/**
 * message types for games
 */
sealed trait GameUpdate
case object SetResult extends GameUpdate
case object ChangeDetails extends GameUpdate
case object NewGame extends GameUpdate



@Singleton()
class BetterDb @Inject() (val dbConfigProvider: DatabaseConfigProvider) (implicit ec: ExecutionContext) extends BetterTables {
 
  import dbConfig._
  import profile.api._
  
   val betterDBLogger = Logger("betterdb")
   
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
         db.run(levels.sortBy(l => l.level).result)
     }

     def allUsers(): Future[Seq[User]] = {
         db.run(users.sortBy(u => u.username).result)
     }

     def allSpecialBetTemplates(): Future[Seq[SpecialBetT]] = {
         db.run(specialbetstore.result)
     }
     
     def allPlayers(): Future[Seq[Player]] = {
         db.run(players.result)
     }
     
     def allBetLogs(): Future[Seq[BetLog]] = {
         db.run(betlogs.result)
     }
     
     def allUsersWithRank(): Future[Seq[(User,Int)]] = {
         val res = users.sortBy(u => ((u.points + u.pointsSpecial).desc, u.id.desc) ).result.map{ sorted =>
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
                   val sorted = betsPerUser.toList.sortBy{ case(u,b) => (u.totalPoints(),u.id) }.reverse
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
         val li = gtt.sortBy(_._1.serverStart).result.map{ list => list.map{ case(g,t1,t2,l) => GameWithTeams(g,t1,t2,l) }}
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
         betterDBLogger.debug(s"inserting player $player.name")
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
     
     def validSPU(sp: SpecialBetByUser, currentTime: OffsetDateTime, closingMinutesToGame: Int, submittingUser: User): Future[ValidationNel[String,String]] = {
         startOfGames().map{ OstartTime =>
             val withStart = OstartTime.map{ startTime => isGameOpen(startTime, currentTime, closingMinutesToGame) }.getOrElse( Failure("no games yet") )
             val ids = compareIds(submittingUser.id.getOrElse(-1), sp.userId, "user ids").toValidationNel
             (withStart.toValidationNel |@| ids) {
                case(time, ids) => Seq(time, ids).mkString("\n")  
             }
         }
     }
     
   

     /**
     * I am loading the special bet by id from the database and check if the user is the actual owner to make sure its not tampered with 
     * 
     * TODO: load template with closing time, compare to local time get rid of global closingMinutesToGame
     * 
     */
     def updateSpecialBetForUser(sp: SpecialBetByUser, currentTime: OffsetDateTime, closingMinutesToGame: Int, submittingUser: User): Future[User] = {
        betterDBLogger.debug(s"attempting updating special bet for user ${submittingUser.username} ${sp.prediction}") 
        validSPU(sp, currentTime, closingMinutesToGame, submittingUser).flatMap{ v =>
           v.fold(
               err => Future.failed(ValidationException(err.list.toList.mkString("\n"))),
               succ => updateSPU(sp, submittingUser)
          )}
     }
     

     //TODO: TEST for exception when specialbet does not exist!
     def updateSPU(sp: SpecialBetByUser, submittingUser: User): Future[User] = {     
         betterDBLogger.debug(s"updating special bet for user ${sp.prediction}") 
         val action = (specialbetsuser.filter(spdb => spdb.id === sp.id && spdb.userId === submittingUser.id && spdb.spId === sp.specialbetId).map( c => c.prediction ).update( sp.prediction ).flatMap{ rowCount =>
             rowCount match {
               case 0 => { 
                      val err = s"could not find specialbet with ids ${sp.id} ${sp.userId} ${sp.specialbetId} - ${submittingUser.id}"; 
                      betterDBLogger.error(err); 
                      DBIO.failed(new ItemNotFoundException(err)) 
               }
               case 1 => DBIO.successful(s"updated special bet with prediction ${sp.prediction}")
               case _ => DBIO.failed(new ItemNotFoundException(s"found multiple special bets with id ${sp.id}"))
             }
         }).transactionally
         
         /***
         * we update the user with hadInstructions = false if any of his predictions are not set
         * maybe cant be done in one transaction because if no empty => exception???
         * 
         */ 
         //val updated =
         val action2 = (for {
                countEmptySpecialBetsForUser <- users.join(specialbetsuser.filter(sp => sp.userId === submittingUser.id && sp.prediction === "")).on(_.id === _.userId).length.result
                updatedUser =  submittingUser.copy(hadInstructions = if(countEmptySpecialBetsForUser == 0) true else false )
                 _ <- users.filter(_.id === submittingUser.id).update(updatedUser)
             } yield( updatedUser )).transactionally               
         val comb = action andThen action2
         db.run(comb)

     
     }

    
     
//     def updateUserHadInstructions(submittingUser: User): Future[User] = {
//         betterDBLogger.debug(s"updating user had instructions ${submittingUser.username}") 
//         val action = (for {
//             userWithEmptySpecialBets <- users.join(specialbetsuser.filter(sp => sp.userId === submittingUser.id && sp.prediction === "")).on(_.id === _.userId).result.head 
//             updatedUser = userWithEmptySpecialBets._1.copy(hadInstructions = false)
//             _ <- users.update(updatedUser)
//         } yield( updatedUser )).transactionally
//         db.run(action) //TODO: check if exception thrown in unit  //.recoverWith{ case ex: NoSuchElementException => Future.failed() }
//     }

     
     def setSpecialBetResult(specialBetId: Long, result: String, submittingUser: User): Future[String] = {
        betterDBLogger.info(s"attempting setting special bet result: ${submittingUser.username} ${specialBetId} ${result}")
        if(submittingUser.isAdmin){
           val action = specialbetstore.filter(_.id === specialBetId).map(_.result).update(result).flatMap{ rowCount =>
             rowCount match {
               case 0 => DBIO.failed(new ItemNotFoundException(s"could not find specialbet with id ${specialBetId}")) 
               case 1 => DBIO.successful(s"updated special bet with result ${result}")
               case _ => DBIO.failed(new ItemNotFoundException(s"found multiple special bets with id ${specialBetId}"))
             }
           }.transactionally
           db.run(action)
        }else Future.failed(AccessViolationException("only admins can set special bet results!"))
     }
     
     
     /**
      * This is for text import  and creation of game game details | team1name | team2name | levelNr (| == tab)
      * ids for teams and levels and results are ignored, taken from db and amended in game object
      * todo: add updatingUser
      *
      */
     def insertGame(game: Game, team1Name: String, team2Name: String, levelNr: Int, submittingUser: User): Future[GameWithTeams] = {
         betterDBLogger.debug(s"inserting new game $game $team1Name $team2Name $levelNr $submittingUser")
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
      * API
      */
     def userWithSpecialBets(userId: Long):  Future[(User, Seq[(SpecialBetT,SpecialBetByUser)])] = {
         userWithSpecialBetsF((u: Users) => u.id === userId, s"id $userId")
     }
     
      /**
      * API
      */
     def userWithSpecialBets(username: String): Future[(User, Seq[(SpecialBetT,SpecialBetByUser)])] = {
         userWithSpecialBetsF((u: Users) => u.username === username, s"username $username")
     }

     /**
      * internal
      */
     def userWithSpecialBetsF(filter: Users => Rep[Boolean], err: String):  Future[(User, Seq[(SpecialBetT,SpecialBetByUser)])] = {
         val us = for{
           ((u,b),t) <- users.join(specialbetsuser).on(_.id === _.userId).join(specialbetstore).on(_._2.spId === _.id ) if filter(u)
         }yield (u,b,t)
         db.run(us.result).flatMap{ ubs =>
          if(ubs.size > 0){
             val user = ubs(0)._1
             val bets = ubs.map{ case(u,b,t) => (t,b)}.sortBy(_._1.id)
             Future{ (user, bets) }
          }else{
             Future.failed(ItemNotFoundException(s"could not find user with $err"))
          }
       }
     }
     
  
              
     
     def userByName(username: String): Future[User] = {
         db.run(users.filter(_.username === username).result.head)
             .recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with username $username")) }
     }
 
     def userByEmail(email: String): Future[User] = {
         db.run(users.filter(_.email === email).result.head)
             .recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with email $email")) }
     }

     def userById(userId: Long):  Future[User] = {
         db.run(users.filter(u => u.id === userId).result.head)
            .recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id $userId")) }
     }

     def authenticate(username: String, inputPassword: String):  Future[User] = {
         val err = Future.failed(ItemNotFoundException(s"wrong password or user not found: $username"))
         db.run(users.filter(u => u.username === username).result).flatMap{ users =>
           if(users.size == 1){
                if(new org.jasypt.util.password.StrongPasswordEncryptor().checkPassword(inputPassword, users(0).passwordHash)){
                  Future{ users(0) } 
                }else{ err }
           }else{ err }
         }
     }

     /**
      * 
      * 
      */
     def betsForUser(user: User): Future[Seq[Bet]] = {
        db.run(bets.filter(_.userId === user.id).result)
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
      * stats
      */
     def betsForGame(id: Long): Future[Seq[GameResult]] = {
         val q = bets.filter(_.gameId === id).map(b => b.result).result
         db.run(q)
     }
     
     /**
      * stats
      * must be a list for e.g. semifinalists
      * 
      */
     def specialBetsPredictions(name: String): Future[Seq[(SpecialBetT,String)]] = {
         val q = for{
           sps <- specialbetstore.filter(_.name === name).join(specialbetsuser).on(_.id === _.spId ).map{ case(t,sp) => (t, sp.prediction) }.result
         } yield {
           sps
         }
         db.run(q)
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
     def isGameOpen(gameTimeStart: OffsetDateTime, currentTime: OffsetDateTime, closingMinutesToGame: Int): Validation[String,String] = {
         val open = DomainHelper.gameOpen(gameTimeStart, currentTime, closingMinutesToGame)
         if(open) Success("valid time") else Failure(s"game closed since ${TimeHelper.compareTimeHuman(DomainHelper.gameClosingTime(gameTimeStart, closingMinutesToGame), currentTime)}")
     }


     /***
      *
      *
      */
     def startOfGames(): Future[Option[OffsetDateTime]] = {
          db.run(games.sortBy(_.serverStart ).result).map{ games => games.headOption.map(_.serverStart) }
     }

     /**
      * 
      * TODO: not needed anymore?
      */
     //def closingTimeSpecialBet(closingMinutesToGame: Int): Future[Option[OffsetDateTime]] = {
     //    startOfGames.map{ s => s.map(_.minusMinutes(closingMinutesToGame)) }
     //}

     /**
      * 
      * TODO: not needed anymore?
      */
    // def betWithGameWithTeamsAndUser(bet: Bet): Future[(Bet,GameWithTeams,User)] = {
    //     val bg = for{
     //     (((((g,t1),t2),l),b),u) <- joinGamesTeamsLevels().join(bets.filter { b =>  b.id === bet.id }).on(_._1._1._1.id === _.gameId).join(users).on(_._2.userId === _.id)
    //     } yield (g, t1, t2, l, b, u)
    //     val bwg = bg.result.head.map{ case(g,t1,t2,l,b,u) => (b, GameWithTeams(g,t1,t2,l),u) }
     //    db.run(bwg).recoverWith{ case ex: NoSuchElementException => Future.failed(new ItemNotFoundException(s"could not find bet in database $bet")) }
    // }

     /**
      * current time should come from date time provider
      * need DI?
      * I reload the bet to make sure its not tampered with gameid or userid
      *
      * The successfull return value is for the messageing functionality (log, ticker, facebook etc...)
      *
      * returns game with teams, oldBet, newBet, betlog, Seq[String] of errors
      *
      *  I have to execute the betlog database insert, so the application has to test for invalid bet insert request by checking the error string
      * 
      */
     def updateBetResult(bet: Bet, submittingUser: User, currentTime: OffsetDateTime): Future[(GameWithTeams, Bet, Bet,BetLog, Seq[String])] = {
          betterDBLogger.debug(s"updating bet result: $bet ${submittingUser.username}")
          val invalid = GameResult(-1,-1,true)
          val bg = (for{
             (((((g,t1),t2),l),b),u) <- joinGamesTeamsLevels().join(bets.filter { b =>  b.id === bet.id }).on(_._1._1._1.id === _.gameId).join(users).on(_._2.userId === _.id).result.head
             (upl, dbAction) <- compareBet(u.canBet, u.id.getOrElse(-1), submittingUser.id.getOrElse(-1), b.gameId, bet.gameId, g.serverStart, currentTime, g.closingMinutesToGame).fold(
                       err => {
                        val errors = err.list.toList.mkString(";")
                        val updatedBet = b.copy(result=invalid)
                        val log = DomainHelper.toBetLog(u, g, b, updatedBet, currentTime, errors)
                        val upL = betlogs += log
                        betterDBLogger.debug(s"updating bet result error: $bet ${submittingUser.username} $errors")
                        DBIO.successful((updatedBet, log, err.list.toList)).zip(DBIO.seq(upL))
                    }, succ => {
                         val result = bet.result.copy(isSet=true)
                         val updatedBet = b.copy(result=result)
                         val log = DomainHelper.toBetLog(u, g, b, updatedBet, currentTime, "regular update")
                         val upB = bets.filter(_.id === updatedBet.id).update(updatedBet)
                         val upL = betlogs += log
                         betterDBLogger.debug(s"updating bet result success: $bet -> $updatedBet ${submittingUser.username}")
                         DBIO.successful((updatedBet, log, Seq.empty[String])).zip(DBIO.seq(upL,upB))
                    })
          }yield{ (GameWithTeams(g,t1,t2,l), b, upl._1, upl._2, upl._3) }).transactionally   
          db.run(bg).recoverWith{ case ex: NoSuchElementException => Future.failed(new ItemNotFoundException(s"could not find bet in database $bet")) }
     }


     def compareIds(original: Long, proposed: Long, idName: String): Validation[String,String] = {
         if(original == proposed) Success("valid id") else Failure(s"$idName differ $original $proposed")
     }

     def canBet(cb: Boolean): Validation[String,String] = {
         if(cb) Success("can bet") else Failure(s"user has not paid, no dice")
     }

     def compareBet(cb: Boolean, userId: Long, betUserId: Long, gameId: Long, betGameId: Long, gameTimeStart: OffsetDateTime, currentTime: OffsetDateTime, closingMinutesToGame: Int): ValidationNel[String,String] = {
         (compareIds(userId, betUserId, "user ids").toValidationNel |@|
             compareIds(gameId, betGameId, "game ids").toValidationNel |@|
             isGameOpen(gameTimeStart, currentTime, closingMinutesToGame).toValidationNel |@|
             canBet(cb).toValidationNel ){
           case(u,g,t,c) => Seq(u,g,t,c).mkString("\n")
         }
     }

//THIS IS NOT USED ANYMORE?
//TODO: check if it was used once
//     def checkSpecial(cb: Boolean, userId: Long, betUserId: Long): ValidationNel[String,String] = {
//         (canBet(cb).toValidationNel |@| compareIds(userId, betUserId, "user ids").toValidationNel){
//           case(c,u) => Seq(c,u).mkString("\n")
//         }
//     }



     /**
      * this should only be called immediately after game creation if there has been an error!
      * not good for users to have team changes!
      *
      * //game open can not set points but can change teams and start time
      */
     def updateGameDetails(game: Game, submittingUser: User, currentTime: OffsetDateTime, gameDuration: Int): Future[(Game,GameUpdate)] = {
         if(! submittingUser.isAdmin){
           return Future.failed(AccessViolationException("only admin user can change game details"))
         }
         val gUp = (for{
           dbGame <- games.filter(_.id === game.id).result.head 
           _ <- isGameOpen(dbGame.serverStart, currentTime, gameDuration*5).fold(err => DBIO.failed(ValidationException("game will start in 5x90 minutes no more changes! "+err)), succ => DBIO.successful("IGNORE"))
           gameWithTeams = dbGame.copy(team1id=game.team1id, team2id=game.team2id, localStart=game.localStart, localtz=game.localtz, serverStart=game.serverStart, servertz=game.servertz, venue=game.venue)
           _ <- games.filter(_.id === gameWithTeams.id).update(gameWithTeams)
         } yield { (gameWithTeams, ChangeDetails) } ).transactionally
         db.run(gUp).recoverWith{ case ex: NoSuchElementException => Future.failed(new ItemNotFoundException(s"could not find game in database $game")) }
     }

      /**
      *
      * from ui with correct foreign keys set by ui
      * contoller should initiate points calculation
      *
      */
     def updateGameResults(game: Game, submittingUser: User, currentTime: OffsetDateTime, gameDuration: Int): Future[(Game,GameUpdate)] = {
         if(! submittingUser.isAdmin){
           return Future.failed(AccessViolationException("must be admin to change game results"))
         }
         val result = game.result.copy(isSet=true)
         val gUp = (for{
           dbGame <- games.filter(_.id === game.id).result.head
           _ <- isGameOpen(dbGame.serverStart, currentTime: OffsetDateTime, -gameDuration).fold(err => DBIO.successful("IGNORE"), succ => DBIO.failed(ValidationException(s"game ${game.id.get} is still not finished")))
           gameWithResult = dbGame.copy(result=result) 
           _ <- games.filter(_.id === gameWithResult.id).update(gameWithResult)
         } yield { (gameWithResult, SetResult) }).transactionally
         db.run(gUp).recoverWith{  case ex: NoSuchElementException => Future.failed(new ItemNotFoundException(s"could not find game in database $game")) }
     }    
      
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
         betterDBLogger.debug(s"fetched games without bets for user $userId yield")
         val allGamesWithoutBetsForUser = games.filterNot(g => g.id in allGamesWithBetsForUser)
         betterDBLogger.debug(s"fetched games without bets for user $userId filtered")
         allGamesWithoutBetsForUser
     }
     
   
     /**
      * one of 2 methods for creating users:
      * this one creates the user directly
      * sets up all bets including special bets
      * TODO: only isResitserging users have to check registeringUser!!
      *
      * canRegister: if registeringUser has registering 
      * 
      *  the other one is token based
      *
      *  
      */
     def insertUser(taintedUser: User, isAdmin: Boolean, isRegistering: Boolean, registeringUser: Option[User]): Future[User] = {
          betterDBLogger.debug(s"inserting user: by ${registeringUser.map(_.username).getOrElse("noUser")} ${taintedUser.username}")
          if( registeringUser.map(r => ! r.isAdmin).getOrElse(false) ){ //if none => the data comes from text file import, no users yet!!!
             return Future.failed(AccessViolationException("must be admin user to be able to create users!"))
          }
       
          val initUser = DomainHelper.userInit(taintedUser, isAdmin, isRegistering, registeringUser.flatMap(_.id))
          
          val userQ = (for{
             userId <- (users returning users.map(_.id)) += initUser
             specialBets <- specialbetstore.result
             spIds <- (specialbetsuser returning specialbetsuser.map(_.id)) ++= specialBets.map(s => SpecialBetByUser(None, userId, s.id.get, "", 0))
             _ <- createBetsForGamesForUser(userId)
          } yield userId).transactionally
          db.run(userQ).map(i => initUser.copy(id=Some(i)))
     }
     
      /**
      * only simple bets no special bets!
      */
     def createBetsForGamesForUser(userId: Long) = {
         betterDBLogger.debug(s"creating bets for games for user $userId")
         for{
            gs <- gamesWithoutBetsForUser(userId).result
            inserts <- (bets returning bets.map(_.id)) ++= gs.map( game => Bet(None, 0, GameResult(0,0,false), game.id.get, userId))
         }yield(inserts)
     }   


     
     /**
      * only simple bets no special bets!
      * 
      * TODO: get userIds.map(u => createBetsForGamesForUser(u)) into the for loop
      * 
      */
     def createBetsForGamesForAllUsers(submittingUser: User): Future[String] = {
         betterDBLogger.debug(s"creating bets for games for all users")
         if(submittingUser.isAdmin){
           val create = (for{
             userIds <- users.map(_.id).result
             _ <- DBIO.sequence(userIds.map(u => createBetsForGamesForUser(u)))     
           } yield()).transactionally
           db.run(create).map(r => "created bets for all users")
         }else{
           Future.failed(AccessViolationException("only admins can create bets for all users"))
         }
     }
  
     def updateUserPassword(passwordHash: String, submittingUser: User): Future[String] = {
             val upd = (for{
               _ <- users.filter(_.id === submittingUser.id).map(u => u.passwordhash).update(passwordHash)
             } yield { 
               betterDBLogger.info(s"updated userpassword for ${submittingUser.id} ${submittingUser.username}")
               "updated password"
             }).transactionally
             db.run(upd).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id ${submittingUser.id} ${submittingUser.username} to update password")) }
      }

     /**
      * only admins can now change firstname and lastname
      */
     def updateUserDetails(email: String, icontype: String, showName: Boolean, institute: String, submittingUser: User): Future[User] = {
         betterDBLogger.debug(s"updating user details ${submittingUser.username}")
         val (u,t) = DomainHelper.gravatarUrl(email, icontype)
         val upd = (for{
             user <- users.filter(u => u.id === submittingUser.id).result.head
             updatedUser = user.copy(email=email, showName=showName, institute=institute, iconurl=u, icontype=t)
             _ <- users.filter(_.id === submittingUser.id).update(updatedUser)
         } yield( updatedUser )).transactionally
         db.run(upd).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id ${submittingUser.id}")) }
     }
     
      /**
      * only admins can now change firstname and lastname
      */
     def updateUserName(username: String, firstName: String, lastName: String, submittingUser: User): Future[User] = {
          betterDBLogger.debug(s"updating user firstname and lastname ${username} ${submittingUser.username}")
          if(submittingUser.isAdmin){
             val upd = (for{
                 user <- users.filter(u => u.username === username).result.head
                 updatedUser = user.copy(firstName=firstName, lastName=lastName)
                 _ <- users.filter(_.id === updatedUser.id).update(updatedUser)
             } yield( updatedUser )).transactionally
             db.run(upd).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id $username")) }
          } else {           
            Future.failed(AccessViolationException(s"only admin user can update the name"))
          }
     }
     
      /**
      * only admins can change id user can bet 
      */
     def updateUserCanBet(username: String, canBet: Boolean, submittingUser: User): Future[User] = {
          betterDBLogger.info(s"updating user can bet ${username} ${submittingUser.username}") //TODO: send email!
          if(submittingUser.isAdmin){
             val upd = (for{
                 user <- users.filter(u => u.username === username).result.head
                 updatedUser = user.copy(canBet=canBet)
                 _ <- users.filter(_.id === updatedUser.id).update(updatedUser)
             } yield{
               betterDBLogger.info(s"updated user can bet to ${canBet} for ${updatedUser.id} ${updatedUser.username} by ${submittingUser.id} ${submittingUser.username}")  
               updatedUser
             }).transactionally
             db.run(upd).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id $username")) }
          } else {           
            Future.failed(AccessViolationException(s"only admin user can update wether the user can bet"))
          }
     }
     
     
     def updateFilterSettings(filterSettings: FilterSettings, submittingUser: User): Future[User] = {
         betterDBLogger.debug(s"updating filtersettings ${submittingUser.username}") 
         val upd = (for{
             user <- users.filter(u => u.id === submittingUser.id).result.head
             updatedUser = user.copy(filterSettings=filterSettings)
             _ <- users.filter(_.id === submittingUser.id).update(updatedUser)
         } yield( updatedUser )).transactionally
         db.run(upd).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find user with id ${submittingUser.id}")) }
     }
     

    

    /**
     * if something was wrong with the game. we set the results for this game to isSet = false
     * This does not delete the set result, but excludes them from accruing points for the user
     *
     * 
     * TODO: recalculate points
     * 
     */
     def invalidateGame(game: Game, submittingUser: User): Future[String] = {
         if(submittingUser.isAdmin){
             val r = games.filter(_.id === game.id).map(_.isSet).update(false)
             betterDBLogger.info("invalidating game $game")
             db.run(r).map(i => s"invalidated game ${game.id.get} count: $i")
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
        
     def gamesWithLevelsAndBetsSet() = {
         ((games.join(levels).on(_.levelId === _.id).join(bets.filter( b => b.isSet )).on(_._1.id === _.gameId))).result
     }

     /***
      * updates all bets which have a valid result with points depending on game results and level
      * also using invalid games (i.e. result not set because after invalidating the game I want to keep the
      * bet result still set and valid (just in case somebody messed up). But the points in the bet will be set to 0 if
      * the game is invalid
      *
      */
     def updateBetsWithPoints() = { 
          betterDBLogger.info("updating bets with points") 
          val glbUpdate = for{
                  glbs <- gamesWithLevelsAndBetsSet()
                   _ <- DBIO.sequence(glbs.map{ case((g,l),b) => 
                       val points = PointsCalculator.calculatePoints(b, g, l)
                       val betWithPoints = b.copy(points=points)
                       bets.filter(_.id === betWithPoints.id).update(betWithPoints)
                    })
          } yield()    
          glbUpdate    
     }
     
     /**
      * calculates but and updates special points for user
      * to be called from updateUserPoints
      *
      */
     def calculateAndUpdateSpecialPointsForUser(user: User) = {
         betterDBLogger.debug(s"calculating special points for user ${user.id}")
         val up = for{
            s <- specialbetstore.join(specialbetsuser.filter(sp => sp.userId === user.id)).on( _.id === _.spId ).result   
            updated = PointsCalculator.calculateSpecialBets(s).map{ case(t,b) => b }
            sum = updated.map(_.points).sum
            specialUpdate <- DBIO.sequence(updated.map{ b => specialbetsuser.filter(_.id === b.id).update(b) })
            sumUpdate <- users.filter(_.id === user.id).update(user.copy(pointsSpecialBet=sum))
         } yield()
         up
     }
     

     /***
      * updates the tally of the bet points in the user
      *
      */
     def updateUsersPoints() = {
         betterDBLogger.info("updating users with points")
         val userUpdate = for{
              suser <- users.join(bets).on(_.id === _.userId).result
              ups <- DBIO.sequence(suser.groupBy{ _._1 }.map{ case(user,ub) =>  
                     val betSum = ub.map{ _._2.points}.sum 
                     val userWithPoints = user.copy(points=betSum)
                     calculateAndUpdateSpecialPointsForUser(userWithPoints) //stores both betSum and calculates special points
                   })
         } yield ()
         userUpdate
     }


    def specialBetsByTemplate(id: Long): Future[(SpecialBetT,Seq[SpecialBetByUser])] = {
       val res = specialbetstore.filter(_.id === id).join(specialbetsuser).on(_.id === _.spId).result
       db.run(res).map{ r => r.groupBy{ case(t, sp) => t }.mapValues{ v => v.unzip._2 }.head }
            .recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find special bet template with id $id")) }
    }
 
   
    def insertMessage(message: Message, userId: Long, token: String, send: Boolean, display: Boolean): Future[(UserMessage,Message)] = {
        betterDBLogger.debug(s"inserting message for $userId $token ${message.messageType} ${message.subject}")
        val action = (for {
          messId <- (messages returning messages.map(_.id)) += message  
          mess = message.copy(id=Some(messId))
          usermessage = UserMessage(None, userId, messId, token, send, None, display, None, message.creatingUser)
          um <- (usersmessages returning usersmessages.map(_.id)) += usermessage
        } yield( (usermessage.copy(id=Some(um)), mess) )).transactionally
        db.run(action)
    }
 
//TODO send bulk email    
//    def insertMessages(message: Message, userIds: Seq[Long], send: Boolean, display: Boolean): Future[String] = {
//        val action = (for {
//          messId <- (messages returning messages.map(_.id)) += message  
//          mess = message.copy(id=Some(messId))
//          usermessage = userIds.map{ UserMessage(None, userId, messId, token, send, None, display, None, sendingUser.id.get)
//          um <- (usersmessages returning usersmessages.map(_.id)) += usermessage
//        } yield()).transactionally
//        db.run(action).map{ r => s"saved sending message to $userId" } 
//    }
    
//    def messageToAll(subject: String, body: String, sendingId: Long): Future[String] = {
//        val action = (
//          for{
//            users <- users.result
//            messages = DBIO.sequence( users.map{user => 
//               val message = MailGenerator.personalize(subject.value, body.value, user, sendingId)
//               val um =  
//            }

          
//         } yield()).transactionally
//        null
//    }   
         
    /**
     * the token usage sets seen = true
     */
    def userByTokenPassword(token: String, now: OffsetDateTime, passwordHash: String): Future[User] = {
        if(token.length == BetterSettings.TOKENLENGTH){
           val action = (for {
            (message, user) <- usersmessages.filter(m => m.token === token && ! m.seen.isDefined).join(users).on( _.userId === _.id ).result.head
            upd <- usersmessages.filter(_.id === message.id).map(_.seen).update(Some(now))
            _ <- users.filter(_.id === user.id).map(_.passwordhash).update(passwordHash)
           } yield(user)).transactionally
           db.run(action).recoverWith{ case ex: NoSuchElementException => Future.failed(ItemNotFoundException(s"could not find message with token that was not seen yet")) }
        } else {
           Future.failed(ValidationException(s"the token was not correct"))
        }
    }
    
    def unseenMailForUser(user: User): Future[Seq[(UserMessage,Message)]] = {
        val query = usersmessages.filter(m => m.userId === user.id && ! m.seen.isDefined).join(messages).on( _.messageId === _.id )
        db.run(query.result)
    }
    
    def setMessageSent(messageId: Long, now: OffsetDateTime): Future[String] = {
        val up = usersmessages.filter(_.id === messageId).map(_.sent).update(Some(now))
        db.run(up).map{ rows => 
           rows match {
             case 0 => val err = "could not find message with id $messageId to set to sent"; betterDBLogger.error(err); err
             case 1 => s"set message with id $messageId to sent"
             case _ => val err = "found multiple messages with id $messageId to set to sent"; betterDBLogger.error(err); err
           }
        }
    }
    
    def setMessageError(um: UserMessage, error: String, now: OffsetDateTime): Future[MessageError] = {
        val me = MessageError(None, um.id.get, error, now)
        db.run((messageserrors returning messageserrors.map(_.id)) += me).map{ lid => me.copy(id=Some(lid))}
    }
    
   /**
    * sets nextGame and closed game
    * 
    */
   def maintainGames(now: OffsetDateTime): Future[Int] = {
      val q = (for{
        closed <- games.filter(g => g.serverStart < now).map(g => (g.gameClosed,g.nextGame)).update((true,false))
        open <- games.filter(g => g.serverStart > now).map(g => (g.gameClosed,g.nextGame)).update((false,false))
        next <- games.filter(g => g.serverStart > now).sortBy(_.serverStart).take(1).result.headOption
        setNext <- games.filter(_.id === next.map(_.id).getOrElse(Some(-1l))).map(_.nextGame).update(true) 
      } yield(setNext)).transactionally
      db.run(q)
   }
   
    def unsentMails(): Future[Seq[(UserMessage,Message,User)]] = {
       val query = 
         for{
           ((um,m),u) <- usersmessages.filter(m => ! m.sent.isDefined && m.send === true) 
                     .join(messages).join(users).on{ case ((um, m), u) =>
                           um.messageId === m.id && um.userId === u.id 
                     }
         }yield{
           (um,m,u)
         }
       db.run(query.result)
   }

}

