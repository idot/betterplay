package models

import akka.actor._
import javax.inject._
import play.api.Configuration
//import play.api.libs.mailer._

//token contains expiry date encrypted with secret key

sealed trait MessageTypes {
  
  
}


//generate messages
//generate email after recaptcha for new password
//generate email after registration for new password, random string in db as token, after usage set viewed = true

//if param newRegistration => make toastr to remind to set password.
//disable links until password is saved
//

//, mailerClient: MailerClient
class Mailer @Inject() (configuration: Configuration) extends Actor {
  
  def receive = {
      case _ =>
  }
  
  val config = configuration.getString("my.config").getOrElse("none")

  def createUserMail(user: User, token: String){
      val subject = "Welcome to the EURO 2016!"
      val text = s"""|Dear ${user.firstName}, 
                    |
                    |in order to play you have to activate your account by choosing a password. Please
                    |click on the link below.
                    |After this, please don't forget to fill out the special bets until the start of the EURO 2016 on the 10. Jun.
                    |
                    | https://ngs.vbcf.ac.at/registerUser/${token}
                    |
                    |
                    |good luck
                    |
        """
        
                    
  }
  
 // def sendMail(
  
}
