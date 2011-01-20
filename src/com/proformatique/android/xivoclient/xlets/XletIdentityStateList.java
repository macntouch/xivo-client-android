/* XiVO Client Android
 * Copyright (C) 2010-2011, Proformatique
 *
 * This file is part of XiVO Client Android.
 *
 * XiVO Client Android is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XiVO Client Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.proformatique.android.xivoclient.xlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.service.Connection;
import com.proformatique.android.xivoclient.service.InitialListLoader;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletIdentityStateList extends XivoActivity {
	
	List<HashMap<String, String>> identityStateList = new ArrayList<HashMap<String, String>>();
	AlternativeAdapter stateAdapter = null;
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

		@SuppressWarnings("unchecked")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

		  View view = super.getView(position, convertView, parent);
		  HashMap<String, String> line = (HashMap<String, String>) lv.getItemAtPosition(position);
		  
	      ImageView icon = (ImageView) view.findViewById(R.id.identity_state_image);
		  String stateIdColor = line.get("color");

		  GraphicsManager.setIconStateDisplay(XletIdentityStateList.this, icon, stateIdColor);
		  return view;
		
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.xlet_identity_state);
		initList();

	}
	
	private void initList() {
		identityStateList = InitialListLoader.getInstance().getStatusList();

		stateAdapter = new AlternativeAdapter(
				this,
				identityStateList,
				R.layout.xlet_identity_state_items,
				new String[] { "longname","stateid", "color"},
				new int[] { R.id.identity_state_longname, R.id.identity_stateid, R.id.identity_color } );
		
		lv= (ListView)findViewById(R.id.identity_state_list);
		lv.setAdapter(stateAdapter);
		
        lv.setOnItemClickListener(new OnItemClickListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				HashMap<String, String> line = (HashMap<String, String>) lv.getItemAtPosition(arg2);
				clickLine(line.get("stateid"), line.get("longname"), line.get("color"));
			}

		});

	}

	/**
	 * Perform a state change
	 * 
	 * @param v
	 */
	public void clickLine(String stateId, String longName, String color){

		/**
		 * TODO : 
		 * - Call Json to change state
		 */
		JSONObject jObj = createJsonState(stateId);
		Connection.getInstance(getApplicationContext()).sendJsonString(jObj);
		
		Intent data = new Intent();
		data.putExtra("stateid", stateId);
		data.putExtra("longname", longName);
		data.putExtra("color", color);
		setResult(Constants.OK, data);
        finish();
	    
	}

	private JSONObject createJsonState(String stateId) {
		JSONObject jObj = new JSONObject();
		try {
			jObj.accumulate("direction", Constants.XIVO_SERVER);
			jObj.accumulate("class", "availstate");
			jObj.accumulate("availstate", stateId);
			
			return jObj;
		} catch (JSONException e) {
			return null;
		}
		
	}


}
