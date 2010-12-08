package com.proformatique.android.xivoclient.xlets;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.tools.Constants;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class XletServicesAsk extends Activity implements XletInterface{

	private String serviceType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_services_ask);
		Intent intent = getIntent();
		serviceType = intent.getExtras().getString("serviceType");
		setTitle(R.string.ServicesFwdTitle);
	}
	
	public void clickOnCancel(View v){
		setResult(Constants.CANCEL);
		finish();
	}

	public void clickOnOk(View v){
		EditText phoneView = (EditText)findViewById(R.id.servicesAskPhone);
		
		Intent intentOk = new Intent();
		intentOk.putExtra("phoneNumber", phoneView.getText().toString());
		setResult(Constants.OK, intentOk);
		finish();
	}

}
