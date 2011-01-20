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
