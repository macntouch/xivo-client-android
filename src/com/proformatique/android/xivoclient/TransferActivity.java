/* XiVO Client Android
 * Copyright (C) 2010-2011, Proformatique
 *
 * This file is part of XiVO Client Android.
 *
 * XiVO Client Android is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XiVO Client Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XiVO client Android.  If not, see <http://www.gnu.org/licenses/>.
 */

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
