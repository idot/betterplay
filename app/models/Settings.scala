package models

import org.joda.time.DateTime

object BetterSettings {
		 
	/**
	 * 
	 * 
	 * @return current time/date
	 */
	def now(): DateTime = new DateTime()
	
	def closingMinutesToGame = 60	
	

}

