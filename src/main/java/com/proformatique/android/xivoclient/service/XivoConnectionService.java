package com.proformatique.android.xivoclient.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xivo.cti.message.CtiResponseMessage;
import org.xivo.cti.model.UserStatus;
import org.xivo.cti.model.Xlet;
import org.xivo.cti.model.XiVOCall;
import org.xivo.cti.MessageDispatcher;
import org.xivo.cti.MessageFactory;
import org.xivo.cti.MessageParser;
import org.xivo.cti.listener.CallHistoryListener;
import org.xivo.cti.listener.UserIdsListener;
import org.xivo.cti.message.CallHistoryReply;
import org.xivo.cti.message.LoginCapasAck;
import org.xivo.cti.message.LoginIdAck;
import org.xivo.cti.message.LoginPassAck;
import org.xivo.cti.message.PhoneConfigUpdate;
import org.xivo.cti.message.UserConfigUpdate;
import org.xivo.cti.message.UserIdsList;
import org.xivo.cti.message.UserStatusUpdate;
import org.xivo.cti.message.PhoneStatusUpdate;
import org.xivo.cti.network.XiVOLink;

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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.SettingsActivity;
import com.proformatique.android.xivoclient.XivoNotification;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.JSONMessageFactory;

public class XivoConnectionService extends Service implements CallHistoryListener, UserIdsListener, XiVOLink {

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
    private DisconnectTask disconnectTask = null;

    // Informations that is relevant to a specific connection
    private String sessionId = null;
    private String xivoId = null;
    private String astId = null;
    private String userId = null;
    private String fullname = null;
    private String mNumber = null;
    private final int[] mwi = new int[3];
    private int capaId = 0;
    // private long stateId = 0L;
    private String phoneStatusLongname = null;
    private String phoneStatusColor = Constants.DEFAULT_HINT_COLOR;
    private String lastCalledNumber = null;
    private String thisChannel = null;
    private String peerChannel = null;
    private String oldChannel = null;
    private boolean wrongHostPort = false;
    private boolean connected = false;
    private boolean connecting = false;
    private boolean authenticated = false;
    private boolean authenticating = false;

    private final MessageParser messageParser;
    private final MessageFactory messageFactory;
    private final MessageDispatcher messageDispatcher;
    private final UserUpdateManager userUpdateManager;

    /**
     * Messages to return from the main loop to the handler
     */
    public enum Messages {
        NO_MESSAGE(0), NO_CLASS(1), DISCONNECT(2), UNKNOWN(3), JSON_EXCEPTION(4), USERS_LIST_COMPLETE(5), PHONES_LOADED(
                6), PRESENCE_UPDATE(7), HISTORY_LOADED(8), FEATURES_LOADED(9);

        private int id;
        private static final Map<Integer, Messages> lookup = new HashMap<Integer, Messages>();
        static {
            for (Messages m : EnumSet.allOf(Messages.class)) {
                lookup.put(m.getId(), m);
            }
        }

        public int getId() {
            return id;
        }

        private Messages(int id) {
            this.id = id;
        }

        public static Messages get(int id) {
            return lookup.get(id);
        }
    }

    public XivoConnectionService() {
        messageParser = new MessageParser();
        messageFactory = new MessageFactory();
        messageDispatcher = new MessageDispatcher();
        userUpdateManager = new UserUpdateManager(this);
        userUpdateManager.setXivoLink(this);
        addDispatchers();
    }

    private void addDispatchers() {
        messageDispatcher.addListener(UserStatusUpdate.class, userUpdateManager);
        messageDispatcher.addListener(UserConfigUpdate.class, userUpdateManager);
        messageDispatcher.addListener(PhoneConfigUpdate.class, userUpdateManager);
        messageDispatcher.addListener(PhoneStatusUpdate.class, userUpdateManager);
        messageDispatcher.addListener(CallHistoryReply.class, this);
        messageDispatcher.addListener(UserIdsList.class, this);
    }

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
            return (networkConnection != null && networkConnection.isConnected() && inputBuffer != null && (disconnectTask == null || disconnectTask
                    .getStatus() != AsyncTask.Status.RUNNING));
        }

        @Override
        public int authenticate() throws RemoteException {
            return XivoConnectionService.this.authenticate();
        }

        @Override
        public boolean isAuthenticated() throws RemoteException {
            return authenticated && this.isConnected();
        }

        @Override
        public void loadData() throws RemoteException {
            getContentResolver().delete(UserProvider.CONTENT_URI, null, null);
            XivoConnectionService.this.refreshFeatures();
            XivoConnectionService.this.refreshHistory();
            XivoConnectionService.this.loadList("users");
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
            return userUpdateManager.getStateId();
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
                return thisChannel != null;
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
            return SettingsActivity.getUseMobile(XivoConnectionService.this) && lastCalledNumber != null
                    && lastCalledNumber.length() < Constants.MAX_PHONE_NUMBER_LEN;
        }

        @Override
        public String getAstId() throws RemoteException {
            return XivoConnectionService.this.astId;
        }

        @Override
        public String getXivoId() throws RemoteException {
            return XivoConnectionService.this.xivoId;
        }

        @Override
        public void setFullname(String fullname) throws RemoteException {
            XivoConnectionService.this.fullname = fullname;
        }
    };

    /**
     * Check the phone status returns true if it's off hook
     *
     * @return
     */
    private boolean isMobileOffHook() {
        switch (((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getCallState()) {
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
            Log.d(TAG, "Cannot atxfer from a null. this = " + thisChannel);
        }
    }

    /**
     * Hang-up the current call if AndroidTools.hangup did not work If we have a
     * Local/ channel we hang-up our peer's channel
     */
    private void hangup() {
        String channel = null;
        if (thisChannel != null) {
            channel = "chan:" + astId + "/" + xivoId + ":" + thisChannel;
        } else if (peerChannel != null) {
            channel = "chan:" + astId + "/" + xivoId + ":" + peerChannel;
        } else if (oldChannel != null) {
            channel = "chan:" + astId + "/" + xivoId + ":" + oldChannel;
        } else {
            Log.d(TAG, "Can't hang-up from the service");
            return;
        }
        sendLine(JSONMessageFactory.createJsonHangupObject(this, channel).toString());
        resetChannels();
    }

    /**
     * Change a feature value
     *
     * @param feature
     * @param value
     * @param phone
     */
    private void sendFeature(String feature, String value, String phone) {
        sendLine(JSONMessageFactory.createJsonFeaturePut(astId, xivoId, feature, value, phone).toString());
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
     *
     * @param number
     * @return OK or error code
     */
    private int call(String number) {
        Log.d(TAG, "Calling " + number);
        lastCalledNumber = number;
        JSONObject jCall = JSONMessageFactory.getJsonCallingObject("originate",
                SettingsActivity.getMobileNumber(getApplicationContext()), number);
        sendLine(jCall.toString());
        return Constants.OK;
    }

    /**
     * Asks the CTI server for a list
     */
    private void loadList(String list) {
        if (thread == null)
            startLooping(getApplicationContext());
        JSONObject query = messageFactory.createGetUsersList();
        sendMessage(query);
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

    private void autoLogin() {
        Log.d(TAG, "Trying to auto-logon");
        if (!connected)
            connect();
        if (connected && !authenticated)
            authenticate();
        if (connected && authenticated)
            refreshLists();
    }

    private void refreshLists() {
        XivoConnectionService.this.refreshFeatures();
        XivoConnectionService.this.refreshHistory();
        XivoConnectionService.this.loadList("users");
    }

    private int authenticate() {
        authenticated = false;
        if (authenticating)
            return Constants.ALREADY_AUTHENTICATING;
        authenticating = true;
        int res = loginCTI();
        authenticating = false;
        if (res == Constants.AUTHENTICATION_OK)
            authenticated = true;
        return res;
    }

    private int connect() {
        connected = false;
        if (connecting == true)
            return Constants.ALREADY_CONNECTING;
        while (disconnectTask != null && disconnectTask.getStatus() == AsyncTask.Status.RUNNING) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Log.d(TAG, "Got interrupted while waiting for the running disconnection");
                e.printStackTrace();
            }
        }
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
        connecting = false;
        authenticated = false;
        authenticating = false;
        if (inputBuffer != null) {
            @SuppressWarnings("unused")
            BufferedReader tmp = inputBuffer;
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
                if (networkConnection != null) {
                    Socket tmp = networkConnection;
                    tmp.close();
                    networkConnection = null;
                }
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
     *
     * @return connection status
     */
    private int connectToServer() {
        if (wrongHostPort)
            return Constants.BAD_HOST;
        resetChannels();
        authenticated = false;
        int port = Constants.XIVO_DEFAULT_PORT;
        try {
            port = Integer.parseInt(prefs.getString("server_port", Integer.toString(Constants.XIVO_DEFAULT_PORT)));
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
            return Constants.CONNECTION_REFUSED;
        }
        bytesReceived = 0L;
        xivoNotif = new XivoNotification(getApplicationContext());
        xivoNotif.createNotification();
        Log.d(TAG, "connected .....");
        try {
            inputBuffer = new BufferedReader(new InputStreamReader(networkConnection.getInputStream()));

        } catch (IOException e) {
            return Constants.NO_NETWORK_AVAILABLE;
        }
        return Constants.CONNECTION_OK;
    }

    private int disconnectFromServer() {
        Log.d(TAG, "Disconnecting");
        if (xivoNotif != null)
            xivoNotif.removeNotif();
        if (!(disconnectTask != null && disconnectTask.getStatus() == AsyncTask.Status.RUNNING)) {
            disconnectTask = new DisconnectTask();
            disconnectTask.execute();
        }
        return Constants.OK;
    }

    private class DisconnectTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            stopThread();
            connectionCleanup();
            resetState();
            return 0;
        }
    }

    private void refreshFeatures() {
        if (thread == null)
            startLooping(getApplicationContext());
        sendLine(JSONMessageFactory.getJsonFeaturesRefresh(astId, xivoId).toString());
    }

    /**
     * Perform authentication on the XiVO CTI server
     *
     * @return error or success code
     */
    private int loginCTI() {
        int res;

        /**
         * Creating first Json login array First step : check that login is
         * allowed on server
         */
        JSONObject jLogin = messageFactory.createLoginId(SettingsActivity.getLogin(this), "android-"
                + android.os.Build.VERSION.RELEASE);
        res = sendLoginCTI(jLogin);
        if (res != Constants.OK)
            return res;

        /**
         * Second step : check that password is allowed on server
         */
        res = sendPasswordCTI();
        if (res != Constants.OK)
            return res;

        /**
         * Third step : send configuration options on server
         */
        return sendCapasCTI();
    }

    /**
     * Starts the main loop to listen to incoming JSON messages from the CTI
     * server
     */
    private void startLooping(Context context) {
        cancel = false;
        handler = new Handler() {
            /**
             * Receives messages from the json loop and broadcast intents
             * accordingly.
             */
            @Override
            public void handleMessage(Message msg) {
                switch (Messages.get(msg.what)) {
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
            @Override
            public void run() {
                Looper.prepare();
                JSONObject newLine = null;
                Log.d(TAG, "Starting the main loop");
                while (!(cancel)) {
                    while ((newLine = readJsonObjectCTI()) == null && !(cancel))
                        ;
                    handler.sendEmptyMessage(parseIncomingJson(newLine).getId());
                }
            };
        };
        thread.start();
    }

    /**
     * Parses an incoming json message from the cti server and dispatches it to
     * other methods for better message handling.
     *
     * @return
     */
    protected Messages parseIncomingJson(JSONObject line) {
        if (cancel || line == null)
            return Messages.NO_MESSAGE;
        try {
            CtiResponseMessage<?> ctiResponseMessage = messageParser.parse(line);
            messageDispatcher.dispatch(ctiResponseMessage);
        } catch (JSONException e1) {
            e1.printStackTrace();
            Log.d(TAG, "unable to decode message received");
        } catch (IllegalArgumentException e2) {
            Log.d(TAG, "not decoded message received");
        }
        try {
            String classRec = line.has("class") ? line.getString("class") : null;
            if (classRec == null)
                return Messages.NO_CLASS;
            if (classRec.equals("presence"))
                return parsePresence(line);
            else if (classRec.equals("phones"))
                return parsePhones(line);
            else if (classRec.equals("features"))
                return parseFeatures(line);
            else if (classRec.equals("groups"))
                return JsonParserHelper.parseGroups(XivoConnectionService.this, line);
            else if (classRec.equals("disconn"))
                return Messages.DISCONNECT;
            // Sheets are ignored at the moment
            else if (classRec.equals("sheet"))
                return Messages.NO_MESSAGE;
            else if (classRec.equals("meetme"))
                return JsonParserHelper.parseMeetme(XivoConnectionService.this, line);
            else {
                Log.d(TAG, "Unknown classrec: " + classRec);
                return Messages.UNKNOWN;
            }
        } catch (JSONException e) {
            Log.d(TAG, "Unhandled JSONException in the main loop.\n"
                    + "Important exceptions are checked before this point");
            e.printStackTrace();
            return Messages.NO_MESSAGE;
        }
    }

    private Messages parseFeatures(JSONObject line) throws JSONException {
        Log.d(TAG, "Parsing features:\n" + line.toString());
        if (!line.has("payload"))
            return Messages.NO_MESSAGE;
        final String[] features = { "enablednd", "callrecord", "incallfilter", "enablevoicemail", "busy", "rna", "unc" };
        JSONObject payload = line.getJSONObject("payload");
        if (line.has("function") && line.getString("function").equals("put")) {
            for (@SuppressWarnings("unchecked")
            Iterator<String> keyIter = payload.keys(); keyIter.hasNext();) {
                String feature = keyIter.next();
                JSONObject jFeature = payload.getJSONObject(feature);
                ContentValues values = new ContentValues();
                values.put(CapaservicesProvider.ENABLED, jFeature.has("enabled")
                        && jFeature.getBoolean("enabled") == true ? 1 : 0);
                if (jFeature.has("number")) {
                    values.put(CapaservicesProvider.NUMBER, jFeature.getString("number"));
                }
                if (!feature.equals("enablednd") && !feature.equals("enablevoicemail"))
                    feature = feature.replace("enable", "");
                getContentResolver().update(CapaservicesProvider.CONTENT_URI, values,
                        CapaservicesProvider.SERVICE + " = '" + feature + "'", null);
                values.clear();
            }
        } else {
            getContentResolver().delete(CapaservicesProvider.CONTENT_URI, null, null);
            for (String feature : features) {
                if (payload.has(feature)) {
                    JSONObject jFeature = payload.getJSONObject(feature);
                    ContentValues values = new ContentValues();
                    values.put(CapaservicesProvider.SERVICE, feature);
                    values.put(CapaservicesProvider.ENABLED,
                            (jFeature.has("enabled") && jFeature.getBoolean("enabled")) == true ? 1 : 0);
                    if (jFeature.has("number")) {
                        values.put(CapaservicesProvider.NUMBER, jFeature.getString("number"));
                    }
                    getContentResolver().insert(CapaservicesProvider.CONTENT_URI, values);
                    values.clear();
                }
            }
        }
        return Messages.FEATURES_LOADED;
    }

    /**
     * Parses incoming messages from the CTI server with class phones
     *
     * @param line
     * @return msg to the handler
     * @throws JSONException
     */
    private Messages parsePhones(JSONObject line) throws JSONException {
        if (line.has("function")) {
            String function = line.getString("function");
            if (function.equals("sendlist")) {
                return parsePhoneList(line);
            } else if (function.equals("update")) {
                return parsePhoneUpdate(line);
            }
        }
        return Messages.NO_MESSAGE;
    }

    /**
     * Parses phone updates
     *
     * @param line
     * @return Message to the handler
     * @throws JSONException
     */
    private Messages parsePhoneUpdate(JSONObject line) throws JSONException {
        /*
         * Check if the update concerns a call I'm doing
         */
        if (lastCalledNumber != null) {
            String number = SettingsActivity.getUseMobile(this) ? SettingsActivity.getMobileNumber(this) : this.mNumber;
            String callerIdNum = JsonParserHelper.getCallerIdNum(line);
            if (number != null && number.equals(callerIdNum)) {
                if (SettingsActivity.getUseMobile(this))
                    parseMyMobilePhoneUpdate(line);
                updatePeerStatus(line);
            }
        }

        if (JSONMessageFactory.checkIdMatch(line, astId, xivoId)) {
            parseMyPhoneUpdate(line);
            try {
                sendMyNewHintstatus(line.getJSONObject("status").getJSONObject("hintstatus"));
            } catch (JSONException e) {
                // No update to send if there's no status or hintstatus
            }
        }
        /*
         * For all updates
         */
        try {
            long id = UserProvider.getUserId(this, line.getString("astid"), line.getJSONObject("status")
                    .getString("id"));
            if (id > 0)
                return updateUserHintStatus(id, line.getJSONObject("status").getJSONObject("hintstatus"));
        } catch (JSONException e) {
            Log.d(TAG, "Could not find and astid and an id for this update");
        }
        return Messages.NO_MESSAGE;
    }

    /**
     * Sends my new phone status
     *
     * @param hintstatus
     * @throws JSONException
     */
    private void sendMyNewHintstatus(JSONObject hintstatus) throws JSONException {
        phoneStatusColor = hintstatus.getString("color");
        phoneStatusLongname = hintstatus.getString("longname");
        Intent i = new Intent();
        i.setAction(Constants.ACTION_MY_PHONE_CHANGE);
        i.putExtra("color", phoneStatusColor);
        i.putExtra("longname", phoneStatusLongname);
        sendBroadcast(i);
    }

    /**
     * Updates a user hintstatus and sends an update intent
     *
     * @param id
     * @param hintstatus
     * @return
     * @throws JSONException
     */
    private Messages updateUserHintStatus(long id, JSONObject hintstatus) throws JSONException {
        ContentValues values = new ContentValues();
        values.put(UserProvider.HINTSTATUS_CODE, hintstatus.getString("code"));
        values.put(UserProvider.HINTSTATUS_COLOR, hintstatus.getString("color"));
        values.put(UserProvider.HINTSTATUS_LONGNAME, hintstatus.getString("longname"));
        getContentResolver().update(Uri.parse(UserProvider.CONTENT_URI + "/" + id), values, null, null);
        Intent iUpdateIntent = new Intent();
        iUpdateIntent.setAction(Constants.ACTION_LOAD_USER_LIST);
        iUpdateIntent.putExtra("id", id);
        sendBroadcast(iUpdateIntent);
        Log.d(TAG, "Update intent sent");
        return Messages.NO_MESSAGE;
    }

    /**
     * Parses phones update for a call from the user (not using his mobile)
     *
     * @param line
     * @throws JSONException
     */
    private void parseMyPhoneUpdate(JSONObject line) throws JSONException {
        Log.d(TAG, "Parsing my phone update");
        if (!line.has("status"))
            return;
        JSONObject status = line.getJSONObject("status");
        /*
         * Sends the new call progress status
         */
        if (status.has("hintstatus")) {
            JSONObject hintstatus = status.getJSONObject("hintstatus");
            if (hintstatus.has("longname") && hintstatus.has("code")) {
                Intent iStatusUpdate = new Intent();
                iStatusUpdate.setAction(Constants.ACTION_CALL_PROGRESS);
                iStatusUpdate.putExtra("status", hintstatus.getString("longname"));
                iStatusUpdate.putExtra("code", hintstatus.getString("code"));
                sendBroadcast(iStatusUpdate);
            }
        }
        /*
         * Save channels
         */
        if (status.has("comms") && !SettingsActivity.getUseMobile(this)) {
            JSONObject comms = status.getJSONObject("comms");
            for (@SuppressWarnings("unchecked")
            Iterator<String> iterKey = comms.keys(); iterKey.hasNext();) {
                String key = iterKey.next();
                JSONObject comm = comms.getJSONObject(key);
                if (comm.has("thischannel"))
                    thisChannel = comm.getString("thischannel");
                if (comm.has("peerchannel"))
                    peerChannel = comm.getString("peerchannel");
            }
        }
    }

    /**
     * Updates channels from the information contained in a given comm.
     *
     * @param comm
     *            -- JSON comm
     * @param myChannel
     *            True if the update is coming for my userid False if it's the
     *            peer's update
     * @throws JSONException
     */
    private void updateChannels(final JSONObject comm, final boolean myChannel) throws JSONException {
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
        Log.d(TAG, "Updating channels: this = " + thisChannel + " peer = " + peerChannel);
    }

    /**
     * Parses our peer's phone update and send an intent containing his new
     * hintstatus
     *
     * @param line
     * @throws JSONException
     */
    private void updatePeerStatus(JSONObject line) throws JSONException {
        if (line.has("status") && line.getJSONObject("status").has("comms")) {
            JSONObject comms = line.getJSONObject("status").getJSONObject("comms");
            for (@SuppressWarnings("unchecked")
            Iterator<String> iter = comms.keys(); iter.hasNext();) {
                String key = iter.next();
                if (comms.getJSONObject(key).has("status")) {
                    String status = comms.getJSONObject(key).getString("status");
                    Intent iPeerStatusUpdate = new Intent();
                    iPeerStatusUpdate.setAction(Constants.ACTION_UPDATE_PEER_HINTSTATUS);
                    iPeerStatusUpdate.putExtra("status", status);
                    sendBroadcast(iPeerStatusUpdate);
                    return;
                }
            }
        }
    }

    /**
     * Parses phones for a call from the user's mobile
     *
     * @param line
     * @throws JSONException
     */
    private void parseMyMobilePhoneUpdate(JSONObject line) throws JSONException {
        List<JSONObject> comms = JsonParserHelper.getMyComms(this, line);
        for (JSONObject comm : comms) {
            String status = JsonParserHelper.getChannelStatus(comm);
            if (status.equals("ringing") || status.equals("linked-called")) {
                updateChannels(comm, false);
            }
        }
        for (JSONObject comm : comms) {
            if (JsonParserHelper.channelsMatch(comm, peerChannel, thisChannel)) {
                try {
                    if (line.getJSONObject("status").getJSONObject("hintstatus").getString("code")
                            .equals(Constants.CALLING_STATUS_CODE)) {
                        Intent iOnGoingCall = new Intent();
                        iOnGoingCall.setAction(Constants.ACTION_ONGOING_CALL);
                        sendBroadcast(iOnGoingCall);
                    }
                } catch (JSONException e) {
                    // Nothing to do if there's no status or hintstatus
                }
                if (JsonParserHelper.getChannelStatus(comm).equals("hangup")) {
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
     *
     * @param line
     * @return Message to the handler
     * @throws JSONException
     */
    private Messages parsePhoneList(JSONObject line) throws JSONException {
        Log.d(TAG, "Parsing phone list");
        if (line.has("payload") == false)
            return Messages.NO_MESSAGE;
        JSONObject payloads = line.getJSONObject("payload");
        JSONArray jAllPhones = new JSONArray();
        for (@SuppressWarnings("unchecked")
        Iterator<String> keyIter = payloads.keys(); keyIter.hasNext();) {
            jAllPhones.put(payloads.getJSONObject(keyIter.next()));
        }

        // logAllPhones(jAllPhones);

        /**
         * For each users in the userslist, find the corresponding phone and
         * update the user's status
         */
        Cursor user = getContentResolver().query(UserProvider.CONTENT_URI, null, null, null, null);
        user.moveToFirst();
        int techlistIndex = user.getColumnIndex(UserProvider.TECHLIST);
        int nbXivo = jAllPhones.length();
        do {
            String techlist = user.getString(techlistIndex);
            for (int i = 0; i < nbXivo; i++) {
                if (jAllPhones.getJSONObject(i).has(techlist)) {
                    setPhoneForUser(user, jAllPhones.getJSONObject(i).getJSONObject(techlist));
                }
            }
        } while (user.moveToNext());
        user.close();
        return Messages.PHONES_LOADED;
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
            getContentResolver().update(Uri.parse(UserProvider.CONTENT_URI + "/" + id), values, null, null);
        } catch (JSONException e) {
            Log.d(TAG, "JSONException, could not update phone status");
        }
        values.clear();

        if (user.getString(user.getColumnIndex(UserProvider.XIVO_USERID)).equals(xivoId)) {
            try {
                mNumber = jPhone.getString("number");
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

    private Messages parsePresence(JSONObject line) {
        Log.d(TAG, "Parsing presences: " + line.toString());
        try {
            String id = line.getString("xivo_userid");
            String astid = line.getString("astid");
            String stateid = line.getJSONObject("capapresence").getJSONObject("state").getString("stateid");
            // TODO this.stateId = CapapresenceProvider.getIndex(this, stateid);
            long index = UserProvider.getIndex(this, astid, id);
            UserProvider.updatePresence(this, index, stateid);
            if (id.equals(xivoId) && astid.equals(astId)) {
                Intent iUpdate = new Intent();
                iUpdate.setAction(Constants.ACTION_MY_STATUS_CHANGE);
                // TODO iUpdate.putExtra("id", this.stateId);
                sendBroadcast(iUpdate);
            }
        } catch (JSONException e) {
            Log.d(TAG, "Failed to update presence");
            return Messages.NO_MESSAGE;
        }
        return Messages.PRESENCE_UPDATE;
    }

    /**
     * Send capacity options to the CTI server
     *
     * @return error or success code
     */
    private int sendCapasCTI() {
        JSONObject jsonCapas = messageFactory.createLoginCapas(capaId);

        int res = sendLine(jsonCapas.toString());
        if (res != Constants.OK)
            return res;

        jsonCapas = readJsonObjectCTI();
        LoginCapasAck loginCapasAck = null;
        try {
            loginCapasAck = (LoginCapasAck) messageParser.parse(jsonCapas);
        } catch (JSONException e1) {
            Log.d(TAG, "Unable to parser login capas ack : " + e1.getMessage());
            return Constants.VERSION_MISMATCH;
        }
        resetState();
        userId = loginCapasAck.userId;
        userUpdateManager.setUserId(Integer.valueOf(userId));
        configureXlets(loginCapasAck.xlets);
        configureUserStatuses(loginCapasAck.capacities.getUsersStatuses());
        userUpdateManager.setCapacities(loginCapasAck.capacities);

        JSONObject jsonCtiMessage = readJsonObjectCTI();
        try {
            CtiResponseMessage<?> ctiResponseMessage = messageParser.parse(jsonCtiMessage);
            messageDispatcher.dispatch(ctiResponseMessage);
        } catch (JSONException e) {
            return Constants.JSON_POPULATE_ERROR;
        }
        jsonCtiMessage = readJsonObjectCTI();
        try {
            CtiResponseMessage<?> ctiResponseMessage = messageParser.parse(jsonCtiMessage);
            messageDispatcher.dispatch(ctiResponseMessage);
        } catch (JSONException e) {
            return Constants.JSON_POPULATE_ERROR;
        }

        authenticated = true;
        return Constants.AUTHENTICATION_OK;

    }

    private void configureUserStatuses(List<UserStatus> userStatuses) {
        Log.d(TAG, "user statuses configuration");
        getContentResolver().delete(CapapresenceProvider.CONTENT_URI, null, null);
        ContentValues presence = new ContentValues();
        for (UserStatus userStatus : userStatuses) {
            presence.put(CapapresenceProvider.NAME, userStatus.getName());
            presence.put(CapapresenceProvider.COLOR, userStatus.getColor());
            presence.put(CapapresenceProvider.LONGNAME, userStatus.getLongName());
            presence.put(CapapresenceProvider.ALLOWED, 1);
            getContentResolver().insert(CapapresenceProvider.CONTENT_URI, presence);
            presence.clear();
        }
    }

    private void configureXlets(List<Xlet> xlets) {
        Log.d(TAG, "Setting xlets");
        // Remove old entries
        getContentResolver().delete(CapaxletsProvider.CONTENT_URI, null, null);
        ContentValues values = new ContentValues();
        for (Xlet xlet : xlets) {
            values.put(CapaxletsProvider.XLET, xlet.getName());
            getContentResolver().insert(CapaxletsProvider.CONTENT_URI, values);
        }
        Intent i = new Intent();
        i.setAction(Constants.ACTION_LOAD_XLETS);
    }

    /**
     * Reset information about a session
     */
    private void resetState() {
        xivoId = null;
        sessionId = null;
        astId = null;
    }

    private int sendPasswordCTI() {
        JSONObject jsonPasswordAuthent = messageFactory.createLoginPass(prefs.getString("password", ""), sessionId);
        int res = sendLine(jsonPasswordAuthent.toString());
        if (res != Constants.OK)
            return res;

        JSONObject ctiAnswer = readJsonObjectCTI();
        try {
            LoginPassAck loginPassAck = (LoginPassAck) messageParser.parse(ctiAnswer);
            capaId = loginPassAck.capalist.get(0);
        } catch (JSONException e1) {
            e1.printStackTrace();
            return Constants.JSON_POPULATE_ERROR;
        } catch (IllegalArgumentException e2) {
            disconnectFromServer();
            return parseLoginError(ctiAnswer);
        }

        return Constants.OK;
    }

    /**
     * Sends login information to the cti server
     *
     * @param jLogin
     * @return error or success code
     */
    private int sendLoginCTI(JSONObject jLogin) {
        JSONObject nextJsonObject;
        int res;
        if ((res = sendLine(jLogin.toString())) != Constants.OK)
            return res;

        if ((nextJsonObject = readJsonObjectCTI()) == null) {
            return Constants.JSON_POPULATE_ERROR;
        }
        try {
            LoginIdAck loginAck = (LoginIdAck) messageParser.parse(nextJsonObject);
            sessionId = loginAck.sesssionId;
        } catch (JSONException e1) {
            Log.d(TAG, "Unexpected answer from cti server");
            e1.printStackTrace();
            return Constants.JSON_POPULATE_ERROR;
        } catch (IllegalArgumentException e2) {
            disconnectFromServer();
            return parseLoginError(nextJsonObject);
        }
        return Constants.OK;
    }

    /**
     * Retrieves the error when the CTI server doesnt return a LOGIN_OK
     *
     * @return error code
     */
    private int parseLoginError(JSONObject jsonObject) {
        try {
            if (jsonObject != null && jsonObject.has("errorstring")) {
                String error = jsonObject.getString("errorstring");
                if (error.equals(Constants.XIVO_LOGIN_PASSWORD) || error.equals(Constants.XIVO_LOGIN_UNKNOWN_USER)) {
                    return Constants.LOGIN_PASSWORD_ERROR;
                } else if (error.contains(Constants.XIVO_CTI_VERSION_NOT_SUPPORTED)) {
                    return Constants.CTI_SERVER_NOT_SUPPORTED;
                } else if (error.equals(Constants.XIVO_VERSION_NOT_COMPATIBLE)) {
                    return Constants.VERSION_MISMATCH;
                }
            } else if (jsonObject != null
                    && jsonObject.has("class")
                    && (jsonObject.getString("class").equals("disconn") || jsonObject.getString("class").equals(
                            "disconnect"))) {
                return Constants.FORCED_DISCONNECT;
            }
        } catch (JSONException e) {
            Log.d(TAG, "JSON exception while parsing error string");
        }
        return Constants.LOGIN_KO;
    }

    /**
     * Perform a read action on the stream from CTI server
     *
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
                } catch (Exception e) {
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
    }

    /**
     * Reads the next incoming line and increment the byte counter and returns
     * the line
     *
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
        Log.d(TAG, "Client >>>> " + line);
        output.println(line);
        return Constants.OK;
    }

    /**
     * Clears all channels
     */
    private void resetChannels() {
        Log.d(TAG, "Reseting channels");
        if (thisChannel != null && !thisChannel.startsWith("Local/")) {
            oldChannel = thisChannel;
        }
        thisChannel = null;
        peerChannel = null;
        lastCalledNumber = null;
    }

    private void sendMessage(JSONObject message) {
        int res;
        if ((res = sendLine(message.toString())) != Constants.OK) {
            Log.d(TAG, "Could not send message");
            switch (res) {
            case Constants.NO_NETWORK_AVAILABLE:
                Log.d(TAG, "No network");
                resetState();
                break;
            }
        }
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

    @Override
    public void onCallHistoryUpdated(List<XiVOCall> callHistory) {
        for (XiVOCall xiVOCall : callHistory) {
            ContentValues values = new ContentValues();
            values.put(HistoryProvider.DURATION, xiVOCall.getDuration());
            values.put(HistoryProvider.TERMIN, "termin");
            values.put(HistoryProvider.DIRECTION, xiVOCall.getCallType().toString());
            values.put(HistoryProvider.FULLNAME, xiVOCall.getFullName());
            values.put(HistoryProvider.TS, xiVOCall.getCallDate());
            this.getApplicationContext().getContentResolver().insert(HistoryProvider.CONTENT_URI, values);
            values.clear();
        }
        Intent iLoadHistory = new Intent();
        iLoadHistory.setAction(Constants.ACTION_LOAD_HISTORY_LIST);
        sendBroadcast(iLoadHistory);
    }

    @Override
    public void onUserIdsLoaded(UserIdsList userIdsList) {
        for (Integer userId : userIdsList.getUserIds()) {
            Log.d(TAG, "User id : " + userId);
            JSONObject getUserMessage = messageFactory.createGetUserConfig(userId);
            sendMessage(getUserMessage);
        }
    }

    @Override
    public void sendGetPhoneConfig(Integer lineId) {
        JSONObject getPhoneConfigMessage = messageFactory.createGetPhoneConfig(lineId);
        sendMessage(getPhoneConfigMessage);
    }

    @Override
    public void sendGetUserStatus(Integer userId) {
        JSONObject jsonGetUserStatusMessage = messageFactory.createGetUserStatus(userId);
        sendMessage(jsonGetUserStatusMessage);
    }

    @Override
    public void sendGetPhoneStatus(Integer lineId) {
        JSONObject jsonGetPhoneStatusMessage = messageFactory.createGetPhoneStatus(lineId);
        sendMessage(jsonGetPhoneStatusMessage);
    }
}
