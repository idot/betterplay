package models

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


class MailerActor @Inject() (configuration: Configuration, mailerClient: MailerClient, betterDb: BetterDb) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global
  val timeout = new Timeout(Duration.create(BetterSettings.MAILTIMEOUT, "seconds"))
  val mailSuccess = "mail delivered successfully"
  
  class SendMailActor extends Actor {
    def receive = {
      case ((um: UserMessage, m: Message, user: User)) ⇒ val s = sender(); Future{ println(m) }; s ! mailSuccess
      case _ => 
    }
  }
  
  val sendMail = context.actorOf(Props(classOf[SendMailActor]))
  val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], Rate(3, (1.minutes))))
  
  throttler ! SetTarget(Some(sendMail))
  
  def receive = {
      case RegistrationMail(user) => sendMail(user,sender())  
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
  def sendMail(user: User, sender: ActorRef){  
      betterDb.unsentMailForUser(user).map{ unsent => 
         unsent.filter{ case(um, m) => m.messageType == MessageTypes.REGISTRATION }.headOption match {
           case Some((um,m)) => throttler.ask((um,m,user))(timeout).mapTo[String]
                                  .onComplete{ mailSent => 
                                     mailSent match {
                                       case Try(s: String) if s == mailSuccess => {
                                         betterDb.setMessageSent(um.id.get, new DateTime())
                                         sender ! "sent email"
                                       }
                                       case _ => {
                                         sender ! _
                                       }
                                     }
                                }
           case None =>  sender ! "no unsent messages"
         }
      }    
  }
  
}
