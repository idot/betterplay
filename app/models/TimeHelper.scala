package models


import java.time.OffsetDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset



object TimeHelper { 
  final val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(BetterSettings.ZONEID)
  
  implicit object OffsetDateTimeOrdering extends Ordering[OffsetDateTime] { def compare(o1: OffsetDateTime, o2: OffsetDateTime) = o1.compareTo(o2)}
  
  def compareTimeHuman(firstTime: OffsetDateTime, lastTime: OffsetDateTime): String = {
      val d = Duration.between(firstTime.toLocalDate(), lastTime.toLocalDate())
      durationToString(d)
  } 
  
  def standardFormatter(): DateTimeFormatter = formatter
  
  final val secPerDay = 3600 * 24
  final val secPerHour = 3600
  
  def durationToString(duration: Duration): String = {
      val s = duration.toMillis() / 1000
      val days = s / secPerDay
      val sdays = s - (days * secPerDay)
      val hours =  sdays / secPerHour
      val shours = sdays - (hours * secPerHour)
      val minutes = shours / 60
      val sminutes = shours - (minutes * 60)
      val seconds = sminutes
      val result = s"$days days, $hours hours, $minutes minutes, $seconds seconds"     
      result  
  }
  
}