package com.proformatique.android.xivoclient;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.tools.Constants;

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
			JSONObject jTransferObject = createJsonTransferObject("atxfer", SettingsActivity
					.getMobileNumber(getApplicationContext()), number);
			Connection.getInstance().sendJsonString(jTransferObject);
			return null;
		}
	}
	
	private JSONObject createJsonTransferObject(String inputClass,
		String numSrc, String numDest) {
		if (numSrc == null || numSrc.equals("")) {
			numSrc = "user:special:me";
		} else {
			numSrc = "ext:"+numSrc;
		}
		
		JSONObject jsonTransfer = new JSONObject();
		try {
			jsonTransfer.accumulate("direction", Constants.XIVO_SERVER);
			jsonTransfer.accumulate("class", inputClass);
			jsonTransfer.accumulate("source",
					"chan:" + InitialListLoader.getInstance().getUserId() + ":"
					+ InitialListLoader.getInstance().getChannelId());
			jsonTransfer.accumulate("destination", "ext:"+numDest);
			
			return jsonTransfer;
		} catch (JSONException e) {
			return null;
		}
	}
}
