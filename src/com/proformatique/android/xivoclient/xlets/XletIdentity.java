package com.proformatique.android.xivoclient.xlets;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.service.InitialListLoader;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletIdentity{
	private static final String LOG_TAG = "XletIdentity";
	Activity activity = null;
	FrameLayout fIdentity = null;

	public XletIdentity(Activity activity) {
		
		this.activity = activity;
		
		TextView userName = (TextView) activity.findViewById(R.id.user_identity);
		
		InitialListLoader init = InitialListLoader.getInstance();
		List<HashMap<String, String>> usersList = null;
		String xivoId = null;
		if (init != null) {
			usersList = init.getUsersList();
			xivoId = init.getXivoId();
			
			if (usersList != null) {
				for (HashMap<String, String> hashMap : usersList) {
					if (hashMap.get("xivo_userid") != null && hashMap.get("xivo_userid").equals(xivoId)){
						userName.setText(hashMap.get("fullname")+" ("+hashMap.get("phonenum")+")");
						break;
					} else {
						Log.i(LOG_TAG, "Users identity loading but it's not a XivoUser");
					}
				}
			}
		}
		
		/**
		 * Define Onclick listener
		 */
		fIdentity = (FrameLayout)activity.findViewById(R.id.includeIdentity);
		FrameLayout clickZone = (FrameLayout)fIdentity.findViewById(R.id.identityClickZone);
		clickZone.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clickIdentity(v);
			} 
		});
		
		changeCurrentState();
		changeCurrentPhone();
		
	}
	
	public void changeCurrentPhone() {
		InitialListLoader init = InitialListLoader.getInstance();
		if (init != null) {
			ImageView iconPhone = (ImageView)fIdentity.findViewById(R.id.identityPhoneStatus);
			TextView textPhone = (TextView)fIdentity.findViewById(R.id.identityPhoneLongnameState);
			String colorString = init.getCapaPresenceState().get("hintstatus_color");
			GraphicsManager.setIconPhoneDisplay(activity, iconPhone, colorString);
			textPhone.setText(init.getCapaPresenceState().get("hintstatus_longname"));
		}
	}

	public void changeCurrentState() {
		InitialListLoader init = InitialListLoader.getInstance();
		if (init != null) {
			String currentStateName = init.getCapaPresenceState().get("longname");
			String stateIdColor = init.getCapaPresenceState().get("color");
			ImageView iconState = (ImageView)fIdentity.findViewById(R.id.identity_current_state_image);
			TextView textState = (TextView)fIdentity.findViewById(R.id.identity_current_state_longname);
			
			GraphicsManager.setIconStateDisplay(activity, iconState, stateIdColor);
			textState.setText(currentStateName);
		}
	}
	
	protected void clickIdentity(View v) {
		Intent defineIntent = new Intent(activity, XletIdentityStateList.class);
		activity.startActivityForResult(defineIntent, Constants.CODE_IDENTITY_STATE_LIST);
	}
	

}
