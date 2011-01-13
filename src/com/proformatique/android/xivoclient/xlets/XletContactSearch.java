package com.proformatique.android.xivoclient.xlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.proformatique.android.xivoclient.InitialListLoader;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletContactSearch extends XivoActivity {
	
	private static final String LOG_TAG = "XiVO XletContactSearch";
	private static final int CALL_MENU = 0;
	private static final int TRANSFER_MENU = 1;
	private static final int CALL_ITEM_INDEX = 0;
	private static final int ATXFER_ITEM_INDEX = 1;
	private static final int TRANSFER_ITEM_INDEX = 2;
	
	private List <HashMap<String, String>> filteredUsersList = new ArrayList<HashMap<String, String>>();
	private List<HashMap<String, String>> contacts = null;
	private AlternativeAdapter usersAdapter = null;
	
	
	private String[] items;
		
	private EditText et;
	private ListView lv;
	private IncomingReceiver receiver;
	private SearchReceiver searchReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.xlet_search);
		contacts = InitialListLoader.getInstance().getUsersList();
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
		
		Button searchButton = (Button) findViewById(R.id.button_search_contacts);
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
						Contacts.CONTENT_URI);  
				startActivityForResult(contactPickerIntent, Constants.CONTACT_PICKER_RESULT); 
			}
		});
	}
	
	/**
	 * Handles results for launched activities
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		if (resultCode == RESULT_OK) {  
			switch (requestCode) {  
			case Constants.CONTACT_PICKER_RESULT:  
				readPickedContact(data);
				break;  
			}
		} else {
			Log.w(LOG_TAG, "Warning: activity result not ok");
		}
	}
	
	/**
	 * Reads the data received from the contact picked, creates a HashMap
	 * and sends the call
	 * 
	 * @param data
	 */
	private void readPickedContact(Intent data) {
		Uri result = data.getData();
		String id = result.getLastPathSegment();
		Cursor cursor = getContentResolver().query(
				Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?",new String[] {id}, null);
		if (cursor.getCount() > 0) {
			HashMap<String, String> contact = new HashMap<String, String>();
			cursor.moveToFirst();
			do {
					String[] cols = cursor.getColumnNames();
					String number = "";
					for (String col: cols) {
						int index = cursor.getColumnIndex(col);
						if (col.equals("display_name")) {
							contact.put("Name", cursor.getString(index));
						} else if (col.equals("data1")) {
							number = cursor.getString(index);
						} else if (col.equals("data2")) {
							contact.put((String) Phone.getTypeLabel(this.getResources(), cursor.getInt(
									cursor.getColumnIndex(Phone.TYPE)), "unknown"), number);
						}
					}
				
			} while (cursor.moveToNext());
			callContact(contact);
		} else {
			Toast.makeText(getApplicationContext(), getString(R.string.call_no_phone_number), Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Prompt the users for the number to call (Home, Office, etc)
	 * 
	 * @param contact
	 */
	private void callContact(HashMap<String, String> contact) {
		Set<String> keys = contact.keySet();
		items = new String[contact.size()];
		String title = "";
		int i = 0;
		for (String key: keys) {
			if (key != "Name") {
				String c = key + " " + contact.get(key);
				items[i++] = c;
			} else {
				title = getString(R.string.context_action_call_short, contact.get(key));
			}
		}
		items[items.length - 1] = getString(R.string.cancel_label);
		new AlertDialog.Builder(this)
		.setTitle(title)
		.setItems(items,
			new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int i) {
					callNumberForResult(i);
				}
			}).show();
	}
	
	/**
	 * Calls the choosen number for the selected android contact
	 * @param i
	 */
	protected void callNumberForResult(int i) {
		if (i == items.length - 1) {
			Toast.makeText(getApplicationContext(), getString(R.string.call_canceled), Toast.LENGTH_LONG).show();
		} else {
			String[] number = items[i].split(" ");
			dialNumber(number[1]);
		}
	}
	
	protected void onResume() {
		super.onResume();
		contacts = InitialListLoader.getInstance().getUsersList();
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
				// Normal menu (Not currently on the phone)
				if (InitialListLoader.getInstance().getThisChannelId() == null) {
					String callAction = getString(R.string.context_action_call, 
							filteredUsersList.get(info.position).get("fullname"), 
							filteredUsersList.get(info.position).get("phonenum"));
					menu.add(CALL_MENU, CALL_ITEM_INDEX, 0, callAction);
				} else { // On the phone menu
					menu.add(TRANSFER_MENU, ATXFER_ITEM_INDEX, 0, getString(R.string.attended_transfer_title));
					menu.add(TRANSFER_MENU, TRANSFER_ITEM_INDEX, 0, getString(R.string.blind_transfer_title));
				}
			}
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(LOG_TAG, item.getTitle().toString());
		switch (item.getGroupId()) {
		// Call menu
		case CALL_MENU:
			switch (item.getItemId()) {
			case CALL_ITEM_INDEX:
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				String phoneNumber = filteredUsersList.get(info.position).get("phonenum");
				dialNumber(phoneNumber);
				break;
			default:
				break;
			}
			break;
		// Transfer menu
		case TRANSFER_MENU:
			switch (item.getItemId()) {
			case ATXFER_ITEM_INDEX:
				Log.d(LOG_TAG, "Attended transfer selected");
				break;
			case TRANSFER_ITEM_INDEX:
				Log.d(LOG_TAG, "Blind transfer selected");
				break;
			}
			break;
			
		default:
			break;
		}
		
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
	public void dialNumber(String numToCall){
		
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
