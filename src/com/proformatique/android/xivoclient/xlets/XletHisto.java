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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.service.HistoryProvider;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletHisto extends XivoActivity {
	
	private static final String LOG_TAG = "XiVO " + XletHisto.class.getSimpleName();
	private  List<HashMap<String, String>> xletList = null;
	AlternativeAdapter xletAdapter = null;
	ListView lv;
	IncomingReceiver receiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_history);
		lv = (ListView)findViewById(R.id.history_list);
		xletList = HistoryProvider.getList(this);
		sortHistory();
		xletAdapter = new AlternativeAdapter(
				this,
				xletList,
				R.layout.xlet_history_items,
				new String[] {"fullname", "ts", "duration"},
				new int[] {R.id.history_fullname, R.id.history_date, R.id.history_duration});
		lv.setAdapter(xletAdapter);
		
		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a change
		 *  in the list from the Activity
		 */
		receiver = new IncomingReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_LOAD_HISTORY_LIST);
		registerReceiver(receiver, new IntentFilter(filter));
		registerForContextMenu(lv);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		switch (v.getId()){
		case R.id.history_list:
			AdapterView.AdapterContextMenuInfo info =
					(AdapterView.AdapterContextMenuInfo)menuInfo;
			menu.setHeaderTitle(getString(R.string.context_action));
			try {
				if (xivoConnectionService.isOnThePhone()) {
					menu.add(Constants.TRANSFER_MENU, Constants.ATXFER_ITEM_INDEX, 0, 
							getString(R.string.attended_transfer_title));
					menu.add(Constants.TRANSFER_MENU, Constants.TRANSFER_ITEM_INDEX, 0, 
							getString(R.string.blind_transfer_title));
				} else {
					String callAction = getString(R.string.context_action_call_short, 
							xletList.get(info.position).get("fullname"));
					menu.add(0, 1, 0, callAction);
				}
			} catch (RemoteException e) {}
			break;
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info =
				(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		int menuItemIndex = item.getItemId();
		String fullname = xletList.get(info.position).get("fullname");
		String phoneString = "";
		try {
			@SuppressWarnings("unused")
			long phoneInt = Long.parseLong(fullname); 
			phoneString = fullname;
		} catch (Exception e) {
			Pattern p = Pattern.compile(".*?<([^>]+)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher m = p.matcher(fullname);
			if (m.find())
				phoneString=m.group(1);
		}
		
		if (!phoneString.equals("")) {
			try {
				if (xivoConnectionService.isOnThePhone()){
					switch (menuItemIndex) {
					case Constants.ATXFER_ITEM_INDEX:
						xivoConnectionService.atxfer(phoneString);
						break;
					case Constants.TRANSFER_ITEM_INDEX:
						xivoConnectionService.transfer(phoneString);
						break;
					}
				} else {
					Intent iCall = new Intent(this, XletDialer.class);
					iCall.putExtra("numToCall", phoneString);
					startActivity(iCall);
				}
			} catch (RemoteException e) {
				Toast.makeText(this, getString(R.string.remote_exception),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(XletHisto.this, R.string.call_no_phone_number, Toast.LENGTH_LONG).show();
		}
		
		return super.onContextItemSelected(item);
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
							xletList = HistoryProvider.getList(XletHisto.this);
							sortHistory();
							xletAdapter.notifyDataSetChanged();
						}
					});
			}
		}
	}
	
	// TODO: Change the adapter to user a cursor instead of a List<HashMap<String, String>>
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
	
	@SuppressWarnings("unchecked")
	private void sortHistory() {
		Collections.sort(xletList, new DateComparator());
	}
	
	@SuppressWarnings("unchecked")
	private class DateComparator implements Comparator
	{
		public int compare(Object obj1, Object obj2)
		{
			SimpleDateFormat sd1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			HashMap<String, String> update1 = (HashMap<String, String>)obj1;
			HashMap<String, String> update2 = (HashMap<String, String>)obj2;
			Date d1 = null, d2 = null;
			try {
				d1 = sd1.parse(update1.get("ts"));
				d2 = sd1.parse(update2.get("ts"));
			} catch (ParseException e) {
				e.printStackTrace();
				return 0;
			}
			return (((d2.getTime()-d1.getTime())>0)?1:-1);
		}
	}
}
