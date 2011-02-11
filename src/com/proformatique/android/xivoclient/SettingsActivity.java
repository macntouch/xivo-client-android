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

package com.proformatique.android.xivoclient;

import com.proformatique.android.xivoclient.service.JsonLoopListener;
import com.proformatique.android.xivoclient.tools.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

public class SettingsActivity extends PreferenceActivity{
	
	@SuppressWarnings("unused")
	private static final String LOG_TAG = "SETTINGS";
	private final static String USE_MOBILE_OPTION = "use_mobile_number";
	private final static boolean USE_MOBILE_DEFAULT = false;
	private static final String MOBILE_PHONE_NUMBER = "mobile_number";
	private static final String DEFAULT_MOBILE_PHONE_NUMBER = "";
	
	SharedPreferences settingsPrefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		settingsPrefs = getPreferenceManager().getSharedPreferences();
		addPreferencesFromResource(R.xml.settings);
		
		if (settingsPrefs.getBoolean("use_fullscreen", false)) {
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		/**
		 * Init value for mobile number
		 */
		if (settingsPrefs.getString("mobile_number", "").equals("")){
			
			/**
			 * TODO : Check that default value is visible when no data exists 
			 *        in EditText field
			 */
			TelephonyManager tMgr =(TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			String mobileNumber = tMgr.getLine1Number();
			SharedPreferences.Editor editor = settingsPrefs.edit();
			editor.putString("mobile_number", mobileNumber);
			
			editor.commit();
		}
		
		
		/**
		 * This Listener will trigger when users disable the "save_login" parameter,
		 * so the app can erase previously saved login and password
		 *  
		 */
		settingsPrefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				
				if (key.equals("save_login")){
					Boolean saveLogin = sharedPreferences.getBoolean(key, true);
					
					if (!saveLogin){
						
						SharedPreferences loginSettings;
						loginSettings = getSharedPreferences("login_settings", 0);
						
						SharedPreferences.Editor editor = loginSettings.edit();
						
						editor.putString("login", "");
						editor.putString("password", "");
						editor.commit();
						
					}
				} else if (key.equals(USE_MOBILE_OPTION)) {
					JsonLoopListener.setUseMobile(getUseMobile(getApplicationContext()));
					if (getUseMobile(getApplicationContext()))
						JsonLoopListener.setMobileNumber(getMobileNumber(getApplicationContext()));
				} else if (key.equals(MOBILE_PHONE_NUMBER)) {
					JsonLoopListener.setMobileNumber(getMobileNumber(getApplicationContext()));
				}
			}
		});
	}
	
	/**
	 * Returns the use_mobile_number preference value
	 * @param context
	 * @return
	 */
	public static boolean getUseMobile(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
			.getBoolean(USE_MOBILE_OPTION, USE_MOBILE_DEFAULT);
	}
	
	/**
	 * Returns the mobile phone number or null if use_mobile_number is not true
	 * @param context
	 * @return
	 */
	public static String getMobileNumber(Context context) {
		if (getUseMobile(context)) {
			return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(MOBILE_PHONE_NUMBER, DEFAULT_MOBILE_PHONE_NUMBER);
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the default context
	 */
	public static String getXivoContext(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(
				"context", Constants.XIVO_CONTEXT);
	}
	
	/**
	 * Returns the Xivo login
	 */
	public static String getLogin(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("login", "");
	}
}
