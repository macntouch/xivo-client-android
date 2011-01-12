package com.proformatique.android.xivoclient;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.tools.Constants;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TransferActivity extends Activity implements OnClickListener {
	
	final static String LOG_TAG = "XiVO transfer";
	
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
	
	protected JSONObject createJsonTransferObject(String inputClass, String numSrc, String numDest) {
		JSONObject jsonTransfer = new JSONObject();
		try {
			jsonTransfer.accumulate("direction", Constants.XIVO_SERVER);
			jsonTransfer.accumulate("class", inputClass);
			jsonTransfer.accumulate("source", numSrc);
			jsonTransfer.accumulate("destination", "ext:"+numDest);
			
			return jsonTransfer;
		} catch (JSONException e) {
			return null;
		}
	}
}
