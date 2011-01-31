package com.proformatique.android.xivoclient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.proformatique.android.xivoclient.service.IXivoConnectionService;
import com.proformatique.android.xivoclient.service.XivoConnectionService;
import com.proformatique.android.xivoclient.tools.Constants;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class ServiceDriver extends XivoActivity {
    
    private XivoConnectionServiceConnection con = null;
    private IXivoConnectionService xivoConnectionService = null;
    private ConnectTask connectTask = null;
    private AuthenticationTask authenticationTask = null;
    private static final String PACK = "com.proformatique.android.xivoclient";
    private static final String TAG = "Service driver";
    private ProgressDialog dialog = null;
    private boolean connecting = false;
    private boolean authenticating = false;
    private final static int CONNECTION_TIMEOUT = 10;
    private final static int AUTHENTICATION_TIMEOUT = 50;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver);
    }
    
    public void onStartClicked(View v) {
        Intent iStartXivoService = new Intent();
        iStartXivoService.setClassName(PACK, XivoConnectionService.class.getName());
        startService(iStartXivoService);
        Toast.makeText(this, "Starting XiVO connection service", Toast.LENGTH_LONG).show();
    }
    
    public void onStopClicked(View v) {
        Intent iStopService = new Intent();
        iStopService.setClassName(PACK, XivoConnectionService.class.getName());
        stopService(iStopService);
        Toast.makeText(this, "XiVO connection service stopping", Toast.LENGTH_LONG).show();
    }
    
    public void onBindClicked(View v) {
        bindXivoConnectionService();
    }
    
    public void onReleaseClicked(View v) {
        releaseXivoConnectionService();
    }
    
    public void onLoadDataClicked(View v) {
        try {
            if (xivoConnectionService != null && xivoConnectionService.isConnected()) {
                xivoConnectionService.loadData();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    public void onConnectClicked(View v) {
        if (con == null) {
            Toast.makeText(this, "Service not binded", Toast.LENGTH_LONG).show();
        } else {
            try {
                if (xivoConnectionService != null && xivoConnectionService.isConnected()) {
                    Toast.makeText(this, "Already connected", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (RemoteException e1) {
                Log.d(TAG, "Could not check connection status");
            }
            connectTask = new ConnectTask();
            connectTask.execute();
            try {
                connectTask.get(CONNECTION_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void onDisconnectClicked(View v) {
        try {
            if (xivoConnectionService != null && xivoConnectionService.isConnected()) {
                switch (xivoConnectionService.disconnect()) {
                case Constants.OK:
                    Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();
                    break;
                case Constants.NO_NETWORK_AVAILABLE:
                    Toast.makeText(this, "No network available", Toast.LENGTH_LONG).show();
                    break;
                }
            } else {
                Toast.makeText(this, "Not event connected", Toast.LENGTH_LONG).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    public void onAuthenticateClicked(View v) {
        if (con == null) {
            Toast.makeText(this, "Service not binded", Toast.LENGTH_LONG).show();
        } else {
            try {
                if (xivoConnectionService != null && xivoConnectionService.isConnected() == false) {
                    Toast.makeText(this, "You need to connect first", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (RemoteException e1) {
                Log.d(TAG, "Could not check connection status");
            }
            authenticationTask = new AuthenticationTask();
            authenticationTask.execute();
            try {
                authenticationTask.get(AUTHENTICATION_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void bindXivoConnectionService() {
    	if (con == null) {
            con = new XivoConnectionServiceConnection();
            Intent iServiceBinder = new Intent();
            iServiceBinder.setClassName(PACK, XivoConnectionService.class.getName());
            bindService(iServiceBinder, con, Context.BIND_AUTO_CREATE);
            Log.d(TAG, "XiVO connection service binded");
        } else {
            Log.d(TAG, "XiVO connection already binded");
        }
    }
    
    private void releaseXivoConnectionService() {
        if (con != null) {
            unbindService(con);
            con = null;
            Log.d(TAG, "XiVO connection service released");
        } else {
            Log.d(TAG, "XiVO connection service not binded");
        }
    }
    
    private class XivoConnectionServiceConnection implements ServiceConnection {
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            xivoConnectionService = IXivoConnectionService.Stub.asInterface((IBinder)service);
            Log.d(TAG, "onServiceConnected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
        }
    };
    
    /**
     * Starts a connection between the XivoConnectionService and the CTI server.
     *
     */
    private class ConnectTask extends AsyncTask<Void, Integer, Integer> {
        
        @Override
        protected void onPreExecute() {
            showDialog("Connecting...");
            connecting = true;
        }
        
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                return xivoConnectionService.connect();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return Constants.CONNECTION_FAILED;
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            connecting = false;
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            switch(result) {
            case Constants.CONNECTION_OK:
                Toast.makeText(ServiceDriver.this, "Connection succesfull",
                        Toast.LENGTH_LONG).show();
                break;
            case Constants.CONNECTION_FAILED:
                Toast.makeText(ServiceDriver.this, "Connection failed", Toast.LENGTH_LONG).show();
                break;
            case Constants.NOT_CTI_SERVER:
                Toast.makeText(ServiceDriver.this, "Not a CTI server", Toast.LENGTH_LONG).show();
                break;
            case Constants.BAD_HOST:
                Toast.makeText(ServiceDriver.this, "Bad host or port", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
    
    /**
     * Starts the authentication between the XivoConnectionService and the CTI server.
     *
     */
    private class AuthenticationTask extends AsyncTask<Void, Integer, Integer> {
        
        @Override
        protected void onPreExecute() {
            showDialog("Authenticating...");
            authenticating = true;
        }
        
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                return xivoConnectionService.authenticate();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return Constants.LOGIN_KO;
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            authenticating = false;
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            switch(result) {
            case Constants.AUTHENTICATION_OK:
                Toast.makeText(ServiceDriver.this, "Authentication succesfull",
                        Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(ServiceDriver.this,
                    "Authentication failed " + Integer.toString(result), Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
    
    private void showDialog(String message) {
        dialog = new ProgressDialog(ServiceDriver.this);
        dialog.setCancelable(true);
        dialog.setMessage(message);
        dialog.show();
    }
    
    @Override
    public void onSaveInstanceState(Bundle state) {
        Log.d(TAG, "Saving instance state");
        state.putBoolean("connecting", connecting);
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        state.putBoolean("binded", con != null);
        if (con != null)
            releaseXivoConnectionService();
        super.onSaveInstanceState(state);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        Log.d(TAG, "Restoring instance state");
        // TODO: Add tests for the network connection and authentication
        if (state.getBoolean("binded"))
            bindXivoConnectionService();
    }
}
