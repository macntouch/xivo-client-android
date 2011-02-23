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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.proformatique.android.xivoclient.service.CapapresenceProvider;
import com.proformatique.android.xivoclient.service.IXivoConnectionService;
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
    
    /*
     * Service
     */
    protected BindingTask bindingTask = null;
    private XivoConnectionServiceConnection con = null;
    protected IXivoConnectionService xivoConnectionService = null;
    private ConnectTask connectTask = null;
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
        registerReceiver(receiver, new IntentFilter(filter));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (!askedToDisconnect) {
            startXivoConnectionService();
            bindXivoConnectionService();
        }
    }
    
    @Override
    protected void onPause() {
        releaseXivoConnectionService();
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        releaseXivoConnectionService();
        unregisterReceiver(receiver);
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
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
            updateFullname(xivoConnectionService.getFullname());
        } catch (RemoteException e) {
            Log.d(TAG, "Could not set my state id");
        }
        launchCTIConnection();
    }
    
    /*
     * GUI
     */
    protected void registerButtons() {
        statusButton = (ImageView) findViewById(R.id.statusContact);
        statusButton.setOnClickListener(this);
        status = (FrameLayout) findViewById(R.id.identityClickZone);
        status.setOnClickListener(this);
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
        case R.id.menu_exit:
            menuExit();
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
                con = null;
            } else {
                askedToDisconnect = false;
                startXivoConnectionService();
                bindXivoConnectionService();
                HomeActivity.startInCallScreenKiller(this);
            }
        } catch (RemoteException e) {
        }
    }
    
    private void menuAbout() {
        Intent defineIntent = new Intent(this, AboutActivity.class);
        startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
    }
    
    private void menuExit() {
        HomeActivity.stopInCallScreenKiller(this);
        finish();
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
    private void stopXivoConnectionService() {
        try {
            xivoConnectionService.disconnect();
        } catch (RemoteException e) {
            // If we are not binded we don't need to unbind anyway
        }
        releaseXivoConnectionService();
        Intent iStopXivoService = new Intent();
        iStopXivoService.setClassName(Constants.PACK, XivoConnectionService.class.getName());
        stopService(iStopXivoService);
        Log.d(TAG, "Stopping XiVO connection service");
    }
    
    /**
     * Makes sure the service is authenticated and that data are loaded
     */
    private void launchCTIConnection() {
        waitForConnection();
        waitForAuthentication();
    }
    
    /**
     * Releases the service before leaving
     */
    private void releaseXivoConnectionService() {
        if (con != null) {
            try {
                unbindService(con);
            } catch (IllegalArgumentException e) {
                // Can't unbind if not binded.
            }
            con = null;
            Log.d(TAG, "XiVO connection service released");
        } else {
            Log.d(TAG, "XiVO connection service not binded");
        }
    }
    
    /**
     * Starts a connection task and wait until it's connected
     */
    private void waitForConnection() {
        try {
            if (xivoConnectionService != null && xivoConnectionService.isConnected()
                    && xivoConnectionService.isAuthenticated())
                return;
        } catch (RemoteException e) {
            dieOnBindFail();
        }
        connectTask = new ConnectTask();
        connectTask.execute();
        try {
            connectTask.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Starts an authentication task and wait until it's authenticated
     */
    private void waitForAuthentication() {
        try {
            if (!(xivoConnectionService.isConnected())) {
                Log.d(TAG, "Cannot start authenticating if not connected");
                return;
            }
            if (xivoConnectionService.isAuthenticated()) return;
        } catch (RemoteException e) {
            dieOnBindFail();
        }
        authenticationTask = new AuthenticationTask();
        authenticationTask.execute();
        try {
            authenticationTask.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            Toast.makeText(this, getString(R.string.authentication_timeout), Toast.LENGTH_SHORT)
                    .show();
        }
    }
    
    /**
     * Check if the service received lists from the CTI server Gets the list if
     * they are not available
     */
    private void startLoading() {
        try {
            if (xivoConnectionService.isAuthenticated()) {
                if (xivoConnectionService.loadDataCalled()) {
                    Log.d(TAG, "Data already loaded");
                    return;
                }
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
    
    private void updateFullname(String name) {
        ((TextView) findViewById(R.id.user_identity)).setText(name);
    }
    
    /**
     * Binds the XivoConnection service
     */
    private void bindXivoConnectionService() {
        bindingTask = new BindingTask();
        bindingTask.execute();
    }
    
    /**
     * Establish a binding between the activity and the XivoConnectionService
     * 
     */
    protected class XivoConnectionServiceConnection implements ServiceConnection {
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            xivoConnectionService = IXivoConnectionService.Stub.asInterface((IBinder) service);
            if (xivoConnectionService == null)
                Log.e(TAG, "xivoConnectionService is null");
            else
                Log.i(TAG, "xivoConnectionService is not null");
            Log.d(TAG, "onServiceConnected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
        }
    };
    
    /**
     * Binds to the service
     */
    protected class BindingTask extends AsyncTask<Void, Void, Integer> {
        
        private final static int OK = 0;
        private final static int FAIL = -1;
        private final static int DELAY = 50;
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
            if (con == null) {
                con = new XivoConnectionServiceConnection();
                Intent iServiceBinder = new Intent();
                iServiceBinder.setClassName(Constants.PACK, XivoConnectionService.class.getName());
                bindService(iServiceBinder, con, Context.BIND_AUTO_CREATE);
                Log.d(TAG, "XiVO connection service binded");
            } else {
                Log.d(TAG, "XiVO connection already binded");
            }
            
            // wait until it's connected...
            int i = 0;
            while (i < MAX_WAIT && (con == null || xivoConnectionService == null)) {
                timer(DELAY);
                i++;
            }
            
            return xivoConnectionService == null ? FAIL : OK;
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
        
        private void timer(int milliseconds) {
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
     * cannot be astablished ___This should NOT happen___
     */
    private void dieOnBindFail() {
        Toast.makeText(this, getString(R.string.binding_error), Toast.LENGTH_LONG).show();
        Log.e(TAG, "Failed to bind to the service");
        finish();
    }
    
    private class AuthenticationTask extends AsyncTask<Void, Void, Integer> {
        
        private int MAX_WAIT = 20;
        private int WAIT_DELAY = 100;
        
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
            int i = 0;
            while (xivoConnectionService == null && i < MAX_WAIT) {
                timer(WAIT_DELAY);
                i++;
            }
            try {
                if (xivoConnectionService != null && xivoConnectionService.isAuthenticated())
                    return Constants.AUTHENTICATION_OK;
                return xivoConnectionService.authenticate();
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
                startLoading();
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
                break;
            case Constants.CTI_SERVER_NOT_SUPPORTED:
                Toast.makeText(XivoActivity.this, getString(R.string.cti_not_supported),
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.VERSION_MISMATCH:
                Toast.makeText(XivoActivity.this, getString(R.string.version_mismatch),
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.ALGORITH_NOT_AVAILABLE:
                Toast.makeText(XivoActivity.this, getString(R.string.algo_exception),
                        Toast.LENGTH_LONG).show();
                break;
            default:
                Log.e(TAG, "Unhandled result " + result);
                Toast.makeText(XivoActivity.this, getString(R.string.login_ko), Toast.LENGTH_LONG)
                        .show();
                break;
            }
        }
        
        private void timer(int milliseconds) {
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
     * Ask to the XivoConnectionService to connect and wait for the result
     */
    private class ConnectTask extends AsyncTask<Void, Void, Integer> {
        
        private int MAX_TRY = 20;
        private int WAIT_DELAY = 100;
        
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
            int i = 0;
            while (xivoConnectionService == null && i < MAX_TRY) {
                timer(WAIT_DELAY);
                i++;
            }
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
                break;
            case Constants.REMOTE_EXCEPTION:
                Toast.makeText(XivoActivity.this, getString(R.string.remote_exception),
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.NOT_CTI_SERVER:
                Toast.makeText(XivoActivity.this, getString(R.string.not_cti_server),
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.BAD_HOST:
                Toast.makeText(XivoActivity.this, getString(R.string.bad_host), Toast.LENGTH_LONG)
                        .show();
                break;
            case Constants.NO_NETWORK_AVAILABLE:
                Toast.makeText(XivoActivity.this, getString(R.string.no_web_connection),
                        Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(XivoActivity.this, getString(R.string.connection_failed),
                        Toast.LENGTH_LONG).show();
                break;
            }
        }
        
        private void timer(int milliseconds) {
            try {
                synchronized (this) {
                    this.wait(milliseconds);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class IntentReceiver extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_MY_STATUS_CHANGE)) {
                updateMyStatus(intent.getLongExtra("id", 0));
            } else if (intent.getAction().equals(Constants.ACTION_MY_PHONE_CHANGE)) {
                updatePhoneStatus(intent.getStringExtra("color"),
                        intent.getStringExtra("longname"));
            } else if (intent.getAction().equals(Constants.ACTION_UPDATE_IDENTITY)) {
                updateFullname(intent.getStringExtra("fullname"));
            }
        }
    }
}
