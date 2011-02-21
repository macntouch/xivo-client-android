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

import com.proformatique.android.xivoclient.service.IXivoConnectionService;
import com.proformatique.android.xivoclient.service.XivoConnectionService;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.xlets.XletDialer;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
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
	private XivoConnectionServiceConnection con = null;
	protected IXivoConnectionService xivoConnectionService = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		final KeyguardManager.KeyguardLock kmkl = km.newKeyguardLock("kCaller");
		phoneStateListener = new PhoneStateListener() {
			/**
			 * Waits 2 seconds after answering a call and launch the XletsContainerTabActivity
			 */
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				Log.d(LOG_TAG, "onCallStateChanged called");
				try {
					if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
						if (xivoConnectionService.killDialer()) {
							Intent i = new Intent(getApplicationContext(), XletDialer.class);
							i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							try {
								Thread.sleep(2000);
								kmkl.disableKeyguard();
							} catch (Exception e) {
								Log.d(LOG_TAG, "Exception: " + e);
							}
							Log.d(LOG_TAG, "Putting the android dialer screen in background");
							startActivity(i);
						} else {
							Log.d(LOG_TAG, "Not killing this dialer.");
							Log.d(LOG_TAG, "Using mobile: " + SettingsActivity.getUseMobile(
									InCallScreenKiller.this));
							Log.d(LOG_TAG, "hasChannels: " + xivoConnectionService.hasChannels());
						}
					}
				} catch (RemoteException e) {
					Log.d(LOG_TAG, "Remote exception");
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
		if (con != null) {
			unbindService(con);
			con = null;
			Log.d(LOG_TAG, "XiVO connection service released");
		} else {
			Log.d(LOG_TAG, "XiVO connection service not binded");
		}
		super.onDestroy();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		/*
		 * Bind to the XivoConnectionService
		 */
		if (con == null) {
			con = new XivoConnectionServiceConnection();
			Intent iServiceBinder = new Intent();
			iServiceBinder.setClassName(Constants.PACK, XivoConnectionService.class.getName());
			bindService(iServiceBinder, con, Context.BIND_AUTO_CREATE);
			Log.d(LOG_TAG, "XiVO connection service binded");
		} else {
			Log.d(LOG_TAG, "XiVO connection already binded");
		}
	}
	
	/**
	 * Establish a binding between the activity and the XivoConnectionService
	 *
	 */
	protected class XivoConnectionServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			xivoConnectionService = IXivoConnectionService.Stub.asInterface((IBinder)service);
			if (xivoConnectionService == null)
				Log.e(LOG_TAG, "xivoConnectionService is null");
			else
				Log.i(LOG_TAG, "xivoConnectionService is not null");
			Log.d(LOG_TAG, "onServiceConnected");
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(LOG_TAG, "onServiceDisconnected");
		}
	};
}
