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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.proformatique.android.xivoclient.tools.Constants;

public class LoginActivity extends XivoActivity {
	
	/**
	 * Creating distinct preferences to avoid multiple references 
	 * of the same data (login/password) in settings screen
	 */
	private SharedPreferences settings;
	private SharedPreferences loginSettings;
	ConnectTask connectTask;
	ProgressDialog dialog;
	private static final String LOG_TAG = "LOGIN_ACTIVITY";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
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
		
		if (Connection.getInstance().isConnected()) {
			displayElements(false);
			Intent defineIntent = new Intent(LoginActivity.this, XletsContainerTabActivity.class);
			LoginActivity.this.startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
		}
		else displayElements(true);
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
			LoginActivity.stopInCallScreenKiller(this);
			if (Connection.getInstance().isConnected())
				Connection.getInstance().disconnect();
			displayElements(true);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void menuAbout() {
		Intent defineIntent = new Intent(this, AboutActivity.class);
		startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
	}
	
	private void menuExit() {
		if (Connection.getInstance().isConnected())
			Connection.getInstance().disconnect();
		finish();
	}
	
	private void menuSettings() {
		Intent defineIntent = new Intent(this, SettingsActivity.class);
		startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
		
	}
	
	public void clickOnButtonOk(View v) {
		if (Connection.getInstance().isConnected()) {
			Intent defineIntent = new Intent(LoginActivity.this, XletsContainerTabActivity.class);
			startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
		} else {
			connectTask = new ConnectTask();
			connectTask.execute();
			
			/**
			 * Timeout Connection : 20 seconds
			 */
			new Thread(new Runnable() {
				public void run() {
					
					try {
						connectTask.get(20, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Connection.getInstance().disconnect();
					} catch (ExecutionException e) {
						Connection.getInstance().disconnect();
					} catch (TimeoutException e) {
						Connection.getInstance().disconnect();
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
	
	/**
	 * Creating a AsyncTask to execute connection process
	 * @author cquaquin
	 */
	private class ConnectTask extends AsyncTask<Void, Integer, Integer> {
		
		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(LoginActivity.this);
			dialog.setMessage(getString(R.string.loading));
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.show();
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
					
					InitialListLoader initList = InitialListLoader.init();
					int connectionCode = connection.initialize();
					
					if (connectionCode >= 1){
						return initList.startLoading();
					}
					return connectionCode;
				} else return Constants.NO_NETWORK_AVAILABLE;
			} else return Constants.NO_NETWORK_AVAILABLE;
		}
		
		protected void onPostExecute(Integer result) {
			
			if (result == Constants.NO_NETWORK_AVAILABLE){
				dialog.dismiss();
				Toast.makeText(LoginActivity.this, R.string.no_web_connection, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.LOGIN_PASSWORD_ERROR) {
				dialog.dismiss();
				Toast.makeText(LoginActivity.this, R.string.bad_login_password, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.BAD_HOST){
				dialog.dismiss();
				Toast.makeText(LoginActivity.this, R.string.bad_host, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.NOT_CTI_SERVER){
				dialog.dismiss();
				Toast.makeText(LoginActivity.this, R.string.not_cti_server, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.VERSION_MISMATCH) {
				dialog.dismiss();
				Toast.makeText(LoginActivity.this, R.string.version_mismatch, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.CTI_SERVER_NOT_SUPPORTED) {
				dialog.dismiss();
				Toast.makeText(LoginActivity.this, R.string.cti_not_supported
						, Toast.LENGTH_LONG).show();
			}
			else if (result < 1){
				dialog.dismiss();
				Toast.makeText(LoginActivity.this, R.string.connection_failed
						, Toast.LENGTH_LONG).show();
			}
			else if(result >= 1){
				
				if (Connection.getInstance().getSaveLogin()){
					saveLoginPassword();
				}
				
				/**
				 * Parsing and Displaying xlets content
				 */
				Intent defineIntent = new Intent(LoginActivity.this, XletsContainerTabActivity.class);
				LoginActivity.this.startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
				displayElements(false);
				dialog.dismiss();
			}
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		Log.d( LOG_TAG, "onActivityResult");
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
		if (Connection.getInstance() != null && Connection.getInstance().isConnected()) {
			Connection.getInstance().disconnect();
		}
		super.onDestroy();
	}
}
