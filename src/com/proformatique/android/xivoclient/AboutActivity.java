package com.proformatique.android.xivoclient;

import com.proformatique.android.xivoclient.service.IXivoService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

public class AboutActivity extends Activity {
	
	private IXivoService xivoService;
	private boolean started = false;
	private RemoteException conn = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.about);
		super.onCreate(savedInstanceState);
		testService();
	}
	
	/**
	 * Temporary method to test the integration of the XiVO service
	 */
	public void testService() {
		startService();
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
}
