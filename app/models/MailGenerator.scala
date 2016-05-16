package models

object MailGenerator {
  
  def createUserRegistrationMail(user: User, token: String, creatingUser: User): Message = {
      val subject = "Welcome to the EURO 2016!"
      val body = s"""|Dear ${user.firstName}, 
                    |
                    |in order to play you have to activate your account by choosing a password. Please
                    |click on the link below.
                    |After this, please don't forget to fill out the special bets until the start of the EURO 2016 on the 10. Jun.
                    |
                    | https://ngs.vbcf.ac.at/euro2016/#/${user.username}/completeRegistration/${token}
                    |
                    |
                    |good luck
                    |
        """
       
       Message(None, MessageTypes.REGISTRATION, subject, body, creatingUser.id.get)             
  }
  
  
}