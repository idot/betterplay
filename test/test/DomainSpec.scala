package models

import org.junit.runner._
import org.specs2.runner._
import org.specs2._
import org.specs2.Specification

import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class DomainSpec extends Specification { def is =

    "The custom exceptions should have a message stored" ! message ^
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


}