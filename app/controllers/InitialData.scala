package controllers

import play.api._
import play.api.Logger
import play.api.db.slick._
import org.apache.commons.io.IOUtils
import models._
import au.com.bytecode.opencsv.CSVParser
import scala.concurrent.{Future,Await}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.Singleton
import org.joda.time.DateTime
import play.api.Environment

object InitialData {
  def specialBets(start: DateTime): Seq[SpecialBetT] = {
    val s = Seq(
      SpecialBetT(None, "topscorer", "highest scoring player", 8, start, "topscorer", SpecialBetType.player, ""),
      SpecialBetT(None, "mvp", "most valuable player", 8, start, "mvp", SpecialBetType.player, ""),
      SpecialBetT(None, "world champion", "world champion", 10, start, "world champion", SpecialBetType.team, ""),
      SpecialBetT(None, "semifinalist", "semifinalist", 5, start, "semifinalist", SpecialBetType.team, ""),
      SpecialBetT(None, "semifinalist", "semifinalist", 5, start, "semifinalist", SpecialBetType.team, ""),
      SpecialBetT(None, "semifinalist", "semifinalist", 5, start, "semifinalist", SpecialBetType.team, ""),
      SpecialBetT(None, "semifinalist", "semifinalist", 5, start, "semifinalist", SpecialBetType.team, ""))
    s
  }

  def toLines(file: String, environment: Environment): Seq[String] = {
    val is = environment.classLoader.getResourceAsStream("data/" + file)
    val string = IOUtils.toString(is, "UTF-8")
    val li = string.split("\n").drop(1)
    is.close
    li
  }

  def fifa2iso(environment: Environment): Map[String, String] = {
    val csv = new CSVParser()
    val lines = toLines("country-codes.csv", environment).drop(1)
    //FIFA = 10
    // ISO3166.1.Alpha.2 = 2
    def FIFA2ISO(line: String): (String, String) = {
      val items = csv.parseLine(line)
      (items(10).toLowerCase, items(2).toLowerCase)
    }
    lines.map(FIFA2ISO).toMap
  }

  def levels(environment: Environment): Seq[GameLevel] = {
      Logger.info("parsing levels")
      val lines = toLines("levels.tab", environment)
      lines.map(parseLevel)    
  }
  
    //level   exact   tendency        nr #must be tab delimited
  //group   3       1       0
  def parseLevel(line: String): GameLevel = {
      val items = line.split("\t")
      GameLevel(None, items(0), items(1).toInt, items(2).toInt, items(3).toInt, 30)
  }
  
  
}

trait InitialData {
    
    def betterDb: BetterDb
    def environment(): Environment
   
    def gamesFile(): String
    def playersFile(): String
    def usersFile(): String
    def levelsFile(): String
        
    def parsePlayer(line: String): Player
    def parseGame(line: String, levelId: Long): (Game,Team,Team)
    
    def importLevels(environment: Environment): Seq[GameLevel] = InitialData.levels(environment)
    def importSpecialBets(start: DateTime, environment: Environment): Seq[SpecialBetT]
    def importTeams(environment: Environment): Seq[Team]
    
    def importGames(environment: Environment, levelId: Long): Seq[(Game,Team,Team)] = {
        Logger.info("parsing games")
        val lines = InitialData.toLines(gamesFile(), environment)
	      lines.map(line => parseGame(line, levelId))
    }
          
    def importPlayers(environment: Environment): Seq[Player] = {
        Logger.info("parsing players")
	      val lines = InitialData.toLines(playersFile(), environment)
	      lines.map(parsePlayer)  
    }
    
    def importUsers(environment: Environment): Seq[User] 
    def fifa2isoMap(environment: Environment): Map[String, String] = InitialData.fifa2iso(environment)

    
    def specialBetsStart(environment: Environment): DateTime
    def teamNameForPlayer(p: Player): String

    
    def dbTeamsMap(teams: Seq[Team]): Map[String,Team]
    
    def insert(debug: Boolean): Unit = { 
        betterDb.dropCreate()
        Logger.info("inserting data in db")
        Await.result(Future.sequence(importSpecialBets(specialBetsStart(environment), environment).map(t => betterDb.insertSpecialBetInStore(t))), 1 seconds)
        Logger.info("inserted special bets")
        Await.result(Future.sequence(importUsers(environment).map{ u => betterDb.insertUser(u, u.isAdmin, u.isRegistrant, None) } ), 1 seconds)
        Logger.info("inserted users")
        val admin = Await.result(betterDb.allUsers(), 1 seconds).sortBy(_.id).head
        Await.result(Future.sequence(importLevels(environment).map(l => betterDb.insertLevel(l, admin))), 1 seconds)  
        Await.result(Future.sequence(importTeams(environment).map(t => betterDb.insertTeam(t, admin))), 1 seconds)
        val dblevel = Await.result(betterDb.allLevels(), 1 second)(0)
        val dbteams = Await.result(betterDb.allTeams(), 1 second)
        val players = importPlayers(environment)
        val gamesWithTeams = importGames(environment, dblevel.id.get)
        Await.result(Future.sequence(gamesWithTeams.map{ case(g,t1,t2) => betterDb.insertGame(g, t1.name, t2.name, dblevel.level, admin)}), 1 seconds)
        Await.result(betterDb.createBetsForGamesForAllUsers(admin), 1 seconds)
        
        Logger.info("done inserting data in db")
      
  }
   
    
    
}

