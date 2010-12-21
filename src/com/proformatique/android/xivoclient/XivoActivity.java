package com.proformatique.android.xivoclient;

import com.proformatique.android.xivoclient.tools.Constants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * An overloaded Activity class to make UI changes and options consistent
 * across the application
 * 
 * @author Pascal Cadotte-Michaud
 *
 */
public class XivoActivity extends Activity {
	
	private SharedPreferences settings;
	private String LOG_TAG = "XivoActivity";
	private ForcedDisconnectReceiver receiver;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		receiver = new ForcedDisconnectReceiver();
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_FORCED_DISCONNECT);
		registerReceiver(receiver, new IntentFilter(filter));
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (settings.getBoolean("use_fullscreen", false)) {
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}
	
	public class ForcedDisconnectReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.ACTION_FORCED_DISCONNECT)) {
				Log.d(LOG_TAG, "Received broadcast: Forced disconnect");
				Toast.makeText(XivoActivity.this, R.string.forced_disconnect, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
}
