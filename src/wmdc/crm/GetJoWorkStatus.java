package wmdc.crm;

import org.json.JSONException;
import org.json.JSONObject;
import wmdc.crm.util.Util;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

@WebServlet("/getjoworkstatus")
public class GetJoWorkStatus extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject json = new JSONObject();

        if (!Util.isOnline(request)) {
            Util.printJsonException(json, "Denied.", out);
            return;
        }

        String qrcode = request.getParameter("qr");
        String jonum = request.getParameter("jo");
        String datetime = request.getParameter("t");
        String deviceToken = request.getParameter("fbuk");
        String hex = request.getParameter("h");
        String secretKey = request.getParameter("secretKey");
        String uk = request.getParameter("uk");
        String pid = request.getParameter("pid");

        if (qrcode == null || jonum == null || deviceToken == null || datetime == null || hex == null || uk == null
                || pid == null) {
            Util.printJsonException(json, "Cannot process query. Missing data.", out);
            return;
        }

        if (qrcode.isEmpty() || jonum.isEmpty() || deviceToken.isEmpty() || datetime.isEmpty() || hex.isEmpty()
                || uk.isEmpty() || pid.isEmpty()) {
            Util.printJsonException(json, "Cannot process query. Missing data.", out);
            return;
        }

        if (jonum.replaceAll("[A-Za-z0-9\\s+]", "").length() > 0) {
            Util.printJsonException(json, "Jo number should be numbers and letters.", out);
            return;
        }

        //System.out.println("sha512hex "+hex);

        if (Util.isHashAuthentic(secretKey, jonum+qrcode+datetime+pid, hex)) {
            HashMap<String, String> params = new HashMap<>();

            params.put("jn", jonum);
            params.put("q", qrcode);
            params.put("t", datetime);
            params.put("h", hex);
            params.put("pid", pid);

            try {
                String sessionId = request.getSession(false).getId();
                String url = Util.getPropertyValue("jourl", getServletContext());
                String httpResponse = Util.getResponse(sessionId, url+"GetJoWorkStatus", params);

                try {
                    JSONObject map = new JSONObject(httpResponse);

                    if (!map.has("success")) {
                        Util.sendNotif(deviceToken, "Wellmade", "Notification for you", out, getServletContext(), map);
                    }

                    out.println(map);
                } catch (JSONException je) {
                    Util.printJsonException(new JSONObject(), "Parse error", out);
                }

            } catch (Exception e) {
                Util.printJsonException(json, "An error has occurred.", out);
                Util.displayStackTraceArray(e.getStackTrace(), "wmdc.crm", "Exception", e.toString());
            }

        } else {
            Util.printJsonException(json, "Cannot Authenticate", out);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}