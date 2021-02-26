package wmdc.crm.util;

import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wmdcprog on 3/9/2019.
 */
public class Util {

    public static final String MCRM_PACKAGE = "wmdc.crm";
    public static final String FIREBASE_PACKAGE = "wmdc.crm.firebase";

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public static boolean validEmail(String email) {
        return email.matches(EMAIL_PATTERN);
    }

    public static boolean isThereSpecialChars(String text)
    {
        Pattern p = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find();
    }

    public static void printSuccessJson(JSONObject responseJson, String message, PrintWriter out) {
        responseJson.put("success", true);
        responseJson.put("reason", message);
        out.println(responseJson);
    }

    public static void printJsonException(JSONObject responseJson, String message, PrintWriter out) {
        responseJson.put("success", false);
        responseJson.put("reason", message);

        out.println(responseJson);
    }

    public static void databaseForName(ServletContext servletContext)
            throws ClassNotFoundException, IOException {
        Class.forName(getPropertyValue("forname", servletContext));
    }

    public static void databaseForName(ServletContext servletContext, String key)
            throws ClassNotFoundException, IOException {
        Class.forName(getPropertyValue(key, servletContext));
    }

    public static Connection getConnection(ServletContext servletContext) throws SQLException, IOException {
        return DriverManager.getConnection(getPropertyValue("url", servletContext),
                getPropertyValue("user", servletContext), getPropertyValue("password", servletContext));
    }

    public static Connection getConnectionByKey(ServletContext servletContext, String key) throws SQLException, IOException {
        return DriverManager.getConnection(getPropertyValue(key, servletContext));
    }

    public static String getPropertyValue(String key, ServletContext servletContext) throws IOException {
        Properties prop = new Properties();
        prop.load(servletContext.getResourceAsStream("/WEB-INF/wellmadecrm.properties"));
        return prop.getProperty(key);
    }

    public static String getFirebaseServerKey(ServletContext ctx) throws IOException {
        return getPropertyValue("serverkey", ctx);
    }

    //  get url crm db
    public static String getUrlFromConfig(ServletContext servletContext) throws SQLException, IOException {

        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;

        ArrayList<String> result = new ArrayList<>(3);

        try {
            databaseForName(servletContext);

            conn = getConnection(servletContext);
            prepStmt = conn.prepareStatement("SELECT value FROM configs");
            resultSet = prepStmt.executeQuery();

            while (resultSet.next()) {
                result.add(resultSet.getString("value"));
            }

            return "jdbc:mysql://" + result.get(1) + ":" + result.get(2) + "/" + result.get(0);
        } catch (ClassNotFoundException | SQLException sqe) {
            sqe.printStackTrace();
            return null;
        } finally {
            closeDBResource(conn, prepStmt, resultSet);
        }
    }

    public static void illegalRequest(HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        JSONObject responseJson = new JSONObject();

        responseJson.put("success", false);
        responseJson.put("reason", "Illegal Request.");

        response.getWriter().println(responseJson);
    }

    public static boolean isOnline(HttpServletRequest request) throws ServletException, IOException {
        try {
            if (request.getSession().getAttribute("token") == null) {
                return false;
            }
        } catch (NullPointerException npe) {
            System.err.println(npe.toString());
            return false;
        }
        return true;
    }

    public static void closeDBResource(Connection conn, PreparedStatement prepStmt, ResultSet resultSet) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println(e.toString());
        }

        try {
            if (prepStmt != null) {
                prepStmt.close();
            }
        } catch (SQLException e) {
            System.err.println(e.toString());
        }

        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            System.err.println(e.toString());
        }
    }

    public static void displayStackTraceArray(
            StackTraceElement[] stackTraceElements, String packageRoot,
            String exceptionName, String toString) {

        print(exceptionName, toString);

        for (StackTraceElement elem : stackTraceElements)
        {
            if (elem.toString().contains(packageRoot)) {
                print("source", elem.toString());
            }
        }
    }

    public static String getRelevantTrace(StackTraceElement[] traceElements, String packageRoot)
    {
        StringBuilder stringBuilder = new StringBuilder();

        for (StackTraceElement elem : traceElements)
        {
            if (elem.toString().contains(packageRoot)) {
                stringBuilder.append(packageRoot+"\n");
            }
        }

        return stringBuilder.toString();
    }

    public static void print(String tag, Object object) {
        System.err.println("P/"+tag+": "+object.toString());
    }

    public static void sendNotif(String token, String title, String body, PrintWriter out, ServletContext ctx,
                                 JSONObject map) {

        String notifImage = "https://plugins.jetbrains.com/files/13666/100004/icon/pluginIcon.png";

        HttpURLConnection conn = null;
        OutputStreamWriter wr;
        InputStream is = null;

        try {
            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            conn = (HttpURLConnection) url.openConnection();

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "key="+Util.getFirebaseServerKey(ctx));
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject json = new JSONObject();
            json.put("to", token);
            json.put("priority", "high");

            JSONObject info = new JSONObject();
            info.put("title", title);
            info.put("body", body);
            info.put("image", notifImage);
            info.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

            //JSONObject data = new JSONObject();
            /*data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");
            data.put("id", "1");
            data.put("sound", "default");
            data.put("status", "done");
            data.put("screen", "screenA");
            data.put("title", title);
            data.put("body", body);
            json.put("notification", info);*/
            //data.put("title", title);
            //data.put("body", body);

            json.put("notification", info);
            json.put("data", map);

            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(json.toString());
            wr.flush();
            is = conn.getInputStream();

        } catch (Exception e) {
            Util.printJsonException(new JSONObject(), e.toString(), out);
            Util.displayStackTraceArray(e.getStackTrace(), "wmdc.crm.util", "Exception", e.toString());

        } finally {
            if (conn != null) {
                conn.disconnect();
            }

            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                System.err.println(e.toString());
            }
        }
    }

    public static String getResponse(String sessionId, String address, HashMap<String, String> params) {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(address);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            conn.setRequestProperty("Cookie", "JSESSIONID="+sessionId);
            conn.setRequestProperty("Host", "localhost:8080");
            conn.setRequestProperty("Referer", "wellmadecrm-app");
            conn.setRequestProperty("X-Requested-Width", "XMLHttpRequest");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            StringBuilder sBuilder = new StringBuilder();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (sBuilder.length() > 0) {
                    sBuilder.append("&"+entry.getKey()+"="+entry.getValue());
                } else {
                    sBuilder.append(entry.getKey()+"="+entry.getValue());
                }
                //System.out.println(entry.getKey()+"\t"+entry.getValue());
            }

            writer.write(sBuilder.toString());
            writer.flush();
            writer.close();
            os.close();
            conn.connect();

            System.out.println("url "+url.toString());
            System.out.println("responseCode "+conn.getResponseCode());

            //if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream input = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                input.close();
                reader.close();

                if (sb.toString().isEmpty()) {
                    return "{\"success\": false, \"reason\": \"No response from server.\"}";
                } else {
                    System.out.println("response "+sb.toString());
                    return sb.toString();
                }
            /*} else {
                return "{\"success\": false, \"reason\": \"Request Failed. Try again.\"}";
            }*/
        } catch (MalformedURLException | ConnectException | SocketTimeoutException e) {
            Util.displayStackTraceArray(e.getStackTrace(), "wmdc.crm.util",
                    "NetworkException", e.toString());
            if (e instanceof MalformedURLException) {
                return "{\"success\": false, \"reason\": \"Malformed URL.\"}";
            } else if (e instanceof ConnectException) {
                return "{\"success\": false, \"reason\": \"Cannot connect to server. " +
                        "Check wifi or mobile data and check if server is available.\"}";
            } else {
                return "{\"success\": false, \"reason\": \"Connection timed out. " +
                        "The server is taking too long to reply.\"}";
            }
        } catch (Exception e) {
            Util.displayStackTraceArray(e.getStackTrace(), "wmdc.crm.util",
                    "Exception", e.toString());
            return "{\"success\": false, \"reason\": \""+e.getMessage()+"\"}";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static boolean isHashAuthentic(String key, String msg, String hex) throws UnsupportedEncodingException {

        byte[] hmacSha512 = hmac512(key.getBytes(StandardCharsets.UTF_8), msg.getBytes(StandardCharsets.UTF_8));
        String hex2 = String.format("%032x", new BigInteger(1, hmacSha512));

        return hex.equals(hex2);
    }

    private static byte[] hmac512(byte[] secretKey, byte[] message) {
        byte[] hmac512;

        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec sks = new SecretKeySpec(secretKey, "HmacSHA512");
            mac.init(sks);
            hmac512 = mac.doFinal(message);
            return hmac512;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate hash");
        }
    }
}