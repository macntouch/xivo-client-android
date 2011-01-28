package com.proformatique.android.xivoclient.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.XivoNotification;
import com.proformatique.android.xivoclient.tools.Constants;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class XivoConnectionService extends Service {
    
    private static final String TAG = "XiVO connection service";
    
    private SharedPreferences prefs = null;
    private Socket networkConnection = null;
    private BufferedReader inputBuffer = null;
    private long bytesReceived = 0L;
    
    // Informations that is relevant to a specific connection
    private boolean authenticationComplete = false;
    private String sessionId = null;
    private String xivoId = null;
    private String astId = null;
    private HashMap<String, String> capaPresenceState = null;
    private XivoNotification xivoNotif = null;
    private List<HashMap<String, String>> statusList = null;
    
    /**
     * Implementation of the methods between the service and the activities
     */
    private final IXivoConnectionService.Stub binder = new IXivoConnectionService.Stub() {
        
        @Override
        public int connect() throws RemoteException {
            return connectToServer();
        }
        
        @Override
        public int disconnect() throws RemoteException {
            return disconnectFromServer();
        }
        
        @Override
        public boolean isConnected() throws RemoteException {
            return (networkConnection != null && networkConnection.isConnected());
        }
        
        @Override
        public int authenticate() throws RemoteException {
            return loginCTI();
        }
        
        @Override
        public boolean isAuthenticated() throws RemoteException {
            return authenticationComplete;
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
    
    @Override
    public void onStart(Intent i, int startId) {
        super.onStart(i, startId);
        Log.d(TAG, "XiVO connection service started");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * Starts a network connection with the server
     * @return connection status
     */
    private int connectToServer() {
        int port = Integer.parseInt(prefs.getString("server_port", "5003"));
        String host = prefs.getString("server_adress", "");
        
        try {
            Log.d(TAG, "Connecting to " + host + " " + port);
            networkConnection = new Socket(host, port);
            bytesReceived = 0L;
            inputBuffer = new BufferedReader(
                    new InputStreamReader(networkConnection.getInputStream()));
            
            String firstLine;
            while ((firstLine = getLine()) != null) {
                if (firstLine.contains("XiVO CTI Server")) {
                    return Constants.CONNECTION_OK;
                }
            }
            return Constants.NOT_CTI_SERVER;
        } catch (UnknownHostException e) {
            Log.d(TAG, "Unknown host " + host);
            return Constants.BAD_HOST;
        } catch (IOException e) {
            return Constants.BAD_HOST;
        }
    }
    
    private int disconnectFromServer() {
        // TODO: Stop input loop
        if (networkConnection != null) {
            try {
                networkConnection.shutdownOutput();
                networkConnection.close();
                networkConnection = null;
                return Constants.OK;
            } catch (IOException e) {
                return Constants.NO_NETWORK_AVAILABLE;
            }
        }
        authenticationComplete = false;
        return Constants.OK;
    }
    
    /**
     * Perform authentication on the XiVO CTI server
     * @return error or success code
     */
    private int loginCTI() {
        String releaseOS = android.os.Build.VERSION.RELEASE;
        String login = getSharedPreferences("login_settings", 0).getString("login", "");
        int res;
        Log.d(TAG, "release OS: " + releaseOS);
        
        /**
         * Creating first Json login array
         */
        JSONObject jLogin = new JSONObject();
        try {
            jLogin.accumulate("class","login_id");
            jLogin.accumulate("company", prefs.getString("context", Constants.XIVO_CONTEXT));
            jLogin.accumulate("ident","android-"+releaseOS);
            jLogin.accumulate("userid",login);
            jLogin.accumulate("version",Constants.XIVO_LOGIN_VERSION);
            jLogin.accumulate("xivoversion",Constants.XIVO_VERSION);
        } catch (JSONException e) {
            return Constants.JSON_POPULATE_ERROR;
        }
        
        /**
         * First step : check that login is allowed on server
         */
        res = sendLoginCTI(jLogin);
        if (res != Constants.OK) return res;
        
        /**
         Second step : check that password is allowed on server
         */
        res = sendPasswordCTI();
        if (res != Constants.OK) return res;
        
        /**
         * Third step : send configuration options on server
         */
        return sendCapasCTI();
    }
    
    /**
     * Send capacity options to the CTI server
     * @return error or success code
     */
    private int sendCapasCTI() {
        JSONObject jsonCapas = new JSONObject();
        
        try {
            jsonCapas.accumulate("class", "login_capas");
            jsonCapas.accumulate("agentlogin", "now");
            jsonCapas.accumulate("capaid", "client");
            jsonCapas.accumulate("lastconnwins", "false");
            jsonCapas.accumulate("loginkind", "agent");
            jsonCapas.accumulate("phonenumber", "101");
            jsonCapas.accumulate("state", "");
        } catch (JSONException e) {
            return Constants.JSON_POPULATE_ERROR;
        }
        
        int res = sendLine(jsonCapas.toString());
        if (res != Constants.OK) return res;
        
        jsonCapas = readJsonObjectCTI();
        try {
            if (jsonCapas != null && jsonCapas.has("class") &&
                    jsonCapas.getString("class").equals(Constants.XIVO_LOGIN_CAPAS_OK)) {
                res = parseCapas(jsonCapas);
                if (res != Constants.OK) return res;
                authenticationComplete = true;
                return Constants.AUTHENTICATION_OK;
            } else {
                return parseLoginError(jsonCapas);
            }
        } catch (JSONException e) {
            return Constants.JSON_POPULATE_ERROR;
        }
    }
    
    /**
     * Parses configuration values received from the CTI server
     * @return error or success code
     */
    private int parseCapas(JSONObject jCapa) {
        resetState();
        try {
            xivoId = jCapa.getString("xivo_userid");
            astId = jCapa.getString("astid");
            
            JSONObject jCapaPresence = jCapa.getJSONObject("capapresence");
            JSONObject jCapaPresenceState = jCapaPresence.getJSONObject("state");
            JSONObject jCapaPresenceStateNames = jCapaPresence.getJSONObject("names");
            JSONObject jCapaPresenceStateAllowed = jCapaPresence.getJSONObject("allowed");
            
            capaPresenceState = new HashMap<String, String>();
            capaPresenceState.put("color", jCapaPresenceState.getString("color"));
            capaPresenceState.put("stateid", jCapaPresenceState.getString("stateid"));
            capaPresenceState.put("longname", jCapaPresenceState.getString("longname"));
            
            statusList = new ArrayList<HashMap<String, String>>();
            feedStatusList("available", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
            feedStatusList("berightback", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
            feedStatusList("away", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
            feedStatusList("donotdisturb", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
            feedStatusList("outtolunch", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
            
            xivoNotif = new XivoNotification(getApplicationContext());
            xivoNotif.createNotification();
        } catch (JSONException e) {
            return Constants.JSON_POPULATE_ERROR;
        }
        return Constants.OK;
    }
    
    private void feedStatusList(String status, JSONObject jNames, JSONObject jAllowed)
        throws JSONException{
        
        if (jAllowed.getBoolean(status)){
            HashMap<String, String> map = new HashMap<String, String>();
            
            JSONObject jCapaPresenceStatus = jNames.getJSONObject(status);
            map.put("stateid", jCapaPresenceStatus.getString("stateid"));
            map.put("color", jCapaPresenceStatus.getString("color"));
            map.put("longname", jCapaPresenceStatus.getString("longname"));
            
            statusList.add(map);
            
            Log.d(TAG, "StatusList: " + jCapaPresenceStatus.getString("stateid") + " " +
                    jCapaPresenceStatus.getString("longname"));
        }
    }
    
    /**
     * Reset information about a session
     */
    private void resetState() {
        xivoId = null;
        authenticationComplete = false;
        sessionId = null;
        astId = null;
        capaPresenceState = null;
        xivoNotif = null;
        statusList = null;
    }
    
    private int sendPasswordCTI() {
        byte[] sDigest = null;
        JSONObject jsonPasswordAuthent = new JSONObject();
        String password = getSharedPreferences("login_settings", 0).getString("password", "");
        int res;
        
        /**
         * Encrypt password for communication with algorithm SHA1
         */
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
            sDigest = sha1.digest((sessionId + ":" + password).getBytes());
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "Encryption algorithm not available");
            return Constants.ALGORITH_NOT_AVAILABLE;
        }
        
        try {
            jsonPasswordAuthent.accumulate("class", "login_pass");
            jsonPasswordAuthent.accumulate("hashedpassword", bytes2String(sDigest));
        } catch (JSONException e) {
            return Constants.JSON_POPULATE_ERROR;
        }
        res = sendLine(jsonPasswordAuthent.toString());
        if (res != Constants.OK) return res;
        
        JSONObject ctiAnswer = readJsonObjectCTI();
        try {
            if (ctiAnswer != null & ctiAnswer.has("class") &&
                    ctiAnswer.getString("class").equals(Constants.XIVO_PASSWORD_OK)) {
                return Constants.OK;
            } else {
                return parseLoginError(ctiAnswer);
            }
        } catch (JSONException e) {
            Log.d(TAG, "Unexpected answer from cti server");
            return Constants.JSON_POPULATE_ERROR;
        }
    }
    
    /**
     * Convents an array of byte to an hexadecimal string
     * @param bytes
     * @return
     */
    private static String bytes2String(byte[] bytes) {
        StringBuilder string = new StringBuilder();
        for (byte b: bytes) {
            String hexString = Integer.toHexString(0x00FF & b);
            string.append(hexString.length() == 1 ? "0" + hexString : hexString);
        }
        return string.toString();
    }
    
    /**
     * Sends login information to the cti server
     * @param jLogin
     * @return error or success code
     */
    private int sendLoginCTI(JSONObject jLogin) {
        JSONObject nextJsonObject;
        int res;
        if ((res = sendLine(jLogin.toString())) != Constants.OK) return res;
        if ((nextJsonObject = readJsonObjectCTI()) == null)
            return Constants.JSON_POPULATE_ERROR;
        try {
            if (nextJsonObject.has("class") && nextJsonObject.getString("class")
                    .equals(Constants.XIVO_LOGIN_OK)) {
                sessionId = nextJsonObject.getString("sessionid");
                return Constants.OK;
            } else {
                return parseLoginError(nextJsonObject);
            }
        } catch (JSONException e) {
            Log.d(TAG, "Unexpected answer from cti server");
            e.printStackTrace();
            return Constants.JSON_POPULATE_ERROR;
        }
    }
    
    /**
     * Retrieves the error when the CTI server doesnt return a LOGIN_OK
     * @return error code
     */
    private int parseLoginError(JSONObject jsonObject) {
        try {
            if (jsonObject.has("errorstring")) {
                String error = jsonObject.getString("errorstring");
                if (error.equals(Constants.XIVO_LOGIN_PASSWORD)
                        || error.equals(Constants.XIVO_LOGIN_UNKNOWN_USER)) {
                    return Constants.LOGIN_PASSWORD_ERROR;
                } else if (error.contains(Constants.XIVO_CTI_VERSION_NOT_SUPPORTED)) {
                    return Constants.CTI_SERVER_NOT_SUPPORTED;
                } else if (error.equals(Constants.XIVO_VERSION_NOT_COMPATIBLE)) {
                    return Constants.VERSION_MISMATCH;
                }
            } else if (jsonObject.has("class") && jsonObject.getString("class").equals("disconn")) {
                lostConnectionEvent();
                return Constants.FORCED_DISCONNECT;
            }
        } catch (JSONException e) {
            Log.d(TAG, "JSON exception while parsing error strin");
        }
        return Constants.LOGIN_KO;
    }
    
    /**
     * Perform a read action on the stream from CTI server
     * @return JSON object retrieved
     */
    public JSONObject readJsonObjectCTI() {
        JSONObject ReadLineObject;
        String responseLine;
        
        try {
            while ((responseLine = getLine()) != null) {
                try {
                    ReadLineObject = new JSONObject(responseLine);
                    Log.d(TAG, "Server: " + responseLine);
                    return ReadLineObject;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            if (networkConnection.isConnected() == false) {
                lostConnectionEvent();
            }
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Handles a connection lost
     */
    private void lostConnectionEvent() {
        resetState();
        disconnectFromServer();
        // TODO: Send an intent to warn activities about the connection lost
        // TODO: try to reconnect
        authenticationComplete = false;
    }
    
    /**
     * Reads the next incoming line and increment the byte counter and returns the line
     * @throws IOException 
     */
    private String getLine() throws IOException {
        if (inputBuffer == null)
            return null;
        
        String line = inputBuffer.readLine();
        if (line != null) {
            bytesReceived += line.getBytes().length;
        }
        return line;
    }
    
    /**
     * Sends a line to the networkConnection
     */
    private int sendLine(String line) {
        PrintStream output;
        try {
            output = new PrintStream(networkConnection.getOutputStream());
        } catch (IOException e) {
            return Constants.NO_NETWORK_AVAILABLE;
        }
        if (output == null)
            return Constants.NO_NETWORK_AVAILABLE;
        Log.d(TAG, "Client: " + line);
        output.println(line);
        return Constants.OK;
    }
}
