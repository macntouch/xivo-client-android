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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.proformatique.android.xivoclient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.proformatique.android.xivoclient.service.Connection;
import com.proformatique.android.xivoclient.service.IXivoService;
import com.proformatique.android.xivoclient.service.InitialListLoader;
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
	private IXivoService xivoService;
	private boolean serviceStarted = false;
	private RemoteServiceConnection conn = null;
	public boolean xivoServiceReady = false;
	ConnectTask connectTask;
	LoadingTask loadingTask;
	private static final String LOG_TAG = "LOGIN_ACTIVITY";
	
	private ProgressDialog progressDialog;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "onCreate");
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
	
	@Override
	public void onResume() {
		super.onResume();
		Log.i(LOG_TAG, "onResume");
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
	
	public static void startInCallScreenKiller(Context context) {
		Intent inCallScreenKillerIntent = new Intent();
		inCallScreenKillerIntent.setClassName(context.getPackageName(), InCallScreenKiller.class.getName());
		context.startService(inCallScreenKillerIntent);
		Log.d(LOG_TAG, "InCallScreenKiller started");
	}
	
	public static void stopInCallScreenKiller(Context context) {
		Intent inCallScreenKillerIntent = new Intent();
		inCallScreenKillerIntent.setClassName(context.getPackageName(), InCallScreenKiller.class.getName());
		context.stopService(inCallScreenKillerIntent);
		Log.d(LOG_TAG, "InCallScreenKilled stopped");
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
			LoginActivity.stopInCallScreenKiller(this);
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
        LoginActivity.stopInCallScreenKiller(this);
        if (Connection.getInstance(LoginActivity.this).isConnected())
            Connection.getInstance(LoginActivity.this).disconnect();
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
		/*saveLoginPassword();
		startXivoService();
		bindXivoService();
		
		if (xivoServiceReady == true)
			startClient();*/
		if (Connection.getInstance(LoginActivity.this).isConnected()) {
			Intent defineIntent = new Intent(LoginActivity.this, XletsContainerTabActivity.class);
			startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
		} else {
			connectTask = new ConnectTask();
			connectTask.execute();
			loadingTask = new LoadingTask();
			
			/**
			 * Timeout Connection : 10 seconds
			 */
			new Thread(new Runnable() {
				public void run() {
					Looper.prepare();
					try {
						connectTask.get(10, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Connection.getInstance(LoginActivity.this).disconnect();
					} catch (ExecutionException e) {
						Connection.getInstance(LoginActivity.this).disconnect();
					} catch (TimeoutException e) {
						Connection.getInstance(LoginActivity.this).disconnect();
					}
					loadingTask.execute();
					try {
						loadingTask.get(60, TimeUnit.SECONDS);
					} catch (TimeoutException e) {
						Log.d(LOG_TAG, e.toString());
					} catch (InterruptedException e) {
						Log.d(LOG_TAG, e.toString());
					} catch (ExecutionException e) {
						Log.d(LOG_TAG, e.toString());
					}
				};
			}).start();
			
		}
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
	
	private class LoadingTask extends AsyncTask<Void, Integer, Integer> {
		
		@Override
		protected Integer doInBackground(Void... params) {
			Log.d(LOG_TAG, "LoadingTask doInBackground");
			InitialListLoader.getInstance().startLoading();
			return 0;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			Log.d(LOG_TAG, "LoadingTask onPostExecute");
			Intent defineIntent = new Intent(LoginActivity.this, XletsContainerTabActivity.class);
			LoginActivity.this.startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
			progressDialog.dismiss();
		}
	}
	
	/**
	 * Creating a AsyncTask to execute connection process
	 * @author cquaquin
	 */
	private class ConnectTask extends AsyncTask<Void, Integer, Integer> {
		
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(LoginActivity.this);
			progressDialog.setCancelable(false);
			progressDialog.setMessage("Connecting");
			progressDialog.show();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			EditText eLogin = (EditText) LoginActivity.this.findViewById(R.id.login); 
			EditText ePassword = (EditText) LoginActivity.this.findViewById(R.id.password); 
			
			 /**
			 * Checking that web connection exists
			 */
			ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getActiveNetworkInfo();
			
			if (!(netInfo == null)) {
				if (netInfo.getState().compareTo(State.CONNECTED)==0) {
					
					Connection connection = Connection.getInstance(eLogin.getText().toString(),
							ePassword.getText().toString(), LoginActivity.this);
					
					//InitialListLoader.init();
					
					return  connection.initialize();
				} else return Constants.NO_NETWORK_AVAILABLE;
			} else return Constants.NO_NETWORK_AVAILABLE;
		}
		
		protected void onPostExecute(Integer result) {
			Log.d(LOG_TAG, "Connect Task onPostExecute");
			progressDialog.dismiss();
			if (result == Constants.NO_NETWORK_AVAILABLE){
				Toast.makeText(LoginActivity.this, R.string.no_web_connection, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.LOGIN_PASSWORD_ERROR) {
				Toast.makeText(LoginActivity.this, R.string.bad_login_password, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.BAD_HOST){
				Toast.makeText(LoginActivity.this, R.string.bad_host, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.NOT_CTI_SERVER){
				Toast.makeText(LoginActivity.this, R.string.not_cti_server, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.VERSION_MISMATCH) {
				Toast.makeText(LoginActivity.this, R.string.version_mismatch, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.CTI_SERVER_NOT_SUPPORTED) {
				Toast.makeText(LoginActivity.this, R.string.cti_not_supported
						, Toast.LENGTH_LONG).show();
			}
			else if (result < 1){
				Toast.makeText(LoginActivity.this, R.string.connection_failed
						, Toast.LENGTH_LONG).show();
			}
			else if(result >= 1){
				if (Connection.getInstance(LoginActivity.this).getSaveLogin()){
					saveLoginPassword();
				}
				displayElements(false);
				progressDialog = new ProgressDialog(LoginActivity.this);
				progressDialog.setCancelable(false);
				progressDialog.setMessage("Loading...");
				progressDialog.show();
			}
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
		Log.d( LOG_TAG, "DESTROY");
		LoginActivity.stopInCallScreenKiller(this);
		if (Connection.getInstance(LoginActivity.this) != null && Connection.getInstance(LoginActivity.this).isConnected()) {
			Connection.getInstance(LoginActivity.this).disconnect();
		}
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
