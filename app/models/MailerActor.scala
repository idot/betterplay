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
import java.time.OffsetDateTime
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

case class MailSettings(from: String, hostname: String, login: String, password: String, port: Int, testReceiver: String){
   def valid(): Boolean = password != ""
   
   override def toString(): String = s"$from $hostname $login XXXXXXXX $port $testReceiver"
}

object MailSettings {
 
  
  def fromConfig(config: Configuration): MailSettings = {
      MailSettings(
        config.getOptional[String]("betterplay.mail.from").getOrElse(""),
        config.getOptional[String]("betterplay.mail.host").getOrElse(""),
        config.getOptional[String]("betterplay.mail.login").getOrElse(""),
        config.getOptional[String]("betterplay.mail.password").getOrElse(""),
        config.getOptional[Int]("betterplay.mail.port").getOrElse(-1),
        config.getOptional[String]("betterplay.mail.testreceiver").getOrElse("")
      )
  }
  
}

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
  
  def sendMail(subject: String, body: String, to: InternetAddress, mailSettings: MailSettings, debug:Boolean): String = {
       mailLogger.debug(s"sending mail $subject ${to.getAddress}")
       
       val tos = scala.collection.JavaConverters.seqAsJavaList(Seq(to))
       val from =  mailSettings.from
       
       val email = new SimpleEmail()
       email.setHostName(mailSettings.hostname)
       email.setAuthenticator(new DefaultAuthenticator(mailSettings.login, mailSettings.password))
       email.setSSLCheckServerIdentity(false)
       email.setStartTLSEnabled(true)
       email.setSSLOnConnect(true)
       email.setFrom(from)
       email.setTo(tos)
       email.setSubject(subject)
       email.setMsg(body)
       email.setSmtpPort(mailSettings.port)
       email.setSslSmtpPort(mailSettings.port.toString)
       email.setDebug(debug)
       email.send()
  }

}



class SendMailActor @Inject()(configuration: Configuration, betterDb: BetterDb) extends Actor {
   val mailLogger = Logger("mail")


   
   val debug = BetterSettings.debug || configuration.getOptional[Boolean]("betterplay.mail.debug").getOrElse(false)

   import scala.concurrent.ExecutionContext.Implicits.global
  
   def mail(um: UserMessage, m: Message, user: User){
       val s = sender()
       mailLogger.debug(m.toString)
       val send = BetterSettings.getMailSettings().valid
       if(send){
         Future{
           blocking {
              try{
                 val result = MailMessages.sendMail(m.subject, m.body, MailMessages.address(user.email,user.firstName+" "+user.lastName), BetterSettings.getMailSettings(), debug)
                 mailLogger.info(s"send ${user.username} ${m.subject} result: $result ")
                 BetterSettings.setSentMail()
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
             val result = MailMessages.sendMail("betterplay test email", MailGenerator.testMail(configuration), MailMessages.address(BetterSettings.getMailSettings().testReceiver, "Test"),  BetterSettings.getMailSettings(), true)
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
  val sendMailInterval = configuration.getOptional[Int]("betterplay.send.interval").getOrElse(0) 
   
  import scala.concurrent.ExecutionContext.Implicits.global
  val timeout = new Timeout(Duration.create(BetterSettings.MAILTIMEOUT, "seconds"))

  val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], Rate(1, (1.minutes))))
  throttler ! SetTarget(Some(sendMailActor))
  
  if(sendMailInterval > 0){
  //TODO: enable interval scanning when error handling complete to prevent sending mails with an error count > 2  
  //   context.system.scheduler.schedule(1 minutes, sendMailInterval minutes, self, SendUnsent())
  }
  
  def receive = {
      case ImmediateMail(user) => sendImmediateMailToUser(user)  
      case TestMail() => mailLogger.info("got test mail"); val s = sender(); throttler.ask(TestMail())(timeout).mapTo[String].map(s ! _)
      case SendUnsent() => sendUnsent()
      case _ =>
  }
  
 
  def sendUnsent(){
    mailLogger.info("sending unsent mail")
    val s = sender()
    blocking{
       betterDb.unsentMails().map{ ms =>
           ms.map{ case(um, m, u) => 
              mailLogger.debug(s"try sending mail to ${u.username}")
              sendMail(um,m,u,s) 
           }
       }      
    }
    mailLogger.info("sent unsent mail")
  }

  def sendMail(userMessage: UserMessage, message: Message, user: User, s: ActorRef) {
    throttler.ask((userMessage, message, user))(timeout).mapTo[String]
      .onComplete { mailSent =>
        mailSent match {
          case Success(succ: String) if succ == MailMessages.mailSuccess => {
            betterDb.setMessageSent(userMessage.id.get, BetterSettings.now())
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
