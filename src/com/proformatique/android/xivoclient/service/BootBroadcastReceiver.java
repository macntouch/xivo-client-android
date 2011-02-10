package com.proformatique.android.xivoclient.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver to receive BOOT_COMPLETED events and start the XiVO service.
 * 
 * @author Pascal Cadotte-Michaud
 *
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
	
	private static final String LOG_TAG = "Boot receiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG, "Boot broadcast received");
		Intent i = new Intent();
		i.setAction(XivoConnectionService.class.getName());
		context.startService(i);
	}
	
}
