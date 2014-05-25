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
  
     
     /**
      * compare special bet 
      * TODO: points for special bets hardcoded
      * b is the bet
      * r is the result
      * 
      */
     def calculateSpecialBets(b: SpecialBet, r: SpecialBet): Int = {
         val mvp = compare(b.mvp, r.mvp, 3)
         val top = compare(b.topScorer, r.topScorer, 3)
         val win = compare(b.winningTeam, r.winningTeam, 3)
         val semis = compareSemi(b.semiIds, r.semiIds, 2)
         Seq(mvp,top,win,semis).sum         
     }
     
     def compareSemi(bet: Set[Long], result: Set[Long], points: Int): Int = {
           (bet & result).size * points
     }
     
     def compare(bet: Option[Long], result: Option[Long], points: Int): Int = {
          (bet, result) match {
               case (Some(b), Some(r)) if b == r => points      
               case _ => 0
          }
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
