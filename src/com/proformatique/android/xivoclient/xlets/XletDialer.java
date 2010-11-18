package com.proformatique.android.xivoclient.xlets;

import com.proformatique.android.xivoclient.R;

import android.app.Activity;
import android.os.Bundle;

public class XletDialer extends Activity implements XletInterface{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_dialer);
	}
	
	

}
