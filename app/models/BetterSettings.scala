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
	val logger: Logger = Logger(this.getClass())
    val TOKENLENGTH = 36
	val MAILTIMEOUT = 30	 
	
  val DEBUGTOKEN = "123456789012345678901234567890123456"
	val ZONEID = ZoneId.of("Europe/Vienna")
    
  private var _mailSettings = MailSettings("","","","", 0, "") 
  private var _debugTime = now()
	private var _debug = false  
	private var _sentMail = false
	
	def setMailSettings(mailSettings: MailSettings): Unit = {
      _mailSettings = mailSettings
  }
	
  def setSentMail(): Unit = {
      _sentMail = true
  }
  
  def getSentMail(): Boolean = _sentMail
        
  def getMailSettings(): MailSettings = {
       _mailSettings
  }
        
  def debug(): Boolean = _debug
	
    
	def setDebugTime(time: OffsetDateTime): Unit = {
    logger.info(s"set time to: ${TimeHelper.log(time)}")
		_debugTime = time
		_debug = true	
	}
	
	def zoneId(): ZoneId = {  ZONEID }
	
	def offset(): ZoneOffset = java.time.ZoneOffset.ofHours(0)
	
	def resetTime(): Unit = {
      logger.info(s"debug time off")
	    _debug = false
	}
		
	/**
	 * 
	 * 
	 * @return current time/date or debugTime if debug
	 * 
	 * 
	 */
	def now(): OffsetDateTime = {
		if(_debug){
		   _debugTime
		} else {
		   OffsetDateTime.now(zoneId())
	  }
	}
	
	/**
	 * how many minutes before game starts you can change a bet
	 */
	def closingMinutesToGame() = 60	
	
	/**
	 * how many minutes before game starts the bets are visible to other ppl
	 * 
	 */
	def viewMinutesToGame(): Int = 59
	

	
	def randomToken(): String = {
	   if(debug()){
	     DEBUGTOKEN 
	   }else{
	     java.util.UUID.randomUUID().toString()
	   }
	}
	
}

