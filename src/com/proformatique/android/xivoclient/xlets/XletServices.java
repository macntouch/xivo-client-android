package com.proformatique.android.xivoclient.xlets;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.Connection;
import com.proformatique.android.xivoclient.InitialListLoader;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.SettingsActivity;
import com.proformatique.android.xivoclient.tools.Constants;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

public class XletServices extends Activity implements XletInterface{

	private static final String LOG_TAG = "XLET SERVICES";
	private IncomingReceiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_services);

		receiver = new IncomingReceiver();

		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a change
		 *  in the list from the Activity
		 */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_LOAD_FEATURES);
        registerReceiver(receiver, new IntentFilter(filter));

		
		sendListRefresh();
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
			 setCheckboxDisplay(requestCode, checkbox, phoneNumber, textDisplay);
		 }

		 else if (requestCode == Constants.CODE_SERVICE_ASK2) {
			 checkbox = (CheckBox) findViewById(R.id.fwdbusy);
			 textDisplay = getString(R.string.servicesFwdbusy);
			 setCheckboxDisplay(requestCode, checkbox, phoneNumber, textDisplay);
		 }

		 if (requestCode == Constants.CODE_SERVICE_ASK3) {
			 checkbox = (CheckBox) findViewById(R.id.fwdunc);
			 textDisplay = getString(R.string.servicesFwdunc);
			 setCheckboxDisplay(requestCode, checkbox, phoneNumber, textDisplay);
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
	 
		private void sendListRefresh(){
			JSONObject jObj = new JSONObject();
			
			try {
				jObj.accumulate("direction", Constants.XIVO_SERVER);
				jObj.accumulate("class","featuresget");
				jObj.accumulate("userid", InitialListLoader.initialListLoader.astId+"/"+
						InitialListLoader.initialListLoader.xivoId);
				
				PrintStream output = new PrintStream(
						Connection.getInstance().networkConnection.getOutputStream());
				output.println(jObj.toString());
				Log.d( LOG_TAG , "Client : "+jObj.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
			HashMap<String, String> featureMap;
			CheckBox checkbox;
			int code=0;
			
			featureMap = InitialListLoader.initialListLoader.featuresBusy;
			if (featureMap.containsKey("enabled")){
				checkbox = (CheckBox) findViewById(R.id.fwdbusy);
				if (featureMap.get("enabled").equals("true")) code = Constants.OK;
				else code = Constants.CANCEL;
				setCheckboxDisplay(code, checkbox, featureMap.get("number"), 
						getString(R.string.servicesFwdbusy));
			}
			
			featureMap = InitialListLoader.initialListLoader.featuresRna;
			if (featureMap.containsKey("enabled")){
				checkbox = (CheckBox) findViewById(R.id.fwdrna);
				if (featureMap.get("enabled").equals("true")) code = Constants.OK;
				else code = Constants.CANCEL;
				setCheckboxDisplay(code, checkbox, featureMap.get("number"), 
						getString(R.string.servicesFwdrna));
			}
			
			featureMap = InitialListLoader.initialListLoader.featuresUnc;
			if (featureMap.containsKey("enabled")){
				checkbox = (CheckBox) findViewById(R.id.fwdunc);
				if (featureMap.get("enabled").equals("true")) code = Constants.OK;
				else code = Constants.CANCEL;
				setCheckboxDisplay(code, checkbox, featureMap.get("number"), 
						getString(R.string.servicesFwdunc));
			}

			featureMap = InitialListLoader.initialListLoader.featuresEnablednd;
			if (featureMap.containsKey("enabled")){
				checkbox = (CheckBox) findViewById(R.id.enablednd);
				if (featureMap.get("enabled").equals("true")) checkbox.setChecked(true);
				else checkbox.setChecked(false);
			}

			featureMap = InitialListLoader.initialListLoader.featuresCallrecord;
			if (featureMap.containsKey("enabled")){
				checkbox = (CheckBox) findViewById(R.id.callrecord);
				if (featureMap.get("enabled").equals("true")) checkbox.setChecked(true);
				else checkbox.setChecked(false);
			}

			featureMap = InitialListLoader.initialListLoader.featuresIncallfilter;
			if (featureMap.containsKey("enabled")){
				checkbox = (CheckBox) findViewById(R.id.incallfilter);
				if (featureMap.get("enabled").equals("true")) checkbox.setChecked(true);
				else checkbox.setChecked(false);
			}
}


}
