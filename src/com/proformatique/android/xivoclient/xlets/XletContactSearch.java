package com.proformatique.android.xivoclient.xlets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
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
import com.proformatique.android.xivoclient.InitialListLoader;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletContactSearch extends XivoActivity {
	
	private static final String LOG_TAG = "XLET DIRECTORY";
	private  List<HashMap<String, String>> usersList = new ArrayList<HashMap<String, String>>();
	private List <HashMap<String, String>> filteredUsersList = new ArrayList<HashMap<String, String>>();
	private EditText et;
	AlternativeAdapter usersAdapter = null;
	ListView lv;
	IncomingReceiver receiver;
	SearchReceiver searchReceiver;
	private SharedPreferences settings;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_search);
		usersList = InitialListLoader.getInstance().getUsersList();
		settings = PreferenceManager.getDefaultSharedPreferences(this);
	        
        if (settings.getBoolean("include_device_contacts", false)) {
        	setAndroidContacts();
        }
		if (usersList.size() != 0){
			Collections.sort(usersList, new fullNameComparator());
		}
		copyList(usersList, filteredUsersList);
		filllist("");
		initList();
		
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
    			
    			public void afterTextChanged(Editable s) {    				
    			}
    			
    			public void beforeTextChanged(CharSequence s, int start, int count, int after) {    				
    			}
    			
    			public void onTextChanged(CharSequence s, int start, int before, int count) {
    				Intent definedIntent = new Intent();
    		    	definedIntent.setAction(Constants.ACTION_REFRESH_USER_LIST);        				
    			    XletContactSearch.this.sendBroadcast(definedIntent);
    			}
    		}
        );
        
	}
	
	/**
	 * Reads Android contacts from the device and adds them to the usersList
	 */
	private void setAndroidContacts() {
		// Get all contacts
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
            null, null, null);
        
        while (cursor.moveToNext()) {
            String contactId =
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            // Read phone numbers
            Cursor phones = cr.query(Phone.CONTENT_URI, null,
                Phone.CONTACT_ID + " = " + contactId, null, null);
            while (phones.moveToNext()) {
                HashMap<String, String> contact = new HashMap<String, String>();
                contact.put("fullname", name);
                contact.put("phonenum", phones.getString(phones.getColumnIndex(Phone.NUMBER)));
                contact.put("hintstatus_longname",
                		(String) Phone.getTypeLabel(
                				this.getResources(), phones.getInt(
                						phones.getColumnIndex(Phone.TYPE)), "test"));
                contact.put("stateid_longname", "Android");
                contact.put("hintstatus_color", "#FFFFFF");
                contact.put("stateid_color", "grey");
                usersList.add(contact);
            }
            phones.close();
        }
        cursor.close();
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

		  String stateIdColor = "#FFFFFF";
		  if (line.containsKey("stateid_color"))
		  	stateIdColor = line.get("stateid_color");
		  
		  ImageView iconState = (ImageView) view.findViewById(R.id.statusContact);
		  
		  GraphicsManager.setIconStateDisplay(XletContactSearch.this, iconState, stateIdColor);
		  
		  String colorString = "#FFFFFF";
		  if (line.containsKey("hintstatus_color"))
			  colorString = line.get("hintstatus_color");
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
	        		usersList = InitialListLoader.getInstance().getUsersList();
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
					initList();
					usersAdapter.notifyDataSetChanged();
				}
			}
		}
	}
	
	/**
	 * Fill the filtered user list to display on the listview
	 * 
	 * @param filter -- The search value to look for
	 */
	private void filllist(String filter) {
		if (filter.equals("")) {
			copyList(usersList, filteredUsersList);
		} else {
			filteredUsersList.clear();
			int len = filter.length();
			for (HashMap<String, String> user: usersList) {
				if (len <= user.get("fullname").length() && filter.equalsIgnoreCase((String) user.get("fullname").subSequence(0, len))) {
					filteredUsersList.add(user);
				}
			}
		}
	}
	
	/**
	 * Initialize the ListView for the searched contacts
	 */
	private void initList() {
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
	
	/**
	 * Copy the first list to the second one
	 * 
	 * @param lhs -- The source list
	 * @param rhs -- The destination list
	 */
	private void copyList(List<HashMap<String, String>> lhs, List<HashMap<String, String>> rhs) {
		rhs.clear();
		for (HashMap<String, String> item: lhs) {
			rhs.add(item);
		}
	}
	
	@SuppressWarnings({"unchecked"})
	private class fullNameComparator implements Comparator
	{
	    public int compare(Object obj1, Object obj2)
	    {
	        HashMap<String, String> update1 = (HashMap<String, String>)obj1;
	        HashMap<String, String> update2 = (HashMap<String, String>)obj2;
	        return update1.get("fullname").compareTo(update2.get("fullname"));
	    }
	}
}
