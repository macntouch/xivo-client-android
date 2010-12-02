package com.proformatique.android.xivoclient.xlets;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.proformatique.android.xivoclient.InitialListLoader;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletIdentity implements XletInterface{
	Activity activity = null;
	FrameLayout fIdentity = null;

	public XletIdentity(Activity activity) {
		
		this.activity = activity;
		
		TextView userName = (TextView) activity.findViewById(R.id.user_identity);
		
		List<HashMap<String, String>> usersList = InitialListLoader.initialListLoader.usersList;
		String xivoId=InitialListLoader.initialListLoader.xivoId;

		for (HashMap<String, String> hashMap : usersList) {
			if (hashMap.get("xivo_userid").equals(xivoId)){
				userName.setText(hashMap.get("fullname")+" ("+hashMap.get("phonenum")+")");
				break;
			}
		}
		
		/**
		 * Define Onclick listener
		 */
		fIdentity = (FrameLayout)activity.findViewById(R.id.includeIdentity);
		fIdentity.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clickIdentity(v);
			}
		});
		
		changeCurrentState();
		
	}

	public void changeCurrentState() {
		String currentState = InitialListLoader.initialListLoader.capaPresenceState.get("stateid");
		String currentStateName = InitialListLoader.initialListLoader.capaPresenceState.get("longname");
		ImageView iconState = (ImageView)fIdentity.findViewById(R.id.identity_current_state_image);
		TextView textState = (TextView)fIdentity.findViewById(R.id.identity_current_state_longname);
		
		iconState.setBackgroundResource(GraphicsManager.getStateIcon(currentState));
		textState.setText(currentStateName);
	}
	
	protected void clickIdentity(View v) {
		Intent defineIntent = new Intent(activity, XletIdentityStateList.class);
		activity.startActivityForResult(defineIntent, Constants.CODE_IDENTITY_STATE_LIST);
	}
	

}
