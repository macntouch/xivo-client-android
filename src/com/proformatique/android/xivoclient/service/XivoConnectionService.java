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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.XivoNotification;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.JSONMessageFactory;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class XivoConnectionService extends Service {
    
    private static final String TAG = "XiVO connection service";
    private static final int NB_FIELDS_USER = 7;
    
    private SharedPreferences prefs = null;
    private Socket networkConnection = null;
    private BufferedReader inputBuffer = null;
    private Handler handler = null;
    private Thread thread = null;
    private Context context = null;
    private long bytesReceived = 0L;
    private boolean cancel = false;
    
    // Informations that is relevant to a specific connection
    private boolean authenticationComplete = false;
    private boolean usersListComplete = false;
    private String sessionId = null;
    private String xivoId = null;
    private String astId = null;
    private String userId = null;
    private HashMap<String, String> capaPresenceState = null;
    private XivoNotification xivoNotif = null;
    private List<HashMap<String, String>> statusList = null;
    private List<HashMap<String, String>> usersList = null;
    private int[] mwi = new int[3];
    
    // Messages from the loop to the handler
    private final static int NO_MESSAGE = 0;
    private final static int NO_CLASS = 1;
    private final static int DISCONNECT = 2;
    private final static int UNKNOWN = 3;
    private final static int JSON_EXCEPTION = 4;
    private static final int USERS_LIST_COMPLETE = 5;
    private static final int PHONES_LOADED = 6;
    
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
        
        @Override
        public void loadData() throws RemoteException {
            XivoConnectionService.this.loadList("users");
        }
    };
    
    /**
     * Asks the CTI server for a list
     */
    private void loadList(String list) {
        if (thread == null)
            startLooping(getApplicationContext());
        usersListComplete = false;
        JSONObject query = JSONMessageFactory.getJsonClassFunction(list, "getlist");
        if (query == null) {
            Log.d(TAG, "Error while creating the getlist query");
            return;
        }
        int res;
        if ((res = sendLine(query.toString())) != Constants.OK) {
            Log.d(TAG, "Could not send the getlist query");
            switch (res) {
            case Constants.NO_NETWORK_AVAILABLE:
                Log.d(TAG, "No network");
                resetState();
                break;
            }
        }
    }
    
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
        if (thread != null && thread.isAlive())
            thread.interrupt();
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
        cancel = true;
        thread.interrupt();
        return Constants.OK;
    }
    
    /**
     * Perform authentication on the XiVO CTI server
     * @return error or success code
     */
    private int loginCTI() {
        String releaseOS = android.os.Build.VERSION.RELEASE;
        String login = prefs.getString("login", "");
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
     * Starts the main loop to listen to incoming JSON messages from the CTI server
     */
    private void startLooping(Context context) {
        this.context = context;
        cancel = false;
        handler = new Handler() {
            /**
             * Receives messages from the json loop and broadcast intents accordingly.
             */
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case USERS_LIST_COMPLETE:
                    Log.d(TAG, "Users list loaded");
                    loadList("phones");
                    break;
                case PHONES_LOADED:
                    Log.d(TAG, "Phones list loaded");
                    break;
                case NO_MESSAGE:
                    break;
                case JSON_EXCEPTION:
                    Log.d(TAG, "JSON error while receiving a message from the CTI server");
                    break;
                default:
                    Log.d(TAG, "handling an unknown message: " + msg.what);
                    break;
                }
            }
        };
        
        thread = new Thread() {
            public void run() {
                Looper.prepare();
                JSONObject newLine = null;
                Log.d(TAG, "Starting the main loop");
                while (!(cancel)) {
                    while ((newLine = readJsonObjectCTI()) == null);
                    handler.sendEmptyMessage(parseIncomingJson(newLine));
                }
            };
        };
        thread.start();
    }
    
    /**
     * Parses an incoming json message from the cti server and dispatches it to other methods
     * for better message handling.
     * @return
     */
    protected int parseIncomingJson(JSONObject line) {
        try {
            String classRec = line.has("class") ? line.getString("class") : null;
            if (classRec == null)
                return NO_CLASS;
            if (classRec.equals("presence"))
                return parsePresence(line);
            else if (classRec.equals("users"))
                return parseUsers(line);
            else if (classRec.equals("phones"))
                return parsePhones(line);
            else if (classRec.equals("history"))
                return parseHistory(line);
            else if (classRec.equals("features"))
                return parsePhones(line);
            else if (classRec.equals("groups"))
                return parseGroups(line);
            else if (classRec.equals("disconn"))
                return DISCONNECT;
            else {
                Log.d(TAG, "Unknown classrec: " + classRec);
                return UNKNOWN;
            }
        } catch (JSONException e) {
            Log.d(TAG, "Unable to get the class from the JSONObject");
            e.printStackTrace();
            return JSON_EXCEPTION;
        }
    }
    
    private int parseHistory(JSONObject line) {
        Log.d(TAG, "Parsing history:\n" + line.toString());
        return NO_MESSAGE;
    }
    
    /**
     * Parses incoming messages from the CTI server with class phones
     * @param line
     * @return msg to the handler
     */
    @SuppressWarnings("unchecked")
    private int parsePhones(JSONObject line) {
        Log.d(TAG, "Parsing phones:\n" + line.toString());
        if (line.has("payload") == false)
            return NO_MESSAGE;
        JSONObject payloads = null;
        try {
            payloads = line.getJSONObject("payload");
        } catch (JSONException e) {
            Log.e(TAG, "Could not retrieve the payload from the phone message");
            return JSON_EXCEPTION;
        }
        JSONArray jAllPhones = new JSONArray();
        for (Iterator<String> keyIter = payloads.keys(); keyIter.hasNext(); ) {
            try {
                jAllPhones.put(payloads.getJSONObject(keyIter.next()));
            } catch (JSONException e) {
                Log.e(TAG, "Could not retrieve phone from payload");
                return JSON_EXCEPTION;
            }
        }
        
        int nbXivo = jAllPhones.length();
        /**
         * For each users in the userslist, find the corresponding phone and update the user's
         * status
         */
        int i = 0;
        for (HashMap<String, String> mapUser: usersList) {
            if (!(mapUser.containsKey("techlist"))) {
                Log.d(TAG, "This user has no phone, skipping");
                Log.d(TAG, mapUser.toString());
                continue;
            }
            JSONObject jPhone = null;
            for (int j = 0; j < nbXivo; j++) {
                JSONObject xivo = null;
                try {
                    xivo = jAllPhones.getJSONObject(j);
                } catch (JSONException e) {
                    Log.d(TAG, "Bad json array index");
                    continue;
                }
                if (xivo.has(mapUser.get("techlist")) == true) {
                    try {
                        jPhone = xivo.getJSONObject(mapUser.get("techlist"));
                    } catch (JSONException e) {
                        Log.e(TAG, "Found an invalid phone");
                        return JSON_EXCEPTION;
                    }
                    break;
                }
            }
            if (jPhone == null) {
                Log.d(TAG, "No phone for this user, skipping");
                continue;
            }
            
            /**
             * "Real" phone number is retrieved from phones list
             */
            try {
                mapUser.put("phonenum", jPhone.getString("number"));
            } catch (JSONException e1) {
                Log.d(TAG, "Phone without a number");
                mapUser.put("phonenum", null);
            }
            try {
                JSONObject jPhoneStatus = jPhone.getJSONObject("hintstatus");
                mapUser.put("hintstatus_color", jPhoneStatus.getString("color"));
                mapUser.put("hintstatus_code", jPhoneStatus.getString("code"));
                mapUser.put("hintstatus_longname", jPhoneStatus.getString("longname"));
            } catch (JSONException e) {
                Log.d(TAG, "No Phones status : "+ jPhone.toString());
                mapUser.put("hintstatus_color", "");
                mapUser.put("hintstatus_code", "");
                mapUser.put("hintstatus_longname", "");
            }
            if (mapUser.get("xivo_userid").equals(xivoId)){
                capaPresenceState.put("phonenum", mapUser.get("phonenum"));
                capaPresenceState.put("hintstatus_color", mapUser.get("hintstatus_color"));
                capaPresenceState.put("hintstatus_code", mapUser.get("hintstatus_code"));
                capaPresenceState.put("hintstatus_longname", mapUser.get("hintstatus_longname"));
            }
            usersList.set(i, mapUser);
            i++;
        }
        return PHONES_LOADED;
    }
    
    /**
     * Parses incoming cti messages of class users
     * @param json
     * @return result parsed by the main loop handler
     */
    @SuppressWarnings("unchecked")
    private int parseUsers(JSONObject line) {
        Log.d(TAG, "Parsing users");
        if (line.has("payload")) {
            JSONArray payload = null;
            try {
                payload = line.getJSONArray("payload");
            } catch (JSONException e) {
                Log.d(TAG, "Could not get payload of the line");
                return JSON_EXCEPTION;
            }
            int len = payload != null ? payload.length() : 0;
            usersList = new ArrayList<HashMap<String, String>>(len);
            for (int i = 0; i < len; i++) {
                HashMap<String, String> user = new HashMap<String, String>(NB_FIELDS_USER);
                JSONObject jUser = null;
                JSONObject jUserState = null;
                try {
                    jUser = payload.getJSONObject(i);
                    jUserState = jUser.getJSONObject("statedetails");
                } catch (JSONException e) {
                    Log.d(TAG, "Could not retrieve user info from the payload");
                    e.printStackTrace();
                    return JSON_EXCEPTION;
                }
                
                /**
                 * Feed the useful fields to store in the list
                 */
                String xivoId = null;
                String userId = null;
                try {
                    xivoId = jUser.getString("xivo_userid");
                    userId = jUser.getString("astid") + "/" + xivoId;
                    user.put("xivo_userid", xivoId);
                    user.put("fullname", jUser.getString("fullname"));
                    user.put("phonenum", jUser.getString("phonenum"));
                    user.put("stateid", jUserState.getString("stateid"));
                    user.put("stateid_longname", jUserState.getString("longname"));
                    user.put("stateid_color", jUserState.getString("color"));
                    user.put("techlist", jUser.getJSONArray("techlist").getString(0));
                    if (userId.equals(this.userId) && jUser.has("mwi")) {
                        JSONArray mwi = jUser.getJSONArray("mwi");
                        int lenmwi = mwi != null ? mwi.length() : 0;
                        for (int j = 0; j < lenmwi; j++) {
                            this.mwi[j] = mwi.getInt(j);
                        }
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "Could not create the user data structure");
                    return JSON_EXCEPTION;
                }
                Log.d(TAG, "Adding user: " + user.toString());
                usersList.add(user);
            }
            if (usersList.size() > 1)
                Collections.sort(usersList, new fullNameComparator());
            usersListComplete = true;
            return USERS_LIST_COMPLETE;
        }
        return NO_MESSAGE;
    }
    
    private int parsePresence(JSONObject line) {
        Log.d(TAG, "Parsing presences: " + line.toString());
        return NO_MESSAGE;
    }
    
    private int parseGroups(JSONObject line) {
        Log.d(TAG, "Parsing groups: " + line.toString());
        return NO_MESSAGE;
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
            userId = astId + "/" + xivoId;
            
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
        usersList = null;
    }
    
    private int sendPasswordCTI() {
        byte[] sDigest = null;
        JSONObject jsonPasswordAuthent = new JSONObject();
        String password = prefs.getString("password", "");
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
    
    @SuppressWarnings("unchecked")
    private class fullNameComparator implements Comparator
    {
        public int compare(Object obj1, Object obj2)
        {
            HashMap<String, String> update1 = (HashMap<String, String>)obj1;
            HashMap<String, String> update2 = (HashMap<String, String>)obj2;
            return update1.get("fullname").compareToIgnoreCase(update2.get("fullname"));
        }
    }
}
