package com.proformatique.android.xivoclient.xlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.service.InitialListLoader;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletContactSearch extends XivoActivity {
	
	private static final String LOG_TAG = "XLET DIRECTORY";
	private List <HashMap<String, String>> filteredUsersList = new ArrayList<HashMap<String, String>>();
	private EditText et;
	AlternativeAdapter usersAdapter = null;
	ListView lv;
	IncomingReceiver receiver;
	SearchReceiver searchReceiver;
	private List<HashMap<String, String>> contacts = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_search);
		contacts = InitialListLoader.getInstance().getAllContacts(getApplicationContext());
		filllist("");
		initListView();
		
		receiver = new IncomingReceiver();
		searchReceiver = new SearchReceiver();
		
		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a change
		 *  in the users list from the Activity
		 */
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_LOAD_USER_LIST);
		registerReceiver(receiver, new IntentFilter(filter));
		
		IntentFilter searchFilter = new IntentFilter();
		searchFilter.addAction(Constants.ACTION_REFRESH_USER_LIST);
		registerReceiver(searchReceiver, new IntentFilter(searchFilter));
		
		registerForContextMenu(lv);
		
		et = (EditText)findViewById(R.id.SearchEdit);
		et.addTextChangedListener(
				new TextWatcher() {
					public void afterTextChanged(Editable s) {}
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						Intent definedIntent = new Intent();
						definedIntent.setAction(Constants.ACTION_REFRESH_USER_LIST);
						XletContactSearch.this.sendBroadcast(definedIntent);
					}
				}
		);
	}
	
	protected void onResume() {
		super.onResume();
		contacts = InitialListLoader.getInstance().getAllContacts(getApplicationContext());
		refreshFilteredList();
		filllist("");
		initListView();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		switch (v.getId()){
		case R.id.users_list:
			{
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
				menu.setHeaderTitle(getString(R.string.context_action));
				String callAction = getString(R.string.context_action_call, 
						filteredUsersList.get(info.position).get("fullname"), 
						filteredUsersList.get(info.position).get("phonenum"));
				menu.add(0, 1, 0, callAction);
			}
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		String phoneNumber = filteredUsersList.get(info.position).get("phonenum");
		clickLine(phoneNumber);
		
		return super.onContextItemSelected(item);
	}
	
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
				if (usersAdapter != null) {
					contacts = InitialListLoader.getInstance().getUsersList();
					filllist(et.getText().toString());
					usersAdapter.notifyDataSetChanged();
				}
			}
		}
	}
	
	/**
	 * BroadcastReceiver, intercept Intents with action ACTION_REFRESH_USER_LIST
	 * to perform a reload of the displayer list
	 * 
	 */
	public class SearchReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.ACTION_REFRESH_USER_LIST)) {
				Log.d(LOG_TAG, "Received search Broadcast");
				if (usersAdapter != null) {
					filllist(et.getText().toString());
					initListView();
					usersAdapter.notifyDataSetChanged();
				}
			}
		}
	}
	
	private void refreshFilteredList() {
		int contactsLen = contacts != null ? contacts.size() : 0;
		
		if (filteredUsersList == null) {
			filteredUsersList = new ArrayList<HashMap<String, String>>(contactsLen);
		} else {
			filteredUsersList.clear();
		}
		
		if (contacts != null)
			filteredUsersList.addAll(contacts);		
	}
	
	/**
	 * Fill the filtered user list to display on the listview
	 * 
	 * @param filter -- The search value to look for
	 */
	private void filllist(String filter) {
		refreshFilteredList();
		if (filter.equals("") == false) {
			List<HashMap<String, String>> tmp = filteredUsersList;
			filteredUsersList = new ArrayList<HashMap<String, String>>();
			int len = filter.length();
			for (HashMap<String, String> user: tmp) {
				if (len <= user.get("fullname").length() && filter.equalsIgnoreCase((String) user.get("fullname").subSequence(0, len))) {
					filteredUsersList.add(user);
				}
			}
		}
	}
	
	/**
	 * Initialize the ListView for the searched contacts
	 */
	private void initListView() {
		usersAdapter = new AlternativeAdapter(
				this,
				filteredUsersList,
				R.layout.xlet_search_items,
				new String[] { "fullname","phonenum","stateid","stateid_longname", "stateid_color",
						"hintstatus_code", "hintstatus_longname", "hintstatus_color" },
				new int[] { R.id.fullname, R.id.phonenum, R.id.stateid, R.id.longname_state, 0,
						R.id.phoneStateCode, R.id.phone_longname_state, R.id.phoneStateColor} );
		
		lv= (ListView)findViewById(R.id.users_list);
		lv.setAdapter(usersAdapter);
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
		et.setText("");
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		unregisterReceiver(searchReceiver);
		super.onDestroy();
	}
}
