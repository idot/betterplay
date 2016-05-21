package models

import org.joda.time.DateTime
import org.jasypt.salt.SaltGenerator
import org.joda.time.format.DateTimeFormat
import  org.jasypt.digest.StandardByteDigester
import scalaz.{\/,-\/,\/-}
import play.api.Logger
import java.security.SecureRandom


object BetterSettings {
  val TOKENLENGTH = 36
	val MAILTIMEOUT = 30	 
        val DEBUGTOKEN = "123456789012345678901234567890123456"
	
        var mailPassword = "" 
    
        def setMailPassword(password: String){
            mailPassword = password
        }
        
        def getMailPassword(): String = {
            mailPassword
        }
        
	var debugTime = new DateTime()
	var debug = false
	
	val formatter = DateTimeFormat.forPattern("yyyyMMddHHmm")
	val logformatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
    
	def setDebugTime(time: DateTime){
                Logger.info(s"set time to: ${logformatter.print(time)}")
		debugTime = time
		debug = true	
	}
	
	def resetTime(){
             Logger.info(s"debug time off")
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
	
	
	def digester(date: DateTime, excelSecret: String): StandardByteDigester = {
		val time = formatter.print(date)	    
		val digester = new StandardByteDigester()
		val combined = time+excelSecret 
		val saltdigest = createSalt(time, excelSecret)
		digester.setSaltGenerator(new org.jasypt.salt.StringFixedSaltGenerator((saltdigest)))
		digester
	}
	
	//it only takes 8 bytes from salt, so we digest time + secret to get info into 1. 8 bytes
	def createSalt(time: String, excelSecret: String): String = {
		val combined = time + excelSecret
		val digester = new org.jasypt.digest.StandardStringDigester()
		digester.setIterations(1)
		val generator = new org.jasypt.salt.StringFixedSaltGenerator("irrelevant")
		digester.setSaltGenerator(generator)
	    digester.digest(combined)
	}	
	
	def fileName(message: Array[Byte], excelSecret: String): String = {
		val time = now()
		val stime = formatter.print(time)
    val dig = digester(time, excelSecret).digest(message)
		val hex = org.jasypt.commons.CommonUtils.toHexadecimal(dig)
		"bets."+stime+"."+hex+".xls"	       
	}
	
	def matchDigest(message: Array[Byte], dates: String, md5inHex: String, excelSecret: String): Boolean = {
		val digest = org.jasypt.commons.CommonUtils.fromHexadecimal(md5inHex)
		val date = formatter.parseDateTime(dates)
		digester(date, excelSecret).matches(message, digest)
	}
	
	def validate(message: Array[Byte], filename: String, excelSecret: String): String \/ String = {
		val fileNameR = """bets\.(\d{12})\.(\w*)\.xls""".r
		filename match {
		    case fileNameR(date, hex) => if(matchDigest(message, date, hex, excelSecret)) \/-("valid file") else -\/("invalid file content changed")
			case _ => -\/("file name pattern not recognized: must be bets.yyyyMMddHHmm.hexdigest.xls")
		}
	}
	
	def closingMinutesToGame = 60	
 
	
	
	def randomToken(): String = {
	   if(debug){
	     DEBUGTOKEN 
	   }else{
	     java.util.UUID.randomUUID().toString()
	   }
	}
	
}

