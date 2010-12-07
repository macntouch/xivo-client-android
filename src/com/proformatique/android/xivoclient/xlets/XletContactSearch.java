package com.proformatique.android.xivoclient.xlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletContactSearch extends Activity implements XletInterface{
	
	private static final String LOG_TAG = "XLET DIRECTORY";
	private  List<HashMap<String, String>> usersList = new ArrayList<HashMap<String, String>>();
	AlternativeAdapter usersAdapter = null;
	ListView lv;
	IncomingReceiver receiver;

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
		  
		  HashMap<String, String> line = (HashMap<String, String>) lv.getItemAtPosition(position);

		  String stateIdColor = line.get("stateid_color");
		  ImageView iconState = (ImageView) view.findViewById(R.id.statusContact);
		  
		  GraphicsManager.setIconStateDisplay(XletContactSearch.this, iconState, stateIdColor);
		  
	      String colorString = line.get("hintstatus_color");
	      ImageView iconPhone = (ImageView) view.findViewById(R.id.phoneStatusContact);
	      GraphicsManager.setIconPhoneDisplay(XletContactSearch.this, iconPhone, colorString);
	      
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
		initList();
		
		receiver = new IncomingReceiver();

		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a change
		 *  in the users list from the Activity
		 */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_LOAD_USER_LIST);
        registerReceiver(receiver, new IntentFilter(filter));
	}

	private void initList() {
		usersList = InitialListLoader.initialListLoader.usersList;

		usersAdapter = new AlternativeAdapter(
				this,
				usersList,
				R.layout.xlet_search_items,
				new String[] { "fullname","phonenum","stateid","stateid_longname", "stateid_color",
						"hintstatus_code", "hintstatus_longname", "hintstatus_color" },
				new int[] { R.id.fullname, R.id.phonenum, R.id.stateid, R.id.longname_state, 0,
						R.id.phoneStateCode, R.id.phone_longname_state, R.id.phoneStateColor} );
		
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

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
}
