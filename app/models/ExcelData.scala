package models
import java.time.OffsetDateTime

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.List

import scala.concurrent.ExecutionContext

import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFDataFormat
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFSheet._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.BuiltinFormats
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.FillPatternType

object ExcelData {
  
  def generateExcel(betterDb: BetterDb, dateTime: OffsetDateTime, viewingUserId: Long) (implicit ec: ExecutionContext) : Array[Byte] = {
     val helper = new StatsHelper(betterDb, dateTime, viewingUserId)  
	   val templates = helper.specialBetsTemplates()
	   val gwts = helper.getGwts()
     val excelD = new ExcelData(helper.createUserRows(), gwts, templates)
	   val excel = excelD.createExcelSheetComplete()
	   excel
  }
  

}



class ExcelData(userRows: Seq[UserRow], gwts: Seq[GameWithTeams], specialBetTemplates: Seq[SpecialBetT]){
    val specialBetTs = specialBetTemplates.sortBy(_.id)
	val headingStrings = specialBetTs.map(_.name)
	
	def createExcelSheetComplete(): Array[Byte] = {
	  var wb = new XSSFWorkbook()
	  fillSheet(wb, "all", 0)
	  fillSheet(wb, "points", 1)
	  fillSheet(wb, "cumulatedPoints", 2)
	  fillSheet(wb, "betFirstTeamGoals", 3)
	  fillSheet(wb, "betSecondTeamGoals", 4)
	  fillSheet(wb, "resultFirstTeamGoals", 5)
	  fillSheet(wb, "resultSecondTeamGoals", 6)
	  fillSpecialBets(wb, 7)
	  createMessage(wb, 8)
	  var out = new ByteArrayOutputStream()
	  wb.write(out)
	  out.close()
	  out.toByteArray
	}
	
	def fillSpecialBets(wb: XSSFWorkbook, nr: Int): Unit = {
		val s = wb.createSheet()
//		 declare a row object reference
	//	val XSSFRow r = null
		//		 declare a cell object reference
	//	XSSFCell c = null
		//		 create 3 cell styles
		val userHeading = wb.createCellStyle()
		val pointsCell = wb.createCellStyle()
		pointsCell.setBorderRight(BorderStyle.THIN )
		userHeading.setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
		userHeading.setBorderBottom(BorderStyle.THIN)
		wb.setSheetName(nr, "special Bets")

		val r = s.createRow(0)		
		r.setHeightInPoints(100)
		val headings = "User" +: headingStrings
		for((h,index) <- headings.zipWithIndex ){
			val c = r.createCell(index)
			c.setCellStyle( userHeading )
			c.setCellValue(new XSSFRichTextString(h))
		}
		for( (ur,rowNr) <- userRows.zipWithIndex.map(t => (t._1, t._2+1)) ){
			val r = s.createRow(rowNr)
			createRow(r, ur)	
		}
	}
	
	def createRow(row: XSSFRow, userRow: UserRow): Unit = {
		val user = userRow.user 
		val c = row.createCell(0)
		c.setCellValue(userRow.user.username)
		specialBetTs.zipWithIndex.foreach{ case(t,i) => 
			val c = row.createCell(i+1)
			val pred = userRow.specialBets.byTemplateId(t.id).map(_.prediction).getOrElse("NA")
			c.setCellValue(pred)
	    }
	}
	
	def createMessage(wb: XSSFWorkbook, nr: Int): Unit = {
	   val s = wb.createSheet()
	   wb.setSheetName(nr, "important")
	   
	   val heading = wb.createCellStyle()
	   heading.setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
	   val r = s.createRow(0)	
	   var c = r.createCell(0)
		 c.setCellStyle( heading )
     c.setCellValue(new XSSFRichTextString("This excel contains all the current bets for all the users."))
     val r2 = s.createRow(1)
		 val c2 = r2.createCell(0)
		 c2.setCellStyle( heading )
		 c2.setCellValue(new XSSFRichTextString("Do not change the content or the file name (i.e. don't save it after opening). The file name contains a sigature so its possible to verify the file."))
	}
	
	def fillSheet(wb: XSSFWorkbook, sheetname: String, nr: Int): Unit = {
		val s = wb.createSheet()
//		 declare a row object reference
	//	val XSSFRow r = null
		//		 declare a cell object reference
	//	XSSFCell c = null
		//		 create 3 cell styles
		val userHeading = wb.createCellStyle()
		val gameStyle = wb.createCellStyle()
        val pointsHeading = wb.createCellStyle()
        pointsHeading.setRotation(90)
        pointsHeading.setBorderBottom(BorderStyle.THIN)
        pointsHeading.setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
		gameStyle.setRotation(70)
		gameStyle.setBorderBottom(BorderStyle.THIN)
		gameStyle.setAlignment(HorizontalAlignment.LEFT)
        gameStyle.setWrapText(true)
		val pointsCell = wb.createCellStyle()
		pointsCell.setBorderRight(BorderStyle.THIN)
		userHeading.setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
		userHeading.setBorderBottom(BorderStyle.THIN)
		gameStyle.setDataFormat(BuiltinFormats.getBuiltinFormat("text"))
        val leadingCell = wb.createCellStyle()
        leadingCell.setBorderRight(BorderStyle.THIN)
        leadingCell.setFillBackgroundColor(new XSSFColor(java.awt.Color.BLUE))
        leadingCell.setFillPattern(FillPatternType.FINE_DOTS)
		wb.setSheetName(nr, sheetname)

		val r = s.createRow(0)		
		r.setHeightInPoints(100)
		var c = r.createCell(0)
		c.setCellStyle( userHeading )
        c.setCellValue(new XSSFRichTextString("User"))
        c = r.createCell(1)
		c.setCellStyle( pointsHeading )
        c.setCellValue(new XSSFRichTextString("Points"))
        gwts.zipWithIndex.foreach{ case(gwt, index) =>
			c = r.createCell((index + 2))
			c.setCellStyle( gameStyle )
			val gameName = gwt.team1.name + "-" + gwt.team2.name + "\n" + gwt.game.result.display
			c.setCellValue( new XSSFRichTextString(gameName) )
		}
		for( (ur,rowNr) <- userRows.zipWithIndex.map(t => (t._1, t._2+1)) ){
			val r = s.createRow(rowNr)
			c = r.createCell(0)
			c.setCellValue(new XSSFRichTextString(ur.user.username))		
			c = r.createCell(1)
            if( ur.rank == 1){
                c.setCellStyle( leadingCell )
            }else{
                c.setCellStyle( pointsCell ) 
            }
			c.setCellValue(ur.user.points)
			gwts.zipWithIndex.foreach{ case (gwt,index) =>
				c = r.createCell(index + 2)
				val cellValue = sheetname match {
					case "all" => ur.games.get(index)
					case "points" => ur.pointsPerGame.get(index)
					case "cumulatedPoints" => ur.cumulatedPoints.get(index)
					case "betFirstTeamGoals" => ur.firstGoals.get(index)
					case "betSecondTeamGoals" => ur.secondGoals.get(index)
					case "resultFirstTeamGoals" => ur.resultFirstTeam.get(index)
					case "resultSecondTeamGoals" => ur.resultSecondTeam.get(index)
				}
				c.setCellValue(new XSSFRichTextString( cellValue ))
			}
		}
        s.setColumnWidth(1, 256 * 4)
	}
	
}
