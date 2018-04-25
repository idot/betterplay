package models

import org.junit.runner._
import org.specs2.runner._
import org.specs2._
import org.specs2.Specification

import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class BetterSettingsSpec extends Specification { def is =

    "A bettersettings can create a digest for a message and compare the resulting file with the message again" ! validate ^
    end
	
	
	def validate = {
		val secret = "the secret"
		val rand = new scala.util.Random()
		val messageSize = rand.nextInt(10000)
		val arr = new Array[Byte](messageSize)
	  val message = rand.nextBytes(arr)
		val filename = BetterSettings.fileName(arr, secret)

		BetterSettings.validate(arr, filename, secret).toOption.get === "valid file"
		val filenameD = filename.replace(filename.substring(5,9), "2013")
		BetterSettings.validate(arr, filenameD, secret).swap.toOption.get === "invalid file content changed"
		BetterSettings.validate(arr, filename, secret+" ").swap.toOption.get === "invalid file content changed"
		arr(0) = (arr(0) + 1).toByte
		BetterSettings.validate(arr, filename, secret).swap.toOption.get === "invalid file content changed"

	}


}




class TimeHelperSpec extends Specification with org.specs2.specification.Tables { def is = s2"""

 TimeHelper can print a nice formatted string of elapsed days, hours, minutes, seconds ${
  "days"   | "hours" | "minutes" | "seconds" |>
   0    !  0  !  0  ! 0  |                 
   0    !  0  !  0  ! 12 |
   0    !  0  !  5  ! 3  |
   0    !  23 !  9  ! 7  |
   4    !  12 !  17 ! 43 |
  { (days, hours, minutes, seconds) => TimeHelper.durationToString(java.time.Duration.ofSeconds(days * (24 * 60 * 60) + hours * (60 * 60) + minutes * 60 + seconds )) must_== s"$days days, $hours hours, $minutes minutes, $seconds seconds" }      
 }
"""

}
  
  
  
  
  
