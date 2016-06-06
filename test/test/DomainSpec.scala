package models

import org.junit.runner._
import org.joda.time.DateTime
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
		  ex1.getMessage === mess
		  
		  val ex2 = ItemNotFoundException(mess)
		  ex2.getMessage === mess
		  
		  val ex3 = ValidationException(mess)
		  ex3.getMessage === mess
	}

  def testMail() = {//disabled just for debugging of mail settings
    BetterSettings.setMailPassword("")
    val send = MailMessages.sendMail("subject", "body",  MailMessages.address("ido.tamir@vbcf.ac.at", "Ido Tamir"), true)
    send === "sent" 
    
  }

  def testViewableTime() = {
      val now = new DateTime()
      val before = now.minusMinutes(10)
      DomainHelper.viewableTime(before, now, 9) === false 
      DomainHelper.viewableTime(before, now, 10) === true 
      DomainHelper.viewable(10, 3, before, now, 9) === false
      DomainHelper.viewable(10, 3, before, now, 10) === true 
      DomainHelper.viewable(3, 3, before, now, 9) === true //your own are alway visible!!!
      DomainHelper.viewable(3, 3, before, now, 10) === true 
   }
  
   def mailGenerator() = {
       val body = "this is {{username}} {{firstname}} {{lastname}} {{username}} end"
       val user = User(None, "theusername", "thefirstname", "thelastname", "theinstitute", 
        false, "email@email.com", "pwhash", false, false, false, true, 0, 0,  "", "", None,
			 FilterSettings("","","")) 
       val message = MailGenerator.personalize("the subject", body, user, 0l)
       message.body === "this is theusername thefirstname thelastname theusername end"
   }

}