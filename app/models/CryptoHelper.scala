package models


import java.time.OffsetDateTime
import org.jasypt.salt.SaltGenerator
import java.time.format.DateTimeFormatter
import  org.jasypt.digest.StandardByteDigester
import scalaz.{\/,-\/,\/-}
import play.api.Logger
import java.security.SecureRandom

object CryptoHelper {
  
  def digester(date: OffsetDateTime, excelSecret: String): StandardByteDigester = {
		val time = TimeHelper.formatter.format(date)	    
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
		val time = BetterSettings.now()
		val stime = TimeHelper.toYYYYMMddHHmm(time)
    val dig = digester(time, excelSecret).digest(message)
		val hex = org.jasypt.commons.CommonUtils.toHexadecimal(dig)
		"bets."+stime+"."+hex+".xlsx"	       
	}
	
	def matchDigest(message: Array[Byte], dates: String, md5inHex: String, excelSecret: String): Boolean = {
		val digest = org.jasypt.commons.CommonUtils.fromHexadecimal(md5inHex)
		val date = TimeHelper.fromYYYYMMddHHmm(dates)
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
  

  
}