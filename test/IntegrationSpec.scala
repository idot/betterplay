import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._
import play.api.{Play, Application}
import play.api.inject.guice._

import scala.concurrent.Future


import play.api.libs.json._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class IntegrationSpec extends PlaySpec with OneServerPerTest with OneBrowserPerTest with HtmlUnitFactory {
  def extractToken(result: Future[play.api.mvc.Result]): Option[String] = {
       (Json.parse(contentAsString((result))) \ "AUTH-TOKEN").asOpt[String]
   }  
  
   implicit override def newAppForTest(testData: TestData): Application = {
      new GuiceApplicationBuilder().configure(
          Map("ehcacheplugin" -> "disabled",
              "betterplay.insertdata" -> "false")   
      ).build()
   }
  
  "Application" should {
/*
     "allow login for users, protect routes and allow logout" in {
       val up = JsObject(Seq("username" -> JsString("admin"), "password" -> JsString("admin")))
       
       val unau = route(app, FakeRequest("POST", "/wm2014/api/createBetsForUsers")).get
       status(unau) must equal(UNAUTHORIZED)
               
       val res = route(app, FakeRequest(POST, "/wm2014/login").withJsonBody(up)).get      
       val authToken = extractToken(res).get 
	   
       val wau = route(app, FakeRequest(method="POST", path="/wm2014/api/createBetsForUsers").withHeaders(("X-AUTH-TOKEN", authToken))).get
       status(wau) must equal(OK)
	   
	     val upd = JsObject(Seq("firstName" -> JsString("xyfirst1"), "lastName" -> JsString("xylast1"), "email" -> JsString("abcd@abcd.com"), "icontype" -> JsString("super")))
	     val details = route(app, FakeRequest(method="POST", path="/wm2014/api/user/irrelevant/details").withJsonBody(upd).withHeaders(("X-AUTH-TOKEN", authToken))).get
	     status(details) must equal(OK)
	          
	     val userf = route(app, FakeRequest(method="GET", path="/wm2014/api/userWithEmail").withHeaders(("X-AUTH-TOKEN", authToken))).get	
	     val user = contentAsString(userf) 	  
	 //    user must /("firstName" -> "xyfirst1")		  
	 //    user must /("lastName" -> "xylast1")	
	 //    user must /("email" -> "abcd@abcd.com")	
	 //    user must /("icontype" -> "super")	
			  
       val out = route(app, FakeRequest(POST, "/wm2014/logout").withHeaders(("X-AUTH-TOKEN", authToken))).get
       status(out) must equal(SEE_OTHER)
   //TODO:    redirectLocation(out) must beSome.which(_ == "/")
       
       val wou = route(app, FakeRequest(method="POST", path="/wm2014/api/createBetsForUsers").withHeaders(("X-AUTH-TOKEN", authToken))).get
       status(wou) must equal(UNAUTHORIZED)      
    }
 */   
    "work from within a browser" in {

      go to ("http://localhost:" + port)

      pageSource must include ("TODO: redirect")
    }
  }
}
