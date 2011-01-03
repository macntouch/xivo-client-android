package com.proformatique.android.xivoclient.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

public class UsersList implements Parcelable {
	
	private static final String LOG_TAG = "UsersList";
	private List<HashMap<String, String>> xivoUsers = null;
	private List<HashMap<String, String>> androidUsers = null;
	private List<HashMap<String, String>> allUsers = null;
	private Context context;
	private boolean androidContactsLoaded = false;
	
	public List<HashMap<String, String>> getAllUsers() {
		return allUsers;
	}
	
	public UsersList(Context context) {
		this.context = context;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		
		xivoUsers = new ArrayList<HashMap<String, String>>();
		allUsers = new ArrayList<HashMap<String, String>>();
		
		if (settings.getBoolean("include_device_contacts", false) == false) {
			androidUsers = null;
		} else {
			loadAndroidUsers();
		}
	}
	
	private void loadAndroidUsers() {
		if (this.context == null)
			Log.d(LOG_TAG, "NULL CONTEXT");
		Cursor cursor = this.context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		androidUsers = new ArrayList<HashMap<String, String>>(cursor.getCount());
		String contactId;
		String name;
		Cursor phones;
		while (cursor.moveToNext()) {
			contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
			name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			// Read phone numbers
			phones = this.context.getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = " + contactId, null, null);
			HashMap<String, String> contact;
			while (phones.moveToNext()) {
				contact = new HashMap<String, String>(Constants.ANDROID_CONTACT_HASH_SIZE);
				contact.put("fullname", name);
				contact.put("phonenum", phones.getString(phones.getColumnIndex(Phone.NUMBER)));
				contact.put("hintstatus_longname",
						(String) Phone.getTypeLabel(this.context.getResources(), phones.getInt(
								phones.getColumnIndex(Phone.TYPE)), "test"));
				contact.put("stateid_longname", "Android");
				contact.put("hintstatus_color", "#FFFFFF");
				contact.put("stateid_color", "grey");
				androidUsers.add(contact);
			}
			androidContactsLoaded  = true;
			phones.close();
		}
		if (androidUsers != null && androidUsers.size() > 0) {
			if (allUsers == null) {
				allUsers = new ArrayList<HashMap<String, String>>(androidUsers.size());
			}
			allUsers.addAll(androidUsers);
		}
	}
	
	public UsersList(Parcel in) {
		loadList(in, xivoUsers);
		loadList(in, androidUsers);
		allUsers = new ArrayList<HashMap<String, String>>(xivoUsers.size() + androidUsers.size());
		if (xivoUsers != null && xivoUsers.size() > 0) {
			allUsers.addAll(xivoUsers);
		}
		if (androidUsers != null && androidUsers.size() > 0) {
			allUsers.addAll(androidUsers);
		}
		sortAllUsers();
	}
	
	@SuppressWarnings("unchecked")
	public void sortAllUsers() {
		if (allUsers.size() != 0){
			Collections.sort(allUsers, new fullNameComparator());
		}
	}
	
	private void loadList(Parcel in, List<HashMap<String, String>> list) {
		int len = in.readInt();
		list = new ArrayList<HashMap<String, String>>(len);
		for (int i = 0; i < len; ++i) {
			String tmp = in.readString();
			HashMap<String, String> user = new HashMap<String, String>();
			for (String line: tmp.substring(1, tmp.length() - 1).split(", ")) {
				String[] vals = line.split("=");
				user.put(vals[0], vals[1]);
			}
			list.add(user);
		}
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		writeList(dest, xivoUsers);
		writeList(dest, androidUsers);
	}
	
	private void writeList(Parcel out, List<HashMap<String, String>> list) {
		out.writeInt(list.size());
		for (HashMap<String, String> user: list) {
			out.writeString(user.toString());
		}
	}
	
	public static final Parcelable.Creator<UsersList> CREATOR = new Parcelable.Creator<UsersList>() {
			public UsersList createFromParcel(Parcel in) {
				return new UsersList(in);
			}
			
			public UsersList[] newArray(int size) {
				return new UsersList[size];
			}
	};
	
	@SuppressWarnings("unchecked")
	private class fullNameComparator implements Comparator

	{
		public int compare(Object obj1, Object obj2)
		{
			HashMap<String, String> update1 = (HashMap<String, String>)obj1;
			HashMap<String, String> update2 = (HashMap<String, String>)obj2;
			return update1.get("fullname").compareTo(update2.get("fullname"));
		}
	}
	

	public void setUsers(List<HashMap<String, String>> usersList) {
		if (xivoUsers != usersList)
			xivoUsers = usersList;
	}

	public void setXivoUser(int i, HashMap<String, String> map) {
		xivoUsers.set(i, map);
	}

	public void addXivoUser(HashMap<String, String> user) {
		xivoUsers.add(user);
	}

	public List<HashMap<String, String>> getXivoUsers() {
		return xivoUsers;
	}
}
