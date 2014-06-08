package models

import org.joda.time.DateTime
import org.jasypt.salt.SaltGenerator
import org.joda.time.format.DateTimeFormat
import  org.jasypt.digest.StandardByteDigester
import scalaz.{\/,-\/,\/-}

object BetterSettings {
    
		 
	var debugTime = new DateTime()
	var debug = false
	
	val formatter = DateTimeFormat.forPattern("yyyyMMddHHmm")
	
	def setDebugTime(time: DateTime){
		debugTime = time
		debug = true	
	}
	
	def resetTime(){
	    debug = false
	}
	
	/**
	 * 
	 * 
	 * @return current time/date
	 */
	def now(): DateTime = {
		if(debug){
			debugTime
		}else{
		    new DateTime()
	    }
	}
	
	
	def digester(date: DateTime): StandardByteDigester = {
		val time = formatter.print(date)	    
		val digester = new StandardByteDigester()
		digester.setSaltGenerator(salter(time))
		digester
	}
	
	def salter(date: String): SaltGenerator = {
		 val s = new org.jasypt.salt.FixedStringSaltGenerator()
		 s.setSalt(date)
		 s   	
	}
	
	def fileName(message: Array[Byte]): String = {
		val time = now()
		val stime = formatter.print(time)
        val dig = digester(time).digest(message)
		val hex = org.jasypt.commons.CommonUtils.toHexadecimal(dig)
		"bets."+stime+"."+hex+".xls"	       
	}
	
	def matchDigest(message: Array[Byte], dates: String, md5inHex: String): Boolean = {
		val digest = org.jasypt.commons.CommonUtils.fromHexadecimal(md5inHex)
		val date = formatter.parseDateTime(dates)
		digester(date).matches(message, digest)
	}
	
	def validate(message: Array[Byte], filename: String): String \/ String = {
		val fileNameR = """bets\.(\d{12})\.(\w*)\.xls""".r
		filename match {
		    case fileNameR(date, hex) => if(matchDigest(message, date, hex)) \/-("valid file") else -\/("invalid file content changed")
			case _ => -\/("file name pattern not recognized: must be bets.yyyyMMddHHmm.hexdigest.xls")
		}
	}
	
	def closingMinutesToGame = 60	
		

}

