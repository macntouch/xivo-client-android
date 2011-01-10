package com.proformatique.android.xivoclient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class BlindTransferActivity extends Activity implements OnClickListener {
	
	private final static String LOG_TAG = "XiVO BlindTransfer";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.blind_transfer);
		Log.d(LOG_TAG, "onCreate");
		
		// GUI stuff
		Button cancelBtn = (Button) findViewById(R.id.button_cancel_blind_tx);
		Button transferBtn = (Button) findViewById(R.id.button_blind_tx);
		
		cancelBtn.setOnClickListener(this);
		transferBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Log.d(LOG_TAG, "onClick");
		switch(v.getId()) {
		case R.id.button_cancel_blind_tx:
			cancelClicked();
			break;
		case R.id.button_blind_tx:
			transferClicked();
			break;
		}
	}
	
	private void transferClicked() {
		EditText et = (EditText) findViewById(R.id.transfer_destination);
		String number = et.getText().toString();
		if (number != null && number.equals("") != true) {
			// transfer the call here
			Toast.makeText(getApplicationContext(), "transfering to " + number, Toast.LENGTH_LONG).show();
			finish();
		}
	}
	
	private void cancelClicked() {
		this.finish();
	}
}
