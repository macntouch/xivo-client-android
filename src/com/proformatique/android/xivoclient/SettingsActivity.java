package com.proformatique.android.xivoclient;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity{

	SharedPreferences settingsPrefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        settingsPrefs = getPreferenceManager().getSharedPreferences();
        
        /**
         * This Listener will trigger when users disable the "save_login" parameter,
         * so the app can erase login and password saved 
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
				}
				
			}
		});
	}


}
