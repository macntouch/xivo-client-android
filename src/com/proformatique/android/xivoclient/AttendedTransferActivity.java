package com.proformatique.android.xivoclient;

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
	
}
