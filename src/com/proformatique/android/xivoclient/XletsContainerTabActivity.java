package com.proformatique.android.xivoclient;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.widget.Toast;

import com.proformatique.android.xivoclient.xlets.XletIdentity;

public class XletsContainerTabActivity extends TabActivity {

	public static final String ACTION_XLET_LOAD = "xivo.intent.action.LOAD_XLET";
	private static final String LOG_TAG = "XLETS_LOADING";
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.xlets_container);

	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    tabHost.setCurrentTab(0);
        
        /**
         * The PackageManager will help us to retrieve all the activities
         * that declared an intent of type ACTION_XLET_LOAD in the Manifest file
         */
	    PackageManager pm = getPackageManager();
        Intent xletIntent = new Intent( ACTION_XLET_LOAD );
        
        List<ResolveInfo> list = pm.queryIntentActivities(xletIntent, PackageManager.GET_RESOLVED_FILTER);
        
	    /**
	     *  Initialize a TabSpec for each tab and add it to the TabHost
	     *  Dynamic loading of each xlet into one tab
	     */
        for( int i = 0 ; i < list.size() ; ++i ) {
        	
	        ResolveInfo rInfo = list.get( i );
			ActivityInfo aInfo = rInfo.activityInfo;
			String label;
			String desc;
			
			try {
				label = getString(aInfo.labelRes);
				desc = getString(aInfo.descriptionRes);
			
			    try {
					intent = new Intent().setClass(this, Class.forName(aInfo.name));
				    spec = tabHost.newTabSpec(label).setIndicator(desc).setContent(intent);
				    tabHost.addTab(spec);
	
			    } catch (ClassNotFoundException e) {
					e.printStackTrace();
				}

			} catch (Exception e1) {
				Log.d( LOG_TAG, "Missing label or description declaration for Xlet Activity : "+ aInfo.name);
			}
        }
	}
}