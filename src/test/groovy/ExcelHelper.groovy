import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

/**
 * Created by jayesh.kariya on 7/29/15.
 */
class ExcelHelper {
    static private def createdRow

    def readPage(filePath, def sheetNum) {
        File file = new File(filePath)
        FileInputStream inpStream = new FileInputStream(file)
        XSSFWorkbook workBook = new XSSFWorkbook(inpStream)
        XSSFSheet sheet = workBook.getSheetAt(sheetNum);
        return sheet
    }

    def readUserData(filePath, rowValue) {
        def userId
        def name

        XSSFSheet excelSheet = readPage(filePath, 0)
        XSSFRow row = excelSheet.getRow(rowValue)
        userId = row.getCell(0).toString()
        name = row.getCell(1).toString()
        return [userId, name]
    }

    def getExcelRowCount(filePath) {
        XSSFSheet excelSheet = readPage(filePath, 0)
        def totalRow = excelSheet.getPhysicalNumberOfRows()
        return totalRow
    }

    def writeExcel(outFile, valueToWrite, rowNum, cellNum, sheetNum) {

        File fileOut = new File(outFile)
        FileInputStream inpStream_out = new FileInputStream(fileOut)
        XSSFWorkbook workBook = new XSSFWorkbook(inpStream_out)
        XSSFSheet sheet = workBook.getSheetAt(sheetNum)
        println(createdRow)
        if (createdRow != rowNum)
        {
            createdRow = rowNum
            sheet.createRow(rowNum).createCell(cellNum).setCellValue(valueToWrite)
        }
        else
        {
            sheet.getRow(createdRow).createCell(cellNum).setCellValue(valueToWrite)
        }

        rowNum++

        FileOutputStream outStreamOut = new FileOutputStream(fileOut)
        workBook.write(outStreamOut)
        outStreamOut.close()

        return rowNum
    }

    def writeToOverAllReport(outPutFile, reports, rowNum) {
//        def sprint = "End" // "Start"
        def sprint = "UI-Framework-18-1-16" // "Start"
//        def sprint = "Slapchop-12-1-16" // "Start"
        File file = new File(outPutFile)
        FileInputStream inpStream = new FileInputStream(file)
        XSSFWorkbook workBook = new XSSFWorkbook(inpStream)
        XSSFSheet sheet = workBook.getSheetAt(0)

        while (!(sheet.getRow(rowNum) == null)) {
//            if (sheet.getRow(rowNum).getCell(0).toString() == "Start")
//                sprint = "End"
//            else
//                sprint = "Start"
            rowNum++
        }
        XSSFRow row = sheet.createRow(rowNum)
        row.createCell(0).setCellValue(sprint)
        for (int i = 0; i < reports.size(); i++) {
            row.createCell(i+1).setCellValue(reports[i])
        }

        FileOutputStream outStream = new FileOutputStream(file)

        workBook.write(outStream)
        outStream.close()
        return sprint
    }

}
