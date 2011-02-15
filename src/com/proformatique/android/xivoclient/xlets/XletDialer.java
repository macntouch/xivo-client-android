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

package com.proformatique.android.xivoclient.xlets;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.SettingsActivity;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.service.Connection;
import com.proformatique.android.xivoclient.service.InitialListLoader;
import com.proformatique.android.xivoclient.tools.Constants;

public class XletDialer extends XivoActivity {
	
	private static final String LOG_TAG = "XLET DIALER";
	private final int VM_DISABLED_FILTER = 0xff555555;
	
	EditText phoneNumber;
	IncomingReceiver receiver;
	Dialog dialog;
	
	private boolean offHook;
	
	private CallTask callTask = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_dialer);
		setPhoneOffHook(false);
		phoneNumber = (EditText) findViewById(R.id.number);
		
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			phoneNumber.setText(bundle.getString("numToCall"));
			callTask = new CallTask();
			callTask.execute();
		}
		
		receiver = new IncomingReceiver();
		
		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a call
		 */
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_HANGUP);
		filter.addAction(Constants.ACTION_OFFHOOK);
		filter.addAction(Constants.ACTION_MWI_UPDATE);
		filter.addAction(Constants.ACTION_CALL_PROGRESS);
		registerReceiver(receiver, new IntentFilter(filter));
		
		registerButtons();
	}
	
	@Override
	protected void onBindingComplete() {
		try {
			newVoiceMail(xivoConnectionService.hasNewVoiceMail());
		} catch (RemoteException e) {
			newVoiceMail(false);
		}
		super.onBindingComplete();
	}
	
	private void newVoiceMail(boolean status) {
		ImageButton vm_button = (ImageButton) findViewById(R.id.voicemailButton);
		vm_button.setEnabled(true);
		if (status) {
			vm_button.setColorFilter(null);
		} else {
			vm_button.setColorFilter(VM_DISABLED_FILTER, PorterDuff.Mode.SRC_ATOP);
		}
	}
	
	public void clickOnCall(View v) {
		if (offHook) {
			new HangupJsonTask().execute();
		} else {
			if (!("").equals(phoneNumber.getText().toString())){
				callTask = new CallTask();
				callTask.execute();
			}
		}
	}
	
	/**
	 * Set the status of the phone to update the dial button
	 * @param offHook
	 */
	public void setPhoneOffHook(boolean offHook) {
		this.offHook = offHook;
		if (offHook) {
			((ImageButton)findViewById(R.id.dialButton)).setImageDrawable(getResources()
					.getDrawable(R.drawable.ic_dial_action_hangup));
		} else {
			((ImageButton)findViewById(R.id.dialButton)).setImageDrawable(getResources()
					.getDrawable(R.drawable.ic_dial_action_call));
			((EditText)findViewById(R.id.number)).setEnabled(true);
			if (dialog != null)
				dialog.dismiss();
		}
	}
	
	/**
	 * Creates and send an hangup command to the CTI server Asynchronously
	 *
	 */
	private class HangupJsonTask extends AsyncTask<Void, Integer, Integer> {
		
		@Override
		protected Integer doInBackground(Void... params) {
			JSONObject jHangupObject = createJsonHangupObject();
			if (jHangupObject != null) {
				Connection.getInstance(XletDialer.this).sendJsonString(jHangupObject);
			} else {
				Log.d(LOG_TAG, "Null hangup object");
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Integer res) {
			setPhoneOffHook(false);
		}
	}
	
	/**
	 * Creating a AsyncTask to run call process
	 * @author cquaquin
	 */
	private class CallTask extends AsyncTask<Void, Integer, Integer> {
		
		public String progress = null;
		private TextView text = null;
		public boolean completeOrCancel = false;
		
		@Override
		protected void onPreExecute() {
			
			progress = getString(R.string.calling, phoneNumber.getText().toString());
			completeOrCancel = false;
			
			phoneNumber.setEnabled(false);
			dialog = new Dialog(XletDialer.this);
			
			dialog.setContentView(R.layout.xlet_dialer_call);
			dialog.setTitle(R.string.calling_title);
			
			text = (TextView) dialog.findViewById(R.id.call_message);
			text.setText(progress);
			
			dialog.show();
			
			super.onPreExecute();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			int result = Constants.OK;
			try {
				while (xivoConnectionService == null)
					timer(100);
				xivoConnectionService.call(phoneNumber.getText().toString().replace("(", "")
						.replace(")", "").replace("-", "").trim());
				while (!completeOrCancel) {
					timer(200);
				}
			} catch (RemoteException e) {
				result = Constants.REMOTE_EXCEPTION;
			}
			return result;
		}
		
		private void timer(int milliseconds){
			try {
				synchronized(this) {
					this.wait(milliseconds);
					} 
				} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			text.setText(progress);
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			Log.d(LOG_TAG, "Call completed");
			if (dialog != null)
				dialog.dismiss();
			phoneNumber.setEnabled(true);
			switch (result) {
			case Constants.OK:
				Log.i(LOG_TAG, "Call completed succesfully");
				break;
			case Constants.REMOTE_EXCEPTION:
				showToast(R.string.remote_exception);
				break;
			}
		}
		
		private void showToast(int messageId) {
			Toast.makeText(XletDialer.this, getString(messageId), Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Create an hangup JSON object
	 * "{
	 *   "class": "ipbxcommand",
	 *   "details": {
	 *     "channelids": "chan:xivo/17:SIP/4002-00000755",
	 *     "command": "hangup"
	 *   },
	 *   "direction": "xivoserver"
	 * }" 
	 * @return
	 */
	public JSONObject createJsonHangupObject() {
		JSONObject j = new JSONObject();
		JSONObject details = new JSONObject();
		InitialListLoader l = InitialListLoader.getInstance();
		String source = "chan:" + l.getUserId() + ":";
		if (SettingsActivity.getUseMobile(this)) {
			if (l.getPeersPeerChannelId() != null
					&& l.getPeersPeerChannelId().startsWith("Local") == false) {
				source += l.getPeersPeerChannelId();
			} else if (l.getThisChannelId() != null) {
				source += l.getThisChannelId();
			} else {
				source += l.getPeerChannelId();
			}
		} else {
			source += l.getThisChannelId();
		}
		try {
			details.accumulate("command", "hangup");
			details.accumulate("channelids", source);
			j.accumulate("class", "ipbxcommand");
			j.accumulate("direction", Constants.XIVO_SERVER);
			j.accumulate("details", details);
			return j;
		} catch (JSONException e) {
			Log.e(LOG_TAG, "JSONException");
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * BroadcastReceiver, intercept Intents with action ACTION_XLET_DIAL_CALL
	 * to perform a call
	 * @author cquaquin
	 *
	 */
	public class IncomingReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.ACTION_HANGUP)) {
				Log.d(LOG_TAG, "Hangup action received");
				setPhoneOffHook(false);
				phoneNumber.setEnabled(true);
				if (dialog != null)
					dialog.dismiss();
			} else if (intent.getAction().equals(Constants.ACTION_OFFHOOK)) {
				Log.d(LOG_TAG, "OffHook action received");
				setPhoneOffHook(true);
				phoneNumber.setEnabled(true);
				if (dialog != null)
					dialog.dismiss();
			} else if (intent.getAction().equals(Constants.ACTION_MWI_UPDATE)) {
				Log.d(LOG_TAG, "MWI update received");
				int[] mwi = intent.getExtras().getIntArray("mwi");
				newVoiceMail(mwi[0] == 1);
			} else if (intent.getAction().equals(Constants.ACTION_CALL_PROGRESS)) {
				String status = intent.getStringExtra("status");
				String code = intent.getStringExtra("code");
				Log.d(LOG_TAG, "New call progress received: " + status + " code: " + code);
				if (callTask != null) {
					if (Integer.parseInt(code) == Constants.HINTSTATUS_AVAILABLE_CODE)
						callTask.completeOrCancel = true;
					callTask.progress = status;
					callTask.onProgressUpdate();
				}
			}
		}
	}
	
	public void clickOn1(View v) {
		phoneNumber.append("1");
	}
	
	public void clickOn2(View v) {
		phoneNumber.append("2");
	}
	
	public void clickOn3(View v) {
		phoneNumber.append("3");
	}
	
	public void clickOn4(View v) {
		phoneNumber.append("4");
	}
	
	public void clickOn5(View v) {
		phoneNumber.append("5");
	}
	
	public void clickOn6(View v) {
		phoneNumber.append("6");
	}
	
	public void clickOn7(View v) {
		phoneNumber.append("7");
	}
	
	public void clickOn8(View v) {
		phoneNumber.append("8");
	}
	
	public void clickOn9(View v) {
		phoneNumber.append("9");
	}
	
	public void clickOn0(View v) {
		phoneNumber.append("0");
	}
	
	public void clickOnStar(View v) {
		phoneNumber.append("*");
	}
	
	public void clickOnSharp(View v) {
		phoneNumber.append("#");
	}
	
	public void clickOnDelete(View v) {
		keyPressed(KeyEvent.KEYCODE_DEL);
	}
	
	private void keyPressed(int keyCode) {
		KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
		phoneNumber.onKeyDown(keyCode, event);
	}
	
	public void clickVoiceMail(View v) {
		if (SettingsActivity.getUseMobile(this)) {
			Toast.makeText(this, "Not available when using your mobile number.",
					Toast.LENGTH_LONG).show();
		} else {
			((EditText) findViewById(R.id.number)).setText("*98");
			new CallTask().execute();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		if (dialog != null)
			dialog.dismiss();
			phoneNumber.setEnabled(true);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		if (dialog != null)
			dialog.dismiss();
		super.onDestroy();
	}
}
