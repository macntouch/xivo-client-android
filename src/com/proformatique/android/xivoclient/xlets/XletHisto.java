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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.proformatique.android.xivoclient.InitialListLoader;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletHisto extends Activity{
	private static final String LOG_TAG = "XLET HISTORY";
	private  List<HashMap<String, String>> xletList = new ArrayList<HashMap<String, String>>();
	AlternativeAdapter xletAdapter = null;
	ListView lv;
	IncomingReceiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_history);
		initList();
		
		receiver = new IncomingReceiver();

		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a change
		 *  in the list from the Activity
		 */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_LOAD_HISTORY_LIST);
        registerReceiver(receiver, new IntentFilter(filter));
	}


	private void initList() {
		xletList = InitialListLoader.getInstance().getHistoryList();

		xletAdapter = new AlternativeAdapter(
				this,
				xletList,
				R.layout.xlet_history_items,
				new String[] { "fullname","ts","duration" },
				new int[] { R.id.history_fullname, R.id.history_date, R.id.history_duration} );

		lv= (ListView)findViewById(R.id.history_list);
		lv.setAdapter(xletAdapter);
		
	}	

	/**
	 * BroadcastReceiver, intercept Intents with action ACTION_LOAD_HISTORY_LIST
	 * to perform an reload of the displayed list
	 * @author cquaquin
	 *
	 */
	private class IncomingReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(Constants.ACTION_LOAD_HISTORY_LIST)) {
	        	Log.d( LOG_TAG , "Received Broadcast "+Constants.ACTION_LOAD_HISTORY_LIST);
	        	if (xletAdapter != null) 
	        		runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							xletList = InitialListLoader.getInstance().getHistoryList();
							xletAdapter.notifyDataSetChanged();
						}
					});
	        }
		}
	}
	
	private class AlternativeAdapter extends SimpleAdapter {

		public AlternativeAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource, String[] from,
				int[] to) {
			super(context, data, resource, from, to);
		}

		@SuppressWarnings("unchecked")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

		  View view = super.getView(position, convertView, parent);
		  
		  HashMap<String, String> line = (HashMap<String, String>) lv.getItemAtPosition(position);
		  String direction = line.get("direction");
		  
	      ImageView icon = (ImageView) view.findViewById(R.id.callStatus);
	      icon.setBackgroundResource(GraphicsManager.getCallIcon(direction));
		  
		  return view;
		
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}

}
