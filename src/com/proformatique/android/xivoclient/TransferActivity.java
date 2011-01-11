package com.proformatique.android.xivoclient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TransferActivity extends Activity implements OnClickListener {
	
	private final static String LOG_TAG = "XiVO transfer";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.transfer);
		Log.d(LOG_TAG, "onCreate");
		
		// GUI stuff
		Button cancelBtn = (Button) findViewById(R.id.button_cancel_tx);
		Button transferBtn = (Button) findViewById(R.id.button_tx);
		
		cancelBtn.setOnClickListener(this);
		transferBtn.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		Log.d(LOG_TAG, "onClick");
		switch(v.getId()) {
		case R.id.button_cancel_tx:
			cancelClicked();
			break;
		case R.id.button_tx:
			this.transferClicked();
			break;
		}
	}
	
	protected void transferClicked() {
		return;
	}

	private void cancelClicked() {
		this.finish();
	}
	
}
