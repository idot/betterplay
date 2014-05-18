package models

import org.specs2._
import org.specs2.Specification
import org.specs2.matcher.DataTables
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.ScalaCheck
import org.scalacheck._

object RG {
  
   def genGoals = Gen.chooseNum(0, 4)
   
   def genResult(setted: Boolean) = {
      for{
         g1 <- genGoals
         g2 <- genGoals
      } yield Result(g1, g2, setted)
   }
  
   def genTieResult(setted: Boolean) = {
      for{
        g1 <- genGoals
      } yield Result(g1, g1, setted)
   }
   
   def genWin2Result(setted: Boolean) = {
       for{
         g1 <- genGoals
       } yield Result(g1, g1 + 1, setted)
   }
   
   def genWin1Result(setted: Boolean) = {
        for{
         g1 <- genGoals
       } yield Result(g1 + 1, g1, setted)
   }
   
   
}

@RunWith(classOf[JUnitRunner])
class PointsCalculatorSpec extends Specification with ScalaCheck { def is =

    "A pointsCalculator returns true for exact hits that are identical" ! exactTrue ^
    "A pointsCalculator returns false for exact hits that are not identical" ! exactFalse ^
    "A pointsCalculator returns true for equals if the game is tied" ! tendencyTie ^
    "A pointsCalculator returns true for win1 if the game was won by 1" ! win1 ^
    "A pointsCalculator returns true for win1 if the game was won by 1" ! win2 ^
    end
    
    val gl = GameLevel("test", 3, 2, 1)
    
    def tendencyTie = {
        Prop.forAll(RG.genTieResult(true), RG.genTieResult(true)){ (bet: Result, game: Result) =>
          Prop.classify(bet != game, "not exact"){
	           if(bet == game){
	               PointsCalculator.checkExact(bet, game) === true and 
	               PointsCalculator.checkTendencyTie(bet, game) === false and 
	               PointsCalculator.checkTendencyTeam1Wins(bet, game) === false and 
	               PointsCalculator.checkTendencyTeam2Wins(bet, game) === false and 
	               PointsCalculator.checkTendency(bet, game) === false and
	               PointsCalculator.pointsForValidGame(bet, game, gl) === 3
	           }else{
	               PointsCalculator.checkExact(bet, game) === false and 
	               PointsCalculator.checkTendencyTie(bet, game) === true and 
	               PointsCalculator.checkTendencyTeam1Wins(bet, game) === false and 
	               PointsCalculator.checkTendencyTeam2Wins(bet, game) === false and
	               PointsCalculator.checkTendency(bet, game) === true and
	               PointsCalculator.pointsForValidGame(bet, game, gl) === 2
	           }
          }
        }
    }
  
    def exactTrue = {
        Prop.forAll(RG.genResult(true)){ bet: Result =>
            PointsCalculator.checkExact(bet, bet) === true and 
            PointsCalculator.checkTendencyTie(bet, bet) === false and 
            PointsCalculator.checkTendencyTeam1Wins(bet, bet) === false and 
            PointsCalculator.checkTendencyTeam2Wins(bet, bet) === false and
	        PointsCalculator.pointsForValidGame(bet, bet, gl) === 3 
        }
    }
    
    def exactFalse = {
         Prop.forAll(RG.genResult(true), RG.genResult(true)){ (bet: Result, game: Result) =>
           if(bet != game){
        	   PointsCalculator.checkExact(bet, game) === false and
	           ( if(PointsCalculator.checkTendency(bet,game)) PointsCalculator.pointsForValidGame(bet, game, gl) === 2 else PointsCalculator.pointsForValidGame(bet, game, gl) === 0 )
           }else{
               PointsCalculator.checkExact(bet, game) === true  and
	           PointsCalculator.pointsForValidGame(bet, game, gl) === 3
           }
        }
    }
    
    def win1 = {
        Prop.forAll(RG.genWin1Result(true), RG.genWin1Result(true)){ (bet: Result, game: Result) =>
           if(bet != game){
        	   PointsCalculator.checkTendencyTeam1Wins(bet, game) === true and 
        	   PointsCalculator.checkExact(bet, game)  === false and 
        	   PointsCalculator.checkTendencyTie(bet, game) === false and
        	   PointsCalculator.checkTendencyTeam2Wins(bet, game) === false and
        	   PointsCalculator.checkTendency(bet, game) === true and
	           PointsCalculator.pointsForValidGame(bet, game, gl) === 2
           }else{
               PointsCalculator.checkExact(bet, game) === true and 
               PointsCalculator.checkTendencyTeam1Wins(bet, game) === false and
               PointsCalculator.checkTendencyTie(bet, game) === false and 
               PointsCalculator.checkTendencyTeam2Wins(bet, game) === false and
               PointsCalculator.checkTendency(bet, game) === false and
	           PointsCalculator.pointsForValidGame(bet, game, gl) === 3
           }
        }
    }
    
    def win2 = {
        Prop.forAll(RG.genWin2Result(true), RG.genWin2Result(true)){ (bet: Result, game: Result) =>
           if(bet != game){
        	   PointsCalculator.checkTendencyTeam2Wins(bet, game) === true and 
        	   PointsCalculator.checkExact(bet, game)  === false and 
        	   PointsCalculator.checkTendencyTie(bet, game) === false and 
        	   PointsCalculator.checkTendencyTeam1Wins(bet, game) === false and 
        	   PointsCalculator.checkTendency(bet, game) === true and
	           PointsCalculator.pointsForValidGame(bet, game, gl) === 2
           }else{
               PointsCalculator.checkExact(bet, game) === true and 
               PointsCalculator.checkTendencyTeam2Wins(bet, game) === false and 
               PointsCalculator.checkTendencyTie(bet, game) === false and 
               PointsCalculator.checkTendencyTeam1Wins(bet, game) === false and 
               PointsCalculator.checkTendency(bet, game) === false and
	           PointsCalculator.pointsForValidGame(bet, game, gl) === 3
           }
        }
    }
    
    
    
}