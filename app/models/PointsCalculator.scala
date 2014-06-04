package models

object PointsCalculator {

     def checkExact(betResult: GameResult, gameResult: GameResult): Boolean = {
           betResult.goalsTeam1 == gameResult.goalsTeam1 && betResult.goalsTeam2 == gameResult.goalsTeam2
     }
     
     /**
      * if equals then tendency would be bet 1:1  game 2:2
      */
     def checkTendencyTie(betResult: GameResult, gameResult: GameResult): Boolean = {
    	   betResult.winner() == 0 && gameResult.winner() == 0 && betResult.goalsTeam1 != gameResult.goalsTeam1
     }
     
     def checkTendencyTeam1Wins(betResult: GameResult, gameResult: GameResult): Boolean = {
    	   ! checkExact(betResult, gameResult) && betResult.winner() == 1 && gameResult.winner() == 1 
     }
     
     def checkTendencyTeam2Wins(betResult: GameResult, gameResult: GameResult): Boolean = {
    	  ! checkExact(betResult, gameResult) && betResult.winner() == 2 && gameResult.winner() == 2 
     } 
  
     def checkTendency(betResult: GameResult, gameResult: GameResult): Boolean = {
         checkTendencyTie(betResult, gameResult) || checkTendencyTeam1Wins(betResult, gameResult) || checkTendencyTeam2Wins(betResult, gameResult)        
     }
     
     def pointsForValidGame(betResult: GameResult, gameResult: GameResult, gameLevel: GameLevel): Int = {
          if(checkExact(betResult, gameResult)){
				gameLevel.pointsExact
          }
		  else if(checkTendency(betResult, gameResult)){ 
			    gameLevel.pointsTendency
		  }
		  else{
			 0
		  }       
     }
     
     /**
      * main method
      * 0 if game not set
      * 0 for invalid or wrong bet
      * gameLevel points for correct exact or tendency result
      * 
      */
     def calculatePoints(bet: Bet, game: Game, gameLevel: GameLevel): Int = {
    	if(bet.result.isSet && game.result.isSet) pointsForValidGame(bet.result, game.result, gameLevel) else 0
     }     
  
     
	 def calculateSpecialBet(t: SpecialBetT, b: SpecialBetByUser): Int = {
	     if(t.result == b.prediction) t.points else 0	 
	 }
	 
	 
     /**
      * compare special bet 
      * TODO: points for special bets hardcoded
      * b is the bet
      * r is the result
      * TODO: the special bets have to be:
	  * 
	  *  grouped by groupId, for the group calculated and sorted approriatley the correct hits to update the points in 
	  *  the correct bet
	  *  Then one has to 
	  *
      */
     def calculateSpecialBets(bets: Seq[(SpecialBetT,SpecialBetByUser)]): Seq[(SpecialBetT,SpecialBetByUser,Int)] = {
		 ???
		 //val points = bets.map{ case(t,b) => calculateSpecialBet(t,b) }      
          
	     
	 }
     
     
   /**
   * descending sorted points to ranks respecting ties
   *
   *
   **/
  def pointsToRanks(sortedPoints: Seq[Int]): Seq[Int] = {
       if(sortedPoints.length == 0){ 
         sortedPoints
       }else{
         var rank = 0
         var currentValue = 0
         for{
           p <- sortedPoints
         }yield{
            if(p == currentValue) rank else {
              currentValue = p
              rank += 1
              rank
            } 
         }
       }
   }
     
     
}
