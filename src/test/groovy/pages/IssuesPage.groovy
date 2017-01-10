import geb.Page
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.openqa.selenium.WebElement
/**
 * Created by jayesh.kariya on 7/31/15.
 */

@Mixin(ExcelHelper)
class IssuesPage extends Page {

//    static url = "http://172.27.209.104:9000/issues/search#resolved=false"
//    static url = "http://172.27.209.104:9000/component_issues/index?id=Test#resolved=false"
//    static url = "http://172.27.209.83:9000/issues/search#resolved=false"
//    static url = "http://172.27.209.125:9000/issues/search#resolved=false"
//    static url = "http://172.27.209.90:9000/component_issues/index?id=DOMOWeb#resolved=false"
    static url = "http://172.27.209.103:9000/component_issues/index?id=Wetl#resolved=false"
//    static at = {IssuesHeader}
//    static at = {issuesLink}
    static content = {
        IssuesHeader {$("h1", text:"Issues")}
        issuesLink {$("a", text:"Issues")}
        authorLink(wait: 30) {$("div[data-property='authors'] i")}
        searchInput(wait:30) {$("div.select2-search input.select2-input[tabindex='-1']")}
        searchList(wait: 10, required:false) { $("div.select2-result-label") }
        fileNameLink(wait: 30)  {$('i.icon-qualifier-fil').parent()}
        parentElement(wait:10) { $('div.js-list')}
        authorName(wait:10) {i -> $("a span.facet-name", text: i)}
        pageFooter(wait:10) {$('#footer')}
        searchBoxTip(wait:10) {$('.select2-choice.select2-default',4)}

        disableSeverities {$('.search-navigator-facet-box[data-property="severities"] a i.icon-checkbox')}
        disableResolution {$('.search-navigator-facet-box[data-property="resolutions"] a i.icon-checkbox')}
    }

    def clickSearch() {
        println("Calling clickSerach()...")
//        def searchBoxTip = $('.select2-choice.select2-default')[4]
        sleep(2000)
        searchBoxTip.click()
//        js.exec('$(".select2-choice.select2-default[tabindex=\'-1\'] div>b",5).click();')
    }

    def selectUser(def user) {
        sleep(2000)
        searchInput << user
        if (searchList.isDisplayed() == true) {
            searchList.click()
            return true
        }
        else
            return false
    }

    def cybageTeam(outPutFile, userlist, reports){
        for (int i = 0; i < userlist.size(); i++) {
            sleep(2000)
            searchBoxTip.click()
            sleep(2000)
            searchInput << userlist[i]
            if (searchList.isDisplayed() == true) {
                searchList.click()
            }
        }
        disableSeverities.click()

        def sprint = "UI-Framework-18-1-16" // "Start"
        def rowNum = 1
        def valueToWrite
        File file = new File(outPutFile)
        FileInputStream inpStream = new FileInputStream(file)
        XSSFWorkbook workBook = new XSSFWorkbook(inpStream)
        XSSFSheet sheet = workBook.getSheetAt(1)

        while (!(sheet.getRow(rowNum) == null)) {
            rowNum++
        }
        XSSFRow row = sheet.createRow(rowNum)
        row.createCell(0).setCellValue(sprint)
        for (int i = 0; i < reports.size(); i++) {
            if(i < 7 || i > 11){
                row.createCell(i+1).setCellValue(reports[i])
            } else {
                switch(i)
                {
                    case 7 :
                        valueToWrite = $('.search-navigator-facet-box .facet[data-value="BLOCKER"] span.facet-stat').text()
                        row.createCell(i+1).setCellValue(valueToWrite)
                        break;
                    case 8 :
                        valueToWrite = $('.search-navigator-facet-box .facet[data-value="CRITICAL"] span.facet-stat').text()
                        row.createCell(i+1).setCellValue(valueToWrite)
                        break;
                    case 9 :
                        valueToWrite = $('.search-navigator-facet-box .facet[data-value="MAJOR"] span.facet-stat').text()
                        row.createCell(i+1).setCellValue(valueToWrite)
                        break;
                    case 10 :
                        valueToWrite = $('.search-navigator-facet-box .facet[data-value="MINOR"] span.facet-stat').text()
                        row.createCell(i+1).setCellValue(valueToWrite)
                        break;
                    case 11 :
                        valueToWrite = $('.search-navigator-facet-box .facet[data-value="INFO"] span.facet-stat').text()
                        row.createCell(i+1).setCellValue(valueToWrite)
                        break;
                    default :
                        println("Invalid value");
                }
            }
        }

        FileOutputStream outStream = new FileOutputStream(file)

        workBook.write(outStream)
        outStream.close()
        return sprint
    }

    def getUserData(date, outputFile, userEmail, rowNum, cellNum, sprint)
    {
        sleep(2000)
        def valueWrite
        def fileName
        def sheetNum = 2
        def valueToWrite = []
        def createdRow
        WebElement ele = pageFooter.firstElement()

//        Browser.drive { js.exec("window.scrollTo(0, ($ele.location.y))") }

        def childEle = parentElement.children()

        File fileOut = new File(outputFile)
        FileInputStream inpStream_out = new FileInputStream(fileOut)
        XSSFWorkbook workBook = new XSSFWorkbook(inpStream_out)
        XSSFSheet sheet = workBook.getSheetAt(sheetNum)
        def issueType;

        for(def fileValue = 0; fileValue < childEle.size(); fileValue++ )
        {

            if(childEle[fileValue].getAttribute("class") == "issues-workspace-list-component")
            {
                fileName = childEle[fileValue].find('.icon-qualifier-fil').parent().text()
            }
            else
            {
                issueType = 'Standard';

//            writeExcel(outputFile,sprint,rowNum,cellNum,sheetNum)
                sheet.createRow(rowNum).createCell(cellNum).setCellValue(sprint)
//            writeExcel(outputFile,date,rowNum,cellNum+1,sheetNum)
                sheet.getRow(rowNum).createCell(cellNum+1).setCellValue(date)
//            writeExcel(outputFile,userEmail,rowNum,cellNum+2,sheetNum)
                sheet.getRow(rowNum).createCell(cellNum+2).setCellValue(userEmail)

//                writeExcel(outputFile,fileName,rowNum,cellNum+3,sheetNum)
                sheet.getRow(rowNum).createCell(cellNum+3).setCellValue(fileName)

                valueWrite = childEle[fileValue].find('.issue-message').text()
                if(valueWrite && valueWrite.indexOf('DOMO - ') == 0) {
                    issueType = 'Custom';
                    valueWrite = valueWrite.substring(7);
                }
//                valueToWrite << valueWrite
                sheet.getRow(rowNum).createCell(cellNum+4).setCellValue(valueWrite)
//                writeExcel(outputFile,valueWrite,rowNum,cellNum+4,sheetNum)

                valueWrite = childEle[fileValue].find('.issue-meta i[class*="icon-severity"]').parent().text()
//                valueToWrite << valueWrite
                sheet.getRow(rowNum).createCell(cellNum+5).setCellValue(valueWrite)
//                writeExcel(outputFile,valueWrite,rowNum,cellNum+5,sheetNum)

                valueWrite = childEle[fileValue].find('.issue-meta').find('span[title="Line Number"]').text()
//                valueToWrite << valueWrite
                sheet.getRow(rowNum).createCell(cellNum+6).setCellValue(valueWrite)
//                rowNum = writeExcel(outputFile,valueWrite,rowNum,cellNum+6,sheetNum)

                sheet.getRow(rowNum).createCell(cellNum+7).setCellValue(issueType)
                rowNum++
            }
//            println(valueToWrite[fileValue])

//            if (createdRow != rowNum)
//            {
//                createdRow = rowNum
//                sheet.createRow(rowNum).createCell(cellNum).setCellValue(valueToWrite)
//            }
//            else
//            {
//                sheet.getRow(createdRow).createCell(cellNum).setCellValue(valueToWrite)
//            }
        }

//    def writeExcel(outFile, valueToWrite, rowNum, cellNum, sheetNum) {

//        if (createdRow != rowNum)
//        {
//            createdRow = rowNum
//            sheet.createRow(rowNum).createCell(cellNum).setCellValue(valueToWrite)
//        }
//        else
//        {
//            sheet.getRow(createdRow).createCell(cellNum).setCellValue(valueToWrite)
//        }
//
        FileOutputStream outStreamOut = new FileOutputStream(fileOut)
        workBook.write(outStreamOut)
        outStreamOut.close()



        return rowNum
    }
}

