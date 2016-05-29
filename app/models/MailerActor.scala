package models

import javax.mail.internet.InternetAddress
import play.api.Logger
import akka.actor._
import akka.pattern.ask
import javax.inject._
import play.api.Configuration
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
   val NEWPASSWORD = "new password"
   val FREE = "free"
  
   val immediate = Set(REGISTRATION,NEWPASSWORD)
}

case class ImmediateMail(user: User)
case class TestMail()
case class SendUnsent()

//generate messages
//generate email after recaptcha for new password
//generate email after registration for new password, random string in db as token, after usage set viewed = true

//if param newRegistration => make toastr to remind to set password.
//disable links until password is saved
// task going over sent messages with timestamps of sent => set seen after 48hours
//
object MailMessages {
  val mailLogger = Logger("mail")
   
  val mailSuccess = "mail delivered successfully"
  val error = "mail not delivered"
  
  def address(mail: String, name: String): InternetAddress = new InternetAddress(mail, name)
  
  def sendMail(subject: String, body: String, to: InternetAddress, debug:Boolean): String = {
       mailLogger.debug(s"sending mail $subject ${to.getAddress}")
       
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
       email.setDebug(debug)
       email.send()
  }

}



class SendMailActor @Inject()(configuration: Configuration, betterDb: BetterDb) extends Actor {
   val mailLogger = Logger("mail")
   val mailUser = configuration.getString("betterplay.mail.user").getOrElse("")
   val mailUserTest = configuration.getString("betterplay.mail.testreceiver").getOrElse("")

   
   val debug = BetterSettings.debug || configuration.getBoolean("betterplay.mail.debug").getOrElse(false)

   import scala.concurrent.ExecutionContext.Implicits.global
  
   def mail(um: UserMessage, m: Message, user: User){
       val s = sender()
       mailLogger.debug(m.toString)
       val send = BetterSettings.getMailPassword() != "" && mailUser != ""      
       if(send){
         Future{
           blocking {
              try{
                 val result = MailMessages.sendMail(m.subject, m.body, MailMessages.address(user.email,user.firstName+" "+user.lastName), debug)
                 mailLogger.debug("send result:"+result)
                 s ! MailMessages.mailSuccess
              } catch {
                case e: EmailException => {
                  val error = e.getMessage()
                  mailLogger.error(error)
                  betterDb.setMessageError(um, error, BetterSettings.now)
                  s ! error
                }
              }
           }
         }
       } else {
           mailLogger.error("did not send message")
          s ! MailMessages.error
       }
   }
   
   def testMail(){
       mailLogger.info("received test mail")
       val s = sender()
       Future{  
          blocking {
             val result = MailMessages.sendMail("betterplay test email", "betterplay test body", MailMessages.address(mailUserTest, "Test"), true)
             mailLogger.debug("test send result:"+result)
             s ! MailMessages.mailSuccess
          }   
       }
   }
   
   def receive = {
      case ((um: UserMessage, m: Message, user: User)) ⇒ mail(um,m,user) 
      case TestMail() => testMail() 
      case _ => 
    }
}


class MailerActor @Inject() (configuration: Configuration, betterDb: BetterDb,  @Named("sendMail") sendMailActor: ActorRef) extends Actor {
  val mailLogger = Logger("mail")
  //how often do we go over unsent messages to send them in minutes
  val sendMailInterval = configuration.getInt("betterplay.send.interval").getOrElse(0) 
   
  import scala.concurrent.ExecutionContext.Implicits.global
  val timeout = new Timeout(Duration.create(BetterSettings.MAILTIMEOUT, "seconds"))

  val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], Rate(3, (1.minutes))))
  throttler ! SetTarget(Some(sendMailActor))
  
  if(sendMailInterval > 0){
       context.system.scheduler.schedule(1 minutes, sendMailInterval minutes, self, SendUnsent())
  }
  
  def receive = {
      case ImmediateMail(user) => sendImmediateMailToUser(user)  
      case TestMail() => mailLogger.info("got test mail"); val s = sender(); throttler.ask(TestMail())(timeout).mapTo[String].map(s ! _)
      case SendUnsent() => sendUnsent()
      case _ =>
  }
  
 
  //NOT DONE
  def sendUnsent(){
    blocking{
   //   betterDb.un
      
      
    }
    //get unsent mails
    //send mail
     //success ==> set sent
    //unsuccessful ==> log + retry in minutes
  }

  def sendMail(userMessage: UserMessage, message: Message, user: User, s: ActorRef) {
    throttler.ask((userMessage, message, user))(timeout).mapTo[String]
      .onComplete { mailSent =>
        mailSent match {
          case Success(succ: String) if succ == MailMessages.mailSuccess => {
            betterDb.setMessageSent(userMessage.id.get, new DateTime())
            s ! "sent email"
          }
          case Success(succ: String) if succ == MailMessages.error => {
            s ! MailMessages.error
          }
          case Success(succ: String) => {
            s ! s
          }
          case Failure(_) => {
            s ! _
          }
        }
      }
  }
  
  def sendMails(mails: Seq[(UserMessage,Message)], user: User, s: ActorRef){
      mails.foreach{ case(um,m) => sendMail(um,m,user, s) }
  }
  
  //extra task => throtteler
  def sendImmediateMailToUser(user: User){  
      val s = sender()
      betterDb.unseenMailForUser(user).map{ unsent => 
         val immediate = unsent.filter{ case(um, m) => MessageTypes.immediate.contains(m.messageType)  }
         immediate.size match {
           case 0 => s ! "no unsent messages"
           case _ => sendMails(immediate, user, s)
         }
      }    
  }
  
}
