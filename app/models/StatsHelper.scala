package models 

import scala.concurrent.{Future,Await}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import collection.mutable.HashMap

case class SpecialBet(betType: String, prediction: String)

case class UserBets(user: User, bets: Seq[Option[Bet]], rank: Int, specialBets: SpecialBets)

case class Row(size: Int){
	
	val values = new Array[String](size)
	
	def set(value: String, index: Int){
		values(index) = value
	}
	
	def get(index: Int) = values(index)
	
}




case class UserRow(user: User, games: Row, pointsPerGame: Row
	, cumulatedPoints: Row, firstGoals: Row, secondGoals: Row, resultFirstTeam: Row, resultSecondTeam: Row,  rank: Int, specialBets: SpecialBets ){
	
}

object UserRow {
	val noBetString = "-"
	
    
	def apply(userBets: UserBets, gwts: Seq[GameWithTeams]): UserRow = {//bets have to be sorted by gameNr
		val user = userBets.user
		val bets = userBets.bets
		val size = bets.size
		val pointsPerGame = new Row(size)
		val cumulatedPoints = new Row(size)
		val firstGoals = new Row(size)
		val secondGoals = new Row(size)
		val resultFirstTeam = new Row(size)
		val resultSecondTeam = new Row(size)
		val games = new Row(size)
		var cPoints = 0
		bets.zipWithIndex.map{ case (beto, index) =>
			val gwt = gwts(index)
			val bet = beto.get //we know that each game/user has a bet, if not please explode
			if(bet.result.isSet){
				var points = bet.points
				cPoints += points
				if(gwt.game.result.isSet){
				   resultFirstTeam.set(gwt.game.result.goalsTeam1.toString, index)
				   resultSecondTeam.set(gwt.game.result.goalsTeam2.toString, index)
				   pointsPerGame.set(points.toString, index)
			    }else{
 				   resultFirstTeam.set(noBetString, index)
 				   resultSecondTeam.set(noBetString, index)
				   pointsPerGame.set(noBetString, index)
				}
			}	
			else{
				pointsPerGame.set(noBetString, index)
				resultFirstTeam.set(noBetString, index)
				resultSecondTeam.set(noBetString, index)
			}
			games.set(bet.result.display, index)
			cumulatedPoints.set(cPoints.toString, index)	
			firstGoals.set(bet.result.goalsTeam1.toString, index)
			secondGoals.set(bet.result.goalsTeam2.toString, index)
		}
		new UserRow(user, games, pointsPerGame, cumulatedPoints, firstGoals, secondGoals, resultFirstTeam, resultSecondTeam, userBets.rank, userBets.specialBets)
	}
}


class StatsHelper(betterDb: BetterDb){
     
      val q = for{
         usersSpRankUnsorted <- betterDb.usersWithSpecialBetsAndRank()
         gwtsUnsorted <- betterDb.allGamesWithTeams()
         allBetsPerUserSeq <- Future.sequence(usersSpRankUnsorted.map{ case(u,sp,r) => betterDb.betsForUser(u) })
       } yield(usersSpRankUnsorted, gwtsUnsorted, allBetsPerUserSeq)
     
      val (usersSpRankUnsorted, gwtsUnsorted, allBetsPerUserSeq) = Await.result(q, 5 seconds)
    
      val gwts = gwtsUnsorted.sortBy(_.game.nr)
      val usersSpRank = usersSpRankUnsorted.sortBy(_._1.username)
      val allBetsMap = createBetsMap(allBetsPerUserSeq)
      
	    def createBetsMap(allBetsPerUserSeq: Seq[Seq[Bet]]): HashMap[(Long,Long),Bet] = {
          val map = new HashMap[(Long,Long),Bet]()
          for{
            sbets <- allBetsPerUserSeq
            bet <- sbets
          } {
             map.update((bet.userId, bet.gameId), bet)
          }
          map
      }
   
	    
	    def createUserBets(user: User, sp: SpecialBets, rank: Int): UserBets = {
	        val bets = gwts.zipWithIndex.map{ case(gwt,i) => allBetsMap.get(user.id.get, gwt.game.id.get) }
	        UserBets(user, bets, rank, sp) 
	    }
	  
	    def createUsersBets(): Seq[UserBets] = usersSpRank.map{ case(u,sp,r) => createUserBets(u, sp, r)}
	     
      def createUserRows(): Seq[UserRow] = {
  	      val bets = createUsersBets()
          bets.map(b => UserRow(b, gwts))
  	  }
	  
  	  def getGwts(): Seq[GameWithTeams] = gwts
	  
}




