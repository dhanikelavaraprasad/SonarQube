/**
 * Created by gayatrisabade on 14/07/15.
 */
import org.openqa.selenium.Platform
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.chrome.ChromeDriver


//adminUser = ''
//adminUsername = System.getProperty("username","")
//adminPassword = System.getProperty("password","")
//editorUsername = ''
//editorPassword = ''
//privilegedUser = ''
//privilegedUsername = ''
//privilegedPassword = ''
//participantUser = ''
//participantUsername = ''
//participantPassword = ''
//// Database Credentials
//mongodbName = ''
//mongodbCollection = ''

//primaryUser = adminUser

//pages = [ dynamicPage : 'New Geb Page' ]
reportsDir = "build/geb-reports"
downloadDir = System.getProperty("user.dir")
dataSetSourceDir = "src/dataSets/"
baseUrl = "http://172.27.209.90:9000/"

waiting {
//    presets {
//        xsmall { timeout = 3 }
//        small { timeout = 10 }
//        medium { timeout = 30 }
//        large { timeout = 60 }
//        xlarge { timeout = 600 }
//    }
  timeout = 10
  retryInterval = 0.5
}

environments {
  // -Dgeb.env=<browser>
  // See: http://code.google.com/p/selenium/wiki/FirefoxDriver
  firefox {
    FirefoxProfile fp = new FirefoxProfile()
    fp.setPreference("browser.download.dir", downloadDir)
    fp.setPreference("browser.download.folderList", 2)
    fp.setPreference("browser.download.useDownloadDir", true)
    fp.setPreference("browser.helperApps.alwaysAsk.force", false)
    fp.setPreference("browser.download.manager.showWhenStarting", false)

    fp.setPreference("browser.helperApps.neverAsk.saveToDisk", "text/csv, application/ms-excel, application/vnd.ms-excel, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/ms-powerpoint, application/vnd.ms-powerpoint, application/vnd.openxmlformats-officedocument.presentationml.presentation, application/msword, application/vnd.msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    fp.setPreference("browser.helperApps.neverAsk.openFile", "text/csv, application/ms-excel, application/vnd.ms-excel, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/ms-powerpoint, application/vnd.ms-powerpoint, application/vnd.openxmlformats-officedocument.presentationml.presentation, application/msword, application/vnd.msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document")

    driver = { new FirefoxDriver(fp) }
  }

  // See: http://code.google.com/p/selenium/wiki/ChromeDriver
  chrome {
    if (Platform.current.is(Platform.LINUX)) {
      System.setProperty("webdriver.chrome.driver","lib/chromedriver_linux64")
    } else if (Platform.current.is(Platform.MAC)) {
      System.setProperty("webdriver.chrome.driver","lib/chromedriver_mac")
    } else if (Platform.current.is(Platform.WINDOWS)) {
      System.setProperty("webdriver.chrome.driver","lib/chromedriver.exe")
    }
    ChromeOptions co = new ChromeOptions()
    co.addArguments("-no-default-browser-check");
    co.addArguments("--disable-touch-drag-drop");
    co.addArguments("--disable-touch-adjustment");
    co.addArguments("--disable-touch-editing");
    co.addArguments("--disable-touch-feedback");
    co.addArguments("--ash-disable-touch-exploration-mode ");
    co.addArguments("--touch-events=disabled");
    co.addArguments("--disabled");
    co.addArguments("--start-maximized")
    Map<String, Object> prefs = new HashMap<String, Object>()
    prefs.put("download.default_directory", downloadDir)
    prefs.put("download.prompt_for_download", false)
    co.setExperimentalOption("prefs", prefs)

    driver = { new ChromeDriver(co) }
  }

}

database {
  dev3 {
    pooled = false
    driverClassname = ""
    host = "dev3-shared.cjeyhncfcola.us-east-1.rds.amazonaws.com"
    username = ""
    password = ""
  }
  domovm {
    pooled = false
    driverClassname = "com.mysql.jdbc.Driver"
    host = "jdbc:mysql://domovm.local.domo.com:3306"
    username = "domo"
    password = "popchart"
  }
}

// Simple array of feature switch names
// To disable a feature switch prepend an '!' to the name
featureSwitches = [
    //'advanced-card-builder'
]
