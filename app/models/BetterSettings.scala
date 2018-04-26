package models

import java.time.OffsetDateTime
import java.time.ZonedDateTime
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
	

    
	def setDebugTime(time: OffsetDateTime){
    Logger.info(s"set time to: ${TimeHelper.logformatter.format(time)}")
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
	
	def closingMinutesToGame = 60	
	
	
	def randomToken(): String = {
	   if(debug){
	     DEBUGTOKEN 
	   }else{
	     java.util.UUID.randomUUID().toString()
	   }
	}
	
}

