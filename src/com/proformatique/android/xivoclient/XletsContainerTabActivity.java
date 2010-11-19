package com.proformatique.android.xivoclient;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.TabActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TabWidget;

public class XletsContainerTabActivity extends TabActivity {

	public static final String ACTION_XLET_LOAD = "xivo.intent.action.LOAD_XLET";
	private static final String LOG_TAG = "XLETS_LOADING";
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.xlets_container);

	    final TabHost tabHost = getTabHost();  // The activity TabHost
	    final TabWidget tabWidget = tabHost.getTabWidget();

	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    /**
	     * Get the list of xlets available for connected user
	     * and delete the suffix starting to "-"
	     */
	    ArrayList<String> xletsTmp = decodeJsonObject(Connection.connection.jCapa, "capaxlets");
	    
	    ArrayList<String> xlets = new ArrayList<String>(xletsTmp.size());
	    for (String x : xletsTmp){
	    	xlets.add(x.substring(0, x.indexOf("-")));
	    }
	    Log.d( LOG_TAG, "xlets avail. : "+ xlets);
	    
	    
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
				
				/**
				 * Control that xlet is available for the connected user,
				 * the key of control is the "label" of the activity
				 */
				if (xlets.indexOf(label)!=-1){

				    try {
						intent = new Intent().setClass(this, Class.forName(aInfo.name));
					    spec = tabHost.newTabSpec(label).setIndicator(desc).setContent(intent);
	
					    tabHost.addTab(spec);
		
				    } catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}

			} catch (Exception e1) {
				Log.d( LOG_TAG, "Missing label or description declaration for Xlet Activity : "+ aInfo.name);
			}
        }
        
	    tabHost.setCurrentTab(0);
        
	}
	
	private ArrayList<String> decodeJsonObject(JSONObject jSonObj, String parent){
		Log.d( LOG_TAG, "JSON : "+ jSonObj);
		
		try {
			JSONArray resArray = jSonObj.getJSONArray(parent);
			ArrayList<String> resList = new ArrayList<String>(resArray.length());
			int len = resArray.length();
			for (int i=0;i<len;i++){
				String curItem = resArray.getString(i);
				resList.add(curItem);
			}
			return resList;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
}