package wmdc.crm;

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

@WebServlet("/authnewmcrm")
public class AuthNewMcrm extends HttpServlet {

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

        String fbuk = request.getParameter("fbuk");
        String uk = request.getParameter("uk");
        String jo = request.getParameter("jo");
        String qr = request.getParameter("qr");
        String h = request.getParameter("h");
        String t = request.getParameter("t");
        String secretKey = request.getParameter("secretKey");

        if (fbuk == null || uk == null || jo == null || qr == null || h == null || t == null || secretKey == null) {
            Util.printJsonException(json, "Cannot process query. Missing data.", out);
            return;
        }

        if (fbuk.isEmpty() || uk.isEmpty() || jo.isEmpty() || qr.isEmpty() || h.isEmpty() || t.isEmpty() || secretKey.isEmpty()) {
            Util.printJsonException(json, "Cannot process query. Missing data.", out);
            return;
        }

        //System.out.println("sha512hex "+h);

        if (Util.isHashAuthentic(secretKey, jo+qr+t, h)) {
            HashMap<String, String> params = new HashMap<>();

            params.put("fbuk", fbuk);
            params.put("uk", uk);
            params.put("jo", jo);
            params.put("qr", qr);
            params.put("h", h);
            params.put("t", t);

            try {
                String sessionId = request.getSession(false).getId();
                String url = Util.getPropertyValue("jourl", getServletContext());
                String httpResponse = Util.getResponse(sessionId, url+"AuthNewMCRM", params);

                JSONObject map = new JSONObject(httpResponse);
                //JSONObject map = new JSONObject("{\"data\":[{\"uk\":\"uVC3-Qd0spM_wcUnwFM5ZAbaIBZTeQIu1Rp2VzUzPgivs\",\"ci\":0,\"ak\":\"6G5CHhvNK\\/6wIj7lPw+HbvFdJswwVIoeZ10NhpukPnk=\"}],\"success\":true}");

                out.println(map);

            } catch (Exception e) {
                Util.printJsonException(json, "An error has occurred.", out);
                Util.displayStackTraceArray(e.getStackTrace(), "wmdc.crm.util", "Exception", e.toString());
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