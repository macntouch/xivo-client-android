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

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.service.CapaservicesProvider;
import com.proformatique.android.xivoclient.service.InitialListLoader;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.JSONMessageFactory;

public class XletServices extends XivoActivity {

	private static final String LOG_TAG = "XLET SERVICES";
	private IncomingReceiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xlet_services);
		
		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a change
		 *  in the list from the Activity
		 */
		receiver = new IncomingReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_LOAD_FEATURES);
		registerReceiver(receiver, new IntentFilter(filter));
		
		registerButtons();
	}
	
	@Override
	protected void onBindingComplete() {
		super.onBindingComplete();
		refreshFeatures();
	}
	
	public void clickOnCallrecord(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			sendFeaturePut("callrecord", "1", null);
		}
		else {
			sendFeaturePut("callrecord", "0", null);
		}
	}
	
	public void clickOnIncallfilter(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			sendFeaturePut("incallfilter", "1", null);
		}
		else {
			sendFeaturePut("incallfilter", "0", null);
		}
	}
	
	public void clickOnEnablednd(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			sendFeaturePut("enablednd", "1", null);
		}
		else {
			sendFeaturePut("enablednd", "0", null);
		}
	}
	
	public void clickOnFwdrna(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			checkbox.setClickable(false);
			Intent defineIntent = new Intent(this, XletServicesAsk.class);
			defineIntent.putExtra("serviceType", "fwdrna");
			startActivityForResult(defineIntent, Constants.CODE_SERVICE_ASK1);
		}
		else {
			checkbox.setText(R.string.servicesFwdrna);
			sendFeaturePut("enablerna", "0", 
					InitialListLoader.getInstance().getFeaturesRna().get("number"));
		}
	}
	
	public void clickOnFwdbusy(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			checkbox.setClickable(false);
			Intent defineIntent = new Intent(this, XletServicesAsk.class);
			defineIntent.putExtra("serviceType", "fwdbusy");
			startActivityForResult(defineIntent, Constants.CODE_SERVICE_ASK2);
		}
		else {
			checkbox.setText(R.string.servicesFwdbusy);
			sendFeaturePut("enablebusy", "0", 
					InitialListLoader.getInstance().getFeaturesBusy().get("number"));
		}
		
	}
	
	public void clickOnFwdunc(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			checkbox.setClickable(false);
			Intent defineIntent = new Intent(this, XletServicesAsk.class);
			defineIntent.putExtra("serviceType", "fwdunc");
			startActivityForResult(defineIntent, Constants.CODE_SERVICE_ASK3);
		}
		else {
			checkbox.setText(R.string.servicesFwdunc);
			sendFeaturePut("enableunc", "0", 
					InitialListLoader.getInstance().getFeaturesUnc().get("number"));
		}
	}
	
	 @Override
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		 super.onActivityResult(requestCode, resultCode, data);
		 
		 CheckBox checkbox;
		 String textDisplay;
		 String phoneNumber = data.getStringExtra("phoneNumber");
		 
		 if (requestCode == Constants.CODE_SERVICE_ASK1) {
			 checkbox = (CheckBox) findViewById(R.id.fwdrna);
			 textDisplay = getString(R.string.servicesFwdrna);
			 setCheckboxDisplay(resultCode, checkbox, phoneNumber, textDisplay);
			 if (resultCode  == Constants.OK)
				 sendFeaturePut("enablerna", "1", phoneNumber);
		}
		
		else if (requestCode == Constants.CODE_SERVICE_ASK2) {
			 checkbox = (CheckBox) findViewById(R.id.fwdbusy);
			 textDisplay = getString(R.string.servicesFwdbusy);
			 setCheckboxDisplay(resultCode, checkbox, phoneNumber, textDisplay);
			 if (resultCode  == Constants.OK)
				 sendFeaturePut("enablebusy", "1", phoneNumber);
		 }
		
		if (requestCode == Constants.CODE_SERVICE_ASK3) {
			 checkbox = (CheckBox) findViewById(R.id.fwdunc);
			 textDisplay = getString(R.string.servicesFwdunc);
			 setCheckboxDisplay(resultCode, checkbox, phoneNumber, textDisplay);
			 if (resultCode  == Constants.OK)
				 sendFeaturePut("enableunc", "1", phoneNumber);
		}
	}
	
	private void setCheckboxDisplay(int code, CheckBox checkbox, 
			String phoneNumber, String textDisplay){
		if (code == Constants.OK){
			checkbox.setText(textDisplay + "\n"+getString(R.string.servicesPhone)+phoneNumber);
			checkbox.setChecked(true);
		} else {
			checkbox.setChecked(false);
			checkbox.setText(textDisplay);
		}
		checkbox.setClickable(true);
	}
	
	/**
	 * BroadcastReceiver, intercept Intents with action ACTION_LOAD_HISTORY_LIST
	 * to perform an reload of the displayed list
	 * @author cquaquin
	 *
	 */
	private class IncomingReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.ACTION_LOAD_FEATURES)) {
				Log.d( LOG_TAG , "Received Broadcast "+Constants.ACTION_LOAD_FEATURES);
				refreshFeatures();
			}
		}
	}
	
	public void refreshFeatures() {
		Cursor c = getContentResolver().query(CapaservicesProvider.CONTENT_URI,
				new String[]{ CapaservicesProvider._ID, CapaservicesProvider.SERVICE},
				null, null, null);
		c.moveToFirst();
		if (c.moveToFirst()) {
			do {
				String service = c.getString(c.getColumnIndex(CapaservicesProvider.SERVICE));
				if (service.equals("fwdbusy"))
					enableFwdbusy(true);
				else if (service.equals("fwdrna"))
					enableFwdrna(true);
				else if (service.equals("fwdunc"))
					enableFwdunc(true);
				else if (service.equals("enablednd"))
					enableEnableDnd(true);
				else if (service.equals("callrecord"))
					enableCallrecord(true);
				else if (service.equals("incallfilter"))
					enableIncallfilter(true);
			} while (c.moveToNext());
		}
		c.close();
	}
	
	private void enableFwdbusy(final boolean status) {
		CheckBox checkbox;
		checkbox = (CheckBox) findViewById(R.id.fwdbusy);
		setCheckboxDisplay(status == true ? Constants.OK : Constants.CANCEL, checkbox, "", 
				getString(R.string.servicesFwdbusy));
	}
	
	private void enableFwdrna(final boolean status) {
		CheckBox checkbox;
		checkbox = (CheckBox) findViewById(R.id.fwdrna);
		setCheckboxDisplay(Constants.OK, checkbox, "", 
				getString(R.string.servicesFwdrna));
	}
	
	private void enableFwdunc(final boolean status) {
		CheckBox checkbox;
		checkbox = (CheckBox) findViewById(R.id.fwdunc);
		setCheckboxDisplay(Constants.OK, checkbox, "", 
				getString(R.string.servicesFwdunc));
	}
	
	private void enableEnableDnd(final boolean status) {
		CheckBox checkbox;
		checkbox = (CheckBox) findViewById(R.id.enablednd);
		checkbox.setChecked(status);
	}
	
	private void enableCallrecord(final boolean status) {
		CheckBox checkbox;
		checkbox = (CheckBox) findViewById(R.id.callrecord);
		checkbox.setChecked(status);
	}
	
	private void enableIncallfilter(final boolean status) {
		CheckBox checkbox;
		checkbox = (CheckBox) findViewById(R.id.incallfilter);
		checkbox.setChecked(status);
	}
	
	private void sendFeaturePut(String feature, String value, String phone){
		try {
			xivoConnectionService.sendFeature(feature, value, phone);
		} catch (RemoteException e) {
			Toast.makeText(this, getString(R.string.remote_exception), Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
}
