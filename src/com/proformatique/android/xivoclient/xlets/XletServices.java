package com.proformatique.android.xivoclient.xlets;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.SettingsActivity;
import com.proformatique.android.xivoclient.tools.Constants;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.CheckBox;

public class XletServices extends Activity implements XletInterface{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_services);

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
		 if (code == Constants.CANCEL){
			 checkbox.setChecked(false);
			 checkbox.setText(textDisplay);
		 } else {
			 checkbox.setText(textDisplay + "\n"+getString(R.string.servicesPhone)+phoneNumber);
		 }
		checkbox.setClickable(true);

	 }


}
