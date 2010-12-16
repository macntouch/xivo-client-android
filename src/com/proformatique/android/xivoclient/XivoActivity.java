package com.proformatique.android.xivoclient;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;

/**
 * An overloaded Activity class to make UI changes and options consistent
 * across the application
 * 
 * @author Pascal Cadotte-Michaud
 *
 */
public class XivoActivity extends Activity {
	
	private SharedPreferences settings;
	
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        if (settings.getBoolean("use_fullscreen", false)) {
        	this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        			WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
	}
}
