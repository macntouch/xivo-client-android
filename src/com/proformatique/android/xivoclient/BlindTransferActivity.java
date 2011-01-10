package com.proformatique.android.xivoclient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class BlindTransferActivity extends Activity implements OnClickListener {
	
	private final static String LOG_TAG = "XiVO BlindTransfer";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.blind_transfer);
		Log.d(LOG_TAG, "onCreate");
	}

	@Override
	public void onClick(View v) {
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
		Log.d(LOG_TAG, "transfer clicked");
	}
	
	private void cancelClicked() {
		Log.d(LOG_TAG, "cancel clicked");
	}
}
