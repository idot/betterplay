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
		val rand = new scala.util.Random()
		val messageSize = rand.nextInt(10000)
		val arr = new Array[Byte](messageSize)
	    val message = rand.nextBytes(arr)
		val filename = BetterSettings.fileName(arr)

		BetterSettings.validate(arr, filename).toOption.get === "valid file"
		val filenameD = filename.replace(filename.substring(5,9), "2013")
		BetterSettings.validate(arr, filenameD).swap.toOption.get === "invalid file content changed"
		arr(0) = (arr(0) + 1).toByte
		BetterSettings.validate(arr, filename).swap.toOption.get === "invalid file content changed"
		
	}


}