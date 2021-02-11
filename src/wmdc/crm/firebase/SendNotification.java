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

/**
 * Created by wmdcprog on 4/10/2019.
 */
@WebServlet("/sendnotification")
public class SendNotification extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        response.setContentType("application/json");
        JSONObject resJson = new JSONObject();
        PrintWriter out = response.getWriter();

        String title;
        String body;
        String deviceToken;
        String notifImage;

        try {
            title = request.getParameter("title");
            body = request.getParameter("body");
            deviceToken = request.getParameter("deviceToken");
            notifImage = request.getParameter("notifImage");

            JSONObject map = new JSONObject();
            map.put("success", true);
            map.put("reason", "test");

            Util.sendNotif(deviceToken, title, body, out, getServletContext(), map);

        } catch (Exception e) {
            Util.printJsonException(resJson, e.toString(), out);
            Util.displayStackTraceArray(e.getStackTrace(), Util.FIREBASE_PACKAGE,
                    "Exc", e.toString());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}