package models

object PointsCalculator {

     def checkExact(betResult: Result, gameResult: Result): Boolean = {
           betResult.goalsTeam1 == gameResult.goalsTeam1 && betResult.goalsTeam2 == gameResult.goalsTeam2
     }
     
     /**
      * if equals then tendency would be bet 1:1  game 2:2
      */
     def checkTendencyTie(betResult: Result, gameResult: Result): Boolean = {
    	   betResult.winner() == 0 && gameResult.winner() == 0 && betResult.goalsTeam1 != gameResult.goalsTeam1
     }
     
     def checkTendencyTeam1Wins(betResult: Result, gameResult: Result): Boolean = {
    	   ! checkExact(betResult, gameResult) && betResult.winner() == 1 && gameResult.winner() == 1 
     }
     
     def checkTendencyTeam2Wins(betResult: Result, gameResult: Result): Boolean = {
    	  ! checkExact(betResult, gameResult) && betResult.winner() == 2 && gameResult.winner() == 2 
     }
  
     def checkTendency(betResult: Result, gameResult: Result): Boolean = {
         checkTendencyTie(betResult, gameResult) || checkTendencyTeam1Wins(betResult, gameResult) || checkTendencyTeam2Wins(betResult, gameResult)        
     }
     
     def pointsForValidGame(betResult: Result, gameResult: Result, gameLevel: GameLevel): Int = {
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
      * None if game not set
      * 0 for invalid or wrong bet
      * gameLevel points for correct exact or tendency result
      * 
      */
     def calculatePoints(bet: Bet, game: Game, gameLevel: GameLevel): Option[Int] = {
    	if(!game.result.isSet){
    		return None
    	}
    	val points = if(bet.result.isSet) pointsForValidGame(bet.result, game.result, gameLevel) else 0
    	Some(points)
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
     
}