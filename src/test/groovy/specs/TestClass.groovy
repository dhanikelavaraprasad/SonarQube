import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import geb.spock.GebReportingSpec
import groovyx.net.http.RESTClient
import org.apache.poi.xssf.usermodel.XSSFSheet

class TestClass extends GebReportingSpec {
  def inputFilePath = new File("src/datasets/User_List_WETL.xlsx").absolutePath
  def userDataFilePath = new File("src/datasets/User_Data_List_WETL.xlsx").absolutePath
  def outputFilePath = new File("src/datasets/SonarQube_Report_WETL.xlsx").absolutePath
  def baseUrl = "http://172.27.209.90:9000/"
  def activeRules, users, connection, url


  def "Check New Test"() {

    when:
    to SonarQubeHome
    driver.manage().window().maximize()

    then:
    waitFor(20) {at SonarQubeHome}

    when:
    System.out.println("Getting Active Rules");
    createConnection("api/profiles/index?name=DomoWeb;language=js;format=json")

    then:
    processRules()

    when:
    System.out.println("Getting Authors");
    createConnection("api/issues/authors?q=&ps=500")

    then:
    processAuthors()
  }

  def createConnection(def getUrl) {
    url = new URL(baseUrl + getUrl)
    connection = url.openConnection()
    connection.setRequestMethod("GET")
    connection.connect()
  }

  def processRules() {
    if (connection.responseCode == 200 || connection.responseCode == 201) {
      Gson gson = new Gson();
      JsonArray qualityProfileList = gson.fromJson(connection.content.text, JsonArray);
      for(JsonObject qualityProfile : qualityProfileList) {
        println("Quality Profile : " + qualityProfile.get("name"));
        println("Active Rules");
        activeRules = qualityProfile.getAsJsonArray("rules");
        for(JsonObject rule : activeRules) {
          println(rule.get("key"))
        }
      }
    }
  }

  def processAuthors() {
    if (connection.responseCode == 200 || connection.responseCode == 201) {
      Gson gson = new Gson();
      JsonObject authors = gson.fromJson(connection.content.text, JsonObject);
      users = authors.getAsJsonArray("authors");
      for(String author : users) {
        println(author);
      }
    }
  }
}
