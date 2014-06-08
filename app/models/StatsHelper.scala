package models 


import play.api.db.slick.Config.driver.simple._

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
				pointsPerGame.set(points.toString, index)
				resultFirstTeam.set(gwt.game.result.goalsTeam1.toString, index)
				resultSecondTeam.set(gwt.game.result.goalsTeam2.toString, index)
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


class StatsHelper()(implicit s: Session){
	  //TODO: allUsersWithSpecialBets
      val usersSpRank = BetterDb.usersWithSpecialBetsAndRank().sortBy(_._1.username)
	  val gwts = BetterDb.allGamesWithTeams().sortBy(gwt => gwt.game.nr)
      val allBetsForGamesPerUser = gwts.map{ gwt => 
		  val bus = BetterDb.betsWitUsersForGame(gwt.game)
	      bus.groupBy{ case(b,u) => u }.mapValues{ bs => bs.head }.toMap	 
      }
	  def betForUser(gameIndex: Int, user: User): Option[Bet] = allBetsForGamesPerUser(gameIndex).get(user).map(_._1)
	  
	  def createUserBets(user: User, sp: SpecialBets, rank: Int): UserBets = {
	      val bets = gwts.zipWithIndex.map{ case(gwt,i) => betForUser(i, user) }
	      UserBets(user, bets, rank, sp) 
	  }
	  
	  def createUsersBets(): Seq[UserBets] = usersSpRank.map{ case(u,sp,r) => createUserBets(u, sp, r)}
	   
      def createUserRows(): Seq[UserRow] = {
	      val bets = createUsersBets()
          bets.map(b => UserRow(b, gwts))
	  }
	  
}




