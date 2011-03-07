package com.proformatique.android.xivoclient.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.proformatique.android.xivoclient.SettingsActivity;

public class XivoServiceStarter extends BroadcastReceiver {
    
    private final static String TAG = "XiVO service starter";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Intent received");
        if (SettingsActivity.getStartOnBoot(context)) {
            Log.d(TAG, "Starting the XiVO service");
            context.startService(new Intent(context, XivoConnectionService.class));
        } else {
            Log.d(TAG, "XiVO auto start disabled");
        }
    }
}
