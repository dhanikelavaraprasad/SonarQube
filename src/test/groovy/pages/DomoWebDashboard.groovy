import geb.Page


/**
 * Created by jayeshk.kariya on 14/07/15.
 */

@Mixin(ExcelHelper)
class DomoWebDashboard extends Page {

  static url = "/dashboard/index/1"
  static at = {overviewLink}
  static content = {
    overviewLink {$("a", text:"Overview")}
//    overviewLink {$("a", text:"Main Dashboard")}
    issuesLink {$("a", text:"Issues")}
    linesOfCode { $('#m_ncloc')}
    files { $('#m_files')}
    functions { $('#m_functions')}
    directories { $('#m_directories')}
    duplications { $('#m_duplicated_lines_density')}
    complexity { $('#m_complexity')}
    blocker { $('#m_blocker_violations')}
    critical { $('#m_critical_violations')}
    major { $('#m_major_violations')}
    minor { $('#m_minor_violations')}
    info { $('#m_info_violations')}
    scaleRating { $('#m_sqale_rating')}
    techinalDebtRatio { $('#m_sqale_debt_ratio')}

  }

  def getOverAllReportData()
  {
      def line = linesOfCode.text()
      def file = files.text()
      def function = functions.text()
      def directory = directories.text()
      def duplication = duplications.text()
      def complex = complexity.text()
      def block = blocker.text()
      def critic = critical.text()
      def major = major.text()
      def minor = minor.text()
      def info = info.text()
      def scaleRating = scaleRating.text()
      def techinalDebtRatio = techinalDebtRatio.text()

      return [line, file, function, directory, duplication, complex,
              block, critic, major, minor, info, scaleRating, techinalDebtRatio ]
  }

}
