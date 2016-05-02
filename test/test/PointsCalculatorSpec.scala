package models

import org.specs2.ScalaCheck 
import org.junit.runner._
import org.specs2.runner._
import org.specs2._
import org.specs2.Specification

import org.scalacheck._

import org.junit.runner.RunWith
import org.joda.time.DateTime

object RG {
  
   def genGoals = Gen.chooseNum(0, 4)
   
   def genResult(setted: Boolean) = {
      for{
         g1 <- genGoals 
         g2 <- genGoals
      } yield GameResult(g1, g2, setted)
   }
  
   def genTieResult(setted: Boolean) = {
      for{
        g1 <- genGoals
      }yield GameResult(g1, g1, setted)
   }
    
   def genWin2Result(setted: Boolean) = {
       for{
         g1 <- genGoals
       } yield GameResult(g1, g1 + 1, setted)
   }
   
   def genWin1Result(setted: Boolean) = {
        for{
         g1 <- genGoals
        } yield GameResult(g1 + 1, g1, setted)
   }
   
   def genPoints() = {
      Gen.containerOf[List,Int](Gen.chooseNum(0,20))
   }
   
//   case class SpecialBetByUser(id: Option[Long], userId: Long,  specialbetId: Long, prediction: String, points: Int)
   /**
   * the betgroupID allows grouping for multiple results e.g. semifinal1 seimifinal2 .. semifinal4 should all have the same groupId
   *
   **/
 //  case class SpecialBetT(id: Option[Long], name: String, description: String, points: Int, closeDate: DateTime, betGroup: String, itemType: String, result: String)
   
   
   def genResult = Gen.oneOf("","A","B","C","D","E","F","G","H","I")
   
   def genTemplate(id: Long, group: String) = {
       genResult.map{ r => 
		   SpecialBetT(Some(id), "name", "description", 3, new DateTime(), group, "itemtype", r)
	   } 
   }
   
   def genSpecialBet(id: Long, group: String) = {
       genResult.map{ r => 
	       SpecialBetByUser(Some(1000), 1l, id, r, 0)
	   }   
   }

	
   def genSpecials(id: Long, group: String) = {
       for{
		   t <- genTemplate(id, group)
	       sp <- genSpecialBet(id, group)
	   } yield (t,sp)
   }
   
}

@RunWith(classOf[JUnitRunner])
class PointsCalculatorSpec extends Specification with ScalaCheck { def is =

    "A pointsCalculator returns true for exact hits that are identical" ! exactTrue ^
    "A pointsCalculator returns false for exact hits that are not identical" ! exactFalse ^
    "A pointsCalculator returns true for equals if the game is tied" ! tendencyTie ^
    "A pointsCalculator returns true for win1 if the game was won by 1" ! win1 ^
    "A pointsCalculator returns true for win1 if the game was won by 1" ! win2 ^
    "A pointsCalculater has a method to turn a decreasingly sorted set of points into ranks" ! torank ^
	"A pointsCalculator can evaluate special bets for a single item" ! specialBetsForGroup1  ^
	"A pointsCalculator can evaluate special bets for a group all wrong item" ! specialBetsForGroupAllWrong ^ 
	"A pointsCalculator can evaluate special bets for a group all correct item" ! specialBetsForGroupAllCorrect ^
	"A pointsCalculator can evaluate special bets for a group partially correct item" ! specialBetsForGroupPartCorrect ^ 
    end
    
    val gl = GameLevel(None, "test", 3, 2, 1, 30)
    
    def tendencyTie = {
        Prop.forAll(RG.genTieResult(true), RG.genTieResult(true)){ (bet: GameResult, game: GameResult) =>
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
        Prop.forAll(RG.genResult(true)){ bet: GameResult =>
            PointsCalculator.checkExact(bet, bet) === true and 
            PointsCalculator.checkTendencyTie(bet, bet) === false and 
            PointsCalculator.checkTendencyTeam1Wins(bet, bet) === false and 
            PointsCalculator.checkTendencyTeam2Wins(bet, bet) === false and
	        PointsCalculator.pointsForValidGame(bet, bet, gl) === 3 
        }
    }
    
    def exactFalse = {
         Prop.forAll(RG.genResult(true), RG.genResult(true)){ (bet: GameResult, game: GameResult) =>
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
        Prop.forAll(RG.genWin1Result(true), RG.genWin1Result(true)){ (bet: GameResult, game: GameResult) =>
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
        Prop.forAll(RG.genWin2Result(true), RG.genWin2Result(true)){ (bet: GameResult, game: GameResult) =>
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
    
    /**
     * I would like to have a property that says only if same value in points it must have same rank,
     * but don't know how
     */
    def torank = {
      Prop.forAll(RG.genPoints) { points => 
         def rankDiff(points: Seq[Int]): Seq[Int] = {
             points.headOption.map{ first =>
	             points.foldLeft((List.empty[Int], first)) { case((li, prev), current) =>
	                  (li :+ scala.math.signum(prev - current), current)
	             }
             }.map{ _._1 }.getOrElse(Nil)
         }         
         val pointsSorted = points.sorted.reverse
         val ranks = PointsCalculator.pointsToRanks(pointsSorted)
         Prop.classify(pointsSorted.size == pointsSorted.toSet.size, "untied", "with ties"){
             (ranks === ranks.sorted ) &&
             (pointsSorted.toSet.size === ranks.toSet.size)
         }         
      }   
      
    }
	
	// calculateSpecialBetForGroup(bets: Seq[(SpecialBetT,SpecialBetByUser)]): Seq[(SpecialBetT,SpecialBetByUser,Int)] 
	
	def specialBetsForGroup1 = {
		val id = 10
	    val t1 = SpecialBetT(Some(id), "name", "description", 3, new DateTime(), "group1", "itemtype", "result")
	    val b1 = SpecialBetByUser(Some(1000), 1l, id, "result", 0)
		val b2 = SpecialBetByUser(Some(1000), 1l, id, "wrong", 0)
		PointsCalculator.calculateSpecialBetForGroup(Seq((t1,b1))) === Seq((t1,b1.copy(points=3))) and
		PointsCalculator.calculateSpecialBetForGroup(Seq((t1,b2))) === Seq((t1,b2))
	
	}
	
	def specialBetsForGroupAllWrong = {
		val id = 10
	    val t1 = SpecialBetT(Some(id), "name", "description", 3, new DateTime(), "group1", "itemtype", "result1")
		val t2 = SpecialBetT(Some(id), "name", "description", 3, new DateTime(), "group1", "itemtype", "result2")
	    val b1 = SpecialBetByUser(Some(1000), 1l, id, "wrong", 0)
		val b2 = SpecialBetByUser(Some(1000), 1l, id, "wrong", 0)
		val input = Seq((t1,b1),(t2,b2))
		PointsCalculator.calculateSpecialBetForGroup(input) === input
	}
	
	def specialBetsForGroupAllCorrect = {
		val id = 10
	    val t1 = SpecialBetT(Some(id), "name", "description", 3, new DateTime(), "group1", "itemtype", "result1")
		val t2 = SpecialBetT(Some(id), "name", "description", 3, new DateTime(), "group1", "itemtype", "result2")
	    val b1 = SpecialBetByUser(Some(1000), 1l, id, "result2", 0)
		val b2 = SpecialBetByUser(Some(1000), 1l, id, "result1", 0)
		val input = Seq((t1,b1),(t2,b2))
		val result = Seq((t1,b1.copy(points=3)),(t2,b2.copy(points=3)))
		PointsCalculator.calculateSpecialBetForGroup(input) === result
	}
	
	def specialBetsForGroupPartCorrect = {
		val id = 10
	    val t1 = SpecialBetT(Some(id), "name", "description", 3, new DateTime(), "group1", "itemtype", "result1")
		val t2 = SpecialBetT(Some(id), "name", "description", 3, new DateTime(), "group1", "itemtype", "result2")
		val t3 = SpecialBetT(Some(id), "name", "description", 3, new DateTime(), "group1", "itemtype", "result3")
	    val b1 = SpecialBetByUser(Some(1000), 1l, id, "result2", 0)
		val b2 = SpecialBetByUser(Some(1000), 1l, id, "result3", 0)
		val b3 = SpecialBetByUser(Some(1000), 1l, id, "wrong", 0)
		val input = Seq((t1,b1),(t2,b2),(t3,b3))
		val result = Seq((t1,b1.copy(points=3)),(t2,b2.copy(points=3)),(t3,b3))
		PointsCalculator.calculateSpecialBetForGroup(input) === result
	}
	
    
}
