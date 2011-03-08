/* XiVO Client Android
 * Copyright (C) 2010-2011, Proformatique
 *
 * This file is part of XiVO Client Android.
 *
 * XiVO Client Android is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XiVO Client Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XiVO client Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.proformatique.android.xivoclient;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.TimeoutException;

import com.proformatique.android.xivoclient.service.CapapresenceProvider;
import com.proformatique.android.xivoclient.service.IXivoConnectionService;
import com.proformatique.android.xivoclient.service.UserProvider;
import com.proformatique.android.xivoclient.service.XivoConnectionService;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;
import com.proformatique.android.xivoclient.xlets.XletIdentityStateList;

/**
 * An overloaded Activity class to make UI changes and options consistent across
 * the application
 * 
 * @author Pascal Cadotte-Michaud
 * 
 */
public class XivoActivity extends Activity implements OnClickListener {
    
    private final static String TAG = "XivoActivity";
    private static boolean askedToDisconnect = false;
    private static boolean wrongLoginInfo = false;
    
    private Handler mHandler = new Handler();
    private final static long UPDATE_DELAY = 200L;
    
    /*
     * Service
     */
    protected BindingTask bindingTask = null;
    protected IXivoConnectionService xivoConnectionService = null;
    private ConnectTask connectTask = null;
    private DisconnectTask disconnectTask = null;
    private AuthenticationTask authenticationTask = null;
    private IntentReceiver receiver = null;
    
    private SharedPreferences settings;
    
    /*
     * UI
     */
    private ImageView statusButton;
    private ProgressDialog dialog;
    private FrameLayout status;
    private MenuItem connectButton;
    private TextView longname;
    
    /*
     * Activity lifecycle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        if (settings.getString("login", "").equals("")
                || settings.getString("server_adress", "").equals("")) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        
        if (settings.getBoolean("use_fullscreen", false)) {
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        
        receiver = new IntentReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_MY_STATUS_CHANGE);
        filter.addAction(Constants.ACTION_MY_PHONE_CHANGE);
        filter.addAction(Constants.ACTION_UPDATE_IDENTITY);
        filter.addAction(Constants.ACTION_SETTINGS_CHANGE);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(receiver, new IntentFilter(filter));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mHandler.removeCallbacks(mUpdateIdentity);
        mHandler.postDelayed(mUpdateIdentity, UPDATE_DELAY);
        if (!(settings.getString("login", "").equals("")
                || settings.getString("server_adress", "").equals(""))) {
            startXivoConnectionService();
            bindXivoConnectionService();
        }
    }
    
    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateIdentity);
            mHandler = null;
        }
        super.onDestroy();
    }
    
    /**
     * Called when the binding to the service is completed
     */
    protected void onBindingComplete() {
        Log.d(TAG, "onBindingComplete");
        try {
            updateMyStatus(xivoConnectionService.getStateId());
            updatePhoneStatus(xivoConnectionService.getPhoneStatusColor(), xivoConnectionService
                    .getPhoneStatusLongname());
            updateFullname();
        } catch (RemoteException e) {
            Log.d(TAG, "Could not set my state id");
        }
        if (!askedToDisconnect && !wrongLoginInfo) {
            try {
                waitForConnection();
            } catch (InterruptedException e) {
                Log.d(TAG, "Connection interrupted");
                e.printStackTrace();
            } catch (TimeoutException e) {
                Log.d(TAG, "Connection timedout");
                e.printStackTrace();
            }
        } else {
            setUiEnabled(true);
        }
    }
    
    protected long getPhoneStatusId() {
        if (xivoConnectionService == null) return 0L;
        try {
            String astid = xivoConnectionService.getAstId();
            String xivoId = xivoConnectionService.getXivoId();
            Cursor c = getContentResolver().query(
                    UserProvider.CONTENT_URI,
                    new String[] {
                        UserProvider._ID,
                        UserProvider.ASTID,
                        UserProvider.XIVO_USERID},
                        UserProvider.ASTID + " = '" + astid + "' AND '" + xivoId + "' = " +
                            UserProvider.XIVO_USERID , null, null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                return c.getLong(c.getColumnIndex(UserProvider._ID));
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Failed to retrieve my hintstatus");
        }
        return 0L;
    }
    
    protected void setUiEnabled(boolean state) {
        if (state == false) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
        }
    }
    
    /*
     * GUI
     */
    protected void registerButtons() {
        statusButton = (ImageView) findViewById(R.id.statusContact);
        statusButton.setOnClickListener(this);
        status = (FrameLayout) findViewById(R.id.identityClickZone);
        status.setOnClickListener(this);
        longname = (TextView) findViewById(R.id.user_identity);
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.statusContact:
            if (!this.getClass().getName().equals(HomeActivity.class.getName()))
                startActivity(new Intent(this, HomeActivity.class));
            break;
        case R.id.identityClickZone:
            startActivity(new Intent(this, XletIdentityStateList.class));
            break;
        default:
            break;
        }
    }
    
    /*
     * Menus
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        /**
         * Handle item selection
         */
        switch (item.getItemId()) {
        case R.id.menu_settings:
            menuSettings();
            return true;
        case R.id.menu_about:
            menuAbout();
            return true;
        case R.id.menu_disconnect:
            menuDisconnect();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        connectButton = menu.findItem(R.id.menu_disconnect);
        connectButton.setVisible(true);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            if (isXivoServiceRunning() && xivoConnectionService != null
                    && xivoConnectionService.isAuthenticated()) {
                connectButton.setTitle(R.string.disconnect);
            } else {
                connectButton.setTitle(R.string.connect);
            }
        } catch (RemoteException e) {
        }
        return true;
    }
    
    private boolean isXivoServiceRunning() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> rs = am.getRunningServices(100);
        for (int i = 0; i < rs.size(); i++) {
            ActivityManager.RunningServiceInfo info = rs.get(i);
            if (info.service.getClassName().equals(XivoConnectionService.class.getName())) {
                Log.d(TAG, "Running == true");
                return true;
            }
        }
        Log.d(TAG, "Running == false");
        return false;
    }
    
    private void menuDisconnect() {
        try {
            if (isXivoServiceRunning() && xivoConnectionService != null
                    && xivoConnectionService.isAuthenticated()) {
                askedToDisconnect = true;
                HomeActivity.stopInCallScreenKiller(this);
                stopXivoConnectionService();
            } else {
                askedToDisconnect = false;
                //startXivoConnectionService();
                bindXivoConnectionService();
                HomeActivity.startInCallScreenKiller(this);
            }
        } catch (RemoteException e) {
        }
    }
    
    private void menuAbout() {
        long datareceived = -1L;
        if (xivoConnectionService != null) {
            try {
                datareceived = xivoConnectionService.getReceivedBytes();
            } catch (RemoteException e) {
                datareceived = -1L;
            }
        }
        Intent defineIntent = new Intent(this, AboutActivity.class);
        defineIntent.putExtra("received_data", datareceived);
        startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
    }
    
    private void menuSettings() {
        Intent defineIntent = new Intent(this, SettingsActivity.class);
        startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
    }
    
    /*
     * Service
     */
    protected void disconnect() {
        if (xivoConnectionService != null) {
            try {
                xivoConnectionService.disconnect();
            } catch (RemoteException e) {
                Toast.makeText(this, getString(R.string.remote_exception), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
    
    /**
     * Starts the XivoConnectionService If the service is not started it will
     * get destroyed when our application is destroyed
     */
    private void startXivoConnectionService() {
        Intent iStartXivoService = new Intent();
        iStartXivoService.setClassName(Constants.PACK, XivoConnectionService.class.getName());
        startService(iStartXivoService);
        Log.d(TAG, "Starting XiVO connection service");
    }
    
    /**
     * Stops the XivoConnectionService
     */
    protected void stopXivoConnectionService() {
        disconnectTask = new DisconnectTask();
        disconnectTask.execute();
        Log.d(TAG, "Stopping XiVO connection service");
    }
    
    /**
     * Starts a connection task and wait until it's connected
     * 
     * @throws TimeoutException
     * @throws InterruptedException
     */
    private void waitForConnection() throws InterruptedException, TimeoutException {
        try {
            if (xivoConnectionService != null && xivoConnectionService.isConnected()
                    && xivoConnectionService.isAuthenticated())
                return;
        } catch (RemoteException e) {
            dieOnBindFail();
        }
        connectTask = new ConnectTask();
        connectTask.execute();
    }
    
    /**
     * Starts an authentication task and wait until it's authenticated
     * 
     * @throws TimeoutException
     * @throws InterruptedException
     */
    private void waitForAuthentication() throws InterruptedException, TimeoutException {
        try {
            if (xivoConnectionService == null || !(xivoConnectionService.isConnected())) {
                Log.d(TAG, "Cannot start authenticating if not connected");
                return;
            }
            if (xivoConnectionService != null && xivoConnectionService.isAuthenticated())
                return;
        } catch (RemoteException e) {
            dieOnBindFail();
        }
        authenticationTask = new AuthenticationTask();
        authenticationTask.execute();
    }
    
    /**
     * Check if the service received lists from the CTI server Gets the list if
     * they are not available
     */
    private void startLoading() {
        try {
            if (xivoConnectionService.isAuthenticated()) {
                xivoConnectionService.loadData();
            }
        } catch (RemoteException e) {
            dieOnBindFail();
        }
    }
    
    /**
     * Retrieves our status from the DB and update the header
     * 
     * @param id
     */
    private void updateMyStatus(long id) {
        Log.d(TAG, "updateMyStatus");
        Cursor c = getContentResolver().query(
                CapapresenceProvider.CONTENT_URI,
                new String[] { CapapresenceProvider._ID, CapapresenceProvider.LONGNAME,
                        CapapresenceProvider.COLOR }, CapapresenceProvider._ID + " = " + id, null,
                null);
        if (c.getCount() != 0) {
            c.moveToFirst();
            ((TextView) findViewById(R.id.identity_current_state_longname)).setText(c.getString(c
                    .getColumnIndex(CapapresenceProvider.LONGNAME)));
            GraphicsManager.setIconStateDisplay(this,
                    (ImageView) findViewById(R.id.identity_current_state_image), c.getString(c
                            .getColumnIndex(CapapresenceProvider.COLOR)));
        }
        c.close();
    }
    
    private void updatePhoneStatus(String color, String longname) {
        ((TextView) findViewById(R.id.identityPhoneLongnameState)).setText(longname);
        GraphicsManager.setIconPhoneDisplay(this,
                (ImageView) findViewById(R.id.identityPhoneStatus), color);
    }
    
    /**
     * Update the fullname of the identity bar
     */
    private void updateFullname() {
        String name = null;
        try {
            if (xivoConnectionService == null) {
                name = getString(R.string.user_identity);
            } else if (xivoConnectionService.isAuthenticated()) {
                name = xivoConnectionService.getFullname();
            } else {
                name = xivoConnectionService.getFullname() + "\n"
                    + getString(R.string.disconnected);
            }
        } catch (RemoteException e) {
            // Can be ignored since xivoConnectionService == null is checked
        }
        if (longname != null) longname.setText(name);
    }
    
    /**
     * Binds the XivoConnection service
     */
    private void bindXivoConnectionService() {
        xivoConnectionService = Connection.INSTANCE.getConnection(getApplicationContext());
        if (xivoConnectionService == null) {
            bindingTask = new BindingTask();
            bindingTask.execute();
        } else {
            onBindingComplete();
        }
    }
    
    /**
     * Binds to the service
     */
    protected class BindingTask extends AsyncTask<Void, Void, Integer> {
        
        private final static int OK = 0;
        private final static int FAIL = -1;
        private final static int DELAY = 100;
        private final static int MAX_WAIT = 50;
        
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "Binding started");
            if (dialog == null)
                dialog = new ProgressDialog(XivoActivity.this);
            dialog.setCancelable(true);
            dialog.setMessage(getString(R.string.binding));
            dialog.show();
        }
        
        @Override
        protected Integer doInBackground(Void... params) {
            xivoConnectionService = Connection.INSTANCE.getConnection(getApplicationContext());
            int i = 0;
            while (xivoConnectionService == null && i < MAX_WAIT) {
                timer(DELAY);
                i++;
                xivoConnectionService = Connection.INSTANCE.getConnection(getApplicationContext());
            }
            return xivoConnectionService != null ? OK : FAIL;
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            Log.d(TAG, "Binding finished");
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            if (result == OK) {
                onBindingComplete();
            } else {
                Toast.makeText(XivoActivity.this, getString(R.string.binding_error),
                        Toast.LENGTH_SHORT).show();
            }
        }
        
        private void timer(long milliseconds) {
            try {
                synchronized (this) {
                    this.wait(milliseconds);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Kills the app and display a message when the binding to the service
     * cannot be established ___This should NOT happen___
     */
    private void dieOnBindFail() {
        Toast.makeText(this, getString(R.string.binding_error), Toast.LENGTH_LONG).show();
        Log.e(TAG, "Failed to bind to the service");
        finish();
    }
    
    private class AuthenticationTask extends AsyncTask<Void, Void, Integer> {
        
        @Override
        protected void onPreExecute() {
            if (dialog == null)
                dialog = new ProgressDialog(XivoActivity.this);
            dialog.setCancelable(true);
            dialog.setMessage(getString(R.string.authenticating));
            dialog.show();
        }
        
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                if (xivoConnectionService != null && xivoConnectionService.isAuthenticated())
                    return Constants.AUTHENTICATION_OK;
                int res = xivoConnectionService.authenticate();
                while (res == Constants.ALREADY_AUTHENTICATING) {
                    synchronized (this) {
                        try {
                            wait(100);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Interrupted while waiting for an authentication");
                            e.printStackTrace();
                        }
                    }
                }
                return res;
            } catch (RemoteException e) {
                return Constants.REMOTE_EXCEPTION;
            }
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            if (result != Constants.OK && result != Constants.AUTHENTICATION_OK) {
                try {
                    xivoConnectionService.disconnect();
                } catch (RemoteException e) {
                    Toast.makeText(XivoActivity.this, getString(R.string.remote_exception),
                            Toast.LENGTH_SHORT).show();
                }
            }
            switch (result) {
            case Constants.OK:
            case Constants.AUTHENTICATION_OK:
                Log.i(TAG, "Authenticated");
                setUiEnabled(true);
                startLoading();
                break;
            case Constants.NO_NETWORK_AVAILABLE:
                Toast.makeText(XivoActivity.this, getString(R.string.no_web_connection),
                        Toast.LENGTH_SHORT).show();
                break;
            case Constants.JSON_POPULATE_ERROR:
                Toast.makeText(XivoActivity.this, getString(R.string.login_ko), Toast.LENGTH_LONG)
                        .show();
                break;
            case Constants.FORCED_DISCONNECT:
                Toast.makeText(XivoActivity.this, getString(R.string.forced_disconnect),
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.LOGIN_PASSWORD_ERROR:
                Toast.makeText(XivoActivity.this, getString(R.string.bad_login_password),
                        Toast.LENGTH_LONG).show();
                wrongLoginInfo = true;
                break;
            case Constants.CTI_SERVER_NOT_SUPPORTED:
                Toast.makeText(XivoActivity.this, getString(R.string.cti_not_supported),
                        Toast.LENGTH_LONG).show();
                wrongLoginInfo = true;
                break;
            case Constants.VERSION_MISMATCH:
                Toast.makeText(XivoActivity.this, getString(R.string.version_mismatch),
                        Toast.LENGTH_LONG).show();
                wrongLoginInfo = true;
                break;
            case Constants.ALGORITH_NOT_AVAILABLE:
                Toast.makeText(XivoActivity.this, getString(R.string.algo_exception),
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.ALREADY_AUTHENTICATING:
                Log.d(TAG, "Already authenticating");
                break;
            default:
                Log.e(TAG, "Unhandled result " + result);
                Toast.makeText(XivoActivity.this, getString(R.string.login_ko), Toast.LENGTH_LONG)
                        .show();
                break;
            }
        }
    }
    
    /**
     * Ask to the XivoConnectionService to connect and wait for the result
     */
    private class ConnectTask extends AsyncTask<Void, Void, Integer> {
        
        @Override
        protected void onPreExecute() {
            if (dialog == null)
                dialog = new ProgressDialog(XivoActivity.this);
            dialog.setCancelable(true);
            dialog.setMessage(getString(R.string.connection));
            dialog.show();
        }
        
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                if (xivoConnectionService != null && xivoConnectionService.isConnected())
                    return Constants.CONNECTION_OK;
                return xivoConnectionService.connect();
            } catch (RemoteException e) {
                return Constants.REMOTE_EXCEPTION;
            }
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            switch (result) {
            case Constants.CONNECTION_OK:
                try {
                    waitForAuthentication();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Authentication interrupted");
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    Log.d(TAG, "Authentication timedout");
                    e.printStackTrace();
                }
                break;
            case Constants.CONNECTION_REFUSED:
                Toast.makeText(XivoActivity.this, R.string.connection_refused,
                        Toast.LENGTH_SHORT).show();
                break;
            case Constants.REMOTE_EXCEPTION:
                Toast.makeText(XivoActivity.this, getString(R.string.remote_exception),
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.NOT_CTI_SERVER:
                Toast.makeText(XivoActivity.this, getString(R.string.not_cti_server),
                        Toast.LENGTH_LONG).show();
                wrongLoginInfo = true;
                break;
            case Constants.BAD_HOST:
                Toast.makeText(XivoActivity.this, getString(R.string.bad_host), Toast.LENGTH_LONG)
                        .show();
                wrongLoginInfo = true;
                break;
            case Constants.NO_NETWORK_AVAILABLE:
                Toast.makeText(XivoActivity.this, getString(R.string.no_web_connection),
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.CONNECTION_TIMEDOUT:
                Toast.makeText(XivoActivity.this, getString(R.string.connection_timedout),
                        Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(XivoActivity.this, getString(R.string.connection_failed),
                        Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
    
    private class DisconnectTask extends AsyncTask<Void, Void, Integer> {
        
        @Override
        protected void onPreExecute() {
            if (dialog == null)
                dialog = new ProgressDialog(XivoActivity.this);
            dialog.setCancelable(true);
            dialog.setMessage(getString(R.string.disconnecting));
            dialog.show();
        }
        
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                if (xivoConnectionService != null) xivoConnectionService.disconnect();
            } catch (RemoteException e) {
                Log.d(TAG, "Could not contact the xivo connection service");
            }
            return 0;
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            Log.d(TAG, "Disconnected");
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        }
    }
    
    private class IntentReceiver extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Constants.ACTION_MY_STATUS_CHANGE)) {
                updateMyStatus(intent.getLongExtra("id", 0));
            } else if (action.equals(Constants.ACTION_MY_PHONE_CHANGE)) {
                updatePhoneStatus(intent.getStringExtra("color"),
                        intent.getStringExtra("longname"));
            } else if (action.equals(Constants.ACTION_UPDATE_IDENTITY)) {
                updateFullname();
            } else if (action.equals(Constants.ACTION_SETTINGS_CHANGE)
                    || action.equals(ConnectivityManager.CONNECTIVITY_ACTION)
                    || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                wrongLoginInfo = false;
            }
        }
    }
    
    /**
     * Runnable to update Longname
     */
    private Runnable mUpdateIdentity = new Runnable() {
        
        @Override
        public void run() {
            updateFullname();
            mHandler.postDelayed(mUpdateIdentity, UPDATE_DELAY);
        }
    };
}
