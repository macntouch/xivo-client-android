package com.proformatique.android.xivoclient.tools;

import com.proformatique.android.xivoclient.R;

public class GraphicsManager {
	
	public static int getStateIcon(String stateId){

		int iconId = 0;
		
		  if (stateId.equals("available")){
			  iconId = R.drawable.sym_presence_available;
		  }
		  else if (stateId.equals("berightback")){
			  iconId = R.drawable.sym_presence_idle;
		  }
		  else if (stateId.equals("away")){
			  iconId = R.drawable.sym_presence_idle;
		  }
		  else if (stateId.equals("donotdisturb")){
			  iconId = R.drawable.sym_presence_away;
		  }
		  else if (stateId.equals("outtolunch")){
			  iconId = R.drawable.sym_presence_idle;
		  }
		  else {
			  iconId = R.drawable.sym_presence_offline;
		  }

		
		return iconId;
	}

	public static int getCallIcon(String direction) {
		int iconId = 0;
		
		if (direction.equals("IN")) {
			iconId = R.drawable.call_received;
		}
		else if (direction.equals("OUT")) {
			iconId = R.drawable.call_sent;
		}
		
		return iconId;
	}

}
