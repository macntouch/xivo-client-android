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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.proformatique.android.xivoclient.service.IXivoConnectionService;
import com.proformatique.android.xivoclient.service.XivoConnectionService;
import com.proformatique.android.xivoclient.tools.Constants;

public class HomeActivity extends XivoActivity implements OnItemClickListener {
	
	/**
	 * Constants
	 */
	private static final String LOG_TAG = "XiVO Home";
	private static final String PACK = "com.proformatique.android.xivoclient";
	
	/**
	 * UI
	 */
	private ProgressDialog dialog;
	private GridView grid;
	
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
		super.registerButtons();	// Set onClickListeners for the XivoActivity
		
		
		grid = (GridView) findViewById(R.id.grid);
		grid.setAdapter(new ImageAdapter());
		grid.setOnItemClickListener(this);
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
	
	public void onClick(View v) {
		Log.d(LOG_TAG, "onClick");
	}
	
	/**
	 * Makes sure the service is authenticated and that data are loaded
	 */
	private void launchCTIConnection() {
		if (xivoConnectionService != null) {
			waitForConnection();
			waitForAuthentication();
			startLoading();
		} else {
			Log.d(LOG_TAG, "launchCTIConnection == null");
			dieOnBindFail();
		}
	}
	
	/**
	 * Starts a connection task and wait until it's connected
	 */
	private void waitForConnection() {
		try {
			if (xivoConnectionService.isConnected())
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
			e.printStackTrace();
		}
	}
	
	/**
	 * Check if the service received lists from the CTI server
	 * Gets the list if they are not available
	 */
	private void startLoading() {
		try {
			if (xivoConnectionService.loadDataCalled()) {
				Log.d(LOG_TAG, "Data already loaded");
				return;
			}
			xivoConnectionService.loadData();
		} catch (RemoteException e) {
			dieOnBindFail();
		}
	}
	
	/**
	 * Kills the app and display a message when the binding to the service cannot be astablished
	 * ___This should NOT happen___
	 */
	private void dieOnBindFail() {
		Toast.makeText(this, getString(R.string.binding_error), Toast.LENGTH_LONG).show();
		Log.e(LOG_TAG, "Failed to bind to the service");
		finish();
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
			
			// wait until it's connected...
			while (con == null || xivoConnectionService == null);
			
			return xivoConnectionService == null ? FAIL : OK;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			Log.d(LOG_TAG, "Binding finished");
			launchCTIConnection();
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
			case Constants.NO_NETWORK_AVAILABLE:
				Toast.makeText(HomeActivity.this, getString(R.string.no_web_connection),
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
	
	private class ImageAdapter extends BaseAdapter {
		
		@Override
		public int getCount() {
			return 20;
		}
		
		@Override
		public Object getItem(int position) {
			return null;
		}
		
		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if (convertView == null) {
				LayoutInflater li = getLayoutInflater();
				v = li.inflate(R.layout.icon, null);
			} else {
				v = convertView;
			}
			TextView tv = (TextView) v.findViewById(R.id.icon_text);
			ImageView iv = (ImageView) v.findViewById(R.id.icon_image);
			switch (position) {
			case 0:
				// Dialer
				tv.setText(getString(R.string.dialer_btn_lbl));
				iv.setImageResource(R.drawable.ic_menu_call);
				break;
			case 1:
				// User List
				tv.setText(getString(R.string.userslist_btn_lbl));
				iv.setImageResource(R.drawable.ic_menu_friendslist);
				break;
			case 2:
				// History
				tv.setText(getString(R.string.history_btn_lbl));
				iv.setImageResource(R.drawable.ic_menu_recent_history);
				break;
			case 3:
				// Services
				tv.setText(getString(R.string.service_btn_lbl));
				iv.setImageResource(R.drawable.ic_menu_manage);
				break;
			default:
				tv.setText("...");
				iv.setImageResource(R.drawable.icon);
				break;
			}
			return v;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch(position) {
		case 0:
			Toast.makeText(this, "clicked first", Toast.LENGTH_SHORT).show();
			break;
		case 1:
			Toast.makeText(this, "clicked second", Toast.LENGTH_SHORT).show();
			break;
		case 2:
		case 3:
		default:
			Toast.makeText(this, getString(R.string.unhandled_click), Toast.LENGTH_LONG).show();
			break;
		}
	}
}
