package models

import play.api.Configuration

object MailGenerator {
  
  def getURL(configuration: Configuration): String = {
       val host = configuration.getOptional[String]("betterplay.host").getOrElse("localhost:4200")
       val prefix = configuration.getOptional[String]("betterplay.prefix").getOrElse("")
       val hosturl = if(prefix != "") host+"/"+prefix else host
       hosturl
  }
  
  def testMail(configuration: Configuration): String = {
      s"test message host + prefix is : ${getURL(configuration)}"
  }
  
  def createUserRegistrationMail(user: User, token: String, creatingUser: User, host: String): Message = {
      val subject = "FIFA2018: Welcome to the FIFA2018 betting!"

      val body = templates.txt.registrationMail.render(user, creatingUser.firstName, token, host).body.trim()
                    
      Message(None, MessageTypes.REGISTRATION, subject, body, creatingUser.id.get)             
  }
  
  def createPasswordRequestMail(user: User, token: String, host: String): Message = {
      val subject = "FIFA2018: new password request"
      
      val body = templates.txt.passwordRequestMail.render(user, token, host).body.trim()
    
      Message(None, MessageTypes.NEWPASSWORD, subject, body, user.id.get)
  }
  
  /**
   * runtime e-mails personalisation
   */
  def personalize(subject: String, body: String, user: User, sendingId: Long): Message = {
      val bU = body.replaceAll("""\{\{username}}""", user.username)
      val bF = bU.replaceAll("""\{\{firstname}}""", user.firstName)
      val bL = bF.replaceAll("""\{\{lastname}}""", user.lastName)
      Message(None, MessageTypes.FREE, subject, bL, sendingId) 
  }
  
  
}