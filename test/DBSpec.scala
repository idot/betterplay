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
 
      def insertAdmin()(implicit s: Session){
          BetterTables.users.list.size === 0
          val admin = insertUser(ObjectMother.adminUser, true, true, None).toOption.get
          BetterTables.users.list.size === 1
      }

      def getAdmin()(implicit s: Session): User = {
          BetterTables.users.list.sortBy(_.id).head
      }

      def insertTeams()(implicit s: Session){
          val admin = getAdmin()
          ObjectMother.dummyTeams.map{t => insertOrUpdateTeamByName(t, admin) }.foreach{ r => r.isRight }
          BetterTables.teams.list.size === 6
      }
      
      def insertLevels()(implicit s: Session){
          val admin = getAdmin()
          ObjectMother.dummyLevels.map{ l => insertOrUpdateLevelByNr(l, admin) }
          BetterTables.levels.list.size === 3
      }
      
      def insertPlayers()(implicit s: Session){
          val admin = getAdmin() 
          ObjectMother.dummyPlayers.map{ p => insertPlayer(p, "t1", admin) }
          BetterTables.players.list.size === 6
      }
      
      def insertGames()(implicit s: Session){
          val admin = getAdmin()
          ObjectMother.dummyGames(firstStart).map{ case(g,t1,t2, l) => insertGame(g, t1, t2, l, admin) }.foreach{ r => r.isRight === true }
          BetterTables.games.list.size === 3
          BetterTables.bets.list.size === 0
      }
      
      def insertUsers()(implicit s: Session){
          val admin = getAdmin()
          BetterTables.bets.list.size === 0
          createBetsForGamesForAllUsers(admin)
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
          createBetsForGamesForAllUsers(admin)
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
         val b1 = gb1(0)._2.copy(result=GameResult(1,2,false))
         updateBetResult(b1, users(0), firstStart, 60).fold(
            fail => fail === "user ids differ 2 1\ngame closed since 0 days, 1 hours, 0 minutes, 0 seconds",
            succ => failure("should not be possible because of time and different user")  
         )
         updateBetResult(b1, users(1), firstStart.minusMinutes(61), 60).fold(
            fail => failure("should be possible") ,
            succ => succ match { case(g,b1,b2) =>
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
      
      def updateGames()(implicit s: Session){
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
          val changes = game1.copy(team1id=game1.team2id,team2id=game1.team1id,result=GameResult(2,2,true),venue="Nowhere",start=changedStart)
          updateGameDetails(changes, admin, firstStart.minusMinutes(90*5+1), 90).fold(
             err => fail("early change possible1 "+err),
             succ => succ match{ case(g, u) =>
                u === ChangeDetails
                g.result === GameResult(0,0,false)
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
          
          
          calculatePoints(None, admin)
          BetterTables.users.list.map(_.points).sum === 0
          
          //only result changes are taken over
          val gameWithResults = changes.copy(team1id=game1.team1id,team2id=game1.team2id,result=GameResult(1,3,false),venue="Everywhere",start=firstStart)
          updateGameResults(gameWithResults, admin, changedStart.plusMinutes(91), 90).fold(
             err => fail("setting result possible now "+err),
             succ => succ match{ case(g, u) =>
                u === SetResult
                g.result === GameResult(1,3,true)
                g.team1id === game1.team2id
                g.team2id === game1.team1id
                g.start === changedStart
                g.venue === "Nowhere"
             }
          )
          
          calculatePoints(None, admin)
          BetterTables.users.list.map(_.points).sum === 4
      }
      
           

 
      def newGames()(implicit s: Session){
          val admin = getAdmin()
          val finalGameStart = firstStart.plusMinutes(100)
          val finalGame = Game(None, GameResult(1,2,true), 10,100, 3333, finalGameStart, "stadium", "groupC")
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
                g.start === finalGameStart
                g.venue === "stadium"
                g.levelId === gwt.level.id.get
             }
          )
          
          BetterTables.users.list.map(_.points).sum === 4  
          calculatePoints(None, admin)
          val pointsBets =  4 + 12 + 5 + 5  //1xexact + 2x tendency          
          val pointsSpecial = 8 // mvp , winner, semi, 3,3,2 user1 , 2x t1 for semi because he's dumb. UI should prevent this
          BetterTables.users.list.map(_.points).sum === pointsBets 
          val players = BetterTables.players.list.sortBy(_.id)

          val sp = SpecialBet(None, players(0).id, players(3).id, gwt.team1.id, gwt.team1.id, gwt.team2.id, teams(2).id, teams(3).id, true, 0)          
         
          calculatePoints(Some(sp), admin)
          BetterTables.users.list.map(_.points).sum === pointsBets
          BetterTables.users.list.map(_.pointsSpecialBet).sum === pointsSpecial 
          BetterTables.users.list.map(_.totalPoints).sum === pointsBets + pointsSpecial 
                 
          val users = BetterTables.users.list.sortBy(_.id)
          invalidateGame(gwt.game, users(2)).fold(
             err => err === "only admin user can invalidate games",
             succ => failure("should be possible only for admin user to invalidate bets")
          )
          
          invalidateGame(gwt.game, admin).isRight === true
          calculatePoints(Some(sp), admin)
          BetterTables.users.list.map(_.points).sum === 4       
          BetterTables.users.list.map(_.pointsSpecialBet).sum === pointsSpecial
          
          val usersSpecialBets = usersWithSpecialBetsAndRank()            
          usersSpecialBets.map(_._2.isSet) === Seq(true,false,false,false)
          usersSpecialBets.map(_._1.totalPoints) === Seq(9, 3, 0, 0)
          usersSpecialBets.map(_._4) === Seq(1,2,3,3)
          usersSpecialBets.map(_._3) === Seq(9, 3, 0, 0)  
        
      }
      
      DB.withSession { implicit s: Session => 
        BetterTables.createTables()
        insertAdmin()
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
