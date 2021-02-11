package wmdc.crm.firebase;

import org.json.JSONObject;
import wmdc.crm.util.Util;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by wmdcprog on 4/10/2019.
 */
@WebServlet("/getdevicetokens")
public class GetDeviceTokens extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement prepStmt = null;

        JSONObject obj;
        ArrayList<JSONObject> arrayList = new ArrayList<>();

        try {
            Util.databaseForName(getServletContext());
            conn = Util.getConnection(getServletContext());

            prepStmt = conn.prepareStatement("SELECT device_token, device_name FROM device_tokens");
            resultSet = prepStmt.executeQuery();

            while (resultSet.next()) {
                obj = new JSONObject();
                obj.put("devicetoken", resultSet.getString("device_token"));
                obj.put("devicename", resultSet.getString("device_name"));

                arrayList.add(obj);
            }

            obj = new JSONObject();
            obj.put("devicesStore", arrayList);
            obj.put("success", true);

            out.println(obj);

        } catch (ClassNotFoundException | SQLException sqe) {

            Util.displayStackTraceArray(sqe.getStackTrace(), Util.FIREBASE_PACKAGE, "DB", sqe.toString());

        } catch (Exception e) {
            Util.displayStackTraceArray(e.getStackTrace(), Util.FIREBASE_PACKAGE, "Exc", e.toString());

        } finally {
            Util.closeDBResource(conn, prepStmt, resultSet);
            out.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}