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

import org.json.JSONObject;

import com.proformatique.android.xivoclient.service.Connection;
import com.proformatique.android.xivoclient.service.InitialListLoader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class AttendedTransferActivity extends TransferActivity {
	
	private String number;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			number = extras.getString("num").replace("-", "");
			Log.d(LOG_TAG, "Extras: " + number);
			if (number != null && number.equals("") != true) {
				new AttendedTransferJSONTask().execute();
				finish();
			}
		}
	}
	
	@Override
	protected void transferClicked() {
		Log.d(LOG_TAG, "Attended transfer clicked");
		EditText et = (EditText) findViewById(R.id.transfer_destination);
		number = et.getText().toString().replace("-", "");
		if (number != null && number.equals("") != true) {
			new AttendedTransferJSONTask().execute();
			finish();
		}
	}
	
	/**
	 * Creates an AsyncTask to run the call transfer
	 */
	private class AttendedTransferJSONTask extends AsyncTask<Void, Integer, Integer> {
		
		@Override
		protected void onPreExecute() {
			Log.d(LOG_TAG, "Transfering call to " + number);
			Toast.makeText(getApplicationContext(), getString(R.string.transfering, number), Toast.LENGTH_LONG).show();
			super.onPreExecute();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			String source ="chan:" + InitialListLoader.getInstance().getUserId() +
			":";
			if (SettingsActivity.getUseMobile(AttendedTransferActivity.this)) {
				Log.i(LOG_TAG, "Using mobile");
				source += InitialListLoader.getInstance().getPeersPeerChannelId();
			} else {
				Log.i(LOG_TAG, "Not using mobile");
				source += InitialListLoader.getInstance().getThisChannelId();
				
			}
			JSONObject jTransferObject = createJsonTransferObject("atxfer", source, number);
			Connection.getInstance(AttendedTransferActivity.this).sendJsonString(jTransferObject);
			return null;
		}
	}
}
