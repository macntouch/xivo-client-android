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
 * along with XiVO client Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.proformatique.android.xivoclient.xlets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.service.CapapresenceProvider;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletIdentityStateList extends XivoActivity {
	
	private final static String TAG = "State list";
	List<HashMap<String, String>> identityStateList = null;
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
		
		lv= (ListView)findViewById(R.id.identity_state_list);
		identityStateList = CapapresenceProvider.getStateList(this);
		Log.d(TAG, identityStateList.toString());
		stateAdapter = new AlternativeAdapter(
				this, identityStateList, R.layout.xlet_identity_state_items,
				new String[] {"longname", "stateid", "color"},
				new int[] {R.id.identity_state_longname,
						R.id.identity_stateid, R.id.identity_color});
		
		lv.setAdapter(stateAdapter);
		lv.setOnItemClickListener(new OnItemClickListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				HashMap<String, String> line = (HashMap<String, String>) lv.getItemAtPosition(arg2);
				clickLine(line.get("stateid"), line.get("longname"), line.get("color"));
			}
		});
		registerButtons();
	}
	
	/**
	 * Perform a state change
	 * 
	 * @param stateId, longName, color
	 */
	public void clickLine(String stateId, String longName, String color){
		
		try {
			xivoConnectionService.setState(stateId);
			finish();
		} catch (RemoteException e) {
			Toast.makeText(this, getString(R.string.remote_exception), Toast.LENGTH_SHORT).show();
		}
	}
    
    @Override
    protected void setUiEnabled(boolean state) {
        super.setUiEnabled(state);
        if (lv != null) {
            lv.setEnabled(state);
        }
    }
}
