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
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
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
    private XivoNotification xivoNotif;
    private IntentReceiver receiver = null;
    
    // Informations that is relevant to a specific connection
    private String sessionId = null;
    private String xivoId = null;
    private String astId = null;
    private String userId = null;
    private String fullname = null;
    private List<HashMap<String, String>> usersList = null;
    private int[] mwi = new int[3];
    private JSONArray capalist = null;
    private long stateId = 0L;
    private String phoneStatusLongname = null;
    private String phoneStatusColor = Constants.DEFAULT_HINT_COLOR;
    private String phoneStatusCode = null;
    private String lastCalledNumber = null;
    private String thisChannel = null;
    private String peerChannel = null;
    private String oldChannel = null;
    private boolean wrongHostPort = false;
    private boolean connected = false;
    private boolean connecting = false;
    private boolean authenticated = false;
    private boolean authenticating = false;
    private boolean wrongLoginInfo = false;
    
    // Messages from the loop to the handler
    private final static int NO_MESSAGE = 0;
    private final static int NO_CLASS = 1;
    private final static int DISCONNECT = 2;
    private final static int UNKNOWN = 3;
    private final static int JSON_EXCEPTION = 4;
    private static final int USERS_LIST_COMPLETE = 5;
    private static final int PHONES_LOADED = 6;
    private static final int PRESENCE_UPDATE = 7;
    private static final int HISTORY_LOADED = 8;
    private static final int FEATURES_LOADED = 9;
    
    /**
     * Implementation of the methods between the service and the activities
     */
    private final IXivoConnectionService.Stub binder = new IXivoConnectionService.Stub() {
        
        @Override
        public int connect() throws RemoteException {
            return XivoConnectionService.this.connect();
        }
        
        @Override
        public int disconnect() throws RemoteException {
            return disconnectFromServer();
        }
        
        @Override
        public boolean isConnected() throws RemoteException {
            return (networkConnection != null && networkConnection.isConnected()
                    && inputBuffer != null);
        }
        
        @Override
        public int authenticate() throws RemoteException {
            return XivoConnectionService.this.authenticate();
        }
        
        @Override
        public boolean isAuthenticated() throws RemoteException {
            return authenticated;
        }
        
        @Override
        public void loadData() throws RemoteException {
            XivoConnectionService.this.refreshFeatures();
            XivoConnectionService.this.refreshHistory();
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
        public String getFullname() throws RemoteException {
            return fullname == null ? getString(R.string.user_identity) : fullname;
        }
        
        @Override
        public int call(String number) throws RemoteException {
            return XivoConnectionService.this.call(number);
        }
        
        @Override
        public boolean isOnThePhone() throws RemoteException {
            if (SettingsActivity.getUseMobile(XivoConnectionService.this)) {
                return XivoConnectionService.this.isMobileOffHook();
            } else {
                return phoneStatusCode == null ? false : !phoneStatusCode.equals(
                        Constants.AVAILABLE_STATUS_CODE);
            }
        }
        
        @Override
        public void setState(String stateId) throws RemoteException {
            XivoConnectionService.this.setState(stateId);
        }
        
        @Override
        public void sendFeature(String feature, String value, String phone) throws RemoteException {
             XivoConnectionService.this.sendFeature(feature, value, phone);
        }
        
        @Override
        public boolean hasChannels() throws RemoteException {
            return thisChannel != null && peerChannel != null;
        }
        
        @Override
        public void hangup() throws RemoteException {
            XivoConnectionService.this.hangup();
        }
        
        @Override
        public void atxfer(String number) throws RemoteException {
            XivoConnectionService.this.atxfer(number);
        }
        
        @Override
        public void transfer(String number) throws RemoteException {
        	XivoConnectionService.this.transfer(number);
        }
        
        @Override
        public boolean killDialer() throws RemoteException {
            return SettingsActivity.getUseMobile(XivoConnectionService.this)
                && lastCalledNumber != null
                && lastCalledNumber.length() < Constants.MAX_PHONE_NUMBER_LEN;
        }
    };
    
    /**
     * Check the phone status returns true if it's off hook
     * @return
     */
    private boolean isMobileOffHook() {
        switch (((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
                .getCallState()) {
        case TelephonyManager.CALL_STATE_OFFHOOK:
        case TelephonyManager.CALL_STATE_RINGING:
            return true;
        default:
            return false;
        }
    }
    
    private void transfer(String number) {
        Log.d(TAG, "Blind transfer to " + number);
        if (this.peerChannel != null) {
            String src = "chan:" + astId + "/" + xivoId + ":" + this.peerChannel;
            sendLine(JSONMessageFactory.createJsonTransfer("transfer", src, number).toString());
        } else {
            Log.d(TAG, "Cannot transfer from a null channel");
        }
    }
    
    private void atxfer(String number) {
        if (this.thisChannel != null) {
            String src = "chan:" + astId + "/" + xivoId + ":" + this.thisChannel;
            sendLine(JSONMessageFactory.createJsonTransfer("atxfer", src, number).toString());
        } else {
            Log.d(TAG, "Cannot atxfer from a null of Local/ channel");
        }
    }
    
    /**
     * Hang-up the current call if AndroidTools.hangup did not work
     * If we have a Local/ channel we hang-up our peer's channel
     */
    private void hangup() {
        String channel = null;
        if (thisChannel != null) {
            channel = "chan:" + astId + "/" + xivoId + ":" + thisChannel;
        } else if (peerChannel != null){
            channel = "chan:" + astId + "/" + xivoId + ":" + peerChannel;
        } else if (oldChannel != null) {
            channel = "chan:" + astId + "/" + xivoId + ":" + oldChannel;
        } else {
            Log.d(TAG, "Can't hang-up from the service");
            return;
        }
        sendLine(JSONMessageFactory.createJsonHangupObject(this, channel).toString());
    }
    
    /**
     * Change a feature value
     * @param feature
     * @param value
     * @param phone
     */
    private void sendFeature(String feature, String value, String phone) {
        sendLine(JSONMessageFactory.createJsonFeaturePut(
                astId, xivoId, feature, value, phone).toString());
    }
    
    /**
     * Sends an history reques to the CTI server
     */
    private void refreshHistory() {
        getContentResolver().delete(HistoryProvider.CONTENT_URI, null, null);
        sendLine(JSONMessageFactory.getJsonHistoRefresh(astId, xivoId, "0", "10").toString());
        sendLine(JSONMessageFactory.getJsonHistoRefresh(astId, xivoId, "1", "10").toString());
        sendLine(JSONMessageFactory.getJsonHistoRefresh(astId, xivoId, "2", "10").toString());
    }
    
    /**
     * Set a new status to the user
     */
    private void setState(String stateId) {
        sendLine(JSONMessageFactory.getJsonState(stateId).toString());
    }
    
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
        receiver = new IntentReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SETTINGS_CHANGE);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(receiver, new IntentFilter(filter));
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        phoneStatusLongname = getString(R.string.default_hint_longname);
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopThread();
        connectionCleanup();
        if (xivoNotif != null) xivoNotif.removeNotif();
        unregisterReceiver(receiver);
        super.onDestroy();
    }
    
    private void stopThread() {
        cancel = true;
        if (thread != null) {
            Thread dead = thread;
            thread = null;
            dead.interrupt();
        }
    }
    
    @Override
    public void onStart(Intent i, int startId) {
        super.onStart(i, startId);
        Log.d(TAG, "XiVO connection service started");
        xivoNotif = new XivoNotification(getApplicationContext());
        xivoNotif.createNotification();
        if (SettingsActivity.getAlwaysConnected(this)) {
            while (!connected && !authenticated && !wrongHostPort && !wrongLoginInfo) {
                autoLogin();
            }
        }
    }
    
    private void autoLogin() {
        Log.d(TAG, "Trying to auto-logon");
        if (!connected) connect();
        if (connected && !authenticated) authenticate();
        //if (connected && authenticated) refreshLists();
    }
    
    private int authenticate() {
        authenticated = false;
        if (authenticating) return Constants.ALREADY_AUTHENTICATING;
        authenticating = true;
        int res = loginCTI();
        authenticating = false;
        if (res == Constants.AUTHENTICATION_OK) authenticated = true;
        return res;
    }
    
    private int connect() {
        connected = false;
        if (connecting == true) return Constants.ALREADY_CONNECTING;
        connecting = true;
        int res = connectToServer();
        connecting = false;
        switch (res) {
        case Constants.CONNECTION_OK:
            connected = true;
            break;
        case Constants.BAD_HOST:
            wrongHostPort = true;
            break;
        default:
            connectionCleanup();
        }
        return res;
    }
    
    /**
     * Clean up the network connection and input/output streams
     */
    private void connectionCleanup() {
        connected = false;
        authenticated = false;
        if (inputBuffer != null) {
            try {
                inputBuffer.close();
            } catch (IOException e) {
                Log.d(TAG, "inputBuffer was already closed");
            }
            inputBuffer = null;
        }
        if (networkConnection != null) {
            try {
                try {
                    networkConnection.shutdownOutput();
                    networkConnection.shutdownInput();
                } catch (IOException e) {
                    Log.d(TAG, "Input and output were already closed");
                }
                networkConnection.close();
                networkConnection = null;
            } catch (IOException e) {
                Log.e(TAG, "Error while cleaning up the network connection");
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public boolean onUnbind(Intent i) {
        Log.d(TAG, "Unbind called");
        return super.onUnbind(i);
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
        if (wrongHostPort) return Constants.BAD_HOST;
        authenticated = false;
        int port = Constants.XIVO_DEFAULT_PORT;
        try {
            port = Integer.parseInt(prefs.getString("server_port",
                    Integer.toString(Constants.XIVO_DEFAULT_PORT)));
        } catch (NumberFormatException e) {
            Log.d(TAG, "Port number cannot be parsed to int, using default port");
        }
        String host = prefs.getString("server_adress", "");
        stopThread();
        while (thread != null && thread.isAlive()) {
            try {
                wait(100);
            } catch (InterruptedException e) {
                Log.d(TAG, "Wait interrupted");
            }
        }
        try {
            Log.d(TAG, "Connecting to " + host + " " + port);
            networkConnection = new Socket(host, port);
        } catch (UnknownHostException e) {
            Log.d(TAG, "Unknown host " + host);
            wrongHostPort = true;
            return Constants.BAD_HOST;
        } catch (IOException e) {
            return Constants.CONNECTION_TIMEDOUT;
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
        stopThread();
        connectionCleanup();
        resetState();
        return Constants.OK;
    }
    
    private void refreshFeatures() {
        if (thread == null)
            startLooping(getApplicationContext());
        sendLine(JSONMessageFactory.getJsonFeaturesRefresh(astId, xivoId).toString());
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
                case PRESENCE_UPDATE:
                    Log.d(TAG, "Presence changed");
                    Intent iLoadPresence = new Intent();
                    iLoadPresence.setAction(Constants.ACTION_LOAD_USER_LIST);
                    sendBroadcast(iLoadPresence);
                    break;
                case HISTORY_LOADED:
                    Intent iLoadHistory = new Intent();
                    iLoadHistory.setAction(Constants.ACTION_LOAD_HISTORY_LIST);
                    sendBroadcast(iLoadHistory);
                    break;
                case FEATURES_LOADED:
                    Intent iLoadFeatures = new Intent();
                    iLoadFeatures.setAction(Constants.ACTION_LOAD_FEATURES);
                    sendBroadcast(iLoadFeatures);
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
        if (cancel || line == null)
            return NO_MESSAGE;
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
                return parseFeatures(line);
            else if (classRec.equals("groups"))
                return parseGroups(line);
            else if (classRec.equals("disconn"))
                return DISCONNECT;
            // Sheets are ignored at the moment
            else if (classRec.equals("sheet"))
                return NO_MESSAGE;
            else if (classRec.equals("meetme"))
                return parseMeetme(line);
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
    
    private int parseMeetme(JSONObject line) {
        try {
            if (line.getString("function").equals("update")) {
                loadList("users");
            }
        } catch (JSONException e) {
            // no function
        }
        return NO_MESSAGE;
    }
    
    private int parseFeatures(JSONObject line) {
        Log.d(TAG, "Parsing features:\n" + line.toString());
        String[] features = {"enablednd", "callrecord", "incallfilter", "enablevoicemail",
            "busy", "rna", "unc"};
        try {
            JSONObject payload = line.getJSONObject("payload");
            if (line.has("function") && line.getString("function").equals("put")) {
                for (@SuppressWarnings("unchecked") Iterator<String> keyIter = payload.keys();
                        keyIter.hasNext(); ) {
                    String feature = keyIter.next();
                    ContentValues values = new ContentValues();
                    values.put(CapaservicesProvider.ENABLED,
                            payload.getJSONObject(feature).getBoolean("enabled") == true ? 1 : 0);
                    if (payload.getJSONObject(feature).has("number")) {
                        values.put(CapaservicesProvider.NUMBER,
                                payload.getJSONObject(feature).getString("number"));
                    }
                    if (!feature.equals("enablednd") && !feature.equals("enablevoicemail"))
                        feature = feature.replace("enable", "");
                    getContentResolver().update(CapaservicesProvider.CONTENT_URI, values,
                            CapaservicesProvider.SERVICE + " = '" + feature + "'", null);
                    values.clear();
                }
            } else {
                getContentResolver().delete(CapaservicesProvider.CONTENT_URI, null, null);
                for (String feature: features) {
                    ContentValues values = new ContentValues();
                    values.put(CapaservicesProvider.SERVICE, feature);
                    values.put(CapaservicesProvider.ENABLED,
                            payload.getJSONObject(feature).getBoolean("enabled") == true ? 1 : 0);
                    if (payload.getJSONObject(feature).has("number")) {
                        values.put(CapaservicesProvider.NUMBER,
                                payload.getJSONObject(feature).getString("number"));
                    }
                    getContentResolver().insert(CapaservicesProvider.CONTENT_URI, values);
                    values.clear();
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "Could not parse features");
            return JSON_EXCEPTION;
        }
        return FEATURES_LOADED;
    }
    
    private int parseHistory(JSONObject line) {
        Log.d(TAG, "Parsing history:\n" + line.toString());
        try {
            JSONArray payload = line.getJSONArray("payload");
            int len = payload.length();
            for (int i = 0; i < len; i++) {
                JSONObject item = payload.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(HistoryProvider.DURATION, item.getString("duration"));
                values.put(HistoryProvider.TERMIN, item.getString("termin"));
                values.put(HistoryProvider.DIRECTION, item.getString("direction"));
                values.put(HistoryProvider.FULLNAME, item.getString("fullname"));
                values.put(HistoryProvider.TS, item.getString("ts"));
                getContentResolver().insert(HistoryProvider.CONTENT_URI, values);
                values.clear();
            }
        } catch (JSONException e) {
            Log.d(TAG, "Could not parse incoming history payload");
            return JSON_EXCEPTION;
        }
        return HISTORY_LOADED;
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
                    if (JSONParserHelper.getCalledIdNum(line).equals(
                            SettingsActivity.getMobileNumber(this))) {
                        parseMyMobilePhoneUpdate(line);
                    }
                }
            } else { // Not using mobile
                if (JSONMessageFactory.checkIdMatch(line, astId, xivoId)) {
                    parseMyPhoneUpdate(line);
                    try {
                        sendMyNewHintstatus(line.getJSONObject("status")
                                .getJSONObject("hintstatus"));
                    } catch (JSONException e) {
                        // No update to send if there's no status or hintstatus
                    }
                }
            }
        }
        
        /*
         * For all updates
         */
        try {
            long id = UserProvider.getUserId(this, line.getString("astid"),
                    line.getJSONObject("status").getString("id"));
            if (id > 0)
                return updateUserHintStatus(id,
                        line.getJSONObject("status").getJSONObject("hintstatus"));
        } catch (JSONException e) {
            Log.d(TAG, "Could not find and astid and an id for this update");
        }
        return NO_MESSAGE;
    }
    
    /**
     * Sends my new phone status
     * @param hintstatus
     * @throws JSONException
     */
    private void sendMyNewHintstatus(JSONObject hintstatus) throws JSONException {
        phoneStatusCode = hintstatus.getString("code");
        phoneStatusColor = hintstatus.getString("color");
        phoneStatusLongname = hintstatus.getString("longname");
        Intent i = new Intent();
        i.setAction(Constants.ACTION_MY_PHONE_CHANGE);
        i.putExtra("color", phoneStatusColor);
        i.putExtra("longname", phoneStatusLongname);
        sendBroadcast(i);
        if (this.phoneStatusCode.equals(Constants.AVAILABLE_STATUS_CODE))
            resetChannels();
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
        try {
            JSONObject comms = line.getJSONObject("status").getJSONObject("comms");
            for (@SuppressWarnings("unchecked") Iterator<String> iterKey = comms.keys();
                    iterKey.hasNext(); ) {
                String key = iterKey.next();
                JSONObject comm = comms.getJSONObject(key);
                if (comm.has("thischannel"))
                    thisChannel = comm.getString("thischannel");
                if (comm.has("peerchannel"))
                    peerChannel = comm.getString("peerchannel");
            }
        } catch (JSONException e) {
            Log.d(TAG, "Could not parse channels");
        }
    }
    
    /**
     * Updates channels from the information contained in a given comm.
     * @param comm -- JSON comm
     * @param myChannel 
     *      True if the update is coming for my userid
     *      False if it's the peer's update
     */
    private void updateChannels(final JSONObject comm, final boolean myChannel) {
        Log.d(TAG, "Updating channels");
        try {
            if (comm.has("thischannel")) {
                if (myChannel) {
                    if (thisChannel != null && !thisChannel.startsWith("Local/")) {
                        oldChannel = thisChannel;
                    }
                    thisChannel = comm.getString("thischannel");
                } else {
                    peerChannel = comm.getString("thischannel");
                }
            }
            
            if (comm.has("peerchannel")) {
                if (myChannel) {
                    peerChannel = comm.getString("peerchannel");
                } else {
                    if (thisChannel != null && !thisChannel.startsWith("Local/")) {
                        oldChannel = thisChannel;
                    }
                    thisChannel = comm.getString("peerchannel");
                }
            }
        } catch (JSONException e) {
            // We can ignore this exception since we tested with .has before getting
        }
    }
    
    /**
     * Parses phones for a call from the user's mobile
     * @param line
     */
    private void parseMyMobilePhoneUpdate(JSONObject line) {
        Log.d(TAG, "Parsing my mobile phone update");
        List<JSONObject> comms = JSONParserHelper.getMyComms(this, line);
        for (JSONObject comm: comms) {
            String status = JSONParserHelper.getChannelStatus(comm);
            if (status.equals("ringing") || status.equals("linked-called")) {
                updateChannels(comm, false);
            }
        }
        for (JSONObject comm: comms) {
            if (JSONParserHelper.channelsMatch(comm, peerChannel, thisChannel)) {
                try {
                    if (line.getJSONObject("status").getJSONObject("hintstatus")
                            .getString("code").equals(Constants.CALLING_STATUS_CODE)) {
                        Intent iOnGoingCall = new Intent();
                        iOnGoingCall.setAction(Constants.ACTION_ONGOING_CALL);
                        sendBroadcast(iOnGoingCall);
                    }
                } catch (JSONException e) {
                    // Nothing to do if there's no status or hintstatus
                }
                if (JSONParserHelper.getChannelStatus(comm).equals("hangup")) {
                    resetChannels();
                    Intent iStatusUpdate = new Intent();
                    iStatusUpdate.setAction(Constants.ACTION_CALL_PROGRESS);
                    iStatusUpdate.putExtra("code", Constants.AVAILABLE_STATUS_CODE);
                    sendBroadcast(iStatusUpdate);
                }
            }
        }
    }
    
    /**
     * Parses the phone list received at the beginning of a session.
     * @param line
     * @return Message to the handler
     */
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
        for (@SuppressWarnings("unchecked") Iterator<String> keyIter = payloads.keys();
                keyIter.hasNext(); ) {
            try {
                jAllPhones.put(payloads.getJSONObject(keyIter.next()));
            } catch (JSONException e) {
                Log.e(TAG, "Could not retrieve phone from payload");
                return JSON_EXCEPTION;
            }
        }
        
        // logAllPhones(jAllPhones);
        
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
    
    @SuppressWarnings("unused")
    private void logAllPhones(JSONArray list) {
        try {
            for (int i = 0; i < list.length(); i++) {
                Log.d(TAG, "XiVO #" + i + "\n---------------------------------------");
                JSONObject xivo = list.getJSONObject(i);
                @SuppressWarnings("unchecked")
                Iterator<String> iterator = xivo.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Log.d(TAG, key + ": " + xivo.getJSONObject(key).toString());
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "JSON exception while logging phones");
        }
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
                phoneStatusCode = jPhoneStatus.getString("code");
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
                    if (userId.equals(this.userId)) {
                        fullname = jUser.getString("fullname");
                        Intent iUpdateIdentity = new Intent();
                        iUpdateIdentity.setAction(Constants.ACTION_UPDATE_IDENTITY);
                        iUpdateIdentity.putExtra("fullname", fullname);
                        sendBroadcast(iUpdateIdentity);
                        if (jUser.has("mwi")) {
                            JSONArray mwi = jUser.getJSONArray("mwi");
                            int lenmwi = mwi != null ? mwi.length() : 0;
                            for (int j = 0; j < lenmwi; j++) {
                                this.mwi[j] = mwi.getInt(j);
                            }
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
        try {
            String id = line.getString("xivo_userid");
            String astid = line.getString("astid");
            String stateid = line.getJSONObject("capapresence").getJSONObject("state")
                    .getString("stateid");
            this.stateId = CapapresenceProvider.getIndex(this, stateid);
            long index = UserProvider.getIndex(this, astid, id);
            UserProvider.updatePresence(this, index, stateid);
            if (id.equals(xivoId) && astid.equals(astId)) {
                Intent iUpdate = new Intent();
                iUpdate.setAction(Constants.ACTION_MY_STATUS_CHANGE);
                iUpdate.putExtra("id", this.stateId);
                sendBroadcast(iUpdate);
            }
        } catch (JSONException e) {
            Log.d(TAG, "Failed to update presence");
            return NO_MESSAGE;
        }
        return PRESENCE_UPDATE;
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
                authenticated = true;
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
        } catch (JSONException e) {
            return Constants.JSON_POPULATE_ERROR;
        }
        return Constants.OK;
    }
    
    /**
     * Parses the incoming capapresence message and update the DB
     */
    private void parseCapapresence(JSONObject jPresence) {
        /*
         * Fill the DB
         */
        Log.d(TAG, "Parsing capapresence");
        getContentResolver().delete(CapapresenceProvider.CONTENT_URI, null, null);
        try {
            ContentValues presence = new ContentValues();
            for (@SuppressWarnings("unchecked") Iterator<String> keyIter =
                    jPresence.getJSONObject("names").keys(); keyIter.hasNext(); ) {
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
        Intent i = new Intent();
        i.setAction(Constants.ACTION_LOAD_XLETS);
    }
    
    /**
     * Reset information about a session
     */
    private void resetState() {
        xivoId = null;
        authenticated = false;
        sessionId = null;
        astId = null;
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
            if (ctiAnswer != null && ctiAnswer.has("class") &&
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
            if (jsonObject != null && jsonObject.has("errorstring")) {
                String error = jsonObject.getString("errorstring");
                if (error.equals(Constants.XIVO_LOGIN_PASSWORD)
                        || error.equals(Constants.XIVO_LOGIN_UNKNOWN_USER)) {
                    return Constants.LOGIN_PASSWORD_ERROR;
                } else if (error.contains(Constants.XIVO_CTI_VERSION_NOT_SUPPORTED)) {
                    return Constants.CTI_SERVER_NOT_SUPPORTED;
                } else if (error.equals(Constants.XIVO_VERSION_NOT_COMPATIBLE)) {
                    return Constants.VERSION_MISMATCH;
                }
            } else if (jsonObject != null && jsonObject.has("class")
                    && jsonObject.getString("class").equals("disconn")) {
                lostConnectionEvent();
                return Constants.FORCED_DISCONNECT;
            }
        } catch (JSONException e) {
            Log.d(TAG, "JSON exception while parsing error string");
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
                    Log.e(TAG, "readJsonObjectCTI error");
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
        Log.d(TAG, "lostConnectionEvent");
        disconnectFromServer();
        // TODO: Send an intent to warn activities about the connection lost
        // TODO: try to reconnect
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
        if (line == null || line.equals("")) {
            return Constants.OK;
        }
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
    
    /**
     * Clears all channels
     */
    private void resetChannels() {
        if (thisChannel != null && !thisChannel.startsWith("Local/")) {
            oldChannel = thisChannel;
        }
        thisChannel = null;
        peerChannel = null;
        lastCalledNumber = null;
    }
    
    private class IntentReceiver extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Constants.ACTION_SETTINGS_CHANGE)
                    || action.equals(ConnectivityManager.CONNECTIVITY_ACTION)
                    || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                wrongHostPort = false;
            }
        }
        
    }
}
