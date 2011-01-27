package com.proformatique.android.xivoclient.service;

import com.proformatique.android.xivoclient.tools.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class XivoServiceStarter extends BroadcastReceiver {
    
    private final static String TAG = "XiVO service starter";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Intent received");
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        if (p.getBoolean(Constants.AUTO_START_SERVICE, false)) {
            Log.d(TAG, "Starting the XiVO service");
            Intent iServiceStarter = new Intent();
            iServiceStarter.setAction(XivoConnectionService.class.getName());
            context.startService(iServiceStarter);
        } else {
            Log.d(TAG, "XiVO auto start disabled");
        }
    }
}
