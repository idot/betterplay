package models

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.jasypt.salt.SaltGenerator
import java.time.format.DateTimeFormatter
import  org.jasypt.digest.StandardByteDigester
import scalaz.{\/,-\/,\/-}
import play.api.Logger
import java.security.SecureRandom


object BetterSettings {
  val TOKENLENGTH = 36
	val MAILTIMEOUT = 30	 
	
  val DEBUGTOKEN = "123456789012345678901234567890123456"
	val ZONEID = ZoneId.of("Europe/Vienna")
  
  
  var mailPassword = "" 
    
  def setMailPassword(password: String){
       mailPassword = password
  }
        
  def getMailPassword(): String = {
       mailPassword
  }
        
	var debugTime = localNow()
	var debug = false
	
	val formatter = DateTimeFormatter.ofPattern( "yyyyMMddHHmm" ).withZone(zoneId)
	val logformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zoneId)
    
	def setDebugTime(time: OffsetDateTime){
    Logger.info(s"set time to: ${logformatter.format(time)}")
		debugTime = time
		debug = true	
	}
	
	def zoneId(): ZoneId = {  ZONEID }
	
	def offset(): ZoneOffset = java.time.ZoneOffset.ofHours(0)
	
	def resetTime(){
      Logger.info(s"debug time off")
	    debug = false
	}
	
	def localNow(): OffsetDateTime = OffsetDateTime.now(zoneId())
	
	/**
	 * 
	 * 
	 * @return current time/date
	 */
	def now(): OffsetDateTime = {
		if(debug){
		   debugTime
		} else {
		   localNow()
	  }
	}
	
	
	def digester(date: OffsetDateTime, excelSecret: String): StandardByteDigester = {
		val time = formatter.format(date)	    
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
		val stime = formatter.format(time)
    val dig = digester(time, excelSecret).digest(message)
		val hex = org.jasypt.commons.CommonUtils.toHexadecimal(dig)
		"bets."+stime+"."+hex+".xlsx"	       
	}
	
	def matchDigest(message: Array[Byte], dates: String, md5inHex: String, excelSecret: String): Boolean = {
		val digest = org.jasypt.commons.CommonUtils.fromHexadecimal(md5inHex)
		val date = OffsetDateTime.parse(dates, formatter)
		digester(date, excelSecret).matches(message, digest)
	}
	
	def validate(message: Array[Byte], filename: String, excelSecret: String): String \/ String = {
		val fileNameR = """bets\.(\d{12})\.(\w*)\.xlsx""".r
		filename match {
		    case fileNameR(date, hex) => if(matchDigest(message, date, hex, excelSecret)) \/-("valid file") else -\/("invalid file content changed")
			case _ => -\/("file name pattern not recognized: must be bets.yyyyMMddHHmm.hexdigest.xlsx")
		}
	}
	
	def validate(path: String, filename: String, excelSecret: String): String \/ String = {
	  import java.nio.file.{Files, Paths}
    val byteArray = Files.readAllBytes(Paths.get(path))
	  validate(byteArray, filename, excelSecret)
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

