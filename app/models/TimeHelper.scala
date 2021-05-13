package models


import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset



object TimeHelper { 
  
  final val formatter = DateTimeFormatter.ofPattern( "yyyyMMddHHmm" ).withZone(BetterSettings.zoneId())
  final val logformatter = DateTimeFormatter.ISO_DATE_TIME.withZone(BetterSettings.zoneId())
  
  def log(time: OffsetDateTime): String = logformatter.format(time)
   
  implicit object OffsetDateTimeOrdering extends Ordering[OffsetDateTime] { def compare(o1: OffsetDateTime, o2: OffsetDateTime) = o1.compareTo(o2)}
  
  def compareTimeHuman(firstTime: OffsetDateTime, lastTime: OffsetDateTime): String = {   
      val diff = java.time.temporal.ChronoUnit.MILLIS.between(firstTime.toLocalDateTime(), lastTime.toLocalDateTime())
      durationToString(diff)
  } 
  
  def fromString(dates: String, pattern: String): OffsetDateTime = {
      val formatter = DateTimeFormatter.ofPattern( pattern ).withZone(BetterSettings.zoneId())
      ZonedDateTime.parse(dates, formatter).toOffsetDateTime()
  }
  
  def fromYYYYMMddHHmm(date: String): OffsetDateTime = ZonedDateTime.parse(date, formatter).toOffsetDateTime()
  def toYYYYMMddHHmm(date: OffsetDateTime): String = formatter.format(date)
  
  def standardFormatter(): DateTimeFormatter = formatter
  
  final val secPerDay = 3600 * 24
  final val secPerHour = 3600
  
  def durationToString(diff: Long): String = {
      val s = diff / 1000L
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
