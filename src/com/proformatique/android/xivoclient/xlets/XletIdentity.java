package com.proformatique.android.xivoclient.xlets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.proformatique.android.xivoclient.InitialListLoader;
import com.proformatique.android.xivoclient.R;

public class XletIdentity implements XletInterface{

	public XletIdentity(Activity activity) {
		
		TextView userName = (TextView) activity.findViewById(R.id.user_identity);
		
		List<HashMap<String, String>> usersList = InitialListLoader.initialListLoader.usersList;
		String xivoId=InitialListLoader.initialListLoader.xivoId;

		for (HashMap<String, String> hashMap : usersList) {
			if (hashMap.get("xivo_userid").equals(xivoId)){
				userName.setText(hashMap.get("fullname")+" ("+hashMap.get("phonenum")+")");
				break;
			}
		}
		String[] statusListArray = new String[InitialListLoader.initialListLoader.statusList.size()];
		int i=0;
		for (HashMap<String, String> map : InitialListLoader.initialListLoader.statusList) {
			statusListArray[i]=map.get("longname");
			i++;
		}
/*
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, R.id.statusList, statusListArray);
		Spinner s = (Spinner) activity.findViewById(R.id.statusList);
		s.setAdapter(adapter);
*/
	}

}
