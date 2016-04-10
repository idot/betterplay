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
import org.specs2.concurrent.ExecutionEnv
import scala.concurrent.{Future,Await}
import scala.concurrent.duration._
import org.specs2.matcher.MatchResult
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Configuration
import play.api.inject.bind
import play.api.Mode


import models.ObjectMother


class DBSpec extends Specification 
           with ThrownMessages  
           with org.specs2.specification.mutable.ExecutionEnvironment { def is(implicit ee: ExecutionEnv) = {
   
   val app = new GuiceApplicationBuilder().configure(
            Configuration.from(
                Map(
                    "slick.dbs.default.driver" -> "slick.driver.H2Driver$",
                    "slick.dbs.default.db.driver" -> "org.h2.Driver",
                    "slick.dbs.default.db.url" -> "jdbc:h2:mem:dbspec",
                    "slick.dbs.default.db.user" -> "sa",
                    "slick.dbs.default.db.password" -> ""
                )
            )
        )
        .in(Mode.Test)
        .build()         
             
   val betterDb = app.injector.instanceOf[BetterDb]
   val dbConfig = betterDb.dbConfigProvider.get[JdbcProfile]
   val db = dbConfig.db
   
   import dbConfig.driver.api._

          //testLength   
   def tl[A,B,C[_]](q: Query[A,B,C], expected: Int) = {
         val res = db.run(q.length.result)
         res must be_==(expected).await
   }
   
   def checkAndUpdateGameDetails[S,F](game: Game, user: User, currentTime: DateTime, gameDuration: Int, succ: PartialFunction[(Game, GameUpdate),MatchResult[S]], fail: PartialFunction[Throwable,MatchResult[F]]) = { 
          val f = betterDb.updateGameDetails(game, user, currentTime, gameDuration)
          f.onSuccess(succ)
          f.onFailure(fail)     
   }
   
   def checkAndUpdateGameResults[S,F](game: Game, user: User, currentTime: DateTime, gameDuration: Int,succ: PartialFunction[(Game, GameUpdate),MatchResult[S]], fail: PartialFunction[Throwable,MatchResult[F]]) = { 
          val f = betterDb.updateGameResults(game, user, currentTime, gameDuration)
          f.onSuccess(succ)
          f.onFailure(fail)     
   } 
   
   def checkAndUpdateSpecialBetForUser[S,F](specialBet: SpecialBetByUser, currentTime: DateTime, closingMinutesToGame: Int, submittingUser: User, succ: PartialFunction[String,MatchResult[S]], fail: PartialFunction[Throwable,MatchResult[F]]) = { 
          val f = betterDb.updateSpecialBetForUser(specialBet, currentTime, closingMinutesToGame, submittingUser)
          f.onSuccess(succ)
          f.onFailure(fail)     
   }
   
   def checkTotalPoints(expected: Int) = {
       Await.result(betterDb.allUsers(), 1 seconds).map(_.points).sum === expected
   }
   
  
   
  "DB" should {
    "be able to play a little game" in {
  ///  "be able to play a little game" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase(
  //    options=Map("DATABASE_TO_UPPER" -> "false", "DB_CLOSE_DELAY" -> "-1")))) {
      
      val firstStart = new DateTime(2014, 3, 9, 10, 0)//y m d h min
      val changedStart = firstStart.minusMinutes(30)
                


    
      def insertAdmin(){
		      tl( betterDb.specialbetsuser,0 )
          tl( betterDb.users,0 )
          val admin = betterDb.insertUser(ObjectMother.adminUser, true, true, None, true).toOption.get
          tl( betterDb.users,1 )
		      tl( betterDb.specialbetsuser,12 )
      }

      def getAdmin(): User = {
          Await.result(db.run(betterDb.users.sortBy { _.id }.result).map{ s => s.head }, 1 second)
      }

      def insertTeams(){
          val admin = getAdmin()
          ObjectMother.dummyTeams.map{t => betterDb.insertTeam(t, admin ) }
          tl( betterDb.teams,6 )
      }
      
      def insertLevels(){
          val admin = getAdmin()
          ObjectMother.dummyLevels.map{ l => betterDb.insertLevel(l, admin) }
          tl( betterDb.levels,3 )
      }
        
      def insertPlayers(){
          val admin = getAdmin() 
          ObjectMother.dummyPlayers.map{ p => betterDb.insertPlayer(p, "t1", admin) }
          tl( betterDb.players,6 )
      }

      def insertGames(){
          val admin = getAdmin()
          ObjectMother.dummyGames(firstStart).map{ case(g,t1,t2, l) => betterDb.insertGame(g, t1, t2, l, admin) }
          tl( betterDb.games,3 )
          tl( betterDb.bets,0 )
      }
    
      def insertUsers(){
          val admin = getAdmin()
		      tl( betterDb.specialbetsuser, 12)
          tl( betterDb.bets,0 )
          betterDb.createBetsForGamesForAllUsers(admin)
          tl( betterDb.bets,3 )
          admin.hadInstructions === true
          admin.canBet === true
          val dbusers = new ArrayBuffer[User]()
          ObjectMother.dummyUsers.map{u => betterDb.insertUser(u, false, false, admin.id, true) }.map{ fu =>
            fu.onSuccess{ case us => {
                 us.registeredBy === admin.id && us.isAdmin === false && us.points === 0 && us.canBet === true
                 dbusers.append(us) }
            }
            fu.onFailure{ case err => fail("inserting user") }
          }
		      tl( betterDb.specialbetsuser, 12 ) //? * BetterTables.users.list.size
          tl( betterDb.bets, 3 * 4)
          betterDb.createBetsForGamesForAllUsers(admin)
          tl( betterDb.bets, 3 * 4)
          betterDb.gamesWithBetForUser(dbusers(2)).map{ s => 
            s.flatMap{ case(g, b) => 
               g.level.level === 0
               b.points === 0
              Set(g.team1,g.team2)
            }.toSet.size === 6
          }
      }
    
      def insertSpecialBetTemplates(){
  		  val specialT = ObjectMother.specialTemplates(SpecialBetType.team, firstStart)
  		  val specialP = ObjectMother.specialTemplates(SpecialBetType.player, firstStart)
  		  (specialT ++ specialP).foreach{ t => 
  		  	   betterDb.insertSpecialBetInStore(t)
  		  }
  	  }
  	  
  	
	
      def makeBets1(){
         val users = Await.result(betterDb.allUsers, 1 seconds).sortBy(_.id)
         val gb1 = Await.result(betterDb.gamesWithBetForUser(users(1)), 1 seconds).sortBy(_._1.game.id)
         gb1.size === 3
         val b1 = gb1(0)._2.copy(result=GameResult(1,2,false))
         val upD = betterDb.updateBetResult(b1, users(0), firstStart, 60)
         upD.onSuccess{ case(g,b1,b2) => failure("should not be possible because of time and different user") }
         upD.onFailure{ case fail =>
             tl( betterDb.betlogs,1 )
             fail.getMessage === "user ids differ 2 1\ngame closed since 0 days, 1 hours, 0 minutes, 0 seconds"
 			       Await.result(db.run(betterDb.betlogs.result.head), 1 seconds) === BetLog(Some(1l), users(1).id.get, gb1(0)._1.game.id.get, b1.id.get, 0, -1, 0, -1, firstStart, "no comment")
         }
         val upD2 = betterDb.updateBetResult(b1, users(1), firstStart.minusMinutes(61), 60)
         upD2.onFailure{ case e => "should be possible upD2" }
         upD2.onSuccess{ case (g,b1,b2) =>
           		 tl( betterDb.betlogs,2 )
           		 val q = betterDb.betlogs.sortBy(_.id.desc).result.head
			         Await.result(db.run(q), 1 seconds) === BetLog(Some(2l), users(1).id.get, gb1(0)._1.game.id.get, b1.id.get, 0, 1, 0, 2, firstStart.minusMinutes(61), "no comment")
               b1.result === GameResult(0,0,false)
               b2.result === GameResult(1,2,true)
         }
         val gb2 = Await.result(betterDb.gamesWithBetForUser(users(2)), 1 second).sortBy(_._1.game.id)
         val b2 = gb2(0)._2.copy(result=GameResult(1,3,false)) 
         val upD3 = betterDb.updateBetResult(b2, users(2), firstStart.minusMinutes(61), 60)
         upD3.onFailure{ case e => "should be possible upD3" }
         upD3.onSuccess{ case (g,b1,b2) => 
               b1.result === GameResult(0,0,false)
               b2.result === GameResult(1,3,true)
         }
      }
   
      def updateGames(){
          val admin = getAdmin()
          val p1 = Await.result(betterDb.allPlayers(), 1 second).head.id
          val users = Await.result(betterDb.allUsers(), 1 second).sortBy(_.id)
          val games = Await.result(betterDb.gamesWithBetForUser(users(1)), 1 second).sortBy(_._1.game.id)
          val gameWt = games(0)._1 
          val game1 = gameWt.game
          val t1 = gameWt.team1.id
          checkAndUpdateGameDetails(game1, users(2), firstStart, 90,
              { case succ => fail("should have refused")},
              { case err => err === "must be admin to change game details" }
          )
          
          checkAndUpdateGameDetails(game1, users(0), firstStart, 90, 
            { case succ => fail("should be too late for changes") }, 
            { case err => err  === "game will start in 5x90 minutes no more changes! game closed since 0 days, 7 hours, 30 minutes, 0 seconds" }
          )    
            
          checkAndUpdateGameResults(game1, users(2), firstStart, 90,
           { case succ => fail("should have refused") },
           { case err => err === "must be admin to change game results" }
          )
          
          checkAndUpdateGameResults(game1, users(0), firstStart, 90,
           { case succ => fail("should be too early for changes") },
           { case err => err === "game is still not finished" }
          )
          
        

          
          users(0).hadInstructions === true
          users(1).hadInstructions === false
		  
    		  val usp = Await.result(betterDb.getSpecialBetsSPUForUser(users(2)), 1 seconds)
    		  val sps = usp.sortBy(_.specialbetId)
		  
		      val sp3 = sps(3).copy(prediction="XY")
		      checkAndUpdateSpecialBetForUser(sp3, firstStart, 90, users(2),
		          { case succ => fail("wrong time") },
              { case err => err === "game closed since 0 days, 1 hours, 30 minutes, 0 seconds" }
          )
		      checkAndUpdateSpecialBetForUser(sp3, firstStart.minusMinutes(91), 90, users(3),
		          { case  succ => fail("wrong user") },
              { case err => err === "user ids differ 4 3" }
          )
          checkAndUpdateSpecialBetForUser(sp3, firstStart.minusMinutes(91), 90, users(2),
              {case succ => {
				          succ === "updated special bet with prediction XY"
                  Await.result(betterDb.userWithSpecialBet(users(2).id.get), 1 second)._1.hadInstructions === false      //this is now done in the UI by activating a separate route        
             }},
             { case err => err ===  fail("should work") }
          ) 
      
		  
		   //   db.run(betterDb.specialbetstore.filter(_.id === sp3.specialbetId).map(_.result).update("XY")) //TODO: whu this was done just above?
          
          Await.result(betterDb.startOfGames(), 1 seconds).get === firstStart
          
          //result changes are ignored
          val changes = game1.copy(team1id=game1.team2id,team2id=game1.team1id,result=GameResult(2,2,true),venue="Nowhere",serverStart=changedStart,localStart=changedStart.minusHours(5))
          checkAndUpdateGameDetails(changes, admin, firstStart.minusMinutes(90*5+1), 90,
             { case succ => succ match{ case(g, u) =>
                 u === ChangeDetails
                 g.result === GameResult(0,0,false)
                 g.team1id === game1.team2id
                 g.team2id === game1.team1id
                 g.serverStart === changedStart
                 g.venue === "Nowhere"
             }},
             { case err => fail("early change possible1 "+err) }
          )
          
          Await.result(betterDb.startOfGames(), 1 seconds).get === changedStart
                  
          
          betterDb.calculateAndUpdatePoints(admin)
          checkTotalPoints(0)
          
          //only result changes are taken over
          val gameWithResults = changes.copy(team1id=game1.team1id,team2id=game1.team2id,result=GameResult(1,3,false),venue="Everywhere",serverStart=firstStart,localStart=firstStart.minusHours(5))
          checkAndUpdateGameResults(gameWithResults, admin, changedStart.plusMinutes(91), 90,
             { case  succ => succ match{ case(g, u) =>
                  u === SetResult
                  g.result === GameResult(1,3,true)
                  g.team1id === game1.team2id
                  g.team2id === game1.team1id
                  g.serverStart === changedStart
                  g.venue === "Nowhere"
             }},
             { case err => fail("setting result possible now "+err) }
          )
          
          betterDb.calculateAndUpdatePoints(admin)
          checkTotalPoints(4)
      }
    

 
      def newGames(){
          val admin = getAdmin()
          val finalGameStart = firstStart.plusMinutes(100)
          val finalGame = Game(None, GameResult(1,2,true), 10,100, 3333, finalGameStart.minusHours(5), "local", finalGameStart, "server",  "stadium", "groupC", 4)
          val teams = Await.result(betterDb.allTeams(), 1 seconds).sortBy(_.id)
          val level = Await.result(betterDb.allLevels, 1 seconds).sortBy(_.level).reverse.head
          val gwt = Await.result(betterDb.insertGame(finalGame, teams(0).name, teams(1).name, level.level, admin), 1 seconds).toOption.get
          tl( betterDb.bets, 3 * 4)           
          betterDb.createBetsForGamesForAllUsers(admin)
          tl( betterDb.bets, 4 * 4)
          val betsForGame = Await.result(betterDb.betsWitUsersForGame(gwt.game), 1 seconds).sortBy(_._2.id)
          //user 1 wins the final
          betsForGame.zipWithIndex.foreach{ case((b,u),i) =>
              val bWithR = b.copy(result=GameResult(3,i,false))
              val upD = betterDb.updateBetResult(bWithR, u, finalGameStart.minusMinutes(100), 60)
              upD.onFailure{ case fail => failure(s"should be able to update bet for final game $b $u $i $fail") }
              upD.onSuccess{ case succ =>
                 succ match { case(g,b1,b2) =>
                   b1.result === GameResult(0,0,false)
                   b2.result === GameResult(3,i,true)
              }}  
          }
          val gwr = gwt.game.copy(result=GameResult(3,1,false))  
          checkAndUpdateGameResults(gwr, admin, finalGameStart.plusMinutes(91), 90, 
              { case  succ => succ match{ case(g, u) =>
                u === SetResult
                g.result === GameResult(3,1,true)
                g.team1id === gwt.team1.id.get
                g.team2id === gwt.team2.id.get
                g.serverStart === finalGameStart
                g.venue === "stadium"
                g.levelId === gwt.level.id.get
             }},
             { case err => fail(s"setting result possible for final game $err")}
          )
          
          checkTotalPoints(4)
          betterDb.calculateAndUpdatePoints(admin)
          val pointsBets =  4 + 12 + 5 + 5  //1xexact + 2x tendency          
          val pointsSpecial = 4 
          checkTotalPoints(pointsBets) 
          val players = Await.result(betterDb.allPlayers, 1 seconds).sortBy(_.id)

   //TODO: calculate special bets also       
         
          val usersNow = Await.result(betterDb.allUsers, 1 seconds).sortBy(_.id)
          usersNow.map(_.points).sum === pointsBets
          usersNow.map(_.pointsSpecialBet).sum === pointsSpecial 
          usersNow.map(_.totalPoints).sum === pointsBets + pointsSpecial 
                 

          val inv1 = betterDb.invalidateGame(gwt.game, usersNow(2))
          inv1.onFailure{ case err => err === "only admin user can invalidate games" }
          inv1.onSuccess{ case succ => failure("should be possible only for admin user to invalidate bets") }
      
          val inv2 = betterDb.invalidateGame(gwt.game, admin)
          inv2.onFailure{ case err => failure("admin should be able to invalidate games!") }
          inv2.onSuccess{ case succ => succ === ("invalidated game $game count: 1") } //TODO: $game
     
          betterDb.calculateAndUpdatePoints(admin)
          checkTotalPoints(4)       
  
        
      }
     
      betterDb.dropCreate()
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
      1 === 1
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
  }}

}
