package models

import play.api.Logger
import akka.actor._
import akka.pattern.ask
import javax.inject._
import play.api.Configuration
import akka.contrib.throttle._
import akka.contrib.throttle.Throttler._
import java.util.concurrent.TimeUnit._
import scala.concurrent.duration._
import akka.util.Timeout
import scala.util.{Try, Success, Failure}
import org.joda.time.DateTime
import scala.concurrent.{Future,blocking,Await}


case class UpdatePoints(user: User)

/***
 * this is not used ATM
 */
class WorkerActor @Inject()(configuration: Configuration, betterDb: BetterDb) extends Actor {
   val workerLogger = Logger("work")

   
   def receive = {
      case UpdatePoints(user: User) => updatePoints(user) 
      case _ => 
   }
   
   /**
    * 
    * this should not be executed concurrently, therefore blocking
    * Its in a transaction on db, but still ...
    * 
    **/
   def updatePoints(user: User){
       val s = sender()
       workerLogger.info(s"calculating and updating points for all users initiated: ${user.username}")
       blocking{
          val result = Await.result(betterDb.calculateAndUpdatePoints(user), 10 seconds)
          s ! result
       }
       workerLogger.info(s"calculated and updated points for all users initiated: ${user.username}")
   }
}