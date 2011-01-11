package com.proformatique.android.xivoclient;

import android.os.AsyncTask;
import android.widget.EditText;
import android.widget.Toast;

public class AttendedTransferActivity extends TransferActivity {
	
	@Override
	protected void transferClicked() {
		EditText et = (EditText) findViewById(R.id.transfer_destination);
		String number = et.getText().toString();
		if (number != null && number.equals("") != true) {
			// transfer the call here
			Toast.makeText(getApplicationContext(), getString(R.string.transfering, number), Toast.LENGTH_LONG).show();
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
			Toast.makeText(getApplicationContext(), getString(R.string.transfering, number), Toast.LENGTH_LONG).show();
			super.onPreExecute();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
