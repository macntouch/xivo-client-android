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

import java.io.IOException;
import java.io.PrintStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import com.proformatique.android.xivoclient.Connection;
import com.proformatique.android.xivoclient.InitialListLoader;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.SettingsActivity;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.XletsContainerTabActivity;
import com.proformatique.android.xivoclient.tools.Constants;

public class XletDialer extends XivoActivity {
	
	private static final String LOG_TAG = "XLET DIALER";
	EditText phoneNumber;
	IncomingReceiver receiver;
	Dialog dialog;
	private boolean offHook;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_dialer);
		setPhoneOffHook(false);
		phoneNumber = (EditText) findViewById(R.id.number);
		
		receiver = new IncomingReceiver();
		
		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a call
		 */
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_XLET_DIAL_CALL);
		filter.addAction(Constants.ACTION_HANGUP);
		filter.addAction(Constants.ACTION_OFFHOOK);
		registerReceiver(receiver, new IntentFilter(filter));
		
	}
	
	public void clickOnCall(View v) {
		if (offHook) {
			new HangupJsonTask().execute();
		} else {
			if (!("").equals(phoneNumber.getText().toString())){
				new CallJsonTask().execute();
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
				Connection.getInstance().sendJsonString(jHangupObject);
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
	private class CallJsonTask extends AsyncTask<Void, Integer, Integer> {
		
		@Override
		protected void onPreExecute() {
			
			phoneNumber.setEnabled(false);
			dialog = new Dialog(XletDialer.this);
			
			dialog.setContentView(R.layout.xlet_dialer_call);
			dialog.setTitle(R.string.calling_title);
			
			TextView text = (TextView) dialog.findViewById(R.id.call_message);
			text.setText(getString(R.string.calling, phoneNumber.getText().toString()));
			
			dialog.show();
			
			super.onPreExecute();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			
			timer(1000);
			
			/**
			 * Creating Call Json object
			 */
			JSONObject jCalling = createJsonCallingObject("originate", SettingsActivity.getMobileNumber(getApplicationContext()), 
					phoneNumber.getText().toString().replaceAll("-", ""));
			try {
				Log.d( LOG_TAG, "jCalling: " + jCalling.toString());
				PrintStream output = new PrintStream(Connection.getInstance().getNetworkConnection().getOutputStream());
				output.println(jCalling.toString());
								
				publishProgress(Constants.OK);
				timer(3000);
				
				return Constants.OK; 
				
			} catch (IOException e) {
				publishProgress(Constants.NO_NETWORK_AVAILABLE);
				
				return Constants.NO_NETWORK_AVAILABLE;
			}
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
			if (values[0]==Constants.OK) {
				TextView text = (TextView) dialog.findViewById(R.id.call_message);
				text.setText(getString(R.string.call_ok));
			}
			else {
				TextView text = (TextView) dialog.findViewById(R.id.call_message);
				text.setText(getString(R.string.no_web_connection));
			}
			super.onProgressUpdate(values);
		}
	}
	
	/**
	 * Prepare the Json string for calling process
	 * 
	 * @param inputClass
	 * @param phoneNumberSrc
	 * @param phoneNumberDest
	 * @return
	 */
	private JSONObject createJsonCallingObject(String inputClass, 
			String phoneNumberSrc,
			String phoneNumberDest) {
		
		JSONObject jObj = new JSONObject();
		String phoneSrc;
		
		if (phoneNumberSrc == null)
			phoneSrc = "user:special:me";
		else if (phoneNumberSrc.equals(""))
			phoneSrc = "user:special:me";
		else phoneSrc = "ext:"+phoneNumberSrc;
		
		try {
			jObj.accumulate("direction", Constants.XIVO_SERVER);
			jObj.accumulate("class",inputClass);
			jObj.accumulate("source", phoneSrc);
			jObj.accumulate("destination", "ext:"+phoneNumberDest);
			
			return jObj;
		} catch (JSONException e) {
			return null;
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
			if (intent.getAction().equals(Constants.ACTION_XLET_DIAL_CALL)) {
				Log.d( LOG_TAG , "Received Broadcast ");
				Bundle extra = intent.getExtras();
				
				if (extra != null){
					XletsContainerTabActivity parentAct;
					phoneNumber.setText(extra.getString("numToCall"));
					parentAct = (XletsContainerTabActivity)XletDialer.this.getParent();
					parentAct.switchTab(0);
					
					new CallJsonTask().execute();
				}
			} else if (intent.getAction().equals(Constants.ACTION_HANGUP)) {
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
