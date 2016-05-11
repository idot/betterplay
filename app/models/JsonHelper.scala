package models

import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.JsString
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.JsResult
import scalaz.{\/,-\/,\/-}
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.Writes
import play.api.libs.json.JsObject


object JsonHelper {
   val DATAREGEX = """data:image/(\w*);base64,(.*)""".r
   
   def parseStringImage(s: String):  String \/ (String,Array[Byte]) = {
       s match {
         case DATAREGEX(format, content) => {
            val arr = new Base64().decode(content)
            \/-((format, arr))
         }
         case _ => -\/(s"could not parse image in json ${s.substring(0,40)}...")
       }
   }
   
   def parseDBImage(s: String): String \/ DBImage = {
        s match {
         case DATAREGEX(format, content) => \/-(DBImage(format, content))
         case _ => -\/(s"could not parse image in json: ${s.substring(0,40)}...")
       }
   } 
 
   //data:[<mediatype>][;base64],<data>
   implicit val formatByteArrayPNG = new Format[Array[Byte]] {
      def writes(array: Array[Byte]): JsValue = {
        val str= new Base64().encode(array)
        JsString(s"data:image/png;base64,$str")
      }
      def reads(sarr: JsValue): JsResult[Array[Byte]] = {
          sarr.validate[String].flatMap{ s => 
              parseStringImage(s).fold(
                 err => JsError(err),
                 succ => JsSuccess(succ._2)
              )
          }
      }
   }
     
   implicit val imageFormat = new Format[DBImage] {
      def writes(dbImage: DBImage): JsValue = {
          if(dbImage.format != ""){
            JsString(s"data:image/$dbImage.format;base64,$dbImage.image")
          }else{
            //smallest trasparent image
            //stackoverflow.com/posts/9967193
            JsString("""data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==""")
          }
      }  
      def reads(sarr: JsValue): JsResult[DBImage] = {
          sarr.validate[String].flatMap{ s => 
              parseDBImage(s).fold(
                 err => JsError(err),
                 succ => JsSuccess(succ)
              )
          }
      }
   }
//the user object is never serialized because of the password, and the email and the firstname lastname that should not be shown in UI
//implicit val userFormat = Json.format[User]  DONTUNCOMMENT
//   case class TimeMessage(serverTime: DateTime)
   
//   implicit val timeMessageFormat = Json.format[TimeMessage]
   implicit val filterSettingsFormat = Json.format[FilterSettings]
   implicit val userNoPWFormat = Json.format[UserNoPw]        
   implicit val levelFormat = Json.format[GameLevel]
   implicit val resultFormat = Json.format[GameResult] 

   
   
   implicit val gameFormat = Json.format[Game]
   implicit val betFormat = Json.format[ViewableBet]

   implicit val specialBetByUserFormat = Json.format[SpecialBetByUser]
   implicit val specialBetTFormat = Json.format[SpecialBetT]
   
   implicit val teamFormat = Json.format[Team]
   implicit val playerFormat = Json.format[Player]
   implicit val gameWithTeams = Json.format[GameWithTeams] 
  
   implicit val playerTeamWrite = new Writes[(Player,Team)]{
     def writes(pt: (Player,Team)): JsValue = {
        Json.obj("player" -> pt._1,"team" -> pt._2)
     }
   }
   
   implicit val specialBetsWrite = new Writes[(SpecialBetT,SpecialBetByUser)]{
	   def writes(tb: (SpecialBetT,SpecialBetByUser)): JsValue = {
		   Json.obj("template" -> tb._1, "bet" -> tb._2)
	   }
   }
   
   implicit val gamesBetsWrite = new Writes[(GameWithTeams,ViewableBet)]{
     def writes(gb: (GameWithTeams,ViewableBet)): JsValue = {
        Json.obj("game" -> gb._1, "bet" -> gb._2)       
     }
   }
   
   implicit val betUserWrite = new Writes[(ViewableBet,UserNoPw)]{
 	   def writes(bu: (ViewableBet, UserNoPw)): JsValue = {
	      Json.obj("bet" -> bu._1, "user" -> bu._2 )
	   }
   }
   
 
}


