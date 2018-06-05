package models

import org.junit.runner._
import org.specs2.runner._
import org.specs2._
import org.specs2.Specification
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class DomainSpec extends Specification { def is =

    "The custom exceptions should have a message stored" ! message ^
    "viewable should not always be true" ! testViewableTime^
    "mail generator pattern" ! mailGenerator^
    //   "test mail" ! testMail ^
    end
	
	
	def message = {
      val mess = "errormessage"
		  val ex1 = AccessViolationException(mess)
		  val ex2 = ItemNotFoundException(mess)
		  val ex3 = ValidationException(mess)
		  ex1.getMessage === mess and 	  
		  ex2.getMessage === mess and 
		  ex3.getMessage === mess
	}

  def testMail() = {//disabled this test is just for debugging of mail settings
  //  BetterSettings.setMailPassword("")
  //  val send = MailMessages.sendMail("subject", "body",  MailMessages.address("ido.tamir@vbcf.ac.at", "Ido Tamir"), true)
  //  send === "sent" 
    
  }

  def testViewableTime() = {
      val now = BetterSettings.now()
      val futu = now.plusMinutes(10)
      DomainHelper.viewableTime(futu, now, 9) === false and 
      DomainHelper.viewableTime(futu, now, 11) === true and
      DomainHelper.viewable(10, 3, futu, now, 9) === false and
      DomainHelper.viewable(10, 3, futu, now, 11) === true and
      DomainHelper.viewable(3, 3, futu, now, 9) === true and //your own are alway visible!!!
      DomainHelper.viewable(3, 3, futu, now, 11) === true 
   }
  
   def mailGenerator() = {
       val body = "this is {{username}} {{firstname}} {{lastname}} {{username}} end"
       val user = User(None, "theusername", "thefirstname", "thelastname", "theinstitute", 
        false, "email@email.com", "pwhash", false, false, true, false, true, 0, 0,  "", "", None, false, 
			 FilterSettings("","","")) 
       val message = MailGenerator.personalize("the subject", body, user, 0l)
       message.body === "this is theusername thefirstname thelastname theusername end"
   }

}