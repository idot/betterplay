package models

import org.joda.time.DateTime

object ObjectMother {

  def adminUser(): User = User(None, "a1", "af1","al1", "emaila1", "pwa1", true, true, false, true, 3, 0, None, None)   
  
  def dummyUsers(): Seq[User] = (1 to 3).map{ nr => User(None, s"u$nr", s"f$nr", s"l$nr", s"f${nr}@betting.at", s"$nr", true, true, true, false, 4 , 2, None, None)}
  
  val dummyTeams: Seq[Team] = (1 to 6).map{nr => Team(None, s"t$nr", DBImage("",""))}
   
  val dummyPlayers: Seq[Player] = (1 to 6).map{ nr => Player(None, s"f$nr", s"l$nr", "role", 0, DBImage("",""))}
  
  val dummyLevels: Seq[GameLevel] = {
      Seq(
        GameLevel(None, "group", 3, 1, 0),   
        GameLevel(None, "semi", 6, 3, 1),
        GameLevel(None, "final", 12, 5, 2)
      )
  }
  
  /**
   * this is like the scala input for BetterDb.insertGame
   * 
   */
  def dummyGames(totalStart: DateTime): Seq[(Game,String,String,Int)] = (1 to 3).map{ nr =>
      val nr2 = nr*2
      val t1 = dummyTeams(nr2 - 2).name
      val t2 = dummyTeams(nr2 - 1).name
      val g = Game(None, GameResult(3,4,true), 0, 0, 0, totalStart.plusMinutes(10 * (nr-1)), "", "")  
      (g,t1,t2,0)
  }
  
  
  
  
}