package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models._
import org.joda.time.DateTime
import org.specs2.matcher.ThrownMessages
import scala.collection.mutable.ArrayBuffer
import javax.inject.Inject
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.driver.JdbcProfile
import org.specs2.specification.ExecutionEnvironment
import org.specs2.concurrent.ExecutionEnv
 
class DBSpec @Inject() (betterDb: BetterDb, dbConfigProvider: DatabaseConfigProvider) extends Specification 
           with ThrownMessages  
           with HasDatabaseConfigProvider[JdbcProfile]
           with ExecutionEnvironment { def is(implicit ee: ExecutionEnv) 
  
  import driver.api._
 
  
  "DB" should {
    "be able to play a little game" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase(
      options=Map("DATABASE_TO_UPPER" -> "false", "DB_CLOSE_DELAY" -> "-1")   
      ))) {
      
      
       
      val firstStart = new DateTime(2014, 3, 9, 10, 0)//y m d h min
      val changedStart = firstStart.minusMinutes(30)
         
      def specialBetsUserSize(should: Int) = ((db.run(betterDb.specialbetsuser.length.result)) must be=(should)).await
          
      def insertAdmin(){
		      betterDb.specialbetsuser.result.length === 0
          BetterTables.users.list.size === 0
          val admin = insertUser(ObjectMother.adminUser, true, true, None).toOption.get
          BetterTables.users.list.size === 1
		  BetterTables.specialbetsuser.list.size === 12
      }

      def getAdmin(): User = {
          BetterTables.users.list.sortBy(_.id).head
      }

      def insertTeams(){
          val admin = getAdmin()
          ObjectMother.dummyTeams.map{t => insertOrUpdateTeamByName(t, admin) }.foreach{ r => r.isRight }
          BetterTables.teams.list.size === 6
      }
      
      def insertLevels(){
          val admin = getAdmin()
          ObjectMother.dummyLevels.map{ l => insertOrUpdateLevelByNr(l, admin) }
          BetterTables.levels.list.size === 3
      }
      
      def insertPlayers(){
          val admin = getAdmin() 
          ObjectMother.dummyPlayers.map{ p => insertPlayer(p, "t1", admin) }
          BetterTables.players.list.size === 6
      }
      
      def insertGames(){
          val admin = getAdmin()
          ObjectMother.dummyGames(firstStart).map{ case(g,t1,t2, l) => insertGame(g, t1, t2, l, admin) }.foreach{ r => r.isRight === true }
          BetterTables.games.list.size === 3
          BetterTables.bets.list.size === 0
      }
      
      def insertUsers(){
          val admin = getAdmin()
		  BetterTables.specialbetsuser.list.size === 12
          BetterTables.bets.list.size === 0
          createBetsForGamesForAllUsers(admin)
          BetterTables.bets.list.size === 3
          admin.hadInstructions === true
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
		  BetterTables.specialbetsuser.list.size === 12 * BetterTables.users.list.size
          BetterTables.bets.list.size === (3 * 4)
          createBetsForGamesForAllUsers(admin)
          BetterTables.bets.list.size === (3 * 4)
          val gamesBets = gamesWithBetForUser(dbusers(2))
          gamesBets.flatMap{case(g, b) => 
             g.level.level === 0
             b.points === 0
             Set(g.team1,g.team2)
          }.toSet.size === 6
      }
      
      def insertSpecialBetTemplates(){
		  val specialT = ObjectMother.specialTemplates(SpecialBetType.team, firstStart)
		  val specialP = ObjectMother.specialTemplates(SpecialBetType.player, firstStart)
		  (specialT ++ specialP).foreach{ t => 
		  	   BetterDb.insertSpecialBetInStore(t)
		  }
	  }
	  
      def makeBets1(){
         val users = BetterTables.users.list.sortBy(_.id)
         val gb1 = gamesWithBetForUser(users(1)).sortBy(_._1.game.id)
         gb1.size === 3
         val b1 = gb1(0)._2.copy(result=GameResult(1,2,false))
         updateBetResult(b1, users(0), firstStart, 60).fold(
            fail => {
				BetterTables.betlogs.list.size === 1
				BetterTables.betlogs.list.head === BetLog(Some(1l), users(1).id.get, gb1(0)._1.game.id.get, b1.id.get, 0, -1, 0, -1, firstStart)
				fail === "user ids differ 2 1\ngame closed since 0 days, 1 hours, 0 minutes, 0 seconds"
			},
            succ => failure("should not be possible because of time and different user")  
         )
         updateBetResult(b1, users(1), firstStart.minusMinutes(61), 60).fold(
            fail => {
				failure("should be possible") 
			},
            succ => succ match { case(g,b1,b2) =>
			   BetterTables.betlogs.list.size === 2
			   BetterTables.betlogs.list.sortBy(_.id).reverse.head === BetLog(Some(2l), users(1).id.get, gb1(0)._1.game.id.get, b1.id.get, 0, 1, 0, 2, firstStart.minusMinutes(61))
               b1.result === GameResult(0,0,false)
               b2.result === GameResult(1,2,true)
            }
         )
         val gb2 = gamesWithBetForUser(users(2)).sortBy(_._1.game.id)
         val b2 = gb2(0)._2.copy(result=GameResult(1,3,false)) 
         updateBetResult(b2, users(2), firstStart.minusMinutes(61), 60).fold(
            fail => failure("should be possible") ,
            succ => succ match { case(g,b1,b2) =>
               b1.result === GameResult(0,0,false)
               b2.result === GameResult(1,3,true)
            }
         )
      }
      
      def updateGames(){
          val admin = getAdmin()
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
          
          
      
          
          users(0).hadInstructions === true
          users(1).hadInstructions === false
		  
		  val usp = getSpecialBetsSPUForUser(users(2))
		  val sps = usp.sortBy(_.specialbetId)
		  
		  val sp3 = sps(3).copy(prediction="XY")
		  updateSpecialBetForUser(sp3, firstStart, 90, users(2)).fold(
             err => err === "game closed since 0 days, 1 hours, 30 minutes, 0 seconds",
             succ => fail("wrong time")
          )
		  updateSpecialBetForUser(sp3, firstStart.minusMinutes(91), 90, users(3)).fold(
              err => err === "user ids differ 4 3",
              succ => fail("wrong user")
          )


          updateSpecialBetForUser(sp3, firstStart.minusMinutes(91), 90, users(2)).fold(
             err => err ===  fail("should work"),
             succ => {
				 succ.prediction === "XY"
                 userWithSpecialBet(users(2).id.get).toOption.get._1.hadInstructions === false      //this is now done in the UI by activating a separate route        
             }           
          ) 
		  
		  BetterTables.specialbetstore.filter(_.id === sp3.specialbetId).map(_.result).update("XY")
          
          startOfGames().get === firstStart
          
          //result changes are ignored
          val changes = game1.copy(team1id=game1.team2id,team2id=game1.team1id,result=GameResult(2,2,true),venue="Nowhere",serverStart=changedStart,localStart=changedStart.minusHours(5))
          updateGameDetails(changes, admin, firstStart.minusMinutes(90*5+1), 90).fold(
             err => fail("early change possible1 "+err),
             succ => succ match{ case(g, u) =>
                u === ChangeDetails
                g.result === GameResult(0,0,false)
                g.team1id === game1.team2id
                g.team2id === game1.team1id
                g.serverStart === changedStart
                g.venue === "Nowhere"
             }
          )
          
          startOfGames().get === changedStart
                  
          
          calculatePoints(admin)
          BetterTables.users.list.map(_.points).sum === 0
          
          //only result changes are taken over
          val gameWithResults = changes.copy(team1id=game1.team1id,team2id=game1.team2id,result=GameResult(1,3,false),venue="Everywhere",serverStart=firstStart,localStart=firstStart.minusHours(5))
          updateGameResults(gameWithResults, admin, changedStart.plusMinutes(91), 90).fold(
             err => fail("setting result possible now "+err),
             succ => succ match{ case(g, u) =>
                u === SetResult
                g.result === GameResult(1,3,true)
                g.team1id === game1.team2id
                g.team2id === game1.team1id
                g.serverStart === changedStart
                g.venue === "Nowhere"
             }
          )
          
          calculatePoints(admin)
          BetterTables.users.list.map(_.points).sum === 4
      }
      

 
      def newGames(){
          val admin = getAdmin()
          val finalGameStart = firstStart.plusMinutes(100)
          val finalGame = Game(None, GameResult(1,2,true), 10,100, 3333, finalGameStart.minusHours(5), "local", finalGameStart, "server",  "stadium", "groupC", 4)
          val teams = BetterTables.teams.list.sortBy(_.id)
          val level = BetterTables.levels.list.sortBy(_.level).reverse.head
          val gwt = insertGame(finalGame, teams(0).name, teams(1).name, level.level, admin).toOption.get
          BetterTables.bets.list.size === (3 * 4)           
          createBetsForGamesForAllUsers(admin)
          BetterTables.bets.list.size === (4 * 4)
          val betsForGame = betsWitUsersForGame(gwt.game).sortBy(_._2.id)
          //user 1 wins the final
          betsForGame.zipWithIndex.foreach{ case((b,u),i) =>
              val bWithR = b.copy(result=GameResult(3,i,false))
              updateBetResult(bWithR, u, finalGameStart.minusMinutes(100), 60).fold(
                fail => failure(s"should be able to update bet for final game $b $u $i $fail"),
                succ => succ match { case(g,b1,b2) =>
                   b1.result === GameResult(0,0,false)
                   b2.result === GameResult(3,i,true)
                }
              ) 
          }
          val gwr = gwt.game.copy(result=GameResult(3,1,false))  
          updateGameResults(gwr, admin, finalGameStart.plusMinutes(91), 90).fold(
             err => fail("setting result possible for final game"+err),
             succ => succ match{ case(g, u) =>
                u === SetResult
                g.result === GameResult(3,1,true)
                g.team1id === gwt.team1.id.get
                g.team2id === gwt.team2.id.get
                g.serverStart === finalGameStart
                g.venue === "stadium"
                g.levelId === gwt.level.id.get
             }
          )
          
          BetterTables.users.list.map(_.points).sum === 4  
          calculatePoints(admin)
          val pointsBets =  4 + 12 + 5 + 5  //1xexact + 2x tendency          
          val pointsSpecial = 4 
          BetterTables.users.list.map(_.points).sum === pointsBets 
          val players = BetterTables.players.list.sortBy(_.id)

          
         
      //    calculatePoints(admin)
          BetterTables.users.list.map(_.points).sum === pointsBets
          BetterTables.users.list.map(_.pointsSpecialBet).sum === pointsSpecial 
          BetterTables.users.list.map(_.totalPoints).sum === pointsBets + pointsSpecial 
                 
          val users = BetterTables.users.list.sortBy(_.id)
          invalidateGame(gwt.game, users(2)).fold(
             err => err === "only admin user can invalidate games",
             succ => failure("should be possible only for admin user to invalidate bets")
          )
          
          invalidateGame(gwt.game, admin).isRight === true
          calculatePoints(admin)
          BetterTables.users.list.map(_.points).sum === 4       
  
        
      }
      
  /*    DB.withSession { implicit s: Session => 
        BetterTables.dropCreate()
		    insertSpecialBetTemplates()
        insertAdmin()
        insertTeams()
        insertLevels()
        insertGames()
        insertUsers()
        insertPlayers()
        makeBets1()
        updateGames()
        newGames()
      }*/
    }
    
    
    
    
 //   "select the correct testing db settings by default" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
 //     DB.withSession { implicit s: Session =>
 //       s.conn.getMetaData.getURL must startWith("jdbc:h2:mem:play-test")
  //    }
  //  }
    

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
