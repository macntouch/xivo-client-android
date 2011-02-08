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

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.proformatique.android.xivoclient.service.Connection;
import com.proformatique.android.xivoclient.service.IXivoConnectionService;
import com.proformatique.android.xivoclient.service.XivoConnectionService;
import com.proformatique.android.xivoclient.tools.Constants;

public class HomeActivity extends XivoActivity {
	
	/**
	 * Constants
	 */
	private static final String LOG_TAG = "XiVO Home";
	private static final String PACK = "com.proformatique.android.xivoclient";
	
	/**
	 * UI
	 */
	private ProgressDialog dialog;
	
	/**
	 * Service
	 */
	private XivoConnectionServiceConnection con = null;
	private IXivoConnectionService xivoConnectionService = null;
	private ConnectTask connectTask = null;
	private AuthenticationTask authenticationTask = null;
	private BindingTask bindingTask = null;
	
	/**
	 * Activity life cycle
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "onCreate");
		setContentView(R.layout.home_activity);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.i(LOG_TAG, "onResume");
		startXivoConnectionService();
		bindXivoConnectionService();
		startInCallScreenKiller(this);
	}
	
	@Override
	protected void onDestroy() {
		Log.d( LOG_TAG, "DESTROY");
		releaseXivoConnectionService();
		stopInCallScreenKiller(this);
		super.onDestroy();
	}
	
	/**
	 * Menu
	 */
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
			HomeActivity.stopInCallScreenKiller(this);
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
		if (xivoConnectionService != null) {
			try {
				xivoConnectionService.disconnect();
			} catch (RemoteException e) {
				Toast.makeText(this, getString(R.string.remote_exception),
						Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private void menuAbout() {
		Intent defineIntent = new Intent(this, AboutActivity.class);
		startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
	}
	
	private void menuExit() {
		Log.i(LOG_TAG, "Menu exit clicked");
		finish();
	}
	
	private void menuSettings() {
		Intent defineIntent = new Intent(this, SettingsActivity.class);
		startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
	}
	
	/**
	 * Starts a connection task and wait until it's connected
	 */
	private void waitForConnection() {
		connectTask = new ConnectTask();
		connectTask.execute();
	}
	
	/**
	 * Starts an authentication task and wait until it's authenticated
	 */
	private void waitForAuthentication() {
		authenticationTask = new AuthenticationTask();
		authenticationTask.execute();
	}
	
	/**
	 * Starts the client
	 * This should be called once the service is started and connected
	 */
	private void startClient() {
		displayElements(false);
		Intent defineIntent = new Intent(HomeActivity.this, XletsContainerTabActivity.class);
		HomeActivity.this.startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
	}
	
	public void clickOnButtonOk(View v) {
		if (Connection.getInstance(HomeActivity.this).isConnected()) {
			Intent defineIntent = new Intent(HomeActivity.this, XletsContainerTabActivity.class);
			startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
		} else {
			
		}
	}
	
	public void displayElements(boolean display){
		EditText eLogin = (EditText) HomeActivity.this.findViewById(R.id.login); 
		EditText ePassword = (EditText) HomeActivity.this.findViewById(R.id.password);
		TextView eLoginV = (TextView) HomeActivity.this.findViewById(R.id.login_text); 
		TextView ePasswordV = (TextView) HomeActivity.this.findViewById(R.id.password_text);
		TextView eStatus = (TextView) HomeActivity.this.findViewById(R.id.connect_status); 
		
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
	
	/**
	 * XivoConnectionService life cycle
	 */
	
	/**
	 * Starts the XivoConnectionService
	 * If the service is not started it will get destroyed when our application is destroyed
	 */
	private void startXivoConnectionService() {
		Intent iStartXivoService = new Intent();
		iStartXivoService.setClassName(PACK, XivoConnectionService.class.getName());
		startService(iStartXivoService);
		Log.d(LOG_TAG, "Starting XiVO connection service");
	}
	
	/**
	 * Binds the XivoConnection service
	 */
	private void bindXivoConnectionService() {
		bindingTask = new BindingTask();
		bindingTask.execute();
	}
	
	/**
	 * Releases the service before leaving
	 */
	private void releaseXivoConnectionService() {
		if (con != null) {
			unbindService(con);
			con = null;
			Log.d(LOG_TAG, "XiVO connection service released");
		} else {
			Log.d(LOG_TAG, "XiVO connection service not binded");
		}
	}
	
	private class XivoConnectionServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			xivoConnectionService = IXivoConnectionService.Stub.asInterface((IBinder)service);
			if (xivoConnectionService == null)
				Log.e(LOG_TAG, "xivoConnectionService is null");
			else
				Log.i(LOG_TAG, "xivoConnectionService is not null");
			Log.d(LOG_TAG, "onServiceConnected");
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(LOG_TAG, "onServiceDisconnected");
		}
	};
	
	/**
	 * InCallScreenKiller service lifecycle
	 */
	public static void startInCallScreenKiller(Context context) {
		Intent inCallScreenKillerIntent = new Intent();
		inCallScreenKillerIntent.setClassName(PACK, InCallScreenKiller.class.getName());
		context.startService(inCallScreenKillerIntent);
		Log.d(LOG_TAG, "InCallScreenKiller started");
	}
	
	public static void stopInCallScreenKiller(Context context) {
		Intent inCallScreenKillerIntent = new Intent();
		inCallScreenKillerIntent.setClassName(PACK, InCallScreenKiller.class.getName());
		context.stopService(inCallScreenKillerIntent);
		Log.d(LOG_TAG, "InCallScreenKilled stopped");
	}
	
	/**
	 * Tasks
	 */
	
	/**
	 * Binds to the service
	 */
	private class BindingTask extends AsyncTask<Void, Void, Integer> {
		private int OK = 0;
		private int FAIL = -1;
		
		@Override
		protected void onPreExecute() {
			Log.d(LOG_TAG, "Binding started");
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			if (con == null) {
				con = new XivoConnectionServiceConnection();
				Intent iServiceBinder = new Intent();
				iServiceBinder.setClassName(PACK, XivoConnectionService.class.getName());
				bindService(iServiceBinder, con, Context.BIND_AUTO_CREATE);
				Log.d(LOG_TAG, "XiVO connection service binded");
			} else {
				Log.d(LOG_TAG, "XiVO connection already binded");
			}
			if (con != null)
				return OK;
			else
				return FAIL;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			Log.d(LOG_TAG, "Binding finished");
			waitForConnection();
		}
	}
	
	/**
	 * Ask to the XivoConnectionService to connect and wait for the result
	 */
	private class ConnectTask extends AsyncTask<Void, Void, Integer> {
		
		public ConnectTask() {
			if (dialog == null)
				dialog = new ProgressDialog(HomeActivity.this);
			dialog.setCancelable(true);
			dialog.setMessage(getString(R.string.connection));
		}
		
		@Override
		protected void onPreExecute() {
			dialog.show();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			try {
				if (xivoConnectionService != null && xivoConnectionService.isConnected())
					return Constants.CONNECTION_OK;
				return xivoConnectionService.connect();
			} catch (RemoteException e) {
				return Constants.REMOTE_EXCEPTION;
			}
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			if (dialog != null) {
				dialog.dismiss();
				dialog = null;
			}
			switch (result) {	
			case Constants.CONNECTION_OK:
				waitForAuthentication();
				break;
			case Constants.REMOTE_EXCEPTION:
				Toast.makeText(HomeActivity.this, getString(R.string.remote_exception),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.NOT_CTI_SERVER:
				Toast.makeText(HomeActivity.this, getString(R.string.not_cti_server),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.BAD_HOST:
				Toast.makeText(HomeActivity.this, getString(R.string.bad_host),
						Toast.LENGTH_LONG).show();
				break;
			default:
				Toast.makeText(HomeActivity.this, getString(R.string.connection_failed),
						Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
	
	private class AuthenticationTask extends AsyncTask<Void, Void, Integer> {
		
		public AuthenticationTask() {
			if (dialog == null)
				dialog = new ProgressDialog(HomeActivity.this);
			dialog.setCancelable(true);
			dialog.setMessage(getString(R.string.authenticating));
		}
		
		@Override
		protected void onPreExecute() {
			dialog.show();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			try {
				if (xivoConnectionService != null && xivoConnectionService.isAuthenticated())
					return Constants.AUTHENTICATION_OK;
				return xivoConnectionService.authenticate();
			} catch (RemoteException e) {
				return Constants.REMOTE_EXCEPTION;
			}
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			if (dialog != null) {
				dialog.dismiss();
				dialog = null;
			}
			switch(result) {
			case Constants.OK:
			case Constants.AUTHENTICATION_OK:
				Log.i(LOG_TAG, "Authenticated");
				break;
			case Constants.JSON_POPULATE_ERROR:
				Toast.makeText(HomeActivity.this, getString(R.string.json_exception),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.FORCED_DISCONNECT:
				Toast.makeText(HomeActivity.this, getString(R.string.forced_disconnect),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.LOGIN_PASSWORD_ERROR:
				Toast.makeText(HomeActivity.this, getString(R.string.bad_login_password),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.CTI_SERVER_NOT_SUPPORTED:
				Toast.makeText(HomeActivity.this, getString(R.string.cti_not_supported),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.VERSION_MISMATCH:
				Toast.makeText(HomeActivity.this, getString(R.string.version_mismatch),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.ALGORITH_NOT_AVAILABLE:
				Toast.makeText(HomeActivity.this, getString(R.string.algo_exception),
						Toast.LENGTH_LONG).show();
				break;
			default:
				Log.e(LOG_TAG, "Unhandled result " + result);
				Toast.makeText(HomeActivity.this, getString(R.string.login_ko),
						Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
}
