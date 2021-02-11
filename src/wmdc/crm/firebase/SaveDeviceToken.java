package wmdc.crm.firebase;

import org.json.JSONObject;
import wmdc.crm.util.Util;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/savedevicetoken")
public class SaveDeviceToken extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        JSONObject json = new JSONObject();
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();

        String newToken = request.getParameter("newToken");
        String deviceInfo = request.getParameter("deviceInfo");

        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;

        try {
            //Util.databaseForName(getServletContext());
            Class.forName("org.mariadb.jdbc.Driver");
            conn = Util.getConnection(getServletContext());

            prepStmt = conn.prepareStatement("SELECT COUNT (*) AS tokenCount FROM device_tokens " +
                    "WHERE device_name = ? AND device_token = ?");
            prepStmt.setString(1, deviceInfo);
            prepStmt.setString(2, newToken);
            resultSet = prepStmt.executeQuery();

            int tokenCount = 0;
            if (resultSet.next()) {
                tokenCount = resultSet.getInt("tokenCount");
            }

            if (tokenCount < 1) {
                prepStmt = conn.prepareStatement(
                        "INSERT INTO device_tokens (device_token, device_name) VALUES (?, ?)");
                prepStmt.setString(1, newToken);
                prepStmt.setString(2, deviceInfo);
                prepStmt.execute();

                Util.printJsonException(json, "You can now start", out);
            } else {
                Util.printJsonException(json, "Registered", out);
            }

            session.invalidate();
            session = request.getSession();
            session.setAttribute("token", newToken);

        } catch (ClassNotFoundException | SQLException sqe) {
            Util.displayStackTraceArray(sqe.getStackTrace(), Util.FIREBASE_PACKAGE, "DB", sqe.toString());
            Util.printJsonException(json, "An error occured in registering device.", out);

        } catch (Exception e) {
            Util.displayStackTraceArray(e.getStackTrace(), Util.FIREBASE_PACKAGE, "Exc", e.toString());
            Util.printJsonException(json, "Cannot register at this time.", out);

        } finally {
            Util.closeDBResource(conn, prepStmt, resultSet);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Util.illegalRequest(response);
    }
}
