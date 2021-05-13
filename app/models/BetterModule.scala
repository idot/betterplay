package models

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

object ActorNames {
  final val mailer = "mailer"
  final val sendMail = "sendMail"
  final val worker = "worker"
}

class BetterModule extends AbstractModule with AkkaGuiceSupport {
  override def configure = {
    bindActor[MailerActor](ActorNames.mailer)
    bindActor[SendMailActor](ActorNames.sendMail)
    bindActor[WorkerActor](ActorNames.worker)
  }
}