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
   
   implicit val levelFormat = Json.format[GameLevel]
   implicit val resultFormat = Json.format[Result]
   implicit val userFormat = Json.format[User]
   implicit val gameFormat = Json.format[Game]
   implicit val betFormat = Json.format[Bet]
   implicit val specialBetFormat = Json.format[SpecialBet]
   implicit val teamFormat = Json.format[Team]
   implicit val playerFormat = Json.format[Player]
   implicit val gameWithTeams = Json.format[GameWithTeams] 
  
}


