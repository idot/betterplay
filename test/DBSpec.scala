package test

import org.specs2.mutable._
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.test._
import play.api.test.Helpers._
import models._
import org.joda.time.DateTime
import org.specs2.matcher.ThrownMessages
import scala.collection.mutable.ArrayBuffer



class DBSpec extends Specification with ThrownMessages {
  

  
  
  "DB" should {
    "be able to play a little game" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase(
      options=Map("DATABASE_TO_UPPER" -> "false", "DB_CLOSE_DELAY" -> "-1")   
      ))) {
      
       
      val firstStart = new DateTime(2014, 3, 9, 10, 0)//y m d h min
      val changedStart = firstStart.minusMinutes(30)
      
      import BetterDb._

      def insertTeams()(implicit s: Session){
          ObjectMother.dummyTeams.map{t => insertOrUpdateTeamByName(t) }.foreach{ r => r must startWith("team inserted:")}
          BetterTables.teams.list.size === 6
      }
      
      def insertLevels()(implicit s: Session){
          ObjectMother.dummyLevels.map{ l => insertOrUpdateLevelByNr(l) }
          BetterTables.levels.list.size === 3
      }
      
      def insertPlayers()(implicit s: Session){
          val admin = BetterTables.users.list.sortBy(_.id).head
          ObjectMother.dummyPlayers.map{ p => insertPlayer(p, "t1", admin) }
          BetterTables.players.list.size === 6
      }
      
      def insertGames()(implicit s: Session){
          ObjectMother.dummyGames(firstStart).map{ case(g,t1,t2, l) => insertGame(g, t1, t2, l) }.foreach{ r => r.isRight === true }
          BetterTables.games.list.size === 3
          BetterTables.bets.list.size === 0
      }
      
      def insertUsers()(implicit s: Session){
          val admin = insertUser(ObjectMother.adminUser, true, true, None).toOption.get 
          BetterTables.bets.list.size === 3
          createBetsForGamesForAllUsers()
          BetterTables.bets.list.size === 3
          BetterTables.specialbets.list.size === 1
          admin.hadInstructions === false
          admin.canBet === true
          val dbusers = new ArrayBuffer[User]()
          ObjectMother.dummyUsers.map{u => insertUser(u, false, false, admin.id) }.foreach{ r =>
            r.fold(
              err => fail("inserting user"),
              us => {
                 us.registeredBy === admin.id && us.isAdmin === false && us.points === 0 && us.canBet === true
                 dbusers.append(us)
              }
            )
          }
          BetterTables.bets.list.size === (3 * 4)
          createBetsForGamesForAllUsers()
          BetterTables.bets.list.size === (3 * 4)
          BetterTables.specialbets.list.size === 4
          val gamesBets = gamesWithBetForUser(dbusers(2))
          gamesBets.flatMap{case(g, b) => 
             g.level.level === 0
             b.points === 0
             Set(g.team1,g.team2)
          }.toSet.size === 6
      }
      
      
      def makeBets1()(implicit s: Session){
         val users = BetterTables.users.list.sortBy(_.id)
         val gb1 = gamesWithBetForUser(users(1)).sortBy(_._1.game.id)
         gb1.size === 3
         val b1 = gb1(0)._2.copy(result=Result(1,2,false))
         updateBetResult(b1, users(0), firstStart, 60).fold(
            fail => fail === "user ids differ 2 1\ngame closed since 0 days, 1 hours, 0 minutes, 0 seconds",
            succ => failure("should not be possible because of time and different user")  
         )
         updateBetResult(b1, users(1), firstStart.minusMinutes(61), 60).fold(
            fail => failure("should be possible") ,
            succ => succ match { case(g,b1,b2) =>
               b1.result === Result(0,0,false)
               b2.result === Result(1,2,true)
            }
         )
         val gb2 = gamesWithBetForUser(users(2)).sortBy(_._1.game.id)
         val b2 = gb2(0)._2.copy(result=Result(1,3,false)) 
         updateBetResult(b2, users(2), firstStart.minusMinutes(61), 60).fold(
            fail => failure("should be possible") ,
            succ => succ match { case(g,b1,b2) =>
               b1.result === Result(0,0,false)
               b2.result === Result(1,3,true)
            }
         )
      }
      
      def updateGames()(implicit s: Session){
          val p1 = BetterTables.players.list.head.id
          val users = BetterTables.users.list.sortBy(_.id)
          val games = gamesWithBetForUser(users(1)).sortBy(_._1.game.id)
          val gameWt = games(0)._1 
          val game1 = gameWt.game
          val t1 = gameWt.team1.id
          updateGameDetails(game1, users(2), firstStart, 90).fold(
             err => err === "must be admin to change game details",
             succ => fail("should have refused")
          )
          updateGameDetails(game1, users(0), firstStart, 90).fold(
             err => err === "game will start in 5x90 minutes no more changes! game closed since 0 days, 7 hours, 30 minutes, 0 seconds"  , 
             succ => fail("should be too late for changes")
          )
          
          updateGameResults(game1, users(2), firstStart, 90).fold(
             err => err === "must be admin to change game results",
             succ => fail("should have refused")
          )
          updateGameResults(game1, users(0), firstStart, 90).fold(
             err => err === "game is still not finished"  , 
             succ => fail("should be too early for changes")
          )
          
          
          val sp = userWithSpecialBet(users(1).id.get).toOption.get._2
          val spn = sp.copy(topScorer=p1,mvp=p1,winningTeam=t1, semi1=t1, semi2=t1)
          
          users(1).hadInstructions === false
          updateSpecialBet(spn, users(2), firstStart, 90).fold(
             err => err === "game closed since 0 days, 1 hours, 30 minutes, 0 seconds",
             succ => fail("wrong time")
          )
          updateSpecialBet(spn, users(2), firstStart.minusMinutes(91), 90).fold(
             err => err === "user ids differ 3 2",
             succ => fail("wrong user")
          ) 
          updateSpecialBet(spn, users(1), firstStart.minusMinutes(91), 90).fold(
             err => err ===  fail("should work"),
             succ => succ match { case (sp,u) =>
               sp.topScorer === p1
               sp.mvp === p1
               sp.winningTeam === t1
               sp.semi1 === t1
               sp.semi2 === t1
               sp.semi3 === None
               sp.semi4 === None
               sp.isSet === true
               userWithSpecialBet(users(1).id.get).toOption.get._1.hadInstructions === true              
             }            
          ) 
          
          startOfGames().get === firstStart
          
          //result changes are ignored
          val changes = game1.copy(team1id=game1.team2id,team2id=game1.team1id,result=Result(2,2,true),venue="Nowhere",start=changedStart)
          updateGameDetails(changes, users(0), firstStart.minusMinutes(90*5+1), 90).fold(
             err => fail("early change possible1 "+err),
             succ => succ match{ case(g, u) =>
                u === ChangeDetails
                g.result === Result(0,0,false)
                g.team1id === game1.team2id
                g.team2id === game1.team1id
                g.start === changedStart
                g.venue === "Nowhere"
             }
          )
          
          startOfGames().get === changedStart
          
          updateSpecialBet(spn, users(1), changedStart.minusMinutes(90), 90).fold(
             err => err === "game closed since 0 days, 0 hours, 0 minutes, 0 seconds",
             succ => fail("wrong time again because of game change ")
          )
          
          
          updateBetsWithPoints()
          BetterTables.users.list.map(_.points).sum === 0
          
          //only result changes are taken over
          val gameWithResults = changes.copy(team1id=game1.team1id,team2id=game1.team2id,result=Result(1,3,false),venue="Everywhere",start=firstStart)
          updateGameResults(gameWithResults, users(0), changedStart.plusMinutes(91), 90).fold(
             err => fail("setting result possible now "+err),
             succ => succ match{ case(g, u) =>
                u === SetResult
                g.result === Result(1,3,true)
                g.team1id === game1.team2id
                g.team2id === game1.team1id
                g.start === changedStart
                g.venue === "Nowhere"
             }
          )
          
          updateBetsWithPoints()
          BetterTables.users.list.map(_.points).sum === 5
      }
      
      
      def newGames()(implicit s: Session){
          //create one more game 
          //make & test for bets
          //set bets
          //set specialbet
         //make tally
         //check leaderboard
        
      }
      
      DB.withSession { implicit s: Session => 
        BetterTables.createTables()
        insertTeams()
        insertLevels()
        insertGames()
        insertUsers()
        insertPlayers()
        makeBets1()
        updateGames()
        newGames()
      }
    }
    
    
    
    
    "select the correct testing db settings by default" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      DB.withSession { implicit s: Session =>
        s.conn.getMetaData.getURL must startWith("jdbc:h2:mem:play-test")
      }
    }
    
//
//    "use the default db settings when no other possible options are available" in new WithApplication {
//      DB.withSession { implicit s: Session =>
//        import BetterDb._
//     //     ddl()
//        //the generated ddl has to be converted to psql with
//        //perl -p -i -e 's/BIGINT GENERATED BY DEFAULT AS IDENTITY\(START WITH 1\) NOT NULL/BIGSERIAL/'
//        s.conn.getMetaData.getURL must equalTo("jdbc:postgresql:better")
//      }
//    }
  }

}
