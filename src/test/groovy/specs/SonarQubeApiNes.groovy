package specs

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import geb.spock.GebReportingSpec
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class SonarQubeApiNes extends GebReportingSpec {
  def outputFilePath = new File("src/datasets/SonarQube_Report_UI-Test.xlsx").absolutePath
  def baseUrl = "http://172.27.209.114:9000/"
  def users, connection, url, componentUuids, projectKey, workBook, fileOut, authorsSheet, activeRulesSheet, userWiseRuleSheet, userWiseRuleDetailsSheet
  def activeRules = new ArrayList<JsonObject>();
  def searchBaseUrl = "api/issues/search?resolved=false"
  def userWiseRuleDetailsRowNum = 1, userWiseRuleNum = 1


  def "Check New Test"() {

    when:
    to SonarQubeHome
    driver.manage().window().maximize()

    then:
    waitFor(20) {at SonarQubeHome}

    when:
    System.out.println("Getting Authors");
    opeExcelFile();
    callToSonarAPI("api/issues/authors?q=&ps=500");

    then:
    processResponse(1);

    when:
    System.out.println("Getting Projects");
    callToSonarAPI("api/projects/index?format=json");

    then:
    processResponse(2);

    when:
    System.out.println("Getting Component ID");
    callToSonarAPI("api/components/show?key=" + projectKey)

    then:
    processResponse(3);

    when:
    System.out.println("Getting Quality Profiles ");
    callToSonarAPI("api/qualityprofiles/search?&projectKey=" + projectKey)

    then:
    processResponse(4);

  }

  def getFileName(component) {
    def indexFind = component.lastIndexOf("/");
    return (indexFind != -1 ? component.substring(indexFind+1, component.length()) : component)
  }

  def callToSonarAPI(serviceUrl) {
    url = new URL(baseUrl + serviceUrl)
    connection = url.openConnection()
    connection.setRequestMethod("GET")
    connection.connect()
  }

  def processResponse(type) {
    boolean result = false;
    if (connection.responseCode == 200 || connection.responseCode == 201) {
      Gson gson = new Gson();
      switch(type){
        case 1:
          JsonObject authors = gson.fromJson(connection.content.text, JsonObject);
          users = authors.getAsJsonArray("authors");
          writeAuthors()
          break;
        case 2:
          JsonArray projects = gson.fromJson(connection.content.text, JsonArray);
          projectKey = projects.get(0).get("k").toString().replace("\"","")
          println("Project Key : " + projectKey);
          break;
        case 3:
          JsonObject component = gson.fromJson(connection.content.text, JsonObject);
          componentUuids = component.get("component").getAsJsonObject().get("id").toString().replace("\"","")
          println("Component UUID : " + componentUuids);
          break;
        default:
          JsonObject profiles = gson.fromJson(connection.content.text, JsonObject);
          for(JsonObject qualityProfile : profiles.get("profiles").getAsJsonArray()) {
            println("Quality Profile key : " + qualityProfile.get("key"));
            println("Quality Profile : " + qualityProfile.get("name"));

            System.out.println("Getting Active Rules");
            url = new URL(baseUrl + "api/rules/search?language=js&activation=true&qprofile=" + qualityProfile.get("key").toString().replace("\"",""))
            connection = url.openConnection()
            connection.setRequestMethod("GET")
            connection.connect()

            if (connection.responseCode == 200 || connection.responseCode == 201) {
              JsonObject qualityProfileRules = gson.fromJson(connection.content.text, JsonObject);
              activeRules.addAll(qualityProfileRules.get("rules").getAsJsonArray())
            }
          }
          writeActiveRules();
          for(String author : users) {
            println("Author : " + author);
            def rulesResultList = new HashMap<JsonObject, JsonArray>()
            def newSearchUrl = searchBaseUrl + "&authors=" + author.replace(" ", "%20") + "&componentUuids=" + componentUuids

            for (JsonObject rule : activeRules) {
              rulesResultList.put(rule, searchIssuesByAuthorAndRule(baseUrl + newSearchUrl + "&rules=" + rule.get("key").getAsString()));
            }
            writeUserWiseDetails(author, rulesResultList)
          }
          closeExcelFile()
          break;
      }
      result = true;
    }
    return  result;
  }

  def searchIssuesByAuthorAndRule(serviceUrl) {
    def loadMore = true;
    def pageNumber = 1;
    def pageSize = 500;
    def searchResults = new JsonArray();
    while(loadMore) {
      url = new URL(serviceUrl + "&p=" + pageNumber + "&ps=" + pageSize)
      connection = url.openConnection()
      connection.setRequestMethod("GET")
      connection.connect()

      if (connection.responseCode == 200 || connection.responseCode == 201) {
        JsonObject countObject = new Gson().fromJson(connection.content.text, JsonObject)
        loadMore = (pageNumber*pageSize < new Integer(countObject.get("total").getAsString()))
        pageNumber++;
        searchResults.addAll(countObject.get("issues"))
      }
    }
    return searchResults;
  }

  def opeExcelFile() {
    fileOut = new File(outputFilePath)
    FileInputStream inpStream_out = new FileInputStream(fileOut)
    workBook = new XSSFWorkbook(inpStream_out)
    authorsSheet = workBook.createSheet("Authors")
    authorsSheet.createRow(0).createCell(0).setCellValue("Author Email")

    activeRulesSheet = workBook.createSheet("Active Rules")
    activeRulesSheet.createRow(0).createCell(0).setCellValue("Rule Key")
    activeRulesSheet.getRow(0).createCell(1).setCellValue("Rule Name")
    activeRulesSheet.getRow(0).createCell(2).setCellValue("Severity")
    activeRulesSheet.getRow(0).createCell(3).setCellValue("Rule Type")

    userWiseRuleSheet = workBook.createSheet("User Wise Rule Count")
    userWiseRuleSheet.createRow(0).createCell(0).setCellValue("Author Email")
    userWiseRuleSheet.getRow(0).createCell(1).setCellValue("Rule Key")
    userWiseRuleSheet.getRow(0).createCell(2).setCellValue("Rule Name")
    userWiseRuleSheet.getRow(0).createCell(3).setCellValue("Severity")
    userWiseRuleSheet.getRow(0).createCell(4).setCellValue("Rule Type")
    userWiseRuleSheet.getRow(0).createCell(5).setCellValue("Count")

    userWiseRuleDetailsSheet = workBook.createSheet("User Wise Rule Details")
    userWiseRuleDetailsSheet.createRow(0).createCell(0).setCellValue("Author Email")
    userWiseRuleDetailsSheet.getRow(0).createCell(1).setCellValue("Rule Key")
    userWiseRuleDetailsSheet.getRow(0).createCell(2).setCellValue("Rule Name")
    userWiseRuleDetailsSheet.getRow(0).createCell(3).setCellValue("Severity")
    userWiseRuleDetailsSheet.getRow(0).createCell(4).setCellValue("Rule Type")
    userWiseRuleDetailsSheet.getRow(0).createCell(5).setCellValue("Failure Message")
    userWiseRuleDetailsSheet.getRow(0).createCell(6).setCellValue("File Name")
    userWiseRuleDetailsSheet.getRow(0).createCell(7).setCellValue("Line Number")

  }

  def writeUserWiseDetails(userEmail, rulesResultList) {
    Iterator it = rulesResultList.entrySet().iterator()
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next()
      userWiseRuleSheet.createRow(userWiseRuleNum).createCell(0).setCellValue(userEmail)
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(1).setCellValue(pair.getKey().get("key").getAsString())
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(2).setCellValue(pair.getKey().get("name").getAsString())
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(3).setCellValue(pair.getKey().get("severity").getAsString())
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(4).setCellValue(pair.getKey().get("ruleType").getAsString())
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(5).setCellValue(pair.getValue().size())
      for(JsonObject failure : pair.getValue()) {
        userWiseRuleDetailsSheet.createRow(userWiseRuleDetailsRowNum).createCell(0).setCellValue(userEmail)
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(1).setCellValue(pair.getKey().get("key").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(2).setCellValue(pair.getKey().get("name").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(3).setCellValue(pair.getKey().get("severity").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(4).setCellValue(pair.getKey().get("ruleType").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(5).setCellValue(failure.get("message").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(6).setCellValue(getFileName(failure.get("component").getAsString()))
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(7).setCellValue(failure.get("line") == null ? "" : failure.get("line").getAsString())
        userWiseRuleDetailsRowNum++;
      }
      userWiseRuleNum++;
    }
  }

  def getRuleType(key) {
    return key.indexOf("custom:") != -1 ? "Custom" : "Standard"
  }

  def writeAuthors() {
    def authorsCount = 1
    createUserName()
    for(String author : users) {
      authorsSheet.createRow(authorsCount).createCell(0).setCellValue(author)
      authorsCount++;
    }
  }

  def createUserName() {
    def tempUsers = users;
    users = new ArrayList<String>()
    for(String author : tempUsers) {
      users.push(author.replace("\"",""))
    }
  }

  def writeActiveRules() {
    def activeRulesCount = 1
    for(JsonObject rule : activeRules) {
      def ruleKey = rule.get("key").getAsString()
      rule.addProperty("ruleType", getRuleType(ruleKey))
      activeRulesSheet.createRow(activeRulesCount).createCell(0).setCellValue(ruleKey)
      activeRulesSheet.getRow(activeRulesCount).createCell(1).setCellValue(rule.get("name").getAsString())
      activeRulesSheet.getRow(activeRulesCount).createCell(2).setCellValue(rule.get("severity").getAsString())
      activeRulesSheet.getRow(activeRulesCount).createCell(3).setCellValue(rule.get("ruleType").getAsString())
      activeRulesCount++;
    }
  }

  def closeExcelFile() {
    FileOutputStream outStreamOut = new FileOutputStream(fileOut)
    workBook.write(outStreamOut)
    outStreamOut.close()
  }
}
