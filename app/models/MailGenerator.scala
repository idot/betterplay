package models

object MailGenerator {
  
  def createUserRegistrationMail(user: User, token: String, creatingUser: User): Message = {
      val subject = "Welcome to the EURO 2016!"

      val body = views.txt.registrationMail.render(user, creatingUser.username, token).body.trim()
                    
      Message(None, MessageTypes.REGISTRATION, subject, body, creatingUser.id.get)             
  }
  
  
  
  
}