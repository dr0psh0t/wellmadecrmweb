package wmdc.crm;

import org.json.JSONObject;
import wmdc.crm.util.Util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@WebServlet("/processqr")
public class ProcessQr extends HttpServlet {

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

        String qrcode = request.getParameter("qrcode");
        String jonum = request.getParameter("jonum");
        String datetime = request.getParameter("datetime");
        String deviceToken = request.getParameter("token");
        String hex = request.getParameter("hex");
        String secretKey = request.getParameter("secretKey");

        if (qrcode == null || jonum == null || deviceToken == null || datetime == null || hex == null) {
            Util.printJsonException(json, "Cannot process query. Missing data.", out);
            return;
        }

        if (qrcode.isEmpty() || jonum.isEmpty() || deviceToken.isEmpty() || datetime.isEmpty() || hex.isEmpty()) {
            Util.printJsonException(json, "Cannot process query. Missing data.", out);
            return;
        }

        if (jonum.replaceAll("[A-Za-z0-9\\s+]", "").length() > 0) {
            Util.printJsonException(json, "Jo number should be numbers and letters.", out);
            return;
        }

        if (isHashAuthentic(secretKey, jonum+qrcode+datetime, hex)) {
            HashMap<String, String> params = new HashMap<>();

            params.put("jonum", jonum);
            params.put("qrcode", qrcode);
            params.put("timestamp", datetime);

            try {
                String sessionId = request.getSession(false).getId();
                String url = Util.getPropertyValue("jourl", getServletContext());
                String httpResponse = Util.getResponse(sessionId, url+"GetJoWorkStatus", params);

                JSONObject map = new JSONObject(httpResponse);

                Util.sendNotif(deviceToken, "Wellmade", "Notification for "+map.getString("joNum"), out,
                        getServletContext(), map);

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

    private boolean isHashAuthentic(String key, String msg, String hex) {
        byte[] hmacSha256 = hmac256(key.getBytes(StandardCharsets.UTF_8), msg.getBytes(StandardCharsets.UTF_8));

        return hex.equals(String.format("%032x", new BigInteger(1, hmacSha256)));
    }

    private byte[] hmac256(byte[] secretKey, byte[] message) {
        byte[] hmac256;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec sks = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(sks);
            hmac256 = mac.doFinal(message);
            return hmac256;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate hash");
        }
    }
}