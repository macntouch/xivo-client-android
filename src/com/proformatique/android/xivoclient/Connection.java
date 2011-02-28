package com.proformatique.android.xivoclient;

import com.proformatique.android.xivoclient.service.IXivoConnectionService;
import com.proformatique.android.xivoclient.service.XivoConnectionService;
import com.proformatique.android.xivoclient.tools.Constants;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Implement a Singleton to avoid binding to the service from each activities
 *
 */
public enum Connection {
    INSTANCE;
    
    private final static String TAG = "XiVO connection";
    private IXivoConnectionService service = null;
    private XivoConnectionServiceConnection con = null;
    private boolean currentlyBinding = false;
    private Context context = null;
    
    /**
     * Establish a binding between the activity and the XivoConnectionService
     * 
     */
    private class XivoConnectionServiceConnection implements ServiceConnection {
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Connection.this.service = IXivoConnectionService.Stub.asInterface(service);
            Log.d(TAG, "Binding complete");
            currentlyBinding = false;
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
        }
    };
    
    /**
     * The first time getConnection is called, it will return a null connection since the
     * binding is done asynchronously.
     * @param context
     * @return
     */
    public IXivoConnectionService getConnection(Context context) {
        Log.d(TAG, "Connection instance request, Already binding: " + currentlyBinding);
        if (this.context == null) this.context = context;
        if (service == null && currentlyBinding == false) bind(context);
        return service;
    }
    
    public void releaseService() {
        try {
            if (con != null && context != null) context.unbindService(con);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Could not release the service");
        }
    }
    
    private void bind(Context context) {
        Log.d(TAG, "Starting a new binding");
        currentlyBinding = true;
        con = new XivoConnectionServiceConnection();
        Intent iServiceBinder = new Intent();
        iServiceBinder.setClassName(Constants.PACK, XivoConnectionService.class.getName());
        context.bindService(iServiceBinder, con, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "XiVO connection service binded");
    }
    
    private void unbind() {
        if (service != null) {
            Log.d(TAG, "Unbinding");
            context.unbindService(con);
            @SuppressWarnings("unused")
            XivoConnectionServiceConnection tmp = con;
            con = null;
            service = null;
        }
    }
}
