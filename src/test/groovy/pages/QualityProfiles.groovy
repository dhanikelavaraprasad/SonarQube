package pages

import geb.Page

/**
 * Created by jayesh.kariya on 14/07/15.
 */
class QualityProfiles extends Page{
  static url = '/profile'
  static at = {title == "Quality Profiles"}
  static content = {
   domoWebLink {$("a", text:"Wetl")}
    qualityProflieLink {$("a", text:"Quality Profiles")}
    //domoWebLink {$("a", text:"DOMOWeb")}
//    domoWebLink {$("a", text:"Test")}
  }

}
