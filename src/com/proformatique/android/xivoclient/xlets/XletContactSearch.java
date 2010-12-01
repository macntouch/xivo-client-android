package com.proformatique.android.xivoclient.xlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.proformatique.android.xivoclient.InitialListLoader;
import com.proformatique.android.xivoclient.JsonLoopListener;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XletsContainerTabActivity;
import com.proformatique.android.xivoclient.tools.Constants;

public class XletContactSearch extends Activity implements XletInterface{
	
	private static final String LOG_TAG = "XLET DIRECTORY";
	private  List<HashMap<String, String>> usersList = new ArrayList<HashMap<String, String>>();
	AlternativeAdapter usersAdapter = null;
	ListView lv;

	/**
	 * Adapter subclass based on SimpleAdapter
	 * Allow modifying fields displayed in the ListView
	 * 
	 * @author cquaquin
	 */
	private class AlternativeAdapter extends SimpleAdapter {

		public AlternativeAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource, String[] from,
				int[] to) {
			super(context, data, resource, from, to);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

		  View view = super.getView(position, convertView, parent);
		  
	      TextView text = (TextView) view.findViewById(R.id.stateid);
	      ImageView icon = (ImageView) view.findViewById(R.id.statusContact);
		  
		  if (text.getText().equals("available")){
			  icon.setBackgroundResource(R.drawable.sym_presence_available);
		  }
		  else if (text.getText().equals("berightback")){
			  icon.setBackgroundResource(R.drawable.sym_presence_idle);
		  }
		  else if (text.getText().equals("away")){
			  icon.setBackgroundResource(R.drawable.sym_presence_idle);
		  }
		  else if (text.getText().equals("donotdisturb")){
			  icon.setBackgroundResource(R.drawable.sym_presence_away);
		  }
		  else if (text.getText().equals("outtolunch")){
			  icon.setBackgroundResource(R.drawable.sym_presence_idle);
		  }
		  else {
			  icon.setBackgroundResource(R.drawable.sym_presence_offline);
		  }

		  return view;
		
		}
	}
	
	/**
	 * BroadcastReceiver, intercept Intents with action ACTION_LOAD_USER_LIST
	 * to perform an reload of the displayed list
	 * @author cquaquin
	 *
	 */
	public class IncomingReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(Constants.ACTION_LOAD_USER_LIST)) {
	        	Log.d( LOG_TAG , "Received Broadcast ");
	        	if (usersAdapter != null) usersAdapter.notifyDataSetChanged();
	        }
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_search);
		initDirectory();
		
		IncomingReceiver receiver = new IncomingReceiver();

		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a change
		 *  in the users list from our Activity
		 */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_LOAD_USER_LIST);
        registerReceiver(receiver, new IntentFilter(filter));
	}

	private void initDirectory() {
		usersList = InitialListLoader.initialListLoader.usersList;

		usersAdapter = new AlternativeAdapter(
				this,
				usersList,
				R.layout.xlet_search_items,
				new String[] { "fullname","phonenum","stateid","stateid_longname" },
				new int[] { R.id.fullname, R.id.phonenum, R.id.stateid, R.id.longname_state } );
		
		lv= (ListView)findViewById(R.id.users_list);
		lv.setAdapter(usersAdapter);
		
        lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				HashMap<String, String> line = (HashMap<String, String>) lv.getItemAtPosition(arg2);
				clickLine(line.get("phonenum"));

			}

		});

	}

	/**
	 * Perform a call via Dial Activity
	 * 
	 * @param v
	 */
	public void clickLine(String numToCall){
		
    	Intent defineIntent = new Intent();
    	defineIntent.setAction(Constants.ACTION_XLET_DIAL_CALL);
    	defineIntent.putExtra("numToCall", numToCall);
		
	    XletContactSearch.this.sendBroadcast(defineIntent);
	    
	}
}
