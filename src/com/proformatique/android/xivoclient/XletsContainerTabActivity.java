package com.proformatique.android.xivoclient;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TabHost;

import com.proformatique.android.xivoclient.service.Connection;
import com.proformatique.android.xivoclient.service.IXivoService;
import com.proformatique.android.xivoclient.service.InitialListLoader;
import com.proformatique.android.xivoclient.service.JsonLoopListener;
import com.proformatique.android.xivoclient.service.XivoService;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.xlets.XletIdentity;

public class XletsContainerTabActivity extends TabActivity {
	
	private static final String LOG_TAG = "XLETS_LOADING";
	private boolean serviceStarted = false;
	private RemoteServiceConnection conn = null;
	private IXivoService xivoService;
	
	/**
	 * TODO : Move xletsList and xletsList loading to InitialListLoader 
	 */
	private static List<String> xletsList = new ArrayList<String>();
	IncomingReceiver receiver;
	XletIdentity xletIdentity;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.xlets_container);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		startXivoService();
		bindXivoService();
		
		if (settings.getBoolean("use_fullscreen", false)) {
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		displayInit();
	}
	
	private void displayInit() {
		
		TabHost tabHost = getTabHost();
		tabHost.clearAllTabs();
		/*
		TabWidget tabWidget = tabHost.getTabWidget();
		Resources res = getResources();
		*/
		TabHost.TabSpec spec;
		Intent intent;
		
		receiver = new IncomingReceiver();
		
		/**
		 *  Register a BroadcastReceiver for Intent action that trigger
		 *  a disconnection
		 */
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_DISCONNECT);
		filter.addAction(Constants.ACTION_LOAD_PHONE_STATUS);
		
		registerReceiver(receiver, new IntentFilter(filter));
		
		/**
		 * Get the list of xlets available for connected user
		 * and delete the suffix starting to "-"
		 */
		ArrayList<String> xletsTmp = decodeJsonObject(Connection.getInstance(getApplicationContext()).getjCapa(), "capaxlets");
		
		ArrayList<String> xlets = null;
		if (xletsTmp != null) {
			xlets = new ArrayList<String>(xletsTmp.size());
			for (String x : xletsTmp){
				xlets.add(x.substring(0, x.indexOf("-")));
			}
			Log.d( LOG_TAG, "xlets avail. : "+ xlets);
		}
		
		/**
		 * The PackageManager will help us to retrieve all the activities
		 * that declared an intent of type ACTION_XLET_LOAD in the Manifest file
		 */
		PackageManager pm = getPackageManager();
		Intent xletIntent = new Intent( Constants.ACTION_XLET_LOAD_TAB );
		
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
				desc = getString(R.string.class.getField("xlet_"+label+"_desc")
						.getInt(null));
				/**
				 * Control that xlet is available for the connected user,
				 * the key of control is the "label" of the activity
				 */
				if (xlets != null && xlets.indexOf(label)!=-1){
					
					try {
						intent = new Intent().setClass(this, Class.forName(aInfo.name));
						spec = tabHost.newTabSpec(label).setIndicator(desc).setContent(intent);
						
						tabHost.addTab(spec);
						
						xletsList.add(label);
						
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e1) {
				Log.d( LOG_TAG, "Missing label or description declaration for Xlet Activity : "+ aInfo.name);
			}
		}
		tabHost.setCurrentTab(0);
		/**
		 * Displaying xlet Identity content
		 */
		xletIdentity = new XletIdentity(XletsContainerTabActivity.this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (Connection.getInstance(getApplicationContext()).isNewConnection()){
			try {
				unregisterReceiver(receiver);
			} catch (Exception e) {
			}
			
			displayInit();
			InitialListLoader init = InitialListLoader.getInstance();
			if (init != null) {
				init.setXletsList(xletsList);
				@SuppressWarnings("unused")
				JsonLoopListener jsonLoop = JsonLoopListener.getInstance(this);
				Connection.getInstance(getApplicationContext()).setNewConnection(false);
			}
		}
	}
	
	public static ArrayList<String> decodeJsonObject(JSONObject jSonObj, String parent){
		Log.d( LOG_TAG, "JSON : "+ jSonObj);
		
		if (jSonObj != null) {
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
		}
		return null;
	}
	
	/**
	 * BroadcastReceiver, intercept Intents with action ACTION_DISCONNECT
	 * to perform an reload of the displayed list
	 * @author cquaquin
	 *
	 */
	public class IncomingReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d( LOG_TAG , "Received Broadcast ");
			if (intent.getAction().equals(Constants.ACTION_DISCONNECT)) {
				Connection.getInstance(getApplicationContext()).disconnect();
				XletsContainerTabActivity.this.finish();
				
			} else if (intent.getAction().equals(Constants.ACTION_LOAD_PHONE_STATUS))
				xletIdentity.changeCurrentPhone();
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_settings_connected, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_settings:
			menuSettings();
			return true;
		case R.id.menu_exit:
			menuExit();
			return true;
		case R.id.menu_disconnect:
			menuDisconnect();
			return true;
		case R.id.menu_about:
			menuAbout();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void menuAbout() {
		Intent defineIntent = new Intent(this, AboutActivity.class);
		startActivity(defineIntent);
	}
	
	private void menuDisconnect() {
		Connection.getInstance(getApplicationContext()).disconnect();
		unregisterReceiver(receiver);
		XletsContainerTabActivity.this.finish();
	}
	
	private void menuExit() {
		Connection.getInstance(getApplicationContext()).disconnect();
		setResult(Constants.CODE_EXIT);
		finish();
	}
	
	private void menuSettings() {
		Intent defineIntent = new Intent(this, SettingsActivity.class);
		startActivity(defineIntent);
	}
	
	public void switchTab(int tab){
		getTabHost().setCurrentTab(tab);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
		case Constants.CODE_IDENTITY_STATE_LIST:
			switch (resultCode){
			case Constants.OK:
				Bundle extraData = data.getExtras();
				InitialListLoader.getInstance().putCapaPresenceState("stateid", extraData.getString("stateid"));
				InitialListLoader.getInstance().putCapaPresenceState("longname", extraData.getString("longname"));
				InitialListLoader.getInstance().putCapaPresenceState("color", extraData.getString("color"));
				
				xletIdentity.changeCurrentState();
			}
			
		}
	}
	
	@Override
	protected void onDestroy() {
		try {
			unregisterReceiver(receiver);
		} catch (Exception e) {
		}
		//Connection.getInstance().disconnect();
		setResult(Constants.CODE_EXIT);
		releaseXivoService();
		
		super.onDestroy();
	}
	
	private void startXivoService() {
		if (serviceStarted == false) {
			Intent i = new Intent();
			i.setClassName("com.proformatique.android.xivoclient", "com.proformatique.android.xivoclient.service.XivoService");
			if (XivoService.isRunning(getApplicationContext())) {
				Log.i(LOG_TAG, "XiVO service was running.");
			} else {
				Log.i(LOG_TAG, "XiVO service was not running.");
				startService(i);
			}
			serviceStarted = true;
			Log.d(LOG_TAG, "XiVO service started");
		} else {
			Log.d(LOG_TAG, "XiVO service already started");
		}
	}
	
	private void releaseXivoService() {
		if (conn != null) {
			unbindService(conn);
			conn = null;
			Log.d(LOG_TAG, "Service released");
		} else {
			Log.d(LOG_TAG, "Service not bounded");
		}
	}
	
	private void bindXivoService() {
		if (conn == null) {
			conn = new RemoteServiceConnection();
			Intent i = new Intent();
			i.setClassName("com.proformatique.android.xivoclient", "com.proformatique.android.xivoclient.service.XivoService");
			bindService(i, conn, Context.BIND_AUTO_CREATE);
			Log.d("SERVICE TEST", "Service binded");
		} else {
			Log.d("SERVICE TEST", "Service already bound");
		}
	}
	
	class RemoteServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			xivoService = IXivoService.Stub.asInterface((IBinder)service);
			Log.d("SERVICE TEST", "onServiceConnected");
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			xivoService = null;
			Log.d("SERVICE TEST", "onServiceDisconnected");
		}
		
	}
}
