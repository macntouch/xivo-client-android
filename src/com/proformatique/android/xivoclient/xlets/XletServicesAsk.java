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

package com.proformatique.android.xivoclient.xlets;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.service.InitialListLoader;
import com.proformatique.android.xivoclient.tools.Constants;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class XletServicesAsk extends XivoActivity {

	private String serviceType;
	private EditText phoneView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_services_ask);
		Intent intent = getIntent();
		serviceType = intent.getExtras().getString("serviceType");
		setTitle(R.string.ServicesFwdTitle);
		
		phoneView = (EditText)findViewById(R.id.servicesAskPhone);
		String phone = "";
		if (serviceType.equals("fwdrna")) 
			phone = InitialListLoader.getInstance().getFeaturesRna().get("number");
		else if (serviceType.equals("fwdbusy")) 
			phone = InitialListLoader.getInstance().getFeaturesBusy().get("number");
		if (serviceType.equals("fwdunc")) 
			phone = InitialListLoader.getInstance().getFeaturesUnc().get("number");

		phoneView.setText(phone);
	}
	
	public void clickOnCancel(View v){
		cancel();
	}

	public void clickOnOk(View v){
		
		Intent intentOk = new Intent();
		intentOk.putExtra("phoneNumber", phoneView.getText().toString());
		setResult(Constants.OK, intentOk);
		finish();
	}
	
	public void onBackPressed() {
		cancel();
	}
	
	private void cancel() {
		Intent intentCancel = new Intent();
		intentCancel.putExtra("phoneNumber", "");
		
		setResult(Constants.CANCEL, intentCancel);
		finish();
	}

}
