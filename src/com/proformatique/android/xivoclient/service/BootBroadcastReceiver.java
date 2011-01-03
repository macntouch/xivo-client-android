package com.proformatique.android.xivoclient.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {
	
	private static final String LOG_TAG = "Boot receiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG, "Boot broadcast received");
	}
	
}
