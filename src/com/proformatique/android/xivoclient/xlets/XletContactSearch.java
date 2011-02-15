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
import java.util.Map;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.proformatique.android.xivoclient.AttendedTransferActivity;
import com.proformatique.android.xivoclient.BlindTransferActivity;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.service.UserProvider;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletContactSearch extends XivoActivity implements OnItemClickListener {
	
	private static final String LOG_TAG = "XiVO search";
	
	private Cursor user;
	private UserAdapter userAdapter = null;
	private IncomingReceiver receiver;
	
	/*
	 * UI
	 */
	private EditText et;
	private ListView lv;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xlet_search);
		
		et = (EditText)findViewById(R.id.SearchEdit);
		lv = (ListView) findViewById(R.id.users_list);
		
		userAdapter = new UserAdapter();
		lv.setAdapter(userAdapter);
		lv.setOnItemClickListener(this);
		
		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a change
		 *  in the users list from the Activity
		 */
		receiver = new IncomingReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_LOAD_USER_LIST);
		filter.addAction(Constants.ACTION_REFRESH_USER_LIST);
		registerReceiver(receiver, new IntentFilter(filter));
		
		registerForContextMenu(findViewById(R.id.users_list));
		
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
				startActivityForResult(contactPickerIntent,
						Constants.CONTACT_PICKER_RESULT);
			}
		});
		
		registerButtons();
	}
	
	/**
	 * Handles results for launched activities
	 */
	protected void onActivityResult(
			int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case Constants.CONTACT_PICKER_RESULT:
				Map<String, String> contact = getPickedContact(data);
				promptAfterSelection(contact);
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
	private Map<String, String> getPickedContact(Intent data) {
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
							contact.put((String) Phone.getTypeLabel(
									this.getResources(), cursor.getInt( cursor.getColumnIndex(
											Phone.TYPE)), "unknown"), number);
						}
					}
				
			} while (cursor.moveToNext());
			cursor.close();
			return contact;
		} else {
			cursor.close();
			return null;
		}
	}
	
	protected void onResume() {
		super.onResume();
		getUserList();
	}
	
	private class UserAdapter extends BaseAdapter {
		
		private final static String TAG = "User Adapter";
		
		public UserAdapter() {
			Log.d(TAG, "User adapter");
			Log.d(TAG, user == null ? "0" : Long.toString(user.getCount()));
		}
		
		@Override
		public int getCount() {
			return user != null ? user.getCount() : 0;
		}
		
		@Override
		public Object getItem(int position) {
			user.moveToPosition(position);
			return user.getExtras();
		}
		
		@Override
		public long getItemId(int position) {
			user.moveToPosition(position);
			return user.getLong(user.getColumnIndex(UserProvider._ID));
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null) {
				final LayoutInflater inflater = LayoutInflater.from(XletContactSearch.this);
				v = inflater.inflate(R.layout.xlet_search_items, null);
			} else {
				v = convertView;
			}
			user.moveToPosition(position);
			
			// Contact info
			((TextView) v.findViewById(R.id.fullname)).setText(
					user.getString(user.getColumnIndex(UserProvider.FULLNAME)));
			((TextView) v.findViewById(R.id.phonenum)).setText(
					user.getString(user.getColumnIndex(UserProvider.PHONENUM)));
			
			// Contact Status
			ImageView iconState = (ImageView) v.findViewById(R.id.statusContact);
			((TextView) v.findViewById(R.id.longname_state)).setText(
					user.getString(user.getColumnIndex(UserProvider.STATEID_LONGNAME)));
			GraphicsManager.setIconStateDisplay(
					XletContactSearch.this, iconState, user.getString(
							user.getColumnIndex(UserProvider.STATEID_COLOR)));
			
			// Phone status
			ImageView iconPhone = (ImageView) v.findViewById(R.id.phoneStatusContact);
			((TextView) v.findViewById(R.id.phone_longname_state)).setText(
					user.getString(user.getColumnIndex(UserProvider.HINTSTATUS_LONGNAME)));
			GraphicsManager.setIconPhoneDisplay(
					XletContactSearch.this, iconPhone, user.getString(
							user.getColumnIndex(UserProvider.HINTSTATUS_COLOR)));
			return v;
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
				getUserList();
				userAdapter.notifyDataSetChanged();
			} else if (intent.getAction().equals(Constants.ACTION_REFRESH_USER_LIST)) {
				Log.d(LOG_TAG, "Received search Broadcast");
				
				
			}
		}
	}
	
	/**
	 * Refresh the user list and notify the list adapter
	 */
	private void getUserList() {
		if (user != null && user.isClosed() == false)
			user.close();
		user = getContentResolver().query(
				UserProvider.CONTENT_URI, null, null, null, UserProvider.FULLNAME);
		userAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		if (user != null)
			user.close();
		super.onDestroy();
	}
	
	/**
	 * When an item on the list is clicked
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		user.moveToPosition(position);
		String phoneNumber = user.getString(user.getColumnIndex(UserProvider.PHONENUM));
		String name = user.getString(user.getColumnIndex(UserProvider.FULLNAME));
		Log.d(LOG_TAG, "Clicked: " + name + " " + phoneNumber);
		HashMap<String, String> contact = new HashMap<String, String>();
		contact.put("Name", name);
		contact.put(getString(R.string.xivo_phone_type_lbl), phoneNumber);
		promptAfterSelection(contact);
	}
	
	/**
	 * Prompt the user to call or transfer to one of his number when selected on the list
	 * or picked on the Android contact picker
	 * @param contact
	 */
	private void promptAfterSelection(Map<String, String> contact) {
		if (contact == null)
			return;
		
		String[] items = null;
		try {
			int i = 0;
			if (xivoConnectionService.isOnThePhone()) {
				items = new String[(contact.size() - 1) * 2 + 1];
				for (String key: contact.keySet()) {
					if (key.equals("Name"))
						continue;
					items[i++] = String.format(
							getString(R.string.context_action_atxfer), key, contact.get(key));
					items[i++] = String.format(
							getString(R.string.context_action_transfer), key, contact.get(key));
				}
			} else {
				items = new String[contact.size()];
				for (String key: contact.keySet()) {
					if (key.equals("Name"))
						continue;
					items[i++] = String.format(
							getString(R.string.context_action_call), key, contact.get(key));
				}
			}
			items[i] = getString(R.string.cancel_label);
		} catch (RemoteException e) {
			Toast.makeText(XletContactSearch.this, getString(R.string.remote_exception),
					Toast.LENGTH_SHORT).show();
			return;
		}
		
		final String[] menuItems = items;
		new AlertDialog.Builder(this)
		.setTitle(contact.get("Name"))
		.setItems(menuItems,
			new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int i) {
					if (i == menuItems.length - 1) {
						Log.d(LOG_TAG, "Cancel selected");
						return;
					}
					String number = menuItems[i].substring(
							menuItems[i].indexOf("(") + 1, menuItems[i].indexOf(")"))
							.replace("-", "").trim();
					try {
						if (xivoConnectionService.isOnThePhone()) {
							if (i % 2 == 1) {
								Intent intent = new Intent(XletContactSearch.this,
										AttendedTransferActivity.class);
								intent.putExtra("num", number);
								startActivity(intent);
							} else {
								Intent intent = new Intent(XletContactSearch.this,
										BlindTransferActivity.class);
								intent.putExtra("num", number);
								startActivity(intent);
							}
						} else {
							Intent intent = new Intent(XletContactSearch.this, XletDialer.class);
							intent.putExtra("numToCall", number);
							startActivity(intent);
						}
						
					} catch (RemoteException e) {
						Toast.makeText(XletContactSearch.this,
								getString(R.string.remote_exception), Toast.LENGTH_SHORT).show();
					}
				}
			}).show();
	}
}
