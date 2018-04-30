package models

import org.junit.runner._
import org.specs2.runner._
import org.specs2._
import org.specs2.Specification
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class CryptoHelperSpec extends Specification { def is =

    "A CryptoHelper can create a digest for a message and compare the resulting file with the message again" ! validate ^
    end
	
	
	def validate = {
		val secret = "the secret"
		val rand = new scala.util.Random()
		val messageSize = rand.nextInt(10000)
		val arr = new Array[Byte](messageSize)
	  val message = rand.nextBytes(arr)
		val filename = CryptoHelper.fileName(arr, secret)

		CryptoHelper.validate(arr, filename, secret).toOption.get === "valid file"
		val filenameD = filename.replace(filename.substring(5,9), "2013")
		CryptoHelper.validate(arr, filenameD, secret).swap.toOption.get === "invalid file content changed"
		CryptoHelper.validate(arr, filename, secret+" ").swap.toOption.get === "invalid file content changed"
		arr(0) = (arr(0) + 1).toByte
		CryptoHelper.validate(arr, filename, secret).swap.toOption.get === "invalid file content changed"

	}


}




class TimeHelperHumanSpec extends Specification with org.specs2.specification.Tables { def is = s2"""

 TimeHelper can print a nice formatted string of elapsed days, hours, minutes, seconds ${
  "days"   | "hours" | "minutes" | "seconds" |>
   0    !  0  !  0  ! 0  |                 
   0    !  0  !  0  ! 12 |
   0    !  0  !  5  ! 3  |
   0    !  23 !  9  ! 7  |
   4    !  12 !  17 ! 43 |
  { (days, hours, minutes, seconds) => TimeHelper.durationToString((days * (24 * 60 * 60) + hours * (60 * 60) + minutes * 60 + seconds ) * 1000) must_== s"$days days, $hours hours, $minutes minutes, $seconds seconds" }      
 }
"""

 
}
  
  
  
class TimeHelperSpec extends Specification { def is =

   "A TimeHelper can print the duration" ! duration ^
   end

	def duration = {
    val pattern = "yyyy-MM-dd'T'HH:mmxxx"
    val t1 = TimeHelper.fromString("2014-06-13T17:00+02:00", pattern)
    val t2 = TimeHelper.fromString("2014-06-13T17:02+02:00", pattern)
    val res = TimeHelper.compareTimeHuman( t1, t2 )
		res === "0 days, 0 hours, 2 minutes, 0 seconds"
  }


}
  
