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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.proformatique.android.xivoclient.service.CapapresenceProvider;
import com.proformatique.android.xivoclient.service.IXivoConnectionService;
import com.proformatique.android.xivoclient.service.XivoConnectionService;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.tools.GraphicsManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An overloaded Activity class to make UI changes and options consistent
 * across the application
 * 
 * @author Pascal Cadotte-Michaud
 *
 */
public class XivoActivity extends Activity implements OnClickListener {
	
	private final static String TAG = "XivoActivity";
	
	/*
	 * Service
	 */
	protected BindingTask bindingTask = null;
	private XivoConnectionServiceConnection con = null;
	protected IXivoConnectionService xivoConnectionService = null;
	private ConnectTask connectTask = null;
	private AuthenticationTask authenticationTask = null;
	private IntentReceiver receiver = null;
	
	private SharedPreferences settings;
	
	/*
	 * UI
	 */
	private ImageView statusButton;
	private ProgressDialog dialog;
	
	/*
	 * Activity lifecycle
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (settings.getBoolean("use_fullscreen", false)) {
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		receiver = new IntentReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_MY_STATUS_CHANGE);
		registerReceiver(receiver, new IntentFilter(filter));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startXivoConnectionService();
		bindXivoConnectionService();
	}
	
	@Override
	protected void onDestroy() {
		releaseXivoConnectionService();
		unregisterReceiver(receiver);
		super.onDestroy();
	}
	
	/**
	 * Called when the binding to the service is completed
	 */
	protected void onBindingComplete() {
		Log.d(TAG, "onBindingComplete");
		launchCTIConnection();
		try {
			updateMyStatus(xivoConnectionService.getStateId());
		} catch (RemoteException e) {
			Log.d(TAG, "Could not set my state id");
		}
	}
	
	/*
	 * GUI
	 */
	protected void registerButtons() {
		statusButton = (ImageView) findViewById(R.id.statusContact);
		statusButton.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.statusContact:
			Toast.makeText(this, "Status button clicked", Toast.LENGTH_LONG).show();
			break;
		default:
			break;
		}
	}
	
	/*
	 * Menus
	 */
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
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_settings, menu);
		MenuItem mi = menu.findItem(R.id.menu_disconnect);
		mi.setVisible(true);
		
		return true;
	}
	
	private void menuDisconnect() {
		disconnect();
	}
	
	private void menuAbout() {
		Intent defineIntent = new Intent(this, AboutActivity.class);
		startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
	}
	
	private void menuExit() {
		finish();
	}
	
	private void menuSettings() {
		Intent defineIntent = new Intent(this, SettingsActivity.class);
		startActivityForResult(defineIntent, Constants.CODE_LAUNCH);
	}
	
	/*
	 * Service
	 */
	protected void disconnect() {
		if (xivoConnectionService != null) {
			try {
				xivoConnectionService.disconnect();
			} catch (RemoteException e) {
				Toast.makeText(this, getString(R.string.remote_exception)
						, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	/**
	 * Starts the XivoConnectionService
	 * If the service is not started it will get destroyed when our application is destroyed
	 */
	private void startXivoConnectionService() {
		Intent iStartXivoService = new Intent();
		iStartXivoService.setClassName(Constants.PACK, XivoConnectionService.class.getName());
		startService(iStartXivoService);
		Log.d(TAG, "Starting XiVO connection service");
	}
	
	/**
	 * Makes sure the service is authenticated and that data are loaded
	 */
	private void launchCTIConnection() {
		if (xivoConnectionService != null) {
			waitForConnection();
			waitForAuthentication();
		} else {
			Log.d(TAG, "launchCTIConnection == null");
			dieOnBindFail();
		}
	}
	
	/**
	 * Releases the service before leaving
	 */
	private void releaseXivoConnectionService() {
		if (con != null) {
			unbindService(con);
			con = null;
			Log.d(TAG, "XiVO connection service released");
		} else {
			Log.d(TAG, "XiVO connection service not binded");
		}
	}
	
	/**
	 * Starts a connection task and wait until it's connected
	 */
	private void waitForConnection() {
		try {
			if (xivoConnectionService.isConnected() && xivoConnectionService.isAuthenticated())
				return;
		} catch (RemoteException e) {
			dieOnBindFail();
		}
		connectTask = new ConnectTask();
		connectTask.execute();
		try {
			connectTask.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts an authentication task and wait until it's authenticated
	 */
	private void waitForAuthentication() {
		try {
			if (!(xivoConnectionService.isConnected())) {
				return;
			}
			if (xivoConnectionService.isAuthenticated())
				return;
		} catch (RemoteException e) {
			dieOnBindFail();
		}
		authenticationTask = new AuthenticationTask();
		authenticationTask.execute();
		try {
			authenticationTask.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			Toast.makeText(this, getString(R.string.authentication_timeout),
					Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Check if the service received lists from the CTI server
	 * Gets the list if they are not available
	 */
	private void startLoading() {
		try {
			if (xivoConnectionService.isAuthenticated()) {
				if (xivoConnectionService.loadDataCalled()) {
					Log.d(TAG, "Data already loaded");
					return;
				}
				xivoConnectionService.loadData();
			}
		} catch (RemoteException e) {
			dieOnBindFail();
		}
	}
	
	/**
	 * Retrieves our status from the DB and update the header
	 * @param id
	 */
	private void updateMyStatus(long id) {
		Cursor c = getContentResolver().query(CapapresenceProvider.CONTENT_URI,
				new String[]{
					CapapresenceProvider._ID,
					CapapresenceProvider.LONGNAME,
					CapapresenceProvider.COLOR},
				CapapresenceProvider._ID + " = " + id, null, null);
		if (c.getCount() != 0) {
			c.moveToFirst();
			((TextView) findViewById(R.id.identity_current_state_longname)).setText(
					c.getString(c.getColumnIndex(CapapresenceProvider.LONGNAME)));
			GraphicsManager.setIconStateDisplay(this,
					(ImageView) findViewById(R.id.identity_current_state_image),
					c.getString(c.getColumnIndex(CapapresenceProvider.COLOR)));
		}
		c.close();
	}
	
	/**
	 * Binds the XivoConnection service
	 */
	private void bindXivoConnectionService() {
		bindingTask = new BindingTask();
		bindingTask.execute();
	}
	
	/**
	 * Establish a binding between the activity and the XivoConnectionService
	 *
	 */
	protected class XivoConnectionServiceConnection implements ServiceConnection {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			xivoConnectionService = IXivoConnectionService.Stub.asInterface((IBinder)service);
			if (xivoConnectionService == null)
				Log.e(TAG, "xivoConnectionService is null");
			else
				Log.i(TAG, "xivoConnectionService is not null");
			Log.d(TAG, "onServiceConnected");
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "onServiceDisconnected");
		}
	};
	
	/**
	 * Binds to the service
	 */
	protected class BindingTask extends AsyncTask<Void, Void, Integer> {
		private int OK = 0;
		private int FAIL = -1;
		
		@Override
		protected void onPreExecute() {
			Log.d(TAG, "Binding started");
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			if (con == null) {
				con = new XivoConnectionServiceConnection();
				Intent iServiceBinder = new Intent();
				iServiceBinder.setClassName(Constants.PACK, XivoConnectionService.class.getName());
				bindService(iServiceBinder, con, Context.BIND_AUTO_CREATE);
				Log.d(TAG, "XiVO connection service binded");
			} else {
				Log.d(TAG, "XiVO connection already binded");
			}
			
			// wait until it's connected...
			while (con == null || xivoConnectionService == null);
			
			return xivoConnectionService == null ? FAIL : OK;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			Log.d(TAG, "Binding finished");
			onBindingComplete();
		}
	}
	
	/**
	 * Kills the app and display a message when the binding to the service cannot be astablished
	 * ___This should NOT happen___
	 */
	private void dieOnBindFail() {
		Toast.makeText(this, getString(R.string.binding_error), Toast.LENGTH_LONG).show();
		Log.e(TAG, "Failed to bind to the service");
		finish();
	}
	
	private class AuthenticationTask extends AsyncTask<Void, Void, Integer> {
		
		public AuthenticationTask() {
			if (dialog == null)
				dialog = new ProgressDialog(XivoActivity.this);
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
			if (result != Constants.OK && result != Constants.AUTHENTICATION_OK) {
				try {
					xivoConnectionService.disconnect();
				} catch (RemoteException e) {
					Toast.makeText(XivoActivity.this, getString(R.string.remote_exception),
							Toast.LENGTH_SHORT).show();
				}
			}
			switch(result) {
			case Constants.OK:
			case Constants.AUTHENTICATION_OK:
				Log.i(TAG, "Authenticated");
				startLoading();
				break;
			case Constants.JSON_POPULATE_ERROR:
				Toast.makeText(XivoActivity.this, getString(R.string.login_ko),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.FORCED_DISCONNECT:
				Toast.makeText(XivoActivity.this, getString(R.string.forced_disconnect),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.LOGIN_PASSWORD_ERROR:
				Toast.makeText(XivoActivity.this, getString(R.string.bad_login_password),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.CTI_SERVER_NOT_SUPPORTED:
				Toast.makeText(XivoActivity.this, getString(R.string.cti_not_supported),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.VERSION_MISMATCH:
				Toast.makeText(XivoActivity.this, getString(R.string.version_mismatch),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.ALGORITH_NOT_AVAILABLE:
				Toast.makeText(XivoActivity.this, getString(R.string.algo_exception),
						Toast.LENGTH_LONG).show();
				break;
			default:
				Log.e(TAG, "Unhandled result " + result);
				Toast.makeText(XivoActivity.this, getString(R.string.login_ko),
						Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
	
	/**
	 * Ask to the XivoConnectionService to connect and wait for the result
	 */
	private class ConnectTask extends AsyncTask<Void, Void, Integer> {
		
		public ConnectTask() {
			if (dialog == null)
				dialog = new ProgressDialog(XivoActivity.this);
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
				break;
			case Constants.REMOTE_EXCEPTION:
				Toast.makeText(XivoActivity.this, getString(R.string.remote_exception),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.NOT_CTI_SERVER:
				Toast.makeText(XivoActivity.this, getString(R.string.not_cti_server),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.BAD_HOST:
				Toast.makeText(XivoActivity.this, getString(R.string.bad_host),
						Toast.LENGTH_LONG).show();
				break;
			case Constants.NO_NETWORK_AVAILABLE:
				Toast.makeText(XivoActivity.this, getString(R.string.no_web_connection),
						Toast.LENGTH_LONG).show();
				break;
			default:
				Toast.makeText(XivoActivity.this, getString(R.string.connection_failed),
						Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
	
	private class IntentReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.ACTION_MY_STATUS_CHANGE)) {
				updateMyStatus(intent.getLongExtra("id", 0));
			}
		}
	}
}
