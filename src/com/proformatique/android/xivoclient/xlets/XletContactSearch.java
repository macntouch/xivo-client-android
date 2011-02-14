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
import com.proformatique.android.xivoclient.service.InitialListLoader;
import com.proformatique.android.xivoclient.service.UserProvider;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

public class XletContactSearch extends XivoActivity implements OnItemClickListener {
	
	private static final String LOG_TAG = "XiVO search";
	
	private Cursor user;
	private UserAdapter userAdapter = null;
	private String[] pickedContactPhoneList;
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
				if (contact != null) {
					setContactPhoneList(contact);
					if (InitialListLoader.getInstance().getThisChannelId()
							== null) {
						callContact(contact);
					} else {
						transferContact(contact);
					}
				} else {
					Toast.makeText(this, getString(R.string.no_phone_error),
							Toast.LENGTH_LONG).show();
				}
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
			return null;
		}
	}
	
	/**
	 * Prompt the user for the number to transfer to
	 * @param contact
	 */
	private void transferContact(Map<String, String> contact) {
		pickedContactPhoneList[pickedContactPhoneList.length - 1] =
			getString(R.string.cancel_label);
		new AlertDialog.Builder(this)
		.setTitle(getContactName(contact))
		.setItems(pickedContactPhoneList,
			new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int i) {
					transferNumberForResult(i);
				}
			}).show();
	}
	
	/**
	 * Prompt the user for the number to call (Home, Office, etc)
	 * 
	 * @param contact
	 */
	private void callContact(Map<String, String> contact) {
		pickedContactPhoneList[pickedContactPhoneList.length - 1] =
			getString(R.string.cancel_label);
		new AlertDialog.Builder(this)
		.setTitle(getContactName(contact))
		.setItems(pickedContactPhoneList,
			new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int i) {
					callNumberForResult(i);
				}
			}).show();
	}
	
	/**
	 * Returns the name of a picked Android contact
	 * 
	 * @param contact
	 * @return
	 */
	private CharSequence getContactName(Map<String, String> contact) {
		if (contact.containsKey("Name")) {
			return contact.get("Name");
		} else {
			return getString(R.string.unknown_contact_name);
		}
	}
	
	/**
	 * Set the field pickedContactPhoneList to the phone numbers
	 * available for a given contact.
	 * 
	 * @param contact
	 */
	private void setContactPhoneList(Map<String, String> contact) {
		Set<String> keys = contact.keySet();
		pickedContactPhoneList = new String[contact.size()];
		int i = 0;
		for (String key: keys) {
			if (key != "Name") {
				String c = key + " " + contact.get(key);
				pickedContactPhoneList[i++] = c;
			}
		}
	}
	
	/**
	 * Calls the choosen number for the selected android contact
	 * @param i
	 */
	protected void callNumberForResult(int i) {
		if (i == pickedContactPhoneList.length - 1) {
			Toast.makeText(getApplicationContext(), getString(R.string.call_canceled),
					Toast.LENGTH_LONG).show();
		} else {
			String[] number = pickedContactPhoneList[i].split(" ");
			dialNumber(number[1]);
		}
	}
	
	/**
	 * Retrieves the number to call from the picked contact number chooser
	 * 
	 * @param i
	 */
	protected void transferNumberForResult(int i) {
		if (i == pickedContactPhoneList.length - 1) {
			Toast.makeText(this, getString(R.string.transfer_canceled),
					Toast.LENGTH_LONG).show();
		} else {
			String[] namePhone = pickedContactPhoneList[i].split(" ");
			promptForTransfer(namePhone[1]);
		}
	}
	
	/**
	 * Asks for blind or attended transfer
	 * @param string
	 */
	private void promptForTransfer(final String number) {
		new AlertDialog.Builder(this)
		.setTitle(number)
		.setItems(new String[] {
				getString(R.string.attended_transfer_title),
				getString(R.string.blind_transfer_title,
				getString(R.string.cancel_label))},
			new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int i) {
					switch (i) {
					case 0:
						atxferNumber(number);
						break;
					case 1:
						transferNumber(number);
						break;
					default:
						Toast.makeText(getApplicationContext(),
								getString(R.string.transfer_canceled),
								Toast.LENGTH_LONG).show();
						break;
					}
				}
			}).show();
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
	
	private void getUserList() {
		user = getContentResolver().query(
				UserProvider.CONTENT_URI, null, null, null, UserProvider.FULLNAME);
		userAdapter.notifyDataSetChanged();
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
	
	/**
	 * Sends an blind transfer to numToCall
	 * 
	 * @param numToCall
	 */
	public void atxferNumber(String numToCall) {
		Intent i = new Intent(XletContactSearch.this, AttendedTransferActivity.class);
		i.putExtra("num", numToCall);
		startActivity(i);
	}
	
	/**
	 * Sends a blind transfer to numToCall
	 * 
	 * @param numToCall
	 */
	public void transferNumber(String numToCall) {
		Intent i = new Intent(XletContactSearch.this, BlindTransferActivity.class);
		i.putExtra("num", numToCall);
		startActivity(i);
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		if (user != null)
			user.close();
		super.onDestroy();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		user.moveToPosition(position);
		String phoneNumber = user.getString(user.getColumnIndex(UserProvider.PHONENUM));
		String name = user.getString(user.getColumnIndex(UserProvider.FULLNAME));
		Log.d(LOG_TAG, "Clicked: " + name + " " + phoneNumber);
		/*switch (item.getGroupId()) {
		// Call menu
		case CALL_MENU:
			switch (item.getItemId()) {
			case CALL_ITEM_INDEX:
				dialNumber(phoneNumber);
				break;
			default:
				break;
			}
			break;
		// Transfer menu
		case Constants.TRANSFER_MENU:
			switch (item.getItemId()) {
			case Constants.ATXFER_ITEM_INDEX:
				Log.d(LOG_TAG, "Attended transfer selected");
				atxferNumber(phoneNumber);
				break;
			case Constants.TRANSFER_ITEM_INDEX:
				Log.d(LOG_TAG, "Blind transfer selected");
				transferNumber(phoneNumber);
				break;
			}
			break;	
		default:
			break;
		}
		et = (EditText)findViewById(R.id.SearchEdit);
		et.setText("");
		return super.onContextItemSelected(item);*/
	}
}
