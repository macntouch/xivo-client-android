package com.proformatique.android.xivoclient.xlets;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.proformatique.android.xivoclient.Connection;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.tools.Constants;

public class XletDirectory extends Activity implements XletInterface{
	
	private static final String LOG_TAG = "XLET DIRECTORY";
	private  List<HashMap<String, String>> usersList = new ArrayList<HashMap<String, String>>();


	/**
	 * Adapter class based on SimpleAdapter
	 * Allow modifying fields displayed in the ListView
	 * 
	 * @author cquaquin
	 *
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
			  icon.setBackgroundResource(R.drawable.sym_presence_away);
		  }
		  else if (text.getText().equals("donotdisturb")){
			  icon.setBackgroundResource(R.drawable.sym_presence_offline);
		  }
		  else if (text.getText().equals("outtolunch")){
			  icon.setBackgroundResource(R.drawable.sym_presence_away);
		  }
		  else {
			  icon.setBackgroundResource(R.drawable.sym_presence_offline);
		  }

		  return view;
		
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_directory);
		initDirectory();
	}

	private void initDirectory() {
		initJsonDirectory();

		AlternativeAdapter usersAdapter = new AlternativeAdapter(
				this,
				usersList,
				R.layout.xlet_directory_items,
				new String[] { "fullname","phonenum","stateid" },
				new int[] { R.id.fullname, R.id.phonenum, R.id.stateid } );
		
		ListView lv= (ListView)findViewById(R.id.users_list);
		lv.setAdapter(usersAdapter);
	}

	private int initJsonDirectory() {
		JSONObject jObj = createJsonDirectoryObject();
		if (jObj!=null){
			try {
				Log.d( LOG_TAG, "Jobj: " + jObj.toString());
				PrintStream output = new PrintStream(Connection.connection.networkConnection.getOutputStream());
				output.println(jObj.toString());
			} catch (IOException e) {
				return Constants.NO_NETWORK_AVAILABLE;
			}
			
			JSONObject ReadLineObject = Connection.connection.readJsonObjectCTI("users");
			if (ReadLineObject!=null){

				try {
					JSONArray jArr = ReadLineObject.getJSONArray("payload");
					int len = jArr.length();

					for(int i = 0; i < len; i++){
						HashMap<String, String> map = new HashMap<String, String>();
						JSONObject jObjUser = jArr.getJSONObject(i);
						
						map.put("xivo_userid", jObjUser.getString("xivo_userid"));
						map.put("fullname", jObjUser.getString("fullname"));
						map.put("phonenum", jObjUser.getString("phonenum"));
						map.put("stateid", jObjUser.getJSONObject("statedetails").getString("stateid"));
						map.put("stateid_longname", jObjUser.getJSONObject("statedetails").getString("longname"));
						usersList.add(map);
						
						Log.d( LOG_TAG, "map : " + map.toString());
					}		
				
				} catch (JSONException e) {
					e.printStackTrace();
					return Constants.JSON_POPULATE_ERROR;
				}
			}
		}

		return Constants.OK;
	}

	private JSONObject createJsonDirectoryObject() {
		JSONObject jObj = new JSONObject();
		try {
			jObj.accumulate("direction", Constants.XIVO_SERVER);
			jObj.accumulate("class","users");
			jObj.accumulate("function","getlist");
			
			return jObj;
		} catch (JSONException e) {
			return null;
		}
	}
	

}
