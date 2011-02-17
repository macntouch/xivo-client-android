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
import com.proformatique.android.xivoclient.tools.Constants;

import static com.proformatique.android.xivoclient.service.CapaservicesProvider.getNumberForFeature;

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
		} else {
			sendFeaturePut("callrecord", "0", null);
		}
	}
	
	public void clickOnIncallfilter(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			sendFeaturePut("incallfilter", "1", null);
		} else {
			sendFeaturePut("incallfilter", "0", null);
		}
	}
	
	public void clickOnEnablednd(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			sendFeaturePut("enablednd", "1", null);
		} else {
			sendFeaturePut("enablednd", "0", null);
		}
	}
	
	public void clickOnFwdrna(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			checkbox.setClickable(false);
			Intent defineIntent = new Intent(this, XletServicesAsk.class);
			defineIntent.putExtra("serviceType", "rna");
			startActivityForResult(defineIntent, Constants.CODE_SERVICE_ASK1);
		} else {
			checkbox.setText(R.string.servicesFwdrna);
			sendFeaturePut("enablerna", "0", getNumberForFeature(this, "rna"));
		}
	}
	
	public void clickOnFwdbusy(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			checkbox.setClickable(false);
			Intent defineIntent = new Intent(this, XletServicesAsk.class);
			defineIntent.putExtra("serviceType", "busy");
			startActivityForResult(defineIntent, Constants.CODE_SERVICE_ASK2);
		} else {
			checkbox.setText(R.string.servicesFwdbusy);
			sendFeaturePut("enablebusy", "0", getNumberForFeature(this, "busy"));
		}
	}
	
	public void clickOnFwdunc(View v){
		CheckBox checkbox = (CheckBox)v;
		if (checkbox.isChecked()){
			checkbox.setClickable(false);
			Intent defineIntent = new Intent(this, XletServicesAsk.class);
			defineIntent.putExtra("serviceType", "unc");
			startActivityForResult(defineIntent, Constants.CODE_SERVICE_ASK3);
		} else {
			checkbox.setText(R.string.servicesFwdunc);
			sendFeaturePut("enableunc", "0", getNumberForFeature(this, "unc"));
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
			setCheckboxDisplay(resultCode == Constants.OK, checkbox, phoneNumber, textDisplay);
			if (resultCode  == Constants.OK)
				sendFeaturePut("enablerna", "1", phoneNumber);
		} else if (requestCode == Constants.CODE_SERVICE_ASK2) {
			checkbox = (CheckBox) findViewById(R.id.fwdbusy);
			textDisplay = getString(R.string.servicesFwdbusy);
			setCheckboxDisplay(resultCode == Constants.OK, checkbox, phoneNumber, textDisplay);
			if (resultCode  == Constants.OK)
				sendFeaturePut("enablebusy", "1", phoneNumber);
		} else if (requestCode == Constants.CODE_SERVICE_ASK3) {
			checkbox = (CheckBox) findViewById(R.id.fwdunc);
			textDisplay = getString(R.string.servicesFwdunc);
			setCheckboxDisplay(resultCode == Constants.OK, checkbox, phoneNumber, textDisplay);
			if (resultCode  == Constants.OK)
				sendFeaturePut("enableunc", "1", phoneNumber);
		}
	}
	
	private void setCheckboxDisplay(boolean checked, CheckBox checkbox, String phoneNumber,
			String textDisplay){
		if (checked) {
			checkbox.setText(textDisplay + "\n" + getString(R.string.servicesPhone) + phoneNumber);
		} else {
			checkbox.setText(textDisplay);
		}
		checkbox.setChecked(checked);
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
				Log.d(LOG_TAG, "Received Broadcast" + Constants.ACTION_LOAD_FEATURES);
				refreshFeatures();
			}
		}
	}
	
	public void refreshFeatures() {
		Cursor c = getContentResolver().query(CapaservicesProvider.CONTENT_URI,
				null, null, null, null);
		c.moveToFirst();
		if (c.moveToFirst()) {
			do {
				String service = c.getString(c.getColumnIndex(CapaservicesProvider.SERVICE));
				boolean enabled = c.getInt(c.getColumnIndex(
						CapaservicesProvider.ENABLED)) == 1 ? true : false;
				if (service.equals("busy"))
					enableFwdbusy(enabled);
				else if (service.equals("rna"))
					enableFwdrna(enabled);
				else if (service.equals("unc"))
					enableFwdunc(enabled);
				else if (service.equals("enablednd"))
					enableEnableDnd(enabled);
				else if (service.equals("callrecord"))
					enableCallrecord(enabled);
				else if (service.equals("incallfilter"))
					enableIncallfilter(enabled);
			} while (c.moveToNext());
		}
		c.close();
	}
	
	private void enableFwdbusy(final boolean status) {
		CheckBox checkbox;
		checkbox = (CheckBox) findViewById(R.id.fwdbusy);
		setCheckboxDisplay(status, checkbox, getNumberForFeature(this, "busy"),
				getString(R.string.servicesFwdbusy));
	}
	
	private void enableFwdrna(final boolean status) {
		CheckBox checkbox;
		checkbox = (CheckBox) findViewById(R.id.fwdrna);
		setCheckboxDisplay(status, checkbox, getNumberForFeature(this, "rna"),
				getString(R.string.servicesFwdrna));
	}
	
	private void enableFwdunc(final boolean status) {
		CheckBox checkbox;
		checkbox = (CheckBox) findViewById(R.id.fwdunc);
		setCheckboxDisplay(status, checkbox, getNumberForFeature(this, "unc"),
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
