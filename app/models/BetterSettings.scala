package models

import org.joda.time.DateTime

object BetterSettings {
    
		 
	var debugTime = new DateTime()
	var debug = false
	
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
	
	def fileName(message: Array[Byte]): String = {
		val digest = new org.jasypt.digest.StandardByteDigester().digest(message)
		val hex = org.jasypt.commons.CommonUtils.toHexadecimal(digest)
        val format = org.joda.time.format.DateTimeFormat.forPattern("yyyyMMddHHmm")
		val time = format.print(now())
		"bets."+time+"."+hex+".xls"	       
	}
	
	def closingMinutesToGame = 60	
		

}

