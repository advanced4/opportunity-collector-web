package org.jf.common.utilities;

import org.jf.common.constants.OppsConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.CharacterIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static final String RELEASE_DATE = "2000/01/01";
    private static final long MIN_TIMESTAMP;

    static {
        try {
            MIN_TIMESTAMP = new SimpleDateFormat("yyyy/MM/dd").parse(RELEASE_DATE).getTime();
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }

    // after the software was release and not in the future.
    public static boolean validTimestamp(long ts) {
        return ts >= MIN_TIMESTAMP && ts <= System.currentTimeMillis();
    }

    public static String getIpFromString(String input){
        String IPADDRESS_PATTERN =
                "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        } else{
            return "0.0.0.0";
        }
    }

    public static String sha512(String input) {
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++){
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedPassword;
    }

    public static <T> List<T> listIntersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();

        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }


    /*
     * Overriding default InetAddress.isReachable() method to add 2 more arguments port and timeout value
     *
     * Address: www.google.com
     * port: 80 or 443
     * timeout: 2000 (in milliseconds)
     */
    public static boolean addressReachable(String address, int port, int timeout) {
        try {

            try (Socket crunchifySocket = new Socket()) {
                // Connects this socket to the server with a specified timeout value.
                crunchifySocket.connect(new InetSocketAddress(address, port), timeout);
            }
            // Return true if connection successful
            return true;
        } catch (IOException exception) {
            // Return false if connection fails
            return false;
        }
    }

    public static void printBytes(byte[] input){
        for (byte b : input) {
            String st = String.format("%02X", b);
            System.out.print(st);
        }
    }

    public static AbstractMap.SimpleEntry<Integer, String> httpGetRequest(String endpoint) throws IOException {
        if (endpoint.startsWith("https:")) {
            return httpsGetRequest(endpoint, null, null, false);
        } else {
            return httpGetRequest(endpoint, null, null, false);
        }
    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    public static AbstractMap.SimpleEntry<Integer, String> httpPostRequest(String endpoint, ArrayList<AbstractMap.SimpleEntry<String, String>> params, String user, String password) throws IOException {
        StringBuilder urlParameters = new StringBuilder();

        //"param1=a&param2=b&param3=c";
        for (AbstractMap.SimpleEntry<String, String> kv : params) {
            if (urlParameters.length() == 0) {
                urlParameters = new StringBuilder(kv.getKey() + "=" + kv.getValue());
            } else {
                urlParameters.append("&").append(kv.getKey()).append("=").append(kv.getValue());
            }
        }

        byte[] postData = urlParameters.toString().getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        if (!Utils.isAnyNullOrEmpty(user, password)) {
            String auth = user + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);
            conn.setRequestProperty("Authorization", authHeaderValue);
        }
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }

        int responseCode = conn.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append("\n");
        }
        in.close();

        // print result
        return new AbstractMap.SimpleEntry<>(responseCode, response.toString());
    }

    public static AbstractMap.SimpleEntry<Integer, String> httpsGetRequest(String endpoint, String user, String password, boolean acceptjson) throws IOException {
        if (endpoint.startsWith("http:")) {
            return httpGetRequest(endpoint, user, password, acceptjson);
        }
        URL obj = new URL(endpoint);

        HttpsURLConnection con;
        con = (HttpsURLConnection) obj.openConnection();
        con.setConnectTimeout(5000);

        con.setRequestMethod("GET");
        if(!Utils.isAnyNullOrEmpty(user, password)) {
            String auth = user + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);
            con.setRequestProperty("Authorization", authHeaderValue);
        }

        if(acceptjson)
            con.setRequestProperty("Accept", "application/json");

        int responseCode = con.getResponseCode();

        if(responseCode == 404){
            return new AbstractMap.SimpleEntry<>(responseCode, "No result");
        }else if(responseCode != 200){
            return new AbstractMap.SimpleEntry<>(responseCode, "An error occurred. ");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));

        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append("\n");
        }
        in.close();

        // print result
        return new AbstractMap.SimpleEntry<>(responseCode, response.toString());
    }

    public static AbstractMap.SimpleEntry<Integer, String> httpGetRequest(String endpoint, String user, String password, boolean acceptjson) throws IOException {
        if(endpoint.startsWith("https:")){
            return httpsGetRequest(endpoint, user, password, acceptjson);
        }

        URL obj = new URL(endpoint);

        HttpURLConnection con;
        con = (HttpURLConnection) obj.openConnection();
        con.setConnectTimeout(5000);

        con.setRequestMethod("GET");
        if(!Utils.isAnyNullOrEmpty(user, password)) {
            String auth = user + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);
            con.setRequestProperty("Authorization", authHeaderValue);
        }

        if(acceptjson)
            con.setRequestProperty("Accept", "application/json");


        int responseCode = con.getResponseCode();
        BufferedReader in;

        //https://stackoverflow.com/questions/613307/read-error-response-body-in-java
        if(responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
            in = new BufferedReader(new InputStreamReader(
                    con.getInputStream(), StandardCharsets.UTF_8));
        }else{
            in = new BufferedReader(new InputStreamReader(
                    con.getErrorStream(), StandardCharsets.UTF_8));
        }

        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append("\n");
        }
        in.close();

        // print result
        return new AbstractMap.SimpleEntry<>(responseCode, response.toString());
    }


    public static AbstractMap.SimpleEntry<Integer, String> httpRequestWithJsonBody(String endpoint, OppsConstants.HttpMethods type, JsonElement body) throws IOException {
        //Change the URL with any other publicly accessible POST resource, which accepts JSON request body
        URL url = new URL (endpoint);

        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod(type.toString());

        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");

        con.setDoOutput(true);

        try(OutputStream os = con.getOutputStream()){
            byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = con.getResponseCode();
        StringBuilder response = new StringBuilder();

        try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))){
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        return new AbstractMap.SimpleEntry<>(code, response.toString());
    }
    

    public static String getFileExtension(String filename){
        String extension = "";

        int i = filename.lastIndexOf('.');
        if (i > 0) {
            extension = filename.substring(i+1);
        }
        return extension;
    }

    // checks token for anything wrong with it
    public static Jws<Claims> checkToken(String rawToken, PublicKey jwt_public_key) {
        if (!isAnyNullOrEmpty(rawToken)) {
            try {
                return Jwts.parser()
                        .setSigningKey(jwt_public_key)
                        .parseClaimsJws(rawToken);
            } catch (ExpiredJwtException exception) {
                log.warn("Request to parse expired JWT : {} failed : {}", rawToken, exception.getMessage());
            } catch (UnsupportedJwtException exception) {
                log.warn("Request to parse unsupported JWT : {} failed : {}", rawToken, exception.getMessage());
            } catch (MalformedJwtException exception) {
                log.warn("Request to parse invalid JWT : {} failed : {}", rawToken, exception.getMessage());
            } catch (SecurityException exception) {
                log.warn("Some security exception occurred : {} failed : {}", rawToken, exception.getMessage());
            } catch (IllegalArgumentException exception) {
                log.warn("Request to parse empty or null JWT : {} failed : {}", rawToken, exception.getMessage());
            }
        }
        return null;
    }

    // an optional helper function that turns the above into an explicit boolean check on the token
    public static boolean checkToken_b(Jws<Claims> vvt) {
        // if the token was null, then there was some problem with it (or it was null to begin with)
        return vvt != null;
    }

    public static boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public static boolean isAnyNullOrEmpty(Object ...objects){
        for(Object o : objects) {
            if (o == null) {
                return true;
            }
            if (o.getClass().equals(String.class)) {
                if (o.getClass().isArray()) {
                    if(((String) o).length() == 0){
                        return true;
                    }
                } else {
                    if(((String) o).isEmpty()){
                        return true;
                    }
                }
            }

            if (o.getClass().equals(JsonArray.class)) {
                if(((JsonArray) o).size() == 0){
                    return true;
                }
            }
        }
        return false;
    }

    public static String bytesToBase64String(byte[] input){
        return Base64.getEncoder().encodeToString(input);
    }

    public static byte[] base64StringToBytes(String base64){
        return Base64.getDecoder().decode(base64);
    }

    public static KeyPair generateKeyPair(SignatureAlgorithm sa){
        return Keys.keyPairFor(sa);
    }

    public static PrivateKey convertArrayToPriKey(byte[] encoded) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance(OppsConstants.Jwt.KEY_FACTORY_INSTANCE_TYPE);
        return keyFactory.generatePrivate(keySpec);
    }

    public static PublicKey convertArrayToPubKey(byte[] encoded) throws InvalidKeySpecException, NoSuchAlgorithmException {
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance(OppsConstants.Jwt.KEY_FACTORY_INSTANCE_TYPE);
        return keyFactory.generatePublic(pubKeySpec);
    }

    // Used for the darknet JWT currently
    public static long msUntilMidnight(){
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return (c.getTimeInMillis()-System.currentTimeMillis());
    }

    public static long getCurrentTimeSeconds(){
        return System.currentTimeMillis()/1000;
    }

    // this prefers the auth header over a cookie, arbitrarily
    public static String chooseTokenFromHeaderOrCookie(String authHeader, String cookie){
        if(!isAnyNullOrEmpty(authHeader)){
            if(authHeader.toLowerCase().startsWith("bearer")){
                authHeader = authHeader.replace("Bearer", "").trim();
            }
            return authHeader;
        }
        return cookie;
    }

    // good idea for future use. can filter what domains we respond to
    // for now, allow anything
    public static String filterOriginCORS(String origin){
        return origin == null ? "*" : origin;
    }

    // yay reflection
    // this checks every field of an object for null values. used for checking config
    public static boolean anyObjectAnyNullField(ArrayList<Object> objs) {
        try {
            for (Object obj : objs) {
                for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()) {
                    Method meth = propertyDescriptor.getReadMethod();
                    if (meth.getName().equals("getClass"))
                        continue;

                    Object res = meth.invoke(obj);
                    if (res == null || res.equals("")) {
                        log.warn("Class: " + obj.getClass().getSimpleName() + " has a method that returns empty: " + meth.getName());
                        return true;
                    }
                }
            }
        } catch(IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            log.error("Error checking config field: " + e.getMessage());
        }
        return false;
    }

    public static ArrayList<String> jsonArrayToArrayList(JsonArray ja){
        ArrayList<String> res = new ArrayList<>();

        if(ja!=null) {
            for (JsonElement je : ja) {
                res.add(je.getAsString());
            }
        }
        return res;
    }

    public static ArrayList<String> jsonArrayToArrayListLower(JsonArray ja){
        ArrayList<String> res = new ArrayList<>();

        if(ja!=null) {
            for (JsonElement je : ja) {
                res.add(je.getAsString().toLowerCase());
            }
        }
        return res;
    }

    public static JsonArray arrayListToJsonArray(ArrayList<String> al){
        JsonArray ja = new JsonArray();
        for(String s: al){
            ja.add(s);
        }
        return ja;
    }

}