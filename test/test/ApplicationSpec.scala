package test


import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.specs2.matcher.JsonMatchers
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import org.specs2.matcher.MatchResult
import org.specs2.matcher.Matcher
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Configuration
import play.api.inject.bind
import play.api.Mode
import java.time.OffsetDateTime
import models._
import JsonHelper._
	     
import play.api.Logger

//TODO: add ApplicationSpec tests from play-gulp-standalone

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification with JsonMatchers {
    val logger = Logger(this.getClass())
    val app = new GuiceApplicationBuilder().configure(
            Configuration.from(
                Map(
                    "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
                    "slick.dbs.default.db.driver" -> "org.h2.Driver",
                    "slick.dbs.default.db.url" -> "jdbc:h2:mem:appspec;TRACE_LEVEL_FILE=4", //TRACE_LEVEL 4 = enable SLF4J
                    "slick.dbs.default.db.user" -> "sa",
                    "slick.dbs.default.db.password" -> "",
                    "play.cache.defaultCache" -> "appspeccache", //prevents error for multiple app 
                    "betterplay.insertdata" -> "test",
                    "betterplay.debug" -> "true"
                )
            )
        )
        .in(Mode.Test)
        .build()         
    
    
    def extractToken(result: Future[play.api.mvc.Result]): Option[String] = {
       (Json.parse(contentAsString((result))) \ "X-AUTH-TOKEN").asOpt[String]
    }  
      
    def setTime(time: OffsetDateTime, authToken: String, message: String) = {
       val nt = JsObject(Seq("serverTime" -> Json.toJson(time)))
       val updt = route(app, FakeRequest(method="POST", path="/api/time").withHeaders(("X-AUTH-TOKEN", authToken)).withJsonBody(nt)).get
       val res = Await.result(updt, 1 second)
       status(updt) must equalTo(OK).setMessage(message)
    }
    
    def setTimeFail(time: OffsetDateTime, authToken: String, message: String) = {
       val nt = JsObject(Seq("serverTime" -> Json.toJson(time)))
       val updt = route(app, FakeRequest(method="POST", path="/api/time").withHeaders(("X-AUTH-TOKEN", authToken)).withJsonBody(nt)).get
       val res = Await.result(updt, 1 second)
       status(updt) must equalTo(UNAUTHORIZED).setMessage(message)
    }
    
    def setBet(authToken: String, bet: ViewableBet, message: String) = {
       val pb = Json.toJson(bet)
       val updatePB = route(app, FakeRequest(method="POST", path=s"/api/bet/${bet.id.get}").withJsonBody(pb).withHeaders(("X-AUTH-TOKEN", authToken))).get
	   	 status(updatePB) must equalTo(OK).setMessage(message)
    }
    
    def setBetFail(authToken: String, bet: ViewableBet, message: String) = {
       val pb = Json.toJson(bet)
       val updatePB = route(app, FakeRequest(method="POST", path=s"/api/bet/${bet.id.get}").withJsonBody(pb).withHeaders(("X-AUTH-TOKEN", authToken))).get
	     contentAsString(updatePB) === """{"error":["game closed since 0 days, 0 hours, 2 minutes, 0 seconds"]}"""
       status(updatePB) must equalTo(406).setMessage(message)
    }
    
    def extractMexCameroon(userBets: String) : (GameWithTeams, ViewableBet) = {      
     val json = try {
       Json.parse(userBets)
     }catch {
       case e: Exception => System.err.println("err parse: "+userBets); throw(e)
     }
	   val gameBets = (json \\ "gameBets").map(x => x.as[Seq[(GameWithTeams,ViewableBet)]]).flatten  //implemented in JsonHelper
	   val (gwt, mexCam) = gameBets.filter{ case(gwt, b) => gwt.team1.name.contains("Mex") && gwt.team2.name.contains("Cam") }.head
	   (gwt, mexCam)
    }
    
    
    def firstGameStart(token: String): OffsetDateTime = {
        val firstGame = route(app, FakeRequest(method="GET", path="/api/game/1").withHeaders(("X-AUTH-TOKEN", token))).get
	      val start = (Json.parse(contentAsString(firstGame)) \ "game" \ "game" \ "serverStart").as[OffsetDateTime]
	      start
    }
    
    def getSpecialBet(username: String, token: String, row: Int): SpecialBetByUser = {
         val specials =  route(app, FakeRequest(method="GET", path=s"/api/user/${username}/specialBets").withHeaders(("X-AUTH-TOKEN", token))).get
         val bets =  (Json.parse(contentAsString(specials)) \\ "templateBets" ).map(x => x.as[Seq[(SpecialBetT,SpecialBetByUser)]]).flatten //implemented in JsonHelper
         bets(row)._2
    }
    
    def change1SpecialBet(username: String, token: String, row: Int, prediction: String, message: String) = {
        val bet = getSpecialBet(username, token, row)
        val changed = Json.toJson(bet.copy(prediction = prediction))
        val updated = route(app, FakeRequest(method="POST", path=s"/api/specialBet").withJsonBody(changed).withHeaders(("X-AUTH-TOKEN", token))).get
        status(updated) must equalTo(OK).setMessage(message) 
        contentAsString(updated)  must /("username" -> username)
        contentAsString(updated)  must /("hadInstructions" -> false)
        val bet2 = getSpecialBet(username, token, row)
        bet2.prediction === prediction 
        bet2.prediction !== bet.prediction
    }
    
    def change1SpecialBetFail(username: String, token: String, row: Int, prediction: String, message: String) = {
        val bet = getSpecialBet(username, token, row)
        val changed = Json.toJson(bet.copy(prediction = prediction))
        val updated = route(app, FakeRequest(method="POST", path=s"/api/specialBet").withJsonBody(changed).withHeaders(("X-AUTH-TOKEN", token))).get
        status(updated) must equalTo(406).setMessage(message) //NotAcceptable
        contentAsString(updated) === """{"error":"game closed since 0 days, 0 hours, 0 minutes, 0 seconds"}"""
        val bet2 = getSpecialBet(username, token, row)
        bet2.prediction must be_==(bet.prediction).setMessage(message)
    }
    
    def checkMexCameroon(username: String, authToken: String, should: Option[GameResult], message: String) = {
       logger.debug(s"mexCam: $username $authToken $should $message")
       val userBetsResult = route(app, FakeRequest(method="GET", path=s"/api/user/$username").withHeaders(("X-AUTH-TOKEN", authToken))).get
       val userBets = contentAsString(userBetsResult)
       val (gwt, mexCam) = extractMexCameroon(userBets)
       mexCam.result must be_==(should).setMessage(message)
    }
    
  "Application" should {
  
  
    "allow login for users, protect routes and allow logout" in new WithApplication(app=app){
       val MexCam = java.time.LocalDateTime.of(2014, 6, 13, 18, 0, 0, 0).atZone(BetterSettings.zoneId()).toOffsetDateTime()//y m d h min
       val betPossible = MexCam.minusMinutes(BetterSettings.viewMinutesToGame() + 2)
       val betForbidden = MexCam.minusMinutes(BetterSettings.viewMinutesToGame() + 1)
       val betVisible = MexCam.minusMinutes(BetterSettings.viewMinutesToGame() - 1)
      
       val up = JsObject(Seq("username" -> JsString("admin"), "password" -> JsString("admin")))
       
       val unau = route(app, FakeRequest("POST", "/api/createBetsForUsers")).get
       status(unau) must equalTo(UNAUTHORIZED)
               
       val unau2 = route(app, FakeRequest("POST", "/api/createBetsForUsers").withHeaders(("X-AUTH-TOKEN", "WRONGTOKEN"))).get
       status(unau2) must equalTo(UNAUTHORIZED)
       
       val res = route(app, FakeRequest(POST, "/api/login").withJsonBody(up)).get      
       val adminUserToken = extractToken(res).get 
	   
       val wau = route(app, FakeRequest(method="POST", path="/api/createBetsForUsers").withHeaders(("X-AUTH-TOKEN", adminUserToken))).get
       status(wau) must equalTo(OK)
	   
	     val upd = JsObject(Seq("email" -> JsString("abcd@abcd.com"), "showname" -> JsBoolean(true), "institute" -> JsString("none"), "icontype" -> JsString("super")))
	     val details = route(app, FakeRequest(method="POST", path="/api/user/details").withJsonBody(upd).withHeaders(("X-AUTH-TOKEN", adminUserToken))).get
	     status(details) must equalTo(OK)
	          
	     val userf = route(app, FakeRequest(method="GET", path="/api/userWithEmail").withHeaders(("X-AUTH-TOKEN", adminUserToken))).get	
	     val user = contentAsString(userf) 	  
	     user must /("firstName" -> "admin")		  
	     user must /("lastName" -> "admin")	
	     user must /("email" -> "abcd@abcd.com")	
	     user must /("icontype" -> "super")	
		   user must /("showName" -> "true")
			 
		   val createUser = JsObject(Seq("username" -> JsString("createduser"), "firstname" -> JsString("Foo"), "lastname" -> JsString("lastName"), "email" -> JsString("email@email.com")))
       val createdUser = route(app, FakeRequest(method="PUT", path="/api/user/create").withJsonBody(createUser).withHeaders(("X-AUTH-TOKEN", adminUserToken))).get
	   	 val createdUserContent = contentAsString(createdUser)
	   	 createdUserContent must /("ok" -> "created user createduser mail not delivered")
	      
	     val token = models.BetterSettings.randomToken()
	     val userTokenPass = JsObject(Seq("token" -> JsString(token), "password" -> JsString("mypassword")))
	     val userByToken = route(app, FakeRequest(method="PUT", path="/api/tokenPassword").withJsonBody(userTokenPass)).get
	     val createdUserToken = extractToken(userByToken).get 
	     val newUser = contentAsString(userByToken)
	     newUser must /("user") */("firstName" -> "Foo") /("username" -> "createduser")
	     
	     val userBetsResult = route(app, FakeRequest(method="GET", path="/api/user/createduser").withHeaders(("X-AUTH-TOKEN", createdUserToken))).get
	     val userBets = contentAsString(userBetsResult)
	     userBets must /("user") */("username" -> "createduser") 
	     userBets must /("specialBets") */("name" -> "topscorer")
	     userBets must /("gameBets") */("goalsTeam1" -> "0.0")
	     val (ugwt,ucam) = extractMexCameroon(userBets)
	          
	     val adminBetsResult = route(app, FakeRequest(method="GET", path="/api/user/admin").withHeaders(("X-AUTH-TOKEN", adminUserToken))).get
	     val adminBets = contentAsString(adminBetsResult)
	     val (agwt,acam) = extractMexCameroon(adminBets)
	    
	     val start = firstGameStart(adminUserToken)
	     val beforeClosing = start.minusMinutes(BetterSettings.closingMinutesToGame() + 1) 
	     setTime(beforeClosing, adminUserToken, s"set time to before start of games ${TimeHelper.log(beforeClosing)}")
	     change1SpecialBet("admin", adminUserToken, 2, "pred1", "changing special bet before game starts admin admintoken")
       change1SpecialBet("createduser", createdUserToken, 2, "pred2", "changing special bet before game starts createduser createdusertoken")
//	     change1SpecialBetFail("createduser", adminUserToken, 2, "pred3", "changing special bet before game starts createduser adminusertoken")
	     
	     
	     val beforeStart = start.minusMinutes(BetterSettings.closingMinutesToGame()) 
	     setTime(beforeStart, adminUserToken, s"set time to before start of games ${TimeHelper.log(beforeStart)}")
       change1SpecialBetFail("admin", adminUserToken, 2, "predX", "changing special bet before game starts admin admintoken")
       change1SpecialBetFail("createduser", createdUserToken, 2, "predY", "changing special bet before game starts createduser createdusertoken")
	     
	     
	     val diff = java.time.Duration.between(agwt.game.serverStart.toLocalDateTime(), MexCam.toLocalDateTime())
	     diff.toMinutes() === 0 //timezones could be a problem in unit tests
	     
	     val SRESULT = Some(GameResult(1,0,false))
	     val GRESULT = Some(GameResult(1,0,true))
	     val NOBET = Some(GameResult(0,0,false))
	     
	     setTimeFail(betPossible, createdUserToken, "only admin user can set time")
	     setTime(betPossible, adminUserToken, "set time possible 1")
	     logger.debug(s"TIMES: now ${TimeHelper.log(BetterSettings.now())} =~= possible: ${TimeHelper.log(betPossible)} visible:${TimeHelper.log(betVisible)} forbidden: ${TimeHelper.log(betForbidden)} orig: ${TimeHelper.log(MexCam)}")
	    
     
	     checkMexCameroon("createduser", createdUserToken, NOBET, "A possible createduser createduser no bet")
	     checkMexCameroon("createduser", adminUserToken, None, "A possible adminuser createduser no bet. not visible yet for other")
	     checkMexCameroon("admin", createdUserToken, None, "A possible createduser adminuser no bet. not visible yet for other")
	     checkMexCameroon("admin", adminUserToken, NOBET, "A possible adminuser adminuser no bet")
	     setTime(betVisible, adminUserToken, "set time visible 1")
	     checkMexCameroon("createduser", createdUserToken, NOBET, "B visible createduser createduser no bet")
	     checkMexCameroon("createduser", adminUserToken, NOBET, "B visible adminuser createduser no bet")
	     checkMexCameroon("admin", createdUserToken, NOBET, "B visible createduser adminuser no bet")
	     checkMexCameroon("admin", adminUserToken, NOBET, "B visible adminuser adminuser no bet")     
	     setBetFail(adminUserToken, acam.copy(result=SRESULT), "C visible adminuser too late")
	     setBetFail(createdUserToken, ucam.copy(result=SRESULT), "C visible createduser too late")
       setTime(betPossible, adminUserToken, "set time possible 2")
	     setBet(adminUserToken, acam.copy(result=SRESULT), "D bettable adminuser")
       setBet(createdUserToken, ucam.copy(result=SRESULT), "D bettable createduser")
	     checkMexCameroon("createduser", createdUserToken, GRESULT, "D bettable  createduser createduser yes bet")
	     checkMexCameroon("createduser", adminUserToken, None, "D bettable  adminuser createduser yes bet")
	     checkMexCameroon("admin", createdUserToken, None, "D bettable createduser adminuser yes bet")
	     checkMexCameroon("admin", adminUserToken, GRESULT, "D bettable adminuser adminuser yes bet")
       setTime(betVisible, adminUserToken, "set time visible 3")
	     checkMexCameroon("createduser", createdUserToken, GRESULT, "E visible  createduser createduser yes bet")
	     checkMexCameroon("createduser", adminUserToken, GRESULT, "E visible   adminuser createduser yes bet")
	     checkMexCameroon("admin", createdUserToken, GRESULT, "E visible  createduser adminuser yes bet")
	     checkMexCameroon("admin", adminUserToken, GRESULT, "E visible  adminuser adminuser yes bet")
	     
         
	     val excelf = route(app, FakeRequest(method="GET", path="/api/statistics/excelAnon").withHeaders(("X-AUTH-TOKEN", createdUserToken))).get
       val excel = Await.result(excelf, 3 second) //TODO: was 1 second why is it slower or blocking???
  //     print(excel)
       excel.body.contentLength.get must be_>(1000L)
	     
	     
       val out = route(app, FakeRequest(POST, "/api/logout").withHeaders(("X-AUTH-TOKEN", adminUserToken))).get
       status(out) must equalTo(OK)
       
       val wou = route(app, FakeRequest(method="POST", path="/api/createBetsForUsers").withHeaders(("X-AUTH-TOKEN", adminUserToken))).get
       status(wou) must equalTo(UNAUTHORIZED)    
       
      
    }
  
  
  
  }
  
  
  
}
