package controllers

import akka.actor.Actor
import akka.actor.Props
import akka.actor.Extension
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem
import akka.event.Logging

import models.BetterDb
import models.UpdatePoints



//A work queue based on one singleton executing actor
//https://groups.google.com/d/msg/akka-user/roY_ZOI2OxU/QzQJlUgCYKEJ

class SystemScopedImpl(system: ActorSystem, props: Props, name: String) extends Extension {
    val instance: ActorRef = system.actorOf(props, name = name)
}

// derive an object from this to get an extension that provides
// one ActorRef global associated with an ActorSystem for the
// system's lifetime
trait SystemScoped extends ExtensionId[SystemScopedImpl] with ExtensionIdProvider {
  final override def lookup = this

  final override def createExtension(system: ExtendedActorSystem) = new SystemScopedImpl(system, instanceProps, instanceName)

  protected def instanceProps: Props

  protected def instanceName: String
}

object WorkQueue extends SystemScoped {
   override val instanceProps = Props[WorkQueue]
   override val instanceName = "workqueue"
}

class WorkQueue extends Actor {
    val log = Logging(context.system, this)
    def receive = {
       case UpdatePoints() => {
		     log.info("updating user points")
	//	     betterDb.updateBetsWithPoints()
		     log.info("done updating points")
	   }
       case _ => log.info("received unknown message")
    }
}


