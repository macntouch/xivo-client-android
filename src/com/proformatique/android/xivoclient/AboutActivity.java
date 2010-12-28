package com.proformatique.android.xivoclient;

import com.proformatique.android.xivoclient.service.IXivoService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class AboutActivity extends Activity {
	
	private IXivoService xivoService;
	private boolean started = false;
	private RemoteServiceConnection conn = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.about);
		super.onCreate(savedInstanceState);
		//testService();
	}
	
	@Override
	protected void onDestroy() {
		releaseService();
		stopService();
		super.onDestroy();
	}
	
	/**
	 * Temporary method to test the integration of the XiVO service
	 */
	public void testService() {
		startService();
		bindService();
	}
	
	private void startService() {
		if (!started) {
			Intent i = new Intent();
			i.setClassName("com.proformatique.android.xivoclient", "com.proformatique.android.xivoclient.service.XivoService");
			startService(i);
			started = true;
			Log.d("SERVICE TEST", "Service started");
		} else {
			Log.d("SERVICE TEST", "Service already running");
		}
	}
	
	private void stopService() {
		if (started) {
			Intent i = new Intent();
			i.setClassName("com.proformatique.android.xivoclient", "com.proformatique.android.xivoclient.service.XivoService");
			stopService(i);
			started = true;
			Log.d("SERVICE TEST", "Service stopped");
		} else {
			Log.d("SERVICE TEST", "Service already stopped");
		}
	}
	
	private void bindService() {
		if (conn == null) {
			conn = new RemoteServiceConnection();
			Intent i = new Intent();
			i.setClassName("com.proformatique.android.xivoclient", "com.proformatique.android.xivoclient.service.XivoService");
			bindService(i, conn, Context.BIND_AUTO_CREATE);
			Log.d("SERVICE TEST", "Service binded");
		} else {
			Log.d("SERVICE TEST", "Service already bound");
		}
	}
	
	private void releaseService() {
		if (conn != null) {
			unbindService(conn);
			conn = null;
			Log.d("SERVICE TEST", "Service released");
		} else {
			Log.d("SERVICE TEST", "Service not bounded");
		}
	}
	
	private void invokeService() {
		if (conn != null) {
			try {
				boolean temp = xivoService.contactsChanged();
				Toast.makeText(getApplicationContext(), "Service invoked and got " + temp, Toast.LENGTH_LONG).show();
			} catch (RemoteException e) {
				Log.d("SERVICE TEST", "Remote exception");
			}
		} else {
			Log.d("SERVICE TEST", "Service unbounded");
		}
	}
	
	class RemoteServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			xivoService = IXivoService.Stub.asInterface((IBinder)service);
			Log.d("SERVICE TEST", "onServiceConnected");
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			xivoService = null;
			Log.d("SERVICE TEST", "onServiceDisconnected");
		}
		
	}
}
