package models

import javax.mail.internet.InternetAddress
import play.api.Logger
import akka.actor._
import akka.pattern.ask
import javax.inject._
import play.api.Configuration
import play.api.libs.mailer._
import akka.contrib.throttle._
import akka.contrib.throttle.Throttler._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration._
import akka.util.Timeout
import scala.util.{Try, Success, Failure}
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.concurrent.blocking
import org.apache.commons.mail._

//token could contain expiry date encrypted with secret key

object MessageTypes {
   val REGISTRATION = "registration"
   val FREE = "free"
  
}

case class RegistrationMail(user: User)

//generate messages
//generate email after recaptcha for new password
//generate email after registration for new password, random string in db as token, after usage set viewed = true

//if param newRegistration => make toastr to remind to set password.
//disable links until password is saved
//
object MailMessages {
  val mailSuccess = "mail delivered successfully"
  
  def address(mail: String, name: String): InternetAddress = new InternetAddress(mail, name)
  
  def sendMail(subject: String, body: String, to: InternetAddress): String = {
       import scala.collection.JavaConversions._
       val tos = seqAsJavaList(Seq(to))
       val from =  "Ido Tamir <ido.tamir@vbcf.ac.at>"
    
       val email = new SimpleEmail()
       email.setHostName("webmail.imp.ac.at")
       email.setAuthenticator(new DefaultAuthenticator("ido.tamir", BetterSettings.getMailPassword()))
       email.setSSLCheckServerIdentity(false)
       email.setStartTLSEnabled(true)
       email.setSSLOnConnect(false)
       email.setFrom(from)
       email.setTo(tos)
       email.setSubject(subject)
       email.setMsg(body)
       email.setSmtpPort(587)
       email.setSslSmtpPort("587")
       email.setDebug(true)
       email.send()
  }

}



class SendMailActor @Inject()(configuration: Configuration, mailerClient: MailerClient) extends Actor {
   val mailLogger = Logger("mail")
   val debugMode = false
   
   
   val conf = new SMTPConfiguration("webmail.imp.ac.at", 587, true, true, Some("ido.tamir"), Some(BetterSettings.getMailPassword()), debugMode)
  // val mailer = new SMTPMailer(conf)
   
   import scala.concurrent.ExecutionContext.Implicits.global
  
   def mail(um: UserMessage, m: Message, user: User, actorRef: ActorRef){
       mailLogger.debug(m.toString)
       val send = BetterSettings.getMailPassword() != ""
       
       if(send){
         blocking {
            val result = MailMessages.sendMail(m.subject, m.body, MailMessages.address(user.email,user.firstName+" "+user.lastName))
            mailLogger.debug(result)
            actorRef ! MailMessages.mailSuccess
         }
       } else {
           mailLogger.error("did not send message ")
           actorRef ! MailMessages.mailSuccess
       }

   }
   
   def receive = {
      case ((um: UserMessage, m: Message, user: User)) ⇒ val s = sender(); Future{ mail(um,m,user,s) }
      case _ => 
    }
}


class MailerActor @Inject() (configuration: Configuration, betterDb: BetterDb,  @Named("sendMail") sendMail: ActorRef) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global
  val timeout = new Timeout(Duration.create(BetterSettings.MAILTIMEOUT, "seconds"))

  val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], Rate(3, (1.minutes))))
  
  throttler ! SetTarget(Some(sendMail))
  
  def receive = {
      case RegistrationMail(user) => sendRegistrationMailToUser(user,sender())  
      case _ =>
  }
  
  val config = configuration.getString("my.config").getOrElse("none")
 
 // def sendEmails
  //regular task activated by timing based actor every 20 minutes
  def sendEMail(){
    //get unsent mails
    //send mail
     //success ==> set sent
    //unsuccessful ==> log + retry in minutes
  }
  
  //extra task => throtteler
  def sendRegistrationMailToUser(user: User, sender: ActorRef){  
      betterDb.unsentMailForUser(user).map{ unsent => 
         unsent.filter{ case(um, m) => m.messageType == MessageTypes.REGISTRATION }.headOption match {
           case Some((um,m)) => throttler.ask((um,m,user))(timeout).mapTo[String]
                                  .onComplete{ mailSent => 
                                     mailSent match {
                                       case Success(s: String) if s == MailMessages.mailSuccess => {
                                         betterDb.setMessageSent(um.id.get, new DateTime())
                                         sender ! "sent email"
                                       }
                                       case Failure(_) => {
                                         sender ! _
                                       }
                                     }
                                }
           case None =>  sender ! "no unsent messages"
         }
      }    
  }
  
}
