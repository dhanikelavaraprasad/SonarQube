import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import geb.spock.GebReportingSpec
import groovyx.net.http.RESTClient
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import java.text.SimpleDateFormat

class SonarQubeApi extends GebReportingSpec {
  def outputFilePath = new File("src/datasets/SonarQube_Report_UI-Test.xlsx").absolutePath
  def baseUrl = "http://172.27.209.114:9000/"
  def users, connection, url, projectKey, workBook, fileOut, authorsSheet, activeRulesSheet, userWiseRuleSheet, userWiseRuleDetailsSheet
  def activeRules = new ArrayList<JsonObject>();
  def allRules = new HashMap<String, JsonObject>();
  def updatedUsers = new ArrayList<JsonObject>();
  def searchBaseUrl = "api/issues/search?resolved=false", sprintName = "DOMO-2016-05-10", projectId="DOMO"
  def userWiseRuleDetailsRowNum = 1, userWiseRuleNum = 1, componentUuids
  def dataToMySql = new DataToMySql();


  def "Check New Test"() {

    when:
    System.out.println("Getting Authors");
    opeExcelFile();
    callToSonarAPI("api/issues/authors?q=&ps=500");

    then:
    processResponse(1)


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
    System.out.println("Getting Active Rules");
    callToSonarAPI("api/qualityprofiles/search?&projectKey=" + projectKey)

    then:
    processResponse(4);

  }

  def getFileName(component) {
    def indexFind = component.lastIndexOf("/");
    return (indexFind != -1 ? component.substring(indexFind+1, component.length()) : component)
  }

  def getFilePath(component) {
    def indexFind = component.lastIndexOf("/");
    return (indexFind != -1 ? component.substring(0, indexFind) : component)
  }

  def callToSonarAPI(serviceUrl) {
    url = new URL(baseUrl + serviceUrl)
    connection = url.openConnection()
    connection.setRequestMethod("GET")
    connection.connect()
  }

  def getUserName(email) {
    def firstName;
    def lastName = '';
    def startIndex = email.indexOf(".")
    def endIndex = email.indexOf("-")
    if(endIndex == -1) {
      endIndex = email.indexOf(" - ")
    }
    if(endIndex == -1) {
      endIndex = email.indexOf("@")
    }
    if(startIndex > endIndex || startIndex == -1) {
      firstName = email.substring(0, endIndex)
      firstName = Character.toString(firstName.charAt(0)).toUpperCase() + firstName.substring(1)
    } else {
      firstName = email.substring(0, startIndex)
      firstName = Character.toString(firstName.charAt(0)).toUpperCase() + firstName.substring(1)
      lastName = email.substring(startIndex + 1, endIndex)
      lastName = Character.toString(lastName.charAt(0)).toUpperCase() + lastName.substring(1)
    }
    return firstName + " " + lastName
  }

  def processResponse(type) {
    boolean result = false;
    if (connection.responseCode == 200 || connection.responseCode == 201) {
      Gson gson = new Gson();
      switch(type){
        case 1:
          JsonObject authors = gson.fromJson(connection.content.text, JsonObject);
          users = authors.getAsJsonArray("authors");
          for(String user : users) {
            if(user.indexOf("@domo.com") != -1 || user.indexOf("@cybage.com") != -1) {
              JsonObject userObject = new JsonObject();
              userObject.addProperty("userName", getUserName(user.replace("\"", '')))
              userObject.addProperty("userEmail", user.replace("\"", ''))
              userObject.addProperty("isCybageUser", (user.indexOf("cybage") != -1))
              updatedUsers.add(userObject);
            }
          }
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
          def ruleIndex = 1;
          for(JsonObject qualityProfile : profiles.get("profiles")) {
            println("Quality Profile : " + qualityProfile.get("key").getAsString());

            System.out.println("Getting All Rules");
            url = new URL(baseUrl + "api/rules/search?language=js&ps=200&s=createdAt&qprofile=" + qualityProfile.get("key").getAsString())
            connection = url.openConnection()
            connection.setRequestMethod("GET")
            connection.connect()

            if (connection.responseCode == 200 || connection.responseCode == 201) {
              JsonObject qualityProfileRules = gson.fromJson(connection.content.text, JsonObject);

              for(JsonObject rule : qualityProfileRules.get("rules").getAsJsonArray()) {
                rule.addProperty("customRuleName", "Rule"+ruleIndex)
                allRules.put(rule.get("key").getAsString(), rule)
                ruleIndex++;
              }
            }

            System.out.println("Getting Active Rules");
            url = new URL(baseUrl + "api/rules/search?language=js&activation=true&ps=200&s=key&qprofile=" + qualityProfile.get("key").getAsString())
            connection = url.openConnection()
            connection.setRequestMethod("GET")
            connection.connect()

            if (connection.responseCode == 200 || connection.responseCode == 201) {
              JsonObject qualityProfileRules = gson.fromJson(connection.content.text, JsonObject);
              activeRules.addAll(qualityProfileRules.get("rules").getAsJsonArray())
            }
          }
          setRuleName()
          writeActiveRules();
          for(JsonObject author : updatedUsers) {
            println("Author : " + author.get("userName").getAsString());
            def rulesResultList = new HashMap<JsonObject, JsonArray>()
            def newSearchUrl = searchBaseUrl + "&authors=" + author.get("userEmail").getAsString().replace(" ", "%20") + "&componentUuids=" + componentUuids

            for (JsonObject rule : activeRules) {
              JsonArray resultsArray = searchIssuesByAuthorAndRule(baseUrl + newSearchUrl + "&rules=" + rule.get("key").getAsString())
              if(resultsArray.size() > 0) {
                rulesResultList.put(rule, resultsArray);
              }
            }
            dataToMySql.writeToDataBase(rulesResultList,
                projectId,
                author.get("isCybageUser").getAsString(),
                author.get("userEmail").getAsString(),
                author.get("userName").getAsString(),
                sprintName)

            writeUserWiseDetails(author.get("userName").getAsString(), author.get("userEmail").getAsString(), rulesResultList)
          }
          closeExcelFile()
          break;
      }
      result = true;
    }
    return  result;
  }

  def setRuleName() {
    for (JsonObject rule : activeRules) {
      allRules.get(rule.get("key").getAsString()).addProperty("active", true);
      rule.addProperty("ruleType", getRuleType(rule.get("key").getAsString()))
      //println(allRules.get(rule.get("key").getAsString()).get("createdAt").toString())
      rule.addProperty("customRuleName", allRules.get(rule.get("key").getAsString()).get("customRuleName").getAsString())
    }
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
    authorsSheet.getRow(0).createCell(1).setCellValue("Author Name")
    authorsSheet.getRow(0).createCell(2).setCellValue("Cybage User")

    activeRulesSheet = workBook.createSheet("Active Rules")
    activeRulesSheet.createRow(0).createCell(0).setCellValue("Rule Key")
    activeRulesSheet.getRow(0).createCell(1).setCellValue("Active Rule")
    activeRulesSheet.getRow(0).createCell(2).setCellValue("Created At")
    activeRulesSheet.getRow(0).createCell(3).setCellValue("Rule Number")
    activeRulesSheet.getRow(0).createCell(4).setCellValue("Rule Name")
    activeRulesSheet.getRow(0).createCell(5).setCellValue("Severity")
    activeRulesSheet.getRow(0).createCell(6).setCellValue("Rule Type")

    userWiseRuleSheet = workBook.createSheet("User Wise Rule Count")
    userWiseRuleSheet.createRow(0).createCell(0).setCellValue("Project ID")
    userWiseRuleSheet.getRow(0).createCell(1).setCellValue("Author Email")
    userWiseRuleSheet.getRow(0).createCell(2).setCellValue("Author Name")
    userWiseRuleSheet.getRow(0).createCell(3).setCellValue("Date")
    userWiseRuleSheet.getRow(0).createCell(4).setCellValue("Sprint Name")
    userWiseRuleSheet.getRow(0).createCell(5).setCellValue("Rule Number")
    userWiseRuleSheet.getRow(0).createCell(6).setCellValue("Rule Key")
    userWiseRuleSheet.getRow(0).createCell(7).setCellValue("Rule Name")
    userWiseRuleSheet.getRow(0).createCell(8).setCellValue("Severity")
    userWiseRuleSheet.getRow(0).createCell(9).setCellValue("Rule Type")
    userWiseRuleSheet.getRow(0).createCell(10).setCellValue("Count")

    userWiseRuleDetailsSheet = workBook.createSheet("User Wise Rule Details")
    userWiseRuleDetailsSheet.createRow(0).createCell(0).setCellValue("Author Email")
    userWiseRuleDetailsSheet.getRow(0).createCell(1).setCellValue("Author Name")
    userWiseRuleDetailsSheet.getRow(0).createCell(2).setCellValue("Rule Number")
    userWiseRuleDetailsSheet.getRow(0).createCell(3).setCellValue("Rule Key")
    userWiseRuleDetailsSheet.getRow(0).createCell(4).setCellValue("Rule Name")
    userWiseRuleDetailsSheet.getRow(0).createCell(5).setCellValue("Severity")
    userWiseRuleDetailsSheet.getRow(0).createCell(6).setCellValue("Rule Type")
    userWiseRuleDetailsSheet.getRow(0).createCell(7).setCellValue("Failure Message")
    userWiseRuleDetailsSheet.getRow(0).createCell(8).setCellValue("File Name")
    userWiseRuleDetailsSheet.getRow(0).createCell(9).setCellValue("File Path")
    userWiseRuleDetailsSheet.getRow(0).createCell(10).setCellValue("Line Number")

  }

  def writeUserWiseDetails(userName, userEmail, rulesResultList) {
    Iterator it = rulesResultList.entrySet().iterator()
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next()
      userWiseRuleSheet.createRow(userWiseRuleNum).createCell(0).setCellValue(projectId)
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(1).setCellValue(userEmail)
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(2).setCellValue(userName)
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(3).setCellValue(new Date().format("dd-MM-yy"))
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(4).setCellValue(sprintName)
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(5).setCellValue(pair.getKey().get("customRuleName").getAsString())
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(6).setCellValue(pair.getKey().get("key").getAsString())
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(7).setCellValue(pair.getKey().get("name").getAsString())
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(8).setCellValue(pair.getKey().get("severity").getAsString())
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(9).setCellValue(pair.getKey().get("ruleType").getAsString())
      userWiseRuleSheet.getRow(userWiseRuleNum).createCell(10).setCellValue(pair.getValue().size())
      for (JsonObject failure : pair.getValue()) {
        userWiseRuleDetailsSheet.createRow(userWiseRuleDetailsRowNum).createCell(0).setCellValue(userEmail)
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(1).setCellValue(userName)
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(2).setCellValue(pair.getKey().get("customRuleName").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(3).setCellValue(pair.getKey().get("key").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(4).setCellValue(pair.getKey().get("name").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(5).setCellValue(pair.getKey().get("severity").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(6).setCellValue(pair.getKey().get("ruleType").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(7).setCellValue(failure.get("message").getAsString())
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(8).setCellValue(getFileName(failure.get("component").getAsString()))
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(9).setCellValue(getFilePath(failure.get("component").getAsString()))
        userWiseRuleDetailsSheet.getRow(userWiseRuleDetailsRowNum).createCell(10).setCellValue(failure.get("line") == null ? "" : failure.get("line").getAsString())
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
    for(JsonObject author : updatedUsers) {
      authorsSheet.createRow(authorsCount).createCell(0).setCellValue(author.get("userEmail").getAsString())
      authorsSheet.getRow(authorsCount).createCell(1).setCellValue(author.get("userName").getAsString())
      authorsSheet.getRow(authorsCount).createCell(2).setCellValue(author.get("isCybageUser").getAsString())
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
    Iterator it = allRules.entrySet().iterator()
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry) it.next()
      def rule = pair.getValue()
      def ruleKey = rule.get("key").getAsString()
      rule.addProperty("ruleType", getRuleType(ruleKey))
      activeRulesSheet.createRow(activeRulesCount).createCell(0).setCellValue(ruleKey)
      activeRulesSheet.getRow(activeRulesCount).createCell(1).setCellValue(rule.get("active").toString() == "true" ? "Yes" : "No")
      activeRulesSheet.getRow(activeRulesCount).createCell(2).setCellValue(getCreatedDate(rule.get("createdAt").toString().replace("\"","")))
      activeRulesSheet.getRow(activeRulesCount).createCell(3).setCellValue(rule.get("customRuleName").getAsString())
      activeRulesSheet.getRow(activeRulesCount).createCell(4).setCellValue(rule.get("name").getAsString())
      activeRulesSheet.getRow(activeRulesCount).createCell(5).setCellValue(rule.get("severity").getAsString())
      activeRulesSheet.getRow(activeRulesCount).createCell(6).setCellValue(rule.get("ruleType").getAsString())
      activeRulesCount++;
    }
  }

  def getCreatedDate(createdDate) {
    createdDate = createdDate.substring(0,createdDate.indexOf("T"))
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat('yyyy-MM-dd');
    return simpleDateFormat.parse(createdDate).format("dd-MM-yy");
  }

  def closeExcelFile() {
    FileOutputStream outStreamOut = new FileOutputStream(fileOut)
    workBook.write(outStreamOut)
    outStreamOut.close()
  }
}
