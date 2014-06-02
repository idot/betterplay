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
	
	def closingMinutesToGame = 60	
		

}

