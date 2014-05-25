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



/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification with JsonMatchers{
    val app = FakeApplication(
        additionalConfiguration = inMemoryDatabase(options=Map("DATABASE_TO_UPPER" -> "false", "DB_CLOSE_DELAY" -> "-1")) 
    )
    def extractToken(result: Future[play.api.mvc.Result]): Option[String] = {
       (Json.parse(contentAsString((result))) \ "AUTH-TOKEN").asOpt[String]
    }  
      
//   val appWithRoutes = () => FakeApplication(withRoutes = {
//      case ("GET", "/") => Action{ Ok }
//   }) 
//  
  "Application" should {

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
       val up = JsObject(Seq("username" -> JsString("name1"), "password" -> JsString("pw1")))
       
       val unau = route(FakeRequest("GET", "/my/user")).get
       status(unau) must equalTo(UNAUTHORIZED)
        
       val res = route(FakeRequest(POST, "/login").withJsonBody(up)).get
       val authToken = extractToken(res).get
       
       val wau = route(FakeRequest(method="GET", path="/my/user").withHeaders(("X-AUTH-TOKEN", authToken))).get
       status(wau) must equalTo(OK)
       
       val out = route(FakeRequest(POST, "/logout").withHeaders(("X-AUTH-TOKEN", authToken))).get
       status(out) must equalTo(SEE_OTHER)
       redirectLocation(out) must beSome.which(_ == "/")
       
       val wou = route(FakeRequest(method="GET", path="/my/user").withHeaders(("X-AUTH-TOKEN", authToken))).get
       status(wou) must equalTo(UNAUTHORIZED)      
    }
  
  
  
  }
  
  
  
}
