package com.proformatique.android.xivoclient.xlets;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.proformatique.android.xivoclient.Connection;
import com.proformatique.android.xivoclient.InitialListLoader;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletHisto extends Activity implements XletInterface{
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
        
        sendListRefresh();

	}

	private void sendListRefresh() {
		new Thread(new Runnable() {
		    public void run() {
		        sendListRefresh("0","10");
		        sendListRefresh("1","10");
		        sendListRefresh("2","10");
		      }
		    }).start();
	}

	private void initList() {
		xletList = InitialListLoader.initialListLoader.historyList;

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
							xletAdapter.notifyDataSetChanged();
						}
					});
	        }
		}
	}
	
	private void sendListRefresh(String mode, String elementsNumber){
		JSONObject jObj = new JSONObject();
		
		try {
			SimpleDateFormat sIso = new SimpleDateFormat("yyyy-MM-dd");
			Date dDay = new Date();
			Calendar c1 = new GregorianCalendar();
			c1.setTime(dDay);
			c1.add(Calendar.DAY_OF_MONTH, -30);
			
			jObj.accumulate("direction", Constants.XIVO_SERVER);
			jObj.accumulate("class","history");
			jObj.accumulate("peer", InitialListLoader.initialListLoader.astId+"/"+
					InitialListLoader.initialListLoader.xivoId);
			jObj.accumulate("size",elementsNumber);
			jObj.accumulate("mode",mode);
			jObj.accumulate("morerecentthan",sIso.format(c1.getTime()));
			
			PrintStream output = new PrintStream(
					Connection.getInstance().networkConnection.getOutputStream());
			output.println(jObj.toString());
			Log.d( LOG_TAG , "Client : "+jObj.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class AlternativeAdapter extends SimpleAdapter {

		public AlternativeAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource, String[] from,
				int[] to) {
			super(context, data, resource, from, to);
		}

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
}
