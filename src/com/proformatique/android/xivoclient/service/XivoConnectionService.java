package com.proformatique.android.xivoclient.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.SettingsActivity;
import com.proformatique.android.xivoclient.XivoNotification;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.JSONMessageFactory;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class XivoConnectionService extends Service {
    
    private static final String TAG = "XiVO connection service";
    
    private SharedPreferences prefs = null;
    private Socket networkConnection = null;
    private BufferedReader inputBuffer = null;
    private Handler handler = null;
    private Thread thread = null;
    private long bytesReceived = 0L;
    private boolean cancel = false;
    
    // Informations that is relevant to a specific connection
    private boolean authenticationComplete = false;
    private String sessionId = null;
    private String xivoId = null;
    private String astId = null;
    private String userId = null;
    private HashMap<String, String> capaPresenceState = null;
    private XivoNotification xivoNotif = null;
    private List<HashMap<String, String>> usersList = null;
    private int[] mwi = new int[3];
    private JSONArray capalist = null;
    private long stateId = 0L;
    private String phoneStatusLongname = null;
    private String phoneStatusColor = Constants.DEFAULT_HINT_COLOR;
    private String lastCalledNumber = null;
    
    // Messages from the loop to the handler
    private final static int NO_MESSAGE = 0;
    private final static int NO_CLASS = 1;
    private final static int DISCONNECT = 2;
    private final static int UNKNOWN = 3;
    private final static int JSON_EXCEPTION = 4;
    private static final int USERS_LIST_COMPLETE = 5;
    private static final int PHONES_LOADED = 6;
    private static final int PRESENCE_UPDATE = 7;
    private static final int MY_STATUS_UPDATE = 8;
    
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
            XivoConnectionService.this.refreshFeatures();
            XivoConnectionService.this.loadList("users");
        }
        
        @Override
        public boolean loadDataCalled() throws RemoteException {
            return usersList != null;
        }
        
        @Override
        public long getReceivedBytes() throws RemoteException {
            return bytesReceived;
        }
        
        @Override
        public boolean hasNewVoiceMail() throws RemoteException {
            return mwi != null ? mwi[0] == 1 : false;
        }
        
        @Override
        public long getStateId() throws RemoteException {
            return stateId;
        }
        
        @Override
        public String getPhoneStatusColor() throws RemoteException {
            return phoneStatusColor;
        }
        
        @Override
        public String getPhoneStatusLongname() throws RemoteException {
            return phoneStatusLongname;
        }
        
        @Override
        public int call(String number) throws RemoteException {
            return XivoConnectionService.this.call(number);
        }
    };
    
    /**
     * Initiate a call
     * @param number
     * @return OK or error code
     */
    private int call(String number) {
        Log.d(TAG, "Calling " + number);
        lastCalledNumber = number;
        JSONObject jCall = JSONMessageFactory.getJsonCallingObject(
                "originate", SettingsActivity.getMobileNumber(getApplicationContext()), number);
        sendLine(jCall.toString());
        return Constants.OK;
    }
    
    /**
     * Asks the CTI server for a list
     */
    private void loadList(String list) {
        if (thread == null)
            startLooping(getApplicationContext());
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
        phoneStatusLongname = getString(R.string.default_hint_longname);
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
        int port = Constants.XIVO_DEFAULT_PORT;
        try {
            port = Integer.parseInt(prefs.getString("server_port",
                    Integer.toString(Constants.XIVO_DEFAULT_PORT)));
        } catch (NumberFormatException e) {
            Log.d(TAG, "Port number cannot be parsed to int, using default port");
        }
        String host = prefs.getString("server_adress", "");
        if (thread != null && thread.isAlive())
            thread.interrupt();
        try {
            Log.d(TAG, "Connecting to " + host + " " + port);
            networkConnection = new Socket(host, port);
        } catch (UnknownHostException e) {
            Log.d(TAG, "Unknown host " + host);
            return Constants.BAD_HOST;
        } catch (IOException e) {
            return Constants.BAD_HOST;
        }
        bytesReceived = 0L;
        try {
            inputBuffer = new BufferedReader(
                    new InputStreamReader(networkConnection.getInputStream()));
            
            String firstLine;
            while ((firstLine = getLine()) != null) {
                if (firstLine.contains("XiVO CTI Server")) {
                    return Constants.CONNECTION_OK;
                }
            }
            return Constants.NOT_CTI_SERVER;
        } catch (IOException e) {
            return Constants.NO_NETWORK_AVAILABLE;
        }
    }
    
    private int disconnectFromServer() {
        Log.d(TAG, "Disconnecting");
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
        resetState();
        authenticationComplete = false;
        cancel = true;
        if (thread != null)
            thread.interrupt();
        return Constants.OK;
    }
    
    private void refreshFeatures() {
        if (thread == null)
            startLooping(getApplicationContext());
        sendLine(JSONMessageFactory.getJsonFeaturesRefresh(xivoId).toString());
    }
    
    /**
     * Perform authentication on the XiVO CTI server
     * @return error or success code
     */
    private int loginCTI() {
        int res;
        
        /**
         * Creating first Json login array
         */
        JSONObject jLogin = JSONMessageFactory.getJsonLogin(this);
       
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
        cancel = false;
        handler = new Handler() {
            /**
             * Receives messages from the json loop and broadcast intents accordingly.
             */
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case USERS_LIST_COMPLETE:
                    Log.d(TAG, "Users list loaded");
                    Intent iLoadUser = new Intent();
                    iLoadUser.setAction(Constants.ACTION_LOAD_USER_LIST);
                    sendBroadcast(iLoadUser);
                    loadList("phones");
                    break;
                case PHONES_LOADED:
                    Log.d(TAG, "Phones list loaded");
                    Intent iLoadPhone = new Intent();
                    iLoadPhone.setAction(Constants.ACTION_LOAD_USER_LIST);
                    sendBroadcast(iLoadPhone);
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
                    while ((newLine = readJsonObjectCTI()) == null && !(cancel));
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
            // Sheets are ignored at the moment
            else if (classRec.equals("sheet"))
                return NO_MESSAGE;
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
    private int parsePhones(JSONObject line) {
        try {
            String function = line.getString("function");
            if (function.equals("sendlist")) {
                return parsePhoneList(line);
            } else if (function.equals("update")) {
                return parsePhoneUpdate(line);
            }
        } catch (JSONException e) {
            Log.d(TAG, "Phone class without function");
            return NO_MESSAGE;
        }
        return NO_MESSAGE;
    }
    
    /**
     * Parses phone updates
     * @param line
     * @return Message to the handler
     */
    private int parsePhoneUpdate(JSONObject line) {
        
        /*
         * Check if the update concerns a call I'm doing
         */
        if (lastCalledNumber != null) {
            if (SettingsActivity.getUseMobile(this)) {
                if (lastCalledNumber.length() < Constants.MAX_PHONE_NUMBER_LEN) {
                    if (JSONMessageFactory.getCalledIdNum(line).equals(
                            SettingsActivity.getMobileNumber(this))) {
                        parseMyMobilePhoneUpdate(line);
                    }
                }
            } else { // Not using mobile
                if (JSONMessageFactory.checkIdMatch(line, astId, xivoId)) {
                    parseMyPhoneUpdate(line);
                }
            }
        }
        
        /*
         * For all updates
         */
        try {
            long id = getUserIdFromUpdate(line.getString("astid"),
                    line.getJSONObject("status").getString("id"));
            if (id > 0)
                return updateUserHintStatus(id,
                        line.getJSONObject("status").getJSONObject("hintstatus"));
        } catch (JSONException e) {
            Log.d(TAG, "Could not find and astid and an id for this update");
            return NO_MESSAGE;
        }
        return NO_MESSAGE;
    }
    
    /**
     * Updates a user hintstatus and sends an update intent
     * @param id
     * @param hintstatus
     * @return
     */
    private int updateUserHintStatus(long id, JSONObject hintstatus) {
        ContentValues values = new ContentValues();
        try {
            values.put(UserProvider.HINTSTATUS_CODE, hintstatus.getString("code"));
            values.put(UserProvider.HINTSTATUS_COLOR, hintstatus.getString("color"));
            values.put(UserProvider.HINTSTATUS_LONGNAME, hintstatus.getString("longname"));
        } catch (JSONException e) {
            Log.d(TAG, "Failed to update the user status");
            return NO_MESSAGE;
        }
        getContentResolver().update(
                Uri.parse(UserProvider.CONTENT_URI + "/" + id), values, null, null);
        Intent iUpdateIntent = new Intent();
        iUpdateIntent.setAction(Constants.ACTION_LOAD_USER_LIST);
        iUpdateIntent.putExtra("id", id);
        sendBroadcast(iUpdateIntent);
        Log.d(TAG, "Update intent sent");
        return NO_MESSAGE;
    }
    
    /**
     * Searches the DB for this user and return it's column id
     * @param astid
     * @param xivoid
     */
    private long getUserIdFromUpdate(String astid, String xivoid) {
        Cursor user = getContentResolver().query(UserProvider.CONTENT_URI,
                new String[] {UserProvider._ID, UserProvider.ASTID, UserProvider.XIVO_USERID},
                UserProvider.ASTID + " = '" + astid 
                + "' AND " + UserProvider.XIVO_USERID + " = '" + xivoid + "'", null, null);
        if (user.getCount() > 0) {
            user.moveToFirst();
            long id = user.getLong(user.getColumnIndex(UserProvider._ID));
            user.close();
            return id;
        } else {
            user.close();
            return -1;
        }
    }
    
    /**
     * Parses phones update for a call from the user (not using his mobile)
     * @param line
     */
    private void parseMyPhoneUpdate(JSONObject line) {
        Log.d(TAG, "Parsing my phone update");
        /*
         * Sends the new call progress status
         */
        try {
            Intent iStatusUpdate = new Intent();
            iStatusUpdate.setAction(Constants.ACTION_CALL_PROGRESS);
            JSONObject hintstatus = line.getJSONObject("status").getJSONObject("hintstatus");
            iStatusUpdate.putExtra("status", hintstatus.getString("longname"));
            iStatusUpdate.putExtra("code", hintstatus.getString("code"));
            sendBroadcast(iStatusUpdate);
        } catch (JSONException e) {
            Log.d(TAG, "Could not get the new status");
        }
        /*
         * Save channels
         */
    }
    
    /**
     * Parses phones for a call from the user's mobile
     * @param line
     */
    private void parseMyMobilePhoneUpdate(JSONObject line) {
        Log.d(TAG, "Parsing my mobile phone update");
    }
    
    /**
     * Parses the phone list received at the beginning of a session.
     * @param line
     * @return Message to the handler
     */
	@SuppressWarnings("unchecked")
	private int parsePhoneList(JSONObject line) {
        Log.d(TAG, "Parsing phone list");
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
        
        /**
         * For each users in the userslist, find the corresponding phone and update the user's
         * status
         */
        Cursor user = getContentResolver().query(UserProvider.CONTENT_URI, null, null, null, null);
        user.moveToFirst();
        int techlistIndex = user.getColumnIndex(UserProvider.TECHLIST);
        int nbXivo = jAllPhones.length();
        do {
            String techlist = user.getString(techlistIndex);
            for (int i = 0; i < nbXivo; i++) {
                try {
                    if (jAllPhones.getJSONObject(i).has(techlist)) {
                        setPhoneForUser(user, jAllPhones.getJSONObject(i).getJSONObject(techlist));
                    }
                } catch (JSONException e) {}
            }
        } while (user.moveToNext());
        user.close();
        return PHONES_LOADED;
    }
    
    private void setPhoneForUser(Cursor user, JSONObject jPhone) {
        
        long id = user.getLong(user.getColumnIndex(UserProvider._ID));
        JSONObject jPhoneStatus = null;
        final ContentValues values = new ContentValues();
        try {
            jPhoneStatus = jPhone.getJSONObject("hintstatus");
            values.put(UserProvider.PHONENUM, jPhone.getString("number"));
            values.put(UserProvider.HINTSTATUS_COLOR, jPhoneStatus.getString("color"));
            values.put(UserProvider.HINTSTATUS_CODE, jPhoneStatus.getString("code"));
            values.put(UserProvider.HINTSTATUS_LONGNAME, jPhoneStatus.getString("longname"));
            getContentResolver().update(
                    Uri.parse(UserProvider.CONTENT_URI + "/" + id), values, null, null);
        } catch (JSONException e) {
            Log.d(TAG, "JSONException, could not update phone status");
        }
        values.clear();
        
        if (user.getString(user.getColumnIndex(UserProvider.XIVO_USERID)).equals(xivoId)) {
            try {
                phoneStatusLongname = jPhoneStatus.getString("longname");
                phoneStatusColor = jPhoneStatus.getString("color");
                Intent i = new Intent();
                i.setAction(Constants.ACTION_MY_PHONE_CHANGE);
                i.putExtra("color", phoneStatusColor);
                i.putExtra("longname", phoneStatusLongname);
                sendBroadcast(i);
            } catch (JSONException e) {
                Log.d(TAG, "Failled to set our status");
            }
        }
    }
    
    /**
     * Parses incoming cti messages of class users
     * @param json
     * @return result parsed by the main loop handler
     */
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
            getContentResolver().delete(UserProvider.CONTENT_URI, null, null);
            ContentValues user = new ContentValues();
            for (int i = 0; i < len; i++) {
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
                try {
                    String xivoId = jUser.getString("xivo_userid");
                    String astId = jUser.getString("astid");
                    String userId = astId + "/" + xivoId;
                    user.put(UserProvider.ASTID, astId);
                    user.put(UserProvider.XIVO_USERID, xivoId);
                    user.put(UserProvider.FULLNAME, jUser.getString("fullname"));
                    user.put(UserProvider.PHONENUM, jUser.getString("phonenum"));
                    user.put(UserProvider.STATEID, jUserState.getString("stateid"));
                    user.put(UserProvider.STATEID_LONGNAME, jUserState.getString("longname"));
                    user.put(UserProvider.STATEID_COLOR, jUserState.getString("color"));
                    user.put(UserProvider.TECHLIST, jUser.getJSONArray("techlist").getString(0));
                    user.put(UserProvider.HINTSTATUS_COLOR, Constants.DEFAULT_HINT_COLOR);
                    user.put(UserProvider.HINTSTATUS_CODE, Constants.DEFAULT_HINT_CODE);
                    user.put(UserProvider.HINTSTATUS_LONGNAME, getString(
                            R.string.default_hint_longname));
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
                getContentResolver().insert(UserProvider.CONTENT_URI, user);
                user.clear();
            }
            return USERS_LIST_COMPLETE;
        }
        return NO_MESSAGE;
    }
    
    private int parsePresence(JSONObject line) {
        Log.d(TAG, "Parsing presences: " + line.toString());
        HashMap<String, String> map = new HashMap<String, String>();
        JSONObject jState = null;
        try {
            jState = line.getJSONObject("capapresence").getJSONObject("state");
            map.put("xivo_userid", line.getString("xivo_userid"));
            map.put("stateid", jState.getString("stateid"));
            map.put("stateid_longname", jState.getString("longname"));
            map.put("stateid_color", jState.getString("color"));
        } catch (JSONException e) {
            Log.d(TAG, "Exception while retrieving presence");
            return NO_MESSAGE;
        }
        return updateUserList(map, "presence");
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
        String capaid = null;
        try {
            capaid = capalist.getString(0);
        } catch (JSONException e1) {
            Log.d(TAG, "Error while parsing capaid, using client");
            capaid = "client";
        }
        try {
            jsonCapas.accumulate("class", "login_capas");
            jsonCapas.accumulate("agentlogin", "now");
            jsonCapas.accumulate("capaid", capaid);
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
            
            if (jCapa.has("capaxlets")) {
                parseCapaxlets(jCapa.getJSONArray("capaxlets"));
            }
            
            if (jCapa.has("capapresence")) {
                parseCapapresence(jCapa.getJSONObject("capapresence"));
            }
            
            if (jCapa.has("capaservices")) {
                parseCapaservices(jCapa.getJSONArray("capaservices"));
            }
            
            xivoNotif = new XivoNotification(getApplicationContext());
            xivoNotif.createNotification();
        } catch (JSONException e) {
            return Constants.JSON_POPULATE_ERROR;
        }
        return Constants.OK;
    }
    
    /**
     * Parses capaservices and update the DB
     */
    private void parseCapaservices(JSONArray jServices) {
        Log.d(TAG, "parsing capaservices");
        // Remove old entries
        getContentResolver().delete(CapaservicesProvider.CONTENT_URI, null, null);
        ContentValues values = new ContentValues();
        for (int i = 0; i < jServices.length(); i++) {
            try {
                values.put(CapaservicesProvider.SERVICE, jServices.getString(i));
                getContentResolver().insert(CapaservicesProvider.CONTENT_URI, values);
                values.clear();
            } catch (JSONException e) {
                Log.d(TAG, "Could not parse capaservices");
            }
        }
    }
    
    /**
     * Parses the incoming capapresence message and update the DB
     */
    @SuppressWarnings("unchecked")
    private void parseCapapresence(JSONObject jPresence) {
        /*
         * Fill the DB
         */
        Log.d(TAG, "Parsing capapresence");
        getContentResolver().delete(CapapresenceProvider.CONTENT_URI, null, null);
        try {
            ContentValues presence = new ContentValues();
            for (Iterator<String> keyIter = jPresence.getJSONObject("names").keys();
                    keyIter.hasNext(); ) {
                String key = keyIter.next();
                presence.put(CapapresenceProvider.NAME, key);
                presence.put(CapapresenceProvider.COLOR,
                        jPresence.getJSONObject("names").getJSONObject(key).getString("color"));
                presence.put(CapapresenceProvider.LONGNAME,
                        jPresence.getJSONObject("names").getJSONObject(key).getString("longname"));
                int allowed = jPresence.getJSONObject("allowed").getBoolean(key) == true ? 1 : 0;
                presence.put(CapapresenceProvider.ALLOWED, allowed);
                getContentResolver().insert(CapapresenceProvider.CONTENT_URI, presence);
                presence.clear();
            }
        } catch (JSONException e) {
            Log.d(TAG, "json exception while parsing presences");
        }
        try {
            JSONObject myPresence = jPresence.getJSONObject("state");
            String code = myPresence.getString("stateid");
            Cursor presence = getContentResolver().query(CapapresenceProvider.CONTENT_URI,
                    new String[] {CapapresenceProvider._ID, CapapresenceProvider.NAME},
                    CapapresenceProvider.NAME + " = '" + code + "'", null, null);
            presence.moveToFirst();
            stateId = presence.getLong(presence.getColumnIndex(CapapresenceProvider._ID));
            presence.close();
            Intent i = new Intent();
            i.setAction(Constants.ACTION_MY_STATUS_CHANGE);
            i.putExtra("id", stateId);
        } catch (JSONException e) {
            Log.d(TAG, "Could not get my presence");
        }
    }
    
    /**
     * Parses the incoming capaxlets message to add it to the DB
     * @param xlets
     */
    private void parseCapaxlets(JSONArray xlets) {
        Log.d(TAG, "Parsing capaxlets");
        // Remove old entries
        getContentResolver().delete(CapaxletsProvider.CONTENT_URI, null, null);
        ContentValues values = new ContentValues();
        for (int i = 0; i < xlets.length(); i++) {
            try {
                values.put(CapaxletsProvider.XLET, xlets.getString(i));
                getContentResolver().insert(CapaxletsProvider.CONTENT_URI, values);
                values.clear();
            } catch (JSONException e) {
                Log.d(TAG, "Could not parse capaxlets");
            }
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
                capalist = ctiAnswer.getJSONArray("capalist");
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
                disconnectFromServer();
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
    
    private int updateUserList(HashMap<String, String> map, String type) {
        int len = usersList != null ? usersList.size() : 0;
        for (int i = 0; i < len; i++) {
            HashMap<String, String> usersMap = usersList.get(i);
            if (usersMap.containsKey("xivo_userid") && usersMap.get("xivo_userid")
                    .equals(map.get("xivo_userid"))) {
                if (type.equals("presence")) {
                    usersMap.put("stateid", map.get("stateid"));
                    usersMap.put("stateid_longname", map.get("stateid_longname"));
                    usersMap.put("stateid_color", map.get("stateid_color"));
                    usersList.set(i, usersMap);
                } else if (type.equals("phone")) {
                    usersMap.put("hintstatus_color", map.get("hintstatus_color"));
                    usersMap.put("hintstatus_code", map.get("hintstatus_code"));
                    usersMap.put("hintstatus_longname", map.get("hintstatus_longname"));
                    usersList.set(i, usersMap);
                    if (map.get("xivo_userid").equals(userId)){
                        capaPresenceState.put("hintstatus_color", map.get("hintstatus_color"));
                        capaPresenceState.put("hintstatus_code", map.get("hintstatus_code"));
                        capaPresenceState.put("hintstatus_longname",
                                map.get("hintstatus_longname"));
                        return MY_STATUS_UPDATE;
                    } 
                }
                return PRESENCE_UPDATE;
            }
        }
        return NO_MESSAGE;
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
            if (networkConnection != null && networkConnection.isConnected() == false) {
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
            if (networkConnection == null)
                return Constants.NO_NETWORK_AVAILABLE;
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
