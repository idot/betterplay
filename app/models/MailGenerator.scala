package models

object MailGenerator {
  
  def createUserRegistrationMail(user: User, token: String, creatingUser: User): Message = {
      val subject = "Euro2016: Welcome to the EURO 2016!"

      val body = views.txt.registrationMail.render(user, creatingUser.firstName, token).body.trim()
                    
      Message(None, MessageTypes.REGISTRATION, subject, body, creatingUser.id.get)             
  }
  
  def createPasswordRequestMail(user: User, token: String): Message = {
      val subject = "Euro2016: new password request"
      
      val body = views.txt.passwordRequestMail.render(user, token).body.trim()
    
      Message(None, MessageTypes.NEWPASSWORD, subject, body, user.id.get)
  }
  
  def personalize(subject: String, body: String, user: User, sendingId: Long): Message = {
      val bU = body.replaceAll("{{username}}", user.username)
      val bF = bU.replaceAll("{{firstname}}", user.firstName)
      val bL = bF.replaceAll("{{lastname}}", user.lastName)
      Message(None, MessageTypes.FREE, subject, bL, sendingId) 
  }
  
  
}