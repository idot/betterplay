package test

import org.specs2.mutable._

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.test._
import play.api.test.Helpers._
import models._

/**
 * test the kitty cat database
 */
class DBSpec extends Specification {

  "DB" should {
    "work as expected" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase(
      options=Map("DATABASE_TO_UPPER" -> "false", "DB_CLOSE_DELAY" -> "-1")   
      ))) {

      //create an instance of the table
      val bets = TableQuery[Bets] 
     
      DB.withSession { implicit s: Session => 
        bets.ddl.create
        val testbets = Seq(
               Bet(None, 0, Result(0,0,false)),
               Bet(None, 1, Result(1,2,true))
            ) 
        bets.insertAll(testbets: _*)
        bets.list must equalTo(testbets)
      }
    }

    "select the correct testing db settings by default" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      DB.withSession { implicit s: Session =>
        s.conn.getMetaData.getURL must startWith("jdbc:h2:mem:play-test")
      }
    }

    "use the default db settings when no other possible options are available" in new WithApplication {
      DB.withSession { implicit s: Session =>
        s.conn.getMetaData.getURL must equalTo("jdbc:h2:mem:play")
      }
    }
  }

}
