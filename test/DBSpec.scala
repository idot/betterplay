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
import org.specs2.matcher.Matcher
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Configuration
import play.api.inject.bind
import play.api.Mode


import models.ObjectMother
import org.junit.runner._
import org.specs2.runner._
import org.junit.runner.RunWith


@RunWith(classOf[JUnitRunner])
class DBSpec extends Specification 
           with ThrownMessages  
           with org.specs2.matcher.ContentMatchers
           with org.specs2.specification.mutable.ExecutionEnvironment { def is(implicit ee: ExecutionEnv) = {
   
   val app = new GuiceApplicationBuilder().configure(
            Configuration.from(
                Map(
                    "slick.dbs.default.driver" -> "slick.driver.H2Driver$",
                    "slick.dbs.default.db.driver" -> "org.h2.Driver",
                    "slick.dbs.default.db.url" -> "jdbc:h2:mem:dbspec;TRACE_LEVEL_FILE=4", //TRACE_LEVEL 4 = enable SLF4J
                    "slick.dbs.default.db.user" -> "sa",
                    "slick.dbs.default.db.password" -> "",
                    "play.cache.defaultCache" -> "dbspeccache", //prevents error for multiple app 
                    "betterplay.insertdata" -> "test"
                )
            )
        )
        .in(Mode.Test)
        .build()         
             
   val betterDb = app.injector.instanceOf[BetterDb]
   val dbConfig = betterDb.dbConfigProvider.get[JdbcProfile]
   val db = dbConfig.db
   
   import dbConfig.driver.api._
   

   def AR[A](future: Future[A]) = {
        Await.result(future, 1 second)     
   }
   
   def AE[A](future: Future[A]) = {
        Await.ready(future, 1 second)     
   }
   
   def ARs[A](futures: Seq[Future[A]]) = {
       val seq = Future.sequence(futures) 
       Await.result(seq, 1 second)
   }
   
          //testLength   
   def tl[A,B,C[_]](q: Query[A,B,C], expected: Int, annot: String) = {
         val res = db.run(q.length.result)
         res must be_==(expected).await.updateMessage{ m => s"$m $annot" }
   }
   
   def userPoints(): Int = {
      Await.result(betterDb.allUsers(), 1 seconds).map(_.points).sum
   }
   
   def betPoints(): Int = {
      Await.result(db.run(betterDb.bets.map(_.points).result), 1 second).sum
   }
   
  "DB" should {
    "be able to play a little game" in {
      
      val firstStart = new DateTime(2014, 3, 9, 10, 0)//y m d h min
      val changedStart = firstStart.minusMinutes(30)
                


    
      def insertAdmin(){
		      tl( betterDb.specialbetsuser,0, "A1" )
          tl( betterDb.users,0, "A2"  )
          AR(betterDb.insertUser(ObjectMother.adminUser, true, true, None))
          tl( betterDb.users,1 , "A3")
		      tl( betterDb.specialbetsuser,12, "A4" )
		      //no games yet, so no normal bets!!!
      }

      def getAdmin(): User = {
          AR(db.run(betterDb.users.sortBy { _.id }.result).map{ s => s.head })
      }

      /**
       * inserts have to be blocked because otherwise underterministic! Bad for unit tests!
       */
      def insertTeams(){
          val admin = getAdmin()
          val fs = ObjectMother.dummyTeams.map{t => AR(betterDb.insertTeam(t, admin )) }
          tl( betterDb.teams,6, "T1" )
      }
      
      def insertLevels(){
          val admin = getAdmin()
          val fs = ObjectMother.dummyLevels.map{ l => AR(betterDb.insertLevel(l, admin)) }
          tl( betterDb.levels,3, "L1" )
      }
        
      def insertPlayers(){
          val admin = getAdmin() 
          val fs = ObjectMother.dummyPlayers.map{ p => AR(betterDb.insertPlayer(p, "t1", admin)) }
          tl( betterDb.players,6 , "P1")
      }

      def insertGames(){
          val admin = getAdmin()
          val fs = ObjectMother.dummyGames(firstStart).map{ case(g,t1,t2, l) => AR(betterDb.insertGame(g, t1, t2, l, admin)) }
          tl( betterDb.games,3, "G1" )
          tl( betterDb.bets,0, "G2" )
      }
    
      def insertUsers(){
          val admin = getAdmin()
		      tl( betterDb.specialbetsuser, 12, "U1" )
          tl( betterDb.bets,0 , "U2")
          val bwgAdmin = Await.result(db.run(betterDb.gamesWithoutBetsForUser(admin.id.get).result), 1 second)
          bwgAdmin.length === 3
          AR(db.run(betterDb.createBetsForGamesForUser(admin.id.get)))
           tl( betterDb.bets,3, "U3a" )
          AR(betterDb.createBetsForGamesForAllUsers(admin))
          tl( betterDb.bets,3, "U3b" )
          admin.hadInstructions === true
          admin.canBet === true
          ObjectMother.dummyUsers.map{u => 
                 val us = AR(betterDb.insertUser(u, false, false, Some(admin)))
                 us.registeredBy === admin.id && us.isAdmin === false && us.points === 0 && us.canBet === true
          }
		      tl( betterDb.specialbetsuser, 4 * 12, "U4" ) 
          tl( betterDb.bets, 3 * 4, "U5")
          AR(betterDb.createBetsForGamesForAllUsers(admin))
          tl( betterDb.bets, 3 * 4, "U6")
          AR(betterDb.allUsers()).flatMap{ user =>
		           AR(betterDb.gamesWithBetForUser(user)).map{ case (g,b) => 
               g.level.level === 0
               b.points === 0
               Set(g.team1,g.team2) }
           }.toSet.flatten.size === 6
           
           val user = AR(betterDb.allUsers()).filter(u => u.firstName == "f1").head
           AR(betterDb.updateUserDetails("newmail", "retro", true, "newinst", user)) 
           betterDb.updateUserName(user.username, "joe", "newlastname", user) must throwAn[AccessViolationException](message = "only admin user can update the name").await
           AR(betterDb.updateUserName(user.username, "joe", "newlastname", admin))
           val dbUser = AR(betterDb.allUsers()).filter(u => u.firstName == "joe").head
           dbUser.lastName === "newlastname"
           dbUser.email === "newmail"
           dbUser.icontype === "retro"
           dbUser.institute === "newinst"
           dbUser.hadInstructions === false

           val fs = FilterSettings("foo","bar","baz")
           val updFilterUser = Await.result(betterDb.updateFilterSettings(fs, user), 1 seconds)
           updFilterUser.filterSettings === fs
           
           AR(betterDb.updateUserHadInstructions(user))
           AR(betterDb.allUsers()).filter(u => u.firstName == "joe").head.hadInstructions === true
       
           AR(betterDb.updateUserPassword("newhash" , user))
           AR(betterDb.allUsers()).filter(u => u.firstName == "joe").head.passwordHash === "newhash"
           
           val (u1, specialWithUserId) = AR(betterDb.userWithSpecialBets(user.id.get))
           val allSpecialTemplates = AR(betterDb.allSpecialBetTemplates()).sortBy(_.id)
           specialWithUserId.length === allSpecialTemplates.size
           specialWithUserId.unzip._1.sortBy(_.id) === allSpecialTemplates
           
           val (u2, specialWithUserName) = AR(betterDb.userWithSpecialBets(user.username))
           specialWithUserId === specialWithUserName
           
           
           val (t,spBs) = AR(betterDb.specialBetsByTemplate(allSpecialTemplates(0).id.get))
           t === allSpecialTemplates(0)
           spBs.map(_.userId).sorted === AR(betterDb.allUsers).map(_.id.get).sorted
           
           //message: Message, userId: Long, token: String, send: Boolean, display: Boolean, sendingUser: User)
           val message = Message(None, MessageTypes.REGISTRATION, "the subject", "the message body", admin.id.get)
           val randomToken = BetterSettings.randomToken()
           randomToken.length === BetterSettings.TOKENLENGTH
           val insertToken = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".substring(0, BetterSettings.TOKENLENGTH)
           val m = AR(betterDb.insertMessage(message, user.id.get, insertToken, true, true)) //the same token gets inserted 2x this could create a problem on userByTokenPassword!
           val m2 = AR(betterDb.insertMessage(message, admin.id.get, randomToken, true, true))
           m._1.userId === user.id.get
           m2._1.userId === admin.id.get
           val tokTime = new DateTime()
           
           val unsent = AR(betterDb.unsentMailForUser(user))
           unsent.length === 1
           
           betterDb.userByTokenPassword(insertToken.substring(0, BetterSettings.TOKENLENGTH -1), tokTime, "thehash") must throwAn[ValidationException](message = "the token was not correct").await
           betterDb.userByTokenPassword(insertToken.substring(0, BetterSettings.TOKENLENGTH -1)+"B", tokTime, "thehash") must throwAn[ItemNotFoundException](message = "could not find message with token that was not seen yet").await
           
          // val r = AR(db.run(betterDb.usersmessages.result))
        
           val tokUser = AR(betterDb.userByTokenPassword(insertToken, tokTime, "tokenhash"))
           tokUser.username === user.username
           AR(betterDb.allUsers()).filter(u => u.username == user.username).head.passwordHash === "tokenhash"
      
           val unsentN = AR(betterDb.unsentMailForUser(user))
           unsentN.length === 0
           
           //now we access it again and it should have been used!
           betterDb.userByTokenPassword(insertToken, tokTime, "thehash") must throwAn[ItemNotFoundException](message = "could not find message with token that was not seen yet").await
     
           val sent1 = AR(betterDb.setMessageSent(m2._1.id.get, new DateTime))
           sent1 === s"set message with id ${m2._1.id.get} to sent"
           
           val sent2 = AR(betterDb.setMessageSent(m2._1.id.get, new DateTime))
           sent2 === s"set message with id ${m2._1.id.get} to sent"
      }
    
      def insertSpecialBetTemplates(){
  		  val specialT = ObjectMother.specialTemplates(SpecialBetType.team, firstStart)
  		  val specialP = ObjectMother.specialTemplates(SpecialBetType.player, firstStart)
  		  (specialT ++ specialP).foreach{ t => 
  		  	   AR(betterDb.insertSpecialBetInStore(t))
  		  }
  	  }
  	  
  	
	
      def makeBets1(){
         val users = AR(betterDb.allUsers).sortBy(_.id)
         val gb1 = AR(betterDb.gamesWithBetForUser(users(1))).sortBy(_._1.game.id)
         gb1.size === 3
         val game = gb1(0)._1.game
         val b1 = gb1(0)._2.copy(result=GameResult(1,2,false))
                                                                         //current time
         betterDb.compareBet(false, 4, 3, 10, 11, firstStart, firstStart.minusMinutes(5), 10).fold(
             fail => fail.list.toList.mkString(";") === "user ids differ 4 3;game ids differ 10 11;game closed since 0 days, 0 hours, 5 minutes, 0 seconds;user has not paid, no dice",
             succ => fail("should be invalid")   
         )
         
         //update the result in the database and check the return values; this bet is submitted by the wrong user and too late!
         AR(db.run(betterDb.betlogs.size.result)) === 0
         val upD = AR(betterDb.updateBetResult(b1, users(0), firstStart, 60))
         AR(db.run(betterDb.betlogs.size.result)) === 1
         val timeDiff = org.joda.time.Minutes.minutesBetween(gb1(0)._1.game.serverStart, firstStart)
         timeDiff.getMinutes === 0
         upD._5 === Seq("user ids differ 2 1", "game closed since 0 days, 1 hours, 0 minutes, 0 seconds")
         tl( betterDb.betlogs,1, "Bet1" )
         Await.result(db.run(betterDb.betlogs.result.head), 1 seconds) === BetLog(Some(1l), users(1).id.get, gb1(0)._1.game.id.get, gb1(0)._1.game.serverStart, b1.id.get, 0, -1, 0, -1, firstStart, "user ids differ 2 1;game closed since 0 days, 1 hours, 0 minutes, 0 seconds")
    
         //update the result in the database and check the return values; this bet is submitted successfully; will be tendency points
         val upD2 = AR(betterDb.updateBetResult(b1, users(1), firstStart.minusMinutes(61), 60))
         upD2._5 === Nil
       	 tl( betterDb.betlogs,2, "Bet2" )
         val q = betterDb.betlogs.sortBy(_.id.desc).result.head
			   Await.result(db.run(q), 1 seconds) === BetLog(Some(2l), users(1).id.get, gb1(0)._1.game.id.get, gb1(0)._1.game.serverStart, b1.id.get, 0, 1, 0, 2, firstStart.minusMinutes(61), "regular update")
         upD2._2.result === GameResult(0,0,false)
         upD2._3.result === GameResult(1,2,true)
         //now check if the value in the database was really changed
         val betFromDb1 = AR(betterDb.betsWitUsersForGame(game)).filter{ case(b,u) => u == users(1) }.map{ case(b,u) => b }.head
         betFromDb1 === upD2._3
 
         
         //update the result for a different user; will be exact Points
         val (bet2,u) = AR(betterDb.betsWitUsersForGame(game)).filter{ case(b,u) => u == users(2) }.head
         val bet2u = bet2.copy(result=GameResult(1,3,false))
         val upD3 = AR(betterDb.updateBetResult(bet2u, users(2), firstStart.minusMinutes(61), 60))
         upD3._2.result === GameResult(0,0,false)
         upD3._3.result === GameResult(1,3,true)   
         val betFromDb2 = AR(betterDb.betsWitUsersForGame(game)).filter{ case(b,u) => u == users(2) }.map{ case(b,u) => b }.head
         betFromDb2 === upD3._3

         
      }
   
      def updateGames(){
          val admin = getAdmin()
          val p1 = Await.result(betterDb.allPlayers(), 1 second).head.id
          val users = Await.result(betterDb.allUsers(), 1 second).sortBy(_.id)
          val games = Await.result(betterDb.gamesWithBetForUser(users(1)), 1 second).sortBy(_._1.game.id)
          val gameWt = games(0)._1 
          val game1 = gameWt.game
          val t1 = gameWt.team1.id
          
          
          betterDb.updateGameDetails(game1, users(2), firstStart, 90) must throwAn[AccessViolationException]("only admin user can change game details").await
          
          val errDet1 = "game will start in 5x90 minutes no more changes! game closed since 0 days, 7 hours, 30 minutes, 0 seconds"
          betterDb.updateGameDetails(game1, users(0), firstStart, 90) must throwAn[ValidationException](errDet1).await
          
                          
          betterDb.updateGameResults(game1, users(2), firstStart, 90) must throwAn[AccessViolationException](message="must be admin to change game results").await
        
                    
          betterDb.updateGameResults(game1, users(0), firstStart, 90) must throwAn[ValidationException](message="game 1 is still not finished").await 
    
                
          users(0).hadInstructions === true
          users(2).hadInstructions === false
		  
    		  val (udb, specials) = Await.result(betterDb.userWithSpecialBets(users(2).id.get), 1 seconds)
    		  val (t, usp) = specials.unzip
    		  val sps = usp.sortBy(_.specialbetId)
		      val sp3 = sps(3).copy(prediction="XY")
		      
		      betterDb.updateSpecialBetForUser(sp3, firstStart, 90, users(2)) must throwAn[ValidationException](message="game closed since 0 days, 1 hours, 30 minutes, 0 seconds").await
            
		      sp3.userId !== users(3).id.get
		      betterDb.updateSpecialBetForUser(sp3, firstStart.minusMinutes(91), 90, users(3)) must throwAn[ValidationException](message="user ids differ 4 3").await
      
          
          Await.result(betterDb.updateSpecialBetForUser(sp3, firstStart.minusMinutes(91), 90, users(2)), 1 seconds)
		      val uwsb = Await.result(betterDb.userWithSpecialBets(users(2).id.get), 1 second)
		      uwsb._1.hadInstructions === false      //this is now done in the UI by activating a separate route        
          uwsb._2.unzip._2.filter(sb => sb.id == sp3.id).head.prediction === "XY"        
		  
          //now set the special bet result on specialbetstore
          val setSpR1 = Await.result(betterDb.setSpecialBetResult(sp3.specialbetId, "XY", admin), 1 seconds)
	        setSpR1 === "updated special bet with result XY" 
              
          Await.result(betterDb.startOfGames(), 1 seconds).get === firstStart
          
          //result changes are ignored
          val changes = game1.copy(team1id=game1.team2id,team2id=game1.team1id,result=GameResult(2,2,true),venue="Nowhere",serverStart=changedStart,localStart=changedStart.minusHours(5))
          val (g,u) = Await.result(betterDb.updateGameDetails(changes, admin, firstStart.minusMinutes(90*5+1), 90), 1 seconds)
          u === ChangeDetails
          g.result === GameResult(0,0,false)
          g.team1id === game1.team2id
          g.team2id === game1.team1id
          g.serverStart === changedStart
          g.venue === "Nowhere"

          Await.result(betterDb.startOfGames(), 1 seconds).get === changedStart
          
          //we check and there should not be any points because the result has not been set yet
          AR(betterDb.calculateAndUpdatePoints(admin))
          userPoints() === 0
          
          //only result changes are taken over
          //update the game with the results
          val gameWithResults = changes.copy(team1id=game1.team1id,team2id=game1.team2id,result=GameResult(1,3,false),venue="Everywhere",serverStart=firstStart,localStart=firstStart.minusHours(5))
          val (g2,u2) = Await.result(betterDb.updateGameResults(gameWithResults, admin, changedStart.plusMinutes(91), 90), 1 seconds)
          u2 === SetResult
          g2.result === GameResult(1,3,true)
          g2.team1id === game1.team2id
          g2.team2id === game1.team1id
          g2.serverStart === changedStart
          g2.venue === "Nowhere"          
          
          //now get the same game from the database and compare it with the return value from the 
          val gwrDB = Await.result(betterDb.getGameByNr(gameWithResults.nr), 1 seconds)
          gwrDB.game === g2
          
          val gwbs = Await.result(db.run(betterDb.gamesWithLevelsAndBetsSet()), 1 seconds)
          gwbs.size === 2
          
          betPoints() === 0 
          Await.result(db.run(betterDb.updateBetsWithPoints()), 1 seconds)
          betPoints() === 4 
          AR(betterDb.calculateAndUpdatePoints(admin))
          betPoints() === 4 
          userPoints() === 4
        
          AR(betterDb.usersWithSpecialBetsAndRank()).map{ case(u,sp,i) => (u.id.get,i) } === Seq((3,1),(2,2),(4,3),(1,3))
          
      }
    

 
      def newGames(){
          val admin = getAdmin()
          val finalGameStart = firstStart.plusMinutes(100)
          val finalGame = Game(None, GameResult(1,2,true), 10,100, 3333, finalGameStart.minusHours(5), "local", finalGameStart, "server",  "stadium", "groupC", 4)
          val teams = Await.result(betterDb.allTeams(), 1 seconds).sortBy(_.id)
          val level = Await.result(betterDb.allLevels, 1 seconds).sortBy(_.level).reverse.head
          val gwt = Await.result(betterDb.insertGame(finalGame, teams(0).name, teams(1).name, level.level, admin), 1 seconds).toOption.get
          val gameCount = AR(betterDb.allGamesWithTeams()).size
          val userCount =  AR(betterDb.allUsers()).size
          tl( betterDb.bets, 3 * 4, "ng1")           
          AR(betterDb.createBetsForGamesForAllUsers(admin))
          tl( betterDb.bets, gameCount * userCount, "ng2")
          val betsForGame = Await.result(betterDb.betsWitUsersForGame(gwt.game), 1 seconds).sortBy(_._2.id)
          //user 1 wins the final
          betsForGame.zipWithIndex.foreach{ case((b,u),i) =>
              val bWithR = b.copy(result=GameResult(3,i,false))
              val (g,b1,b2,l, err) = AR(betterDb.updateBetResult(bWithR, u, finalGameStart.minusMinutes(100), 60))
              b1.result === GameResult(0,0,false)
              b2.result === GameResult(3,i,true)
          }
          val gwr = gwt.game.copy(result=GameResult(3,1,false))  
          val (g,u) = AR(betterDb.updateGameResults(gwr, admin, finalGameStart.plusMinutes(91), 90))
          u === SetResult
          g.result === GameResult(3,1,true)
          g.team1id === gwt.team1.id.get
          g.team2id === gwt.team2.id.get
          g.serverStart === finalGameStart
          g.venue === "stadium"
          g.levelId === gwt.level.id.get
          //TODO: compare with db values
          
          userPoints() === 4
          AR(betterDb.calculateAndUpdatePoints(admin))
          val pointsBets =  4 + 12 + 5 + 5  //1xexact + 2x tendency          
          val pointsSpecial = 4 
          betPoints() === pointsBets
          userPoints() === pointsBets
          val players = Await.result(betterDb.allPlayers, 1 seconds).sortBy(_.id)

     
         
          val usersNow = Await.result(betterDb.allUsers, 1 seconds).sortBy(_.id)
          usersNow.map(_.points).sum === pointsBets
          usersNow.map(_.pointsSpecialBet).sum === pointsSpecial 
          usersNow.map(_.totalPoints).sum === pointsBets + pointsSpecial 
                 

          betterDb.invalidateGame(gwt.game, usersNow(2)) must throwAn[AccessViolationException].await
         
          AR(betterDb.invalidateGame(gwt.game, admin)) === s"invalidated game ${gwt.game.id.get} count: 1"
          AR(betterDb.getGameByNr(gwt.game.nr)).game.result.isSet === false
          
          AR(betterDb.calculateAndUpdatePoints(admin))
          userPoints() === 4  
      
          AR(betterDb.allUsersWithRank()).map{ case(u,r) => (u.id.get, r)} === Seq((3,1),(2,2),(4,3),(1,3))
        
          
          val excel = ExcelData.generateExcel(betterDb, firstStart, admin.id.get)
          val bos = new java.io.BufferedOutputStream(new java.io.FileOutputStream("testData/excel.xls"))
          Stream.continually(bos.write(excel))
          bos.close()
          new java.io.File("testData/excel.xls") must haveSameMD5As(new java.io.File("testData/excel.expect.xls"))
          
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
      success
    }

    
    
    
   }}

}
