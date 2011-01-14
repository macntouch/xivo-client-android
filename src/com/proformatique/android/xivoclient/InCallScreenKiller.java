package com.proformatique.android.xivoclient;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * This service will return the the XletsContainerTabActivity after receiving a call.
 * The goal is to be able to use the XiVO client after receiving a call back to do
 * transfers or other operations.
 *
 */
public class InCallScreenKiller extends Service {
	
	private TelephonyManager telephonyManager;
	private PhoneStateListener phoneStateListener;
	private final static String LOG_TAG = "InCallScreenKiller";
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		KeyguardManager km = (KeyguardManager)	getSystemService(Context.KEYGUARD_SERVICE);
		final KeyguardManager.KeyguardLock kmkl = km.newKeyguardLock("kCaller");
		phoneStateListener = new PhoneStateListener() {
			/**
			 * Waits 2 seconds after answering a call and launch the XletsContainerTabActivity
			 */
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				Log.d(LOG_TAG, "onCallStateChanged called");
				if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
					Intent i = new Intent(getApplicationContext(), XletsContainerTabActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					try {
						Thread.sleep(2000);
						kmkl.disableKeyguard();
					} catch (Exception e) {
						Log.d(LOG_TAG, "Exception: " + e);
					}
					startActivity(i);
				}
			}
		};
		// Start listening
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}
	
	@Override
	public void onDestroy() {
		// Stop listening
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		super.onDestroy();
	}

}
