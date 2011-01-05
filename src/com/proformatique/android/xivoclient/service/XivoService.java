package com.proformatique.android.xivoclient.service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.tools.Constants;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * The XiVO service connects to the CTI server and listen to incoming events
 * 
 * @author Pascal Cadotte-Michaud
 *
 */
public class XivoService extends Service {
	
	private Handler serviceHandler;
	private Task myTask= new Task();
	boolean changed = false;
	ConnectTask connectTask;
	private SharedPreferences settings;
	private SharedPreferences loginSettings;
	String login;
	String password;
	private static final String LOG_TAG = "XiVO " + XivoService.class.getSimpleName();
	private static final String name = "com.proformatique.android.xivoclient.service.XivoService";
	private int xivoConnectionStatus = Constants.XIVO_DISCONNECTED;
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(LOG_TAG, "onBind");
		return xivoServiceStub;
	}
	
	/**
	 * Check if the service is running
	 * @param context
	 * @return
	 */
	public static boolean isRunning(Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> rs = am.getRunningServices(50);
		for (int i = 0; i < rs.size(); i++) {
			ActivityManager.RunningServiceInfo serviceInfo = rs.get(i);
			if (serviceInfo.service.getClassName().equals(name))
				return true;
		}
		Log.i(LOG_TAG, "XiVO service not running");
		return false;
	}
	
	private IXivoService.Stub xivoServiceStub = new IXivoService.Stub() {
		/**
		 * Query the service to know if the contact list has changed since the last query
		 */
		public boolean contactsChanged() throws RemoteException {
			return changed;
		}

		@Override
		public boolean isConnected() throws RemoteException {
			return xivoConnectionStatus == Constants.XIVO_CONNECTED;
		}
	};
	
	@Override
	public void onCreate() {
		Log.d(LOG_TAG,"onCreate()");
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		Log.d(LOG_TAG,"onDestroy");
		serviceHandler.removeCallbacks(myTask);
		serviceHandler = null;
		xivoConnectionStatus = Constants.XIVO_DISCONNECTED;
		super.onDestroy();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(LOG_TAG, "onStart");
		super.onStart(intent, startId);
		// Check for a username and password here
		settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		loginSettings = getApplication().getSharedPreferences("login_settings", 0);
		if (settings.getBoolean("save_login", true)
				&& loginSettings.getString("login", "").equals("") == false
				&& loginSettings.getString("password", "").equals("") == false){
			serviceHandler = new Handler();
			serviceHandler.post(myTask);
			InitialListLoader.getInstance().init(getApplicationContext());
		} else {
			Log.e(LOG_TAG, "No login/password settings available");
			xivoConnectionStatus |= Constants.LOGIN_MISSING;
		}
	}
	
	class Task implements Runnable {
		
		@Override
		public void run() {
			ConnectXivo();
		}
	}
	
	private void ConnectXivo() {
		if (Connection.getInstance(getApplicationContext()).isConnected()) {
			Log.d(LOG_TAG, "Already connected");
		} else {
			connectTask = new ConnectTask();
			connectTask.execute();
			
			/**
			 * Timeout Connection : 10 seconds
			 */
			new Thread(new Runnable() {
				public void run() {
					
					try {
						connectTask.get(10, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Connection.getInstance(getApplicationContext()).disconnect();
					} catch (ExecutionException e) {
						Connection.getInstance(getApplicationContext()).disconnect();
					} catch (TimeoutException e) {
						Connection.getInstance(getApplicationContext()).disconnect();
					}
				};
			}).start();
			Log.d(LOG_TAG, "ConnectXivo completed");
		}
	}
	
	/**
	 * Creating a AsyncTask to execute connection process
	 * @author cquaquin
	 */
	private class ConnectTask extends AsyncTask<Void, Integer, Integer> {
		
		@Override
		protected Integer doInBackground(Void... params) {
			 /**
			 * Checking that web connection exists
			 */
			ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getActiveNetworkInfo();
			
			if (!(netInfo == null)) {
				if (netInfo.getState().compareTo(State.CONNECTED)==0) {
					
					try {
						setLoginPassword();
					} catch (Exception e) {
						return Constants.LOGIN_PASSWORD_ERROR;
					}
					
					Connection connection = Connection.getInstance(login, password, XivoService.this);
					
					InitialListLoader initList = InitialListLoader.getInstance();
					int connectionCode = connection.initialize();
					
					if (connectionCode >= 1){
						return initList.startLoading(getContentResolver(), getResources(), getApplicationContext());
					}
					return connectionCode;
				} else return Constants.NO_NETWORK_AVAILABLE;
			} else return Constants.NO_NETWORK_AVAILABLE;
		}
		
		private void setLoginPassword() throws Exception {
			settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			loginSettings = getApplication().getSharedPreferences("login_settings", 0);
			if (settings.getBoolean("save_login", true)){
				Log.d(LOG_TAG, "login settings available");
				login = loginSettings.getString("login","");
				password = loginSettings.getString("password","");
			} else {
				throw new Exception();
			}
		}
		
		@Override
		protected void onPreExecute() {
			Log.i(LOG_TAG, "Connection task starting");
		}
		
		protected void onPostExecute(Integer result) {
			xivoConnectionStatus = Constants.XIVO_DISCONNECTED;
			if (result == Constants.NO_NETWORK_AVAILABLE){
				Log.e(LOG_TAG, getString(R.string.no_web_connection));
			}
			else if (result == Constants.LOGIN_PASSWORD_ERROR) {
				Log.e(LOG_TAG, getString(R.string.bad_login_password));
			}
			else if (result == Constants.BAD_HOST){
				Log.e(LOG_TAG, getString(R.string.bad_host));
			}
			else if (result == Constants.NOT_CTI_SERVER){
				Log.e(LOG_TAG, getString(R.string.not_cti_server));
			}
			else if (result == Constants.VERSION_MISMATCH) {
				Log.e(LOG_TAG, getString(R.string.version_mismatch));
			}
			else if (result == Constants.CTI_SERVER_NOT_SUPPORTED) {
				Log.e(LOG_TAG, getString(R.string.cti_not_supported));
			}
			else if (result < 1){
				Log.e(LOG_TAG, getString(R.string.connection_failed));
			}
			else if(result >= 1){
				Log.i(LOG_TAG, "Connection established");
				xivoConnectionStatus = Constants.XIVO_CONNECTED;
			}
		}
	}
}
