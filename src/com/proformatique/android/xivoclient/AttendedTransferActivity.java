package com.proformatique.android.xivoclient;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class AttendedTransferActivity extends TransferActivity {
	
	private String number;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = this.getIntent().getExtras();
		if (extras != null) {
			number = extras.getString("num").replace("-", "");
			Log.d(LOG_TAG, "Extras: " + number);
			if (number != null && number.equals("") != true) {
				new AttendedTransferJSONTask().execute();
				finish();
			}
		}
	}
	
	@Override
	protected void transferClicked() {
		Log.d(LOG_TAG, "Attended transfer clicked");
		EditText et = (EditText) findViewById(R.id.transfer_destination);
		number = et.getText().toString().replace("-", "");
		if (number != null && number.equals("") != true) {
			new AttendedTransferJSONTask().execute();
			finish();
		}
	}
	
	/**
	 * Creates an AsyncTask to run the call transfer
	 */
	private class AttendedTransferJSONTask extends AsyncTask<Void, Integer, Integer> {
		
		@Override
		protected void onPreExecute() {
			Log.d(LOG_TAG, "Transfering call to " + number);
			Toast.makeText(getApplicationContext(), getString(R.string.transfering, number), Toast.LENGTH_LONG).show();
			super.onPreExecute();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			JSONObject jTransferObject = createJsonTransferObject("atxfer", "chan:" + InitialListLoader.getInstance().getUserId() + ":"
					+ InitialListLoader.getInstance().getThisChannelId(), number);
			Connection.getInstance().sendJsonString(jTransferObject);
			return null;
		}
	}
}
