package com.proformatique.android.xivoclient;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity{

	SharedPreferences settingsPrefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        settingsPrefs = getPreferenceManager().getSharedPreferences();
	}


}
