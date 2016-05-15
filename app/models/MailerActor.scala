package models

import akka.actor._
import javax.inject._
import play.api.Configuration
import play.api.libs.mailer._
import akka.contrib.throttle._
import akka.contrib.throttle.Throttler._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration._


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
  class SendMailActor extends Actor {
    def receive = {
      case x ⇒ println(x)
    }
  }
  
  val sendMail = context.actorOf(Props(classOf[SendMailActor]))
  val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], Rate(3, (1.minutes))))
  
  throttler ! SetTarget(Some(sendMail))
  
  def receive = {
      case RegistrationMail(user) => //createMail(user)  
      case _ =>
  }
  
  val config = configuration.getString("my.config").getOrElse("none")

 // def sendEmails
  //regular task activated by timing based actor every 20 minutes
  def sendMail(){
    //get unsent mails
    //send mail
     //success ==> set sent
    //unsuccessful ==> log + retry in minutes
  }
  
  //extra task => throtteler
  def sendMail(messageId: Long){
    
    
  }
  
}
