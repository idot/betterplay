package models
import org.joda.time.DateTime

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.List

import org.apache.poi.hssf.usermodel.HSSFCell
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.hssf.usermodel.HSSFRichTextString
import org.apache.poi.hssf.usermodel.HSSFRow
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFSheet._
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.CellStyle

object ExcelData {
  
  def generateExcel(betterDb: BetterDb, dateTime: DateTime, viewingUserId: Long): Array[Byte] = {
     val helper = new StatsHelper(betterDb, dateTime, viewingUserId)  
	   val templates = helper.specialBetsTemplates()
	   val gwts = helper.getGwts()
     val excelD = new ExcelData(helper.createUserRows, gwts, templates)
	   val excel = excelD.createExcelSheetComplete()
	   excel
  }
  
}


class ExcelData(userRows: Seq[UserRow], gwts: Seq[GameWithTeams], specialBetTemplates: Seq[SpecialBetT]){
    val specialBetTs = specialBetTemplates.sortBy(_.id)
	val headingStrings = specialBetTs.map(_.name)
	
	def createExcelSheetComplete(): Array[Byte] = {
	  var wb = new HSSFWorkbook()
	  fillSheet(wb, "all", 0)
	  fillSheet(wb, "points", 1)
	  fillSheet(wb, "cumulatedPoints", 2)
	  fillSheet(wb, "betFirstTeamGoals", 3)
	  fillSheet(wb, "betSecondTeamGoals", 4)
	  fillSheet(wb, "resultFirstTeamGoals", 5)
	  fillSheet(wb, "resultSecondTeamGoals", 6)
	  fillSpecialBets(wb, 7)
	  var out = new ByteArrayOutputStream()
	  wb.write(out)
	  out.close()
	  out.toByteArray
	}
	
	def fillSpecialBets(wb: HSSFWorkbook, nr: Int){
		val s = wb.createSheet()
//		 declare a row object reference
	//	val HSSFRow r = null
		//		 declare a cell object reference
	//	HSSFCell c = null
		//		 create 3 cell styles
		val userHeading = wb.createCellStyle()
		val pointsCell = wb.createCellStyle()
		pointsCell.setBorderRight(CellStyle.BORDER_THIN )
		userHeading.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"))
		userHeading.setBorderBottom(CellStyle.BORDER_THIN)
		wb.setSheetName(nr, "special Bets")

		val r = s.createRow(0)		
		r.setHeightInPoints(100)
		val headings = "User" +: headingStrings
		for((h,index) <- headings.zipWithIndex ){
			val c = r.createCell(index)
			c.setCellStyle( userHeading )
			c.setCellValue(new HSSFRichTextString(h))
		}
		for( (ur,rowNr) <- userRows.zipWithIndex.map(t => (t._1, t._2+1)) ){
			val r = s.createRow(rowNr)
			createRow(r, ur)	
		}
	}
	
	def createRow(row: HSSFRow, userRow: UserRow){
		val user = userRow.user 
		val c = row.createCell(0)
		c.setCellValue(userRow.user.username)
		specialBetTs.zipWithIndex.foreach{ case(t,i) => 
			val c = row.createCell(i+1)
			val pred = userRow.specialBets.byTemplateId(t.id).map(_.prediction).getOrElse("NA")
			c.setCellValue(pred)
	    }
	}
		
	
	def fillSheet(wb: HSSFWorkbook, sheetname: String, nr: Int){
		val s = wb.createSheet()
//		 declare a row object reference
	//	val HSSFRow r = null
		//		 declare a cell object reference
	//	HSSFCell c = null
		//		 create 3 cell styles
		val userHeading = wb.createCellStyle()
		val gameStyle = wb.createCellStyle()
        val pointsHeading = wb.createCellStyle()
        pointsHeading.setRotation(90)
        pointsHeading.setBorderBottom(CellStyle.BORDER_THIN)
        pointsHeading.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"))
		gameStyle.setRotation(70)
		gameStyle.setBorderBottom(CellStyle.BORDER_THIN)
		gameStyle.setAlignment(CellStyle.ALIGN_LEFT)
        gameStyle.setWrapText(true)
		val pointsCell = wb.createCellStyle()
		pointsCell.setBorderRight(CellStyle.BORDER_THIN )
		userHeading.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"))
		userHeading.setBorderBottom(CellStyle.BORDER_THIN)
		gameStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("text"))
        val leadingCell = wb.createCellStyle()
        leadingCell.setBorderRight(CellStyle.BORDER_THIN )
        leadingCell.setFillBackgroundColor(HSSFColor.AQUA.index)
        leadingCell.setFillPattern(CellStyle.FINE_DOTS)
		wb.setSheetName(nr, sheetname)

		val r = s.createRow(0)		
		r.setHeightInPoints(100)
		var c = r.createCell(0)
		c.setCellStyle( userHeading )
        c.setCellValue(new HSSFRichTextString("User"))
        c = r.createCell(1)
		c.setCellStyle( pointsHeading )
        c.setCellValue(new HSSFRichTextString("Points"))
        gwts.zipWithIndex.foreach{ case(gwt, index) =>
			c = r.createCell((index + 2))
			c.setCellStyle( gameStyle )
			val gameName = gwt.team1.name + "-" + gwt.team2.name + "\n" + gwt.game.result.display
			c.setCellValue( new HSSFRichTextString(gameName) )
		}
		for( (ur,rowNr) <- userRows.zipWithIndex.map(t => (t._1, t._2+1)) ){
			val r = s.createRow(rowNr)
			c = r.createCell(0)
			c.setCellValue(new HSSFRichTextString(ur.user.username))		
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
				c.setCellValue(new HSSFRichTextString( cellValue ))
			}
		}
        s.setColumnWidth(1, 256 * 4)
	}
	
}
