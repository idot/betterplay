package models

import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime
import play.api.db.DB

import scalaz.{\/,-\/,\/-}

object BetterDb {
   import BetterTables._
  
   def allTeams()(implicit s: Session): Seq[Team] = {
       teams.list
   }
  
   def gamesWithTeams()(implicit s: Session): Seq[GameWithTeams] = {
       val gtt = (for{
         (((g, t1), t2),l) <- games.innerJoin(teams).on(_.team1Id === _.id).innerJoin(teams).on(_._1.team2Id === _.id).innerJoin(levels).on(_._1._1.levelId === _.id)
       } yield {
         (g, t1, t2,l)
       })   
       gtt.list.map{ case(g,t1,t2,l) => GameWithTeams(g,t1,t2,l) }
   }
  
   def insertOrUpdateLevelByNr(level: GameLevel)(implicit s: Session): GameLevel = {
       levelByNr(level.level).map{ l =>
           levels.update(l)
           l
       }.getOrElse{
          levels.insert(level)
          level //would be nice to get the level with the new id
       }
   }    
   
   def levelByNr(levelNr: Int)(implicit s: Session): String \/ GameLevel = {
       levels.filter(_.level === levelNr).firstOption.map{ \/-(_) }.getOrElse{ -\/(s"level not found by nr: $levelNr") }
   }
   
   def insertOrUpdateTeamByName(team: Team)(implicit s: Session): Boolean = {
       teamByName(team.name).map{ t =>
          teams.update(t)
          false
       }.getOrElse{
         teams.insert(team)
          true
       }
   }
   
   def teamByName(teamName: String)(implicit s: Session): String \/ Team = {
       teams.filter(_.name === teamName).firstOption.map{ \/-(_) }.getOrElse{ -\/(s"team not found by name: $teamName") }    
   }
   
   
   def deleteGame(game: Game)(implicit s: Session): String \/ String = {
       if(games.filter(_.id === game.id).delete == 1){
          \/-(s"deleted game $game")  
       }else{
         -\/(s"could not delete game $game")
       }
   }
   
   
   
   /**
    * This is for text import   game details | team1name | team2name | levelNr (| == tab)
    * 
    */
   def insertGame(game: Game, team1Name: String, team2Name: String, levelNr: Int)(implicit s: Session): String \/ String = {
         import scalaz._
         import Scalaz._

       val result = ( teamByName(team1Name).validation.toValidationNel |@| teamByName(team2Name).validation.toValidationNel |@| levelByNr(levelNr).validation.toValidationNel ){
           case (team1, team2, level) => (team1.id, team2.id, level.id)
        }
        result.fold(
            err => -\/(err.toList.mkString("\n")),
            succ => succ match {
              case (Some(t1), Some(t2), Some(l)) => {
                 val gamesWithTeamsAndLevel = game.copy(team1id=t1, team2id=t2, levelId=l, result=Result(0,0,false))
                 games.update(gamesWithTeamsAndLevel)
                 \/-(s"updated game $game")
              }
              case _ => -\/("problem with ids of team1, team2 or level")
            }
        )   
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
         ((((g, t1), t2),l),b) <- games.innerJoin(teams).on(_.team1Id === _.id).innerJoin(teams).on(_._1.team2Id === _.id).innerJoin(levels).on(_._1._1.levelId === _.id).innerJoin(bets).on(_._1._1._1.id === _.gameId)
       } yield {
         (g, t1, t2,l,b)
       })   
       gtt.list.map{ case(g,t1,t2,l,b) => (GameWithTeams(g,t1,t2,l),b) }
   }
   
   def updateBet(bet: Bet, user: User)(implicit s: Session){
      //check if game closed
     //check if bet from user
     
   }
   
   def updateSpecialBet(specialBet: SpecialBet, user: User)(implicit s: Session){
      //check if start of games
     //check if specialBet from user
      //set hadInstructions in user
      
     
   }
   
   /**
    * 
    * from ui with correct foreign keys set by ui
    * 
    */
   def updateGame(game: Game)(implicit s: Session){
       //check for settable points?-
      //get game from db 
     //if result != result => recalculateAllpoints
       
   }
   
   /**
    * sets up all bets including special bets
    * 
    */
   def insertUser(user: User)(implicit s: Session){
      s.withTransaction{ 
         val userId = (users returning users.map(_.id)) += user
            val userWithId = user.copy(id=Some(userId))
            users.filter(_.id === userId).firstOption.map{ user =>
              specialbets.insert(SpecialBet(None, None, None, None, None, None, None, None, false, userId ))
              createBetsForGamesForUser(user)
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
       val allGamesWithOptBets = for{
         (g, b) <- games.leftJoin(bets).on(_.id === _.gameId) if b.userId === user.id
       } yield {
         (g, b.id?)
       }
       allGamesWithOptBets.list.collect{ case(g,None) => g }
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
              bets.filter(_.id === b.id).update(betWithPoints)
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
              b <- bets if(b.userId === user.id)
            } yield {
              b.points
            }
            val p = points.list.sum 
            val specialPoints = calculateSpecialPointsForUser(user, specialBetResult).getOrElse(0)
            val userWithPoints = user.copy(points=p, pointsSpecialBet=specialPoints)      
            users.update(userWithPoints)
        }
   }
   
   
   
   /**
    * calculates but does not set special points for user
    * 
    */
   def calculateSpecialPointsForUser(user: User, specialBetResult: Option[SpecialBet])(implicit s: Session): Option[Int] = {
       specialBetResult.map{ spr =>
            specialbets.filter(_.userId === user.id).firstOption.map{ spb =>
                PointsCalculator.calculateSpecialBets(spb, spr)               
            }.getOrElse(0)
       }
   }
   
   
}

