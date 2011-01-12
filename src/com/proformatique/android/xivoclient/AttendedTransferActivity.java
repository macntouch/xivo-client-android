package com.proformatique.android.xivoclient;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class AttendedTransferActivity extends TransferActivity {
	
	@Override
	protected void transferClicked() {
		Log.d(LOG_TAG, "Attended transfer clicked");
		EditText et = (EditText) findViewById(R.id.transfer_destination);
		String number = et.getText().toString();
		if (number != null && number.equals("") != true) {
			new AttendedTransferJSONTask().execute();
			finish();
		}
	}
	
	/**
	 * Creates an AsyncTask to run the call transfer
	 */
	private class AttendedTransferJSONTask extends AsyncTask<Void, Integer, Integer> {
		
		private String number;
		
		@Override
		protected void onPreExecute() {
			EditText et = (EditText) findViewById(R.id.transfer_destination);
			number = et.getText().toString().replace("-", "");
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
