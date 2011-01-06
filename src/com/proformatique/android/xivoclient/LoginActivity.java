package com.proformatique.android.xivoclient;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.proformatique.android.xivoclient.service.Connection;
import com.proformatique.android.xivoclient.service.IXivoService;
import com.proformatique.android.xivoclient.service.XivoService;
import com.proformatique.android.xivoclient.tools.Constants;

public class LoginActivity extends XivoActivity {
	
	/**
	 * Creating distinct preferences to avoid multiple references 
	 * of the same data (login/password) in settings screen
	 */
	private SharedPreferences settings;
	private SharedPreferences loginSettings;
	ProgressDialog dialog;
	private static final String LOG_TAG = "XiVO " + LoginActivity.class.getSimpleName();
	private IXivoService xivoService;
	private boolean serviceStarted = false;
	private RemoteServiceConnection conn = null;
	public boolean xivoServiceReady = false;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		if (XivoService.isRunning(getApplicationContext()))
			Log.i(LOG_TAG, "XiVO service is running");
		else {
			Log.i(LOG_TAG, "XiVO service is not running");
			startXivoService();
		}
		bindXivoService();
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		loginSettings = this.getSharedPreferences("login_settings", 0);
		
		/**
		 * Set the default saved login/password into corresponding fields 
		 * if parameter "save_login" is on
		 */
		if (settings.getBoolean("save_login", true)){
			
			String login = loginSettings.getString("login","");
			String password = loginSettings.getString("password","");
			
			EditText eLogin = (EditText) findViewById(R.id.login);
			eLogin.setText(login);
			
			EditText ePassword = (EditText) findViewById(R.id.password);
			ePassword.setText(password);
		}
	}
	
	/**
	 * Starts the client
	 * This should be called once the service is started and connected
	 */
	private void startClient() {
		displayElements(false);
		Intent defineIntent = new Intent(LoginActivity.this, XletsContainerTabActivity.class);
		LoginActivity.this.startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
	}
	
	private void bindXivoService() {
		if (conn == null) {
			conn = new RemoteServiceConnection();
			Intent i = new Intent();
			i.setClassName("com.proformatique.android.xivoclient", "com.proformatique.android.xivoclient.service.XivoService");
			bindService(i, conn, Context.BIND_AUTO_CREATE);
			Log.d(LOG_TAG, "Service binded");
		} else {
			Log.d(LOG_TAG, "Service already bound");
		}
	}
	
	private void startXivoService() {
		if (serviceStarted == false) {
			Intent i = new Intent();
			i.setClassName("com.proformatique.android.xivoclient", "com.proformatique.android.xivoclient.service.XivoService");
			if (!(XivoService.isRunning(getApplicationContext()))) {
				startService(i);
			}
			serviceStarted = true;
			Log.d(LOG_TAG, "XiVO service started");
		} else {
			Log.d(LOG_TAG, "XiVO service already started");
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_settings, menu);
		MenuItem mi = menu.findItem(R.id.menu_disconnect);
		mi.setVisible(true);
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		/**
		 *  Handle item selection
		 */
		switch (item.getItemId()) {
		case R.id.menu_settings:
			menuSettings();
			return true;
		case R.id.menu_exit:
			menuExit();
			return true;
		case R.id.menu_about:
			menuAbout();
			return true;
		case R.id.menu_disconnect:
			menuDisconnect();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void menuDisconnect() {
		Log.i(LOG_TAG, "Menu disconnect clicked");
		displayElements(true);
		Intent iDisconnectIntent = new Intent();
		iDisconnectIntent.setAction(Constants.ACTION_DISCONNECT_REQUEST);
		getApplicationContext().sendBroadcast(iDisconnectIntent);
	}
	
	private void menuAbout() {
		Intent defineIntent = new Intent(this, AboutActivity.class);
		startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
	}
	
	private void menuExit() {
		Log.i(LOG_TAG, "Menu exit clicked");
		if (Connection.getInstance(getApplicationContext()).isConnected())
			Connection.getInstance(getApplicationContext()).disconnect();
		finish();
	}
	
	private void menuSettings() {
		Intent defineIntent = new Intent(this, SettingsActivity.class);
		startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
	}
	
	public void clickOnButtonOk(View v) {
		saveLoginPassword();
		startXivoService();
		bindXivoService();
		
		if (xivoServiceReady == true)
			startClient();
	}
	
	private void saveLoginPassword() {
		
		String savedLogin = loginSettings.getString("login","");
		String savedPassword = loginSettings.getString("password","");
		SharedPreferences.Editor editor = loginSettings.edit();
		
		EditText eLogin = (EditText) findViewById(R.id.login);
		EditText ePassword = (EditText) findViewById(R.id.password);
		
		if (! eLogin.getText().toString().equals(savedLogin)){
			editor.putString("login", eLogin.getText().toString());
			editor.commit();
		}
		
		if (! ePassword.getText().toString().equals(savedPassword)){
			editor.putString("password", ePassword.getText().toString());
			editor.commit();
		}
	}
	
	public void displayElements(boolean display){
		EditText eLogin = (EditText) LoginActivity.this.findViewById(R.id.login); 
		EditText ePassword = (EditText) LoginActivity.this.findViewById(R.id.password);
		TextView eLoginV = (TextView) LoginActivity.this.findViewById(R.id.login_text); 
		TextView ePasswordV = (TextView) LoginActivity.this.findViewById(R.id.password_text);
		TextView eStatus = (TextView) LoginActivity.this.findViewById(R.id.connect_status); 
		
		if (display){
			eLogin.setVisibility(View.VISIBLE);
			ePassword.setVisibility(View.VISIBLE);
			eLoginV.setVisibility(View.VISIBLE);
			ePasswordV.setVisibility(View.VISIBLE);
			eStatus.setVisibility(View.INVISIBLE);
		} else {
			eLogin.setVisibility(View.INVISIBLE);
			ePassword.setVisibility(View.INVISIBLE);
			eLoginV.setVisibility(View.INVISIBLE);
			ePasswordV.setVisibility(View.INVISIBLE);
			eStatus.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == Constants.CODE_LAUNCH) {
			Log.d( LOG_TAG, "onActivityResult : CODE_LAUNCH");
			if (resultCode == Constants.CODE_EXIT) {
				Log.d( LOG_TAG, "onActivityResult : CODE_EXIT");
				this.finish();
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		releaseXivoService();
		super.onDestroy();
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

	class RemoteServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			xivoService = IXivoService.Stub.asInterface((IBinder)service);
			Log.d(LOG_TAG, "onServiceConnected");
			xivoServiceReady  = true;
			startClient();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			xivoService = null;
			xivoServiceReady = false;
			Log.d(LOG_TAG, "onServiceDisconnected");
		}
		
	}
}
