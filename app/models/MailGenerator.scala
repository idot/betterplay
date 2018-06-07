package models

object MailGenerator {
  
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