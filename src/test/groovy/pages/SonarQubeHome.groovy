import geb.Page

/**
 * Created by jayesh.kariya on 14/07/15.
 */
class SonarQubeHome extends Page{
  static url = '/'
  static at = {title == "SonarQube"}
  static content = {
   domoWebLink {$("a", text:"Wetl")}
    qualityProflieLink {$("a", text:"Quality Profiles")}
    //domoWebLink {$("a", text:"DOMOWeb")}
//    domoWebLink {$("a", text:"Test")}
  }

}
