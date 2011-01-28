package com.proformatique.android.xivoclient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.proformatique.android.xivoclient.service.IXivoConnectionService;
import com.proformatique.android.xivoclient.service.XivoConnectionService;
import com.proformatique.android.xivoclient.tools.Constants;

import android.app.Activity;
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
import android.view.View;
import android.widget.Toast;

public class ServiceDriver extends Activity {
    
    private XivoConnectionServiceConnection con = null;
    private IXivoConnectionService xivoConnectionService = null;
    private ConnectTask connectTask = null;
    private static final String PACK = "com.proformatique.android.xivoclient";
    private static final String TAG = "Service driver";
    private ProgressDialog dialog = null;
    private boolean connecting = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver);
        
        final ConnectTask ct = (ConnectTask) getLastNonConfigurationInstance();
        if (ct != null)
            connectTask = ct;
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
        if (con == null) {
            con = new XivoConnectionServiceConnection();
            Intent iServiceBinder = new Intent();
            iServiceBinder.setClassName(PACK, XivoConnectionService.class.getName());
            bindService(iServiceBinder, con, Context.BIND_AUTO_CREATE);
            Toast.makeText(this, "XiVO connection service binded", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "XiVO connection already binded", Toast.LENGTH_LONG).show();
        }
    }
    
    public void onReleaseClicked(View v) {
        if (con != null) {
            unbindService(con);
            con = null;
            Toast.makeText(this, "XiVO connection service released", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "XiVO connection service not binded", Toast.LENGTH_LONG).show();
        }
    }
    
    public void onConnectClicked(View v) {
        if (con == null) {
            Toast.makeText(this, "Service not binded", Toast.LENGTH_LONG).show();
        } else {
            connectTask = new ConnectTask();
            connectTask.execute();
            // Connection timeout 10 secs.
            new Thread(new Runnable() {
                
                @Override
                public void run() {
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
            });
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
            dialog = new ProgressDialog(ServiceDriver.this);
            dialog.setCancelable(false);
            dialog.setMessage("Connection...");
            dialog.show();
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
            if (dialog != null)
                dialog.dismiss();
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
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        final ConnectTask ct = connectTask;
        return ct;
    }
    
}
