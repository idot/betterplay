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
import org.joda.time.DateTime
import models._

//TODO: add ApplicationSpec tests from play-gulp-standalone

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification with JsonMatchers {
    val app = new GuiceApplicationBuilder().configure(
            Configuration.from(
                Map(
                    "slick.dbs.default.driver" -> "slick.driver.H2Driver$",
                    "slick.dbs.default.db.driver" -> "org.h2.Driver",
                    "slick.dbs.default.db.url" -> "jdbc:h2:mem:appspec;TRACE_LEVEL_FILE=4", //TRACE_LEVEL 4 = enable SLF4J
                    "slick.dbs.default.db.user" -> "sa",
                    "slick.dbs.default.db.password" -> "",
                    "play.cache.defaultCache" -> "appspeccache", //prevents error for multiple app 
                    "betterplay.insertdata" -> "test"
                )
            )
        )
        .in(Mode.Test)
        .build()         
    
    
    def extractToken(result: Future[play.api.mvc.Result]): Option[String] = {
       (Json.parse(contentAsString((result))) \ "AUTH-TOKEN").asOpt[String]
    }  
      
    def setTime(time: DateTime, authToken: String) = {
       val nt = JsObject(Seq("serverTime" -> JsString(JSON.format(time))))
       val updt = route(app, FakeRequest(method="POST", path="'em2016/api/time").withHeaders(("X-AUTH-TOKEN", authToken)).withJsonBody(nt)).get
       val res = Await.result(updt, 1 second)
       res === "set time to debug"
    }
    
  //  def bet(authToken: String, bet: Bet) = {
  //      val viewableBet = 
  //  }
    
    def checkMexCameroon(username: String, authToken: String, should: String) = {
       val userBetsResult = route(app, FakeRequest(method="GET", path=s"/em2016/api/user/$username").withHeaders(("X-AUTH-TOKEN", authToken))).get
       val userBets = contentAsString(userBetsResult)
       userBets must /("gameBets") */("goalsTeam1" -> "XY")
    }
    
//   val appWithRoutes = () => FakeApplication(withRoutes = {
//      case ("GET", "/") => Action{ Ok }
//   }) 
//  
  "Application" should {
	  // cant have more tests because of CACHE/JVM problem
//    "send 404 on a bad request" in new WithApplication(app=app) {
//      route(FakeRequest(GET, "/boum")) must beNone
//    }
//
//    "render the index page" in new WithApplication(app=app){
//      val home = route(FakeRequest(GET, "/")).get
//
//      status(home) must equalTo(OK)
//      contentType(home) must beSome.which(_ == "text/html")
//      contentAsString(home) must contain ("Your new application is ready.")
//    }
  
  
    "allow login for users, protect routes and allow logout" in new WithApplication(app=app){
       val MexCam = new DateTime(2014, 6, 13, 18, 0)//y m d h min
       val betPossible = MexCam.minusMinutes(61)
       val betForbidden = MexCam.minusMinutes(60)
       val betVisible = MexCam.minusMinutes(59)
      
       val up = JsObject(Seq("username" -> JsString("admin"), "password" -> JsString("admin")))
       
       val unau = route(app, FakeRequest("POST", "/em2016/api/createBetsForUsers")).get
       status(unau) must equalTo(UNAUTHORIZED)
               
       val res = route(app, FakeRequest(POST, "/em2016/api/login").withJsonBody(up)).get      
       val authToken = extractToken(res).get 
	   
       val wau = route(app, FakeRequest(method="POST", path="/em2016/api/createBetsForUsers").withHeaders(("X-AUTH-TOKEN", authToken))).get
       status(wau) must equalTo(OK)
	   
	     val upd = JsObject(Seq("email" -> JsString("abcd@abcd.com"), "showname" -> JsBoolean(true), "institute" -> JsString("none"), "icontype" -> JsString("super")))
	     val details = route(app, FakeRequest(method="POST", path="/em2016/api/user/details").withJsonBody(upd).withHeaders(("X-AUTH-TOKEN", authToken))).get
	     status(details) must equalTo(OK)
	          
	     val userf = route(app, FakeRequest(method="GET", path="/em2016/api/userWithEmail").withHeaders(("X-AUTH-TOKEN", authToken))).get	
	     val user = contentAsString(userf) 	  
	     user must /("firstName" -> "admin")		  
	     user must /("lastName" -> "admin")	
	     user must /("email" -> "abcd@abcd.com")	
	     user must /("icontype" -> "super")	
			 user must /("showName" -> "true")
			 
			 val createUser = JsObject(Seq("username" -> JsString("createduser"), "firstname" -> JsString("Foo"), "lastname" -> JsString("lastName"), "email" -> JsString("email@email.com")))
       val createdUser = route(app, FakeRequest(method="PUT", path="/em2016/api/user/create").withJsonBody(createUser).withHeaders(("X-AUTH-TOKEN", authToken))).get
	   	 val createdUserContent = contentAsString(createdUser)
	   	 createdUserContent === "created user createduser mail not delivered"
	      
	     val token = models.BetterSettings.randomToken()
	     val userTokenPass = JsObject(Seq("token" -> JsString(token), "password" -> JsString("mypassword")))
	     val userByToken = route(app, FakeRequest(method="PUT", path="/em2016/api/tokenPassword").withJsonBody(userTokenPass)).get
	     val authToken2 = extractToken(userByToken).get 
	     val newUser = contentAsString(userByToken)
	     newUser must /("user") */("firstName" -> "Foo") /("username" -> "createduser")
	     
	     val userBetsResult = route(app, FakeRequest(method="GET", path="/em2016/api/user/createduser").withHeaders(("X-AUTH-TOKEN", authToken2))).get
	     val userBets = contentAsString(userBetsResult)
	     userBets must /("user") */("username" -> "createduser") 
	     userBets must /("specialBets") */("name" -> "topscorer")
	     userBets must /("gameBets") */("goalsTeam1" -> "0.0")
	     val json = Json.parse(userBets)
	     import JsonHelper._
	     val gameBets = (json \\ "gameBets").map(_.as[(GameWithTeams,ViewableBet)])
	     val mexCam = gameBets.filter{ case(gwt, b) => gwt.team1.name.contains("Mex") && gwt.team2.name.contains("Cam") } 
	   
         
	     val excelf = route(app, FakeRequest(method="GET", path="/em2016/api/statistics/excel").withHeaders(("X-AUTH-TOKEN", authToken2))).get
       val excel = Await.result(excelf, 1 second)
       print(excel)
       excel.body.contentLength.get must be_>(1000l)
	     
	     
       val out = route(app, FakeRequest(POST, "/em2016/api/logout").withHeaders(("X-AUTH-TOKEN", authToken))).get
       status(out) must equalTo(SEE_OTHER)
       redirectLocation(out) must beSome.which(_ == "/")
       
       val wou = route(app, FakeRequest(method="POST", path="/em2016/api/createBetsForUsers").withHeaders(("X-AUTH-TOKEN", authToken))).get
       status(wou) must equalTo(UNAUTHORIZED)    
       
      
    }
  
  
  
  }
  
  
  
}
