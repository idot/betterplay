package models

import org.joda.time.DateTime

object ObjectMother {

  def g(email: String): (String,String) = DomainHelper.randomGravatarUrl(email)

  def adminUser(): User = User(None, "a1", "af1","al1", "", true, "emaila1", "pwa1", true, true, true, true, 3, 0, g("emaila1")._1, g("emaila1")._2, None, DomainHelper.filterSettings())   
  
  def dummyUsers(): Seq[User] = (1 to 3).map{ nr => 
	  val email =  s"f${nr}@betting.at"
	  val (g,t) = DomainHelper.randomGravatarUrl(email)
	  User(None, s"u$nr", s"f$nr", s"l$nr", "", true, email, s"$nr", true, true, true, true, 4 , 2, g, t, None, DomainHelper.filterSettings())
  }
  
  val dummyTeams: Seq[Team] = (1 to 6).map{nr => Team(None, s"t$nr", s"t$nr", s"t$nr")}
   
  val dummyPlayers: Seq[Player] = (1 to 6).map{ nr => Player(None, s"playername$nr", "role", s"clubname$nr", 0, DBImage("",""))}
  
  val dummyLevels: Seq[GameLevel] = {
      Seq(
        GameLevel(None, "group", 3, 1, 0),   
        GameLevel(None, "semi", 6, 3, 1),
        GameLevel(None, "final", 12, 5, 2)
      )
  }
  
  def specialTemplates(stype: String, start: DateTime): Seq[SpecialBetT] = {
	  val nm = "template "+stype
	  (1 to 6).map{ i =>  
		  if(i < 4){ //first 3 -> grouped bets 
		     SpecialBetT(None, s"$nm $i", s"$nm $i", 1 , start, stype+"1" , stype, "" )
	      }else{
		      SpecialBetT(None, s"$nm $i", s"$nm $i", i , start, stype+"1" , stype, "" )
		  }
	   }.toList
   }
   
  
  /**
   * this is like the scala input for BetterDb.insertGame
   * 
   */
  def dummyGames(totalStart: DateTime): Seq[(Game,String,String,Int)] = (1 to 3).map{ nr =>
      val nr2 = nr*2
      val t1 = dummyTeams(nr2 - 2).name
      val t2 = dummyTeams(nr2 - 1).name
      val st = totalStart.plusMinutes(10 * (nr-1))
      val g = Game(None, GameResult(3,4,true), 0, 0, 0, st.minusHours(5),"local", st, "server", "", "", nr, 59, false, false)  
      (g,t1,t2,0)
  } 
  
  
  
  
}