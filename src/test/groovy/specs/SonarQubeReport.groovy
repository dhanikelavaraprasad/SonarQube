import geb.spock.GebReportingSpec
import org.apache.poi.xssf.usermodel.XSSFSheet
/**
 * Created by jayesh.kariya on 14/07/15.
 */

@Mixin(ExcelHelper)
class SonarQubeReport extends GebReportingSpec {
    /*def inputFilePath = new File("src/datasets/User_List.xlsx").absolutePath
    def userDataFilePath = new File("src/datasets/User_Data_List.xlsx").absolutePath
    def outputFilePath = new File("src/datasets/SonarQube_Report_UI_FrameWork.xlsx").absolutePath*/
    //def outputFilePath = new File("src/datasets/OverAll_Report.xlsx").absolutePath
    def inputFilePath = new File("src/datasets/User_List_WETL.xlsx").absolutePath
    def userDataFilePath = new File("src/datasets/User_Data_List_WETL.xlsx").absolutePath
    def outputFilePath = new File("src/datasets/SonarQube_Report_WETL.xlsx").absolutePath
    def userId = []
    def reportValues = []
    def userList = []
    def profileName = "DomoWeb"

  def "Check New Test"() {
    when:
    to SonarQubeHome
    driver.manage().window().maximize()

    then:
    waitFor(20) {at SonarQubeHome}

    when:
    domoWebLink.click()

    then:
    waitFor(20) {at DomoWebDashboard}

    when:
    def dateValue = new Date().getDateString()
    reportValues.add(dateValue)
    reportValues.addAll(getOverAllReportData())

    def rowValue = 1
    def sprintValue = writeToOverAllReport(outputFilePath,reportValues,rowValue)

    then:
    true

    when:
    to IssuesPage
//    issuesLink.click()

    then:
    true
//    waitFor(30) {at IssuesPage}

    when:

    authorLink.click()
    def totalCount = getExcelRowCount(inputFilePath)
    def newRow = 1
    def newCell = 0

//    XSSFSheet sheet = readPage(userDataFilePath)
    XSSFSheet sheet = readPage(outputFilePath, 2)

    while (!(sheet.getRow(newRow) == null)) {
        newRow++
    }
    println(newRow)
    disableSeverities.click()
    //disableResolution.click()
    sleep(3000)

    def newUrl = getCurrentUrl() + "authors="
//    for(def count = 1; count < totalCount; count++)
//    {
//        sleep(2000);
////        clickSearch()
//        userId = readUserData(inputFilePath, count)//.toString()
//
//        def userUrl = newUrl + userId[0]
////        go userUrl
//        clickSearch()
//        sleep(5000)
//
//        if (selectUser(userId[0])==true) {
//            WebElement element = driver.findElement(By.cssSelector('div.search-navigator-workspace-list-more.js-more'));
//            println(element.isDisplayed())
//            while(element.isDisplayed()) {
//                JavascriptExecutor jse = (JavascriptExecutor)driver;
//                jse.executeScript("window.scrollTo(0,Math.max(document.documentElement.scrollHeight,document.body.scrollHeight,document.documentElement.clientHeight));");
//                println(element.isDisplayed())
//            }
//            newRow = getUserData(dateValue, outputFilePath, userId[1], newRow, newCell,sprintValue)
//            authorName(userId[0]).click()
//        }
//        else
//            clickSearch()
//
////        WebElement element = driver.findElement(By.cssSelector('div.search-navigator-workspace-list-more.js-more'));
////        println(element.isDisplayed())
////        while(element.isDisplayed()) {
////            JavascriptExecutor jse = (JavascriptExecutor)driver;
////            jse.executeScript("window.scrollTo(0,Math.max(document.documentElement.scrollHeight,document.body.scrollHeight,document.documentElement.clientHeight));");
////            println(element.isDisplayed())
////        }
////        newRow = getUserData(dateValue, outputFilePath, userId[1], newRow, newCell,sprintValue)
//
//    }

    for(def count = 1; count < totalCount; count++) {
        sleep(2000);
//        clickSearch()
        userId = readUserData(inputFilePath, count)//.toString()
        println(userId[0])
        userList.add(userId[0])
    }
    cybageTeam(outputFilePath, userList, reportValues)

    then:
    true
  }
}
