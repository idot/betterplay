#!/bin/sh
exec scala "$0" "$@"
!#

//squad data from:https://github.com/openfootball/world-cup
case class Player(name: String, country: String, position: String, club: String){
	def toTab(): String = Seq(name,position,country,club).mkString("\t")
}

def getCommentItem(line: String): String = {
    line.replaceAll("#","").trim
}

def getPlayers(lines: List[String]): Seq[Player] = {
    val country = getCommentItem(lines(1))      
    val separators = lines.zipWithIndex.drop(2).filter{ case(line,index) => line.startsWith("####")}.map(_._2)
    val allSeparators = separators :+ (lines.length + 2)
    val positions = allSeparators.sliding(2,1)	
	(positions.flatMap{ li => 
		val s = li(0)
		val e = li(1)
	    val slice = lines.slice(s,e)
		parsePosition(slice, country)
	}).toList
}

def parsePosition(lines: Seq[String], country: String): Seq[Player] = {
	val positions = getCommentItem(lines(1))
	val position = positions.substring(0, positions.length - 1) //depluralize
	val namelines = lines.filter{ l => ! l.startsWith("#") && ! (l.trim.length == 0)}
	namelines.map{ l =>
		val items = l.split("##")
		val name = items(0).trim
		val team = items(1).trim
		Player(name, country, position, team)
	}
}

def parseFile(file: String): Seq[Player] = {
	println("parsing: "+file)
    val src = scala.io.Source.fromFile(file)
    val lines = src.getLines.toList
    src.close
	getPlayers(lines)
}


def parseAll(){
	val squads = new java.io.File("squads").listFiles.map(_.getPath)
    val teams = squads.map{ parseFile }
    val wrong = teams.zipWithIndex.filter{ case(t,i) => t.length != 30 } 
    if(wrong.length != 0 ){
       println("something wrong ") 
       wrong.map{ case(t,i)=>
	     println(squads(i)+" "+t.length)     
       }
    }
	val writer = new java.io.BufferedWriter(new java.io.FileWriter("players.tab"))
    teams.flatten.map(_.toTab).foreach(t => writer.write(t+"\n"))
    writer.close()
    println("done")
}

//val b = parseFile("squads/be-belgium.txt")
parseAll()








