import com.google.gson.JsonObject

import java.sql.Connection
import java.sql.Date
import java.sql.DriverManager
import java.sql.PreparedStatement;
/**
 * Created by jayesh.kariya on 08/01/16.
 */



public class DataToMySql
{
    private static final String DB_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DB_CONNECTION = "jdbc:mysql://localhost:3306/sonarqubereport";
    private static final String DB_USER = "sonar";
    private static final String DB_PASSWORD = "sonar";

    public writeToDataBase(rulesResultList, String projectId, String isCybageUser, String userEmail, String userName, String sprintName) {

        String sqlQuery = "insert into sonar_qube_report values (?,?,?,?,?,?,?,?,?,?,?,?)";
        Connection connection = null;
        try {
            Class.forName(DB_DRIVER).newInstance();
            connection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false);
            PreparedStatement pstmt = connection.prepareStatement(sqlQuery);
            Iterator it = rulesResultList.entrySet().iterator()
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next()
                pstmt.setString(1, projectId);
                pstmt.setString(2, isCybageUser);
                pstmt.setString(3, userEmail);
                pstmt.setString(4, userName);
                pstmt.setString(5, new java.util.Date().format('yyyy-MM-dd'));
                pstmt.setString(6, sprintName);
                pstmt.setString(7, pair.getKey().get("customRuleName").getAsString());
                pstmt.setString(8, pair.getKey().get("key").getAsString());
                pstmt.setString(9, pair.getKey().get("name").getAsString());
                pstmt.setString(10, pair.getKey().get("severity").getAsString());
                pstmt.setString(11, pair.getKey().get("ruleType").getAsString());
                pstmt.setInt(12, pair.getValue().size());
                pstmt.addBatch();
            }
            System.out.println("Commit the batch");
            def result = pstmt.executeBatch();
            System.out.println("Number of rows inserted: " + result.length);
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            connection.rollBack();
        } finally {
            if (pstmt != null)
                pstmt.close();
            if (connection != null)
                connection.close();
        }
    }
}
