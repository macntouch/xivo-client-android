package com.proformatique.android.xivoclient.service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.tools.Constants;

import android.app.ActivityManager;
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
import android.widget.Toast;

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
	private static final String LOG_TAG = "XiVO service";
	private static final String name = "com.proformatique.android.xivoclient.service.XivoService";
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(getClass().getSimpleName(), "onBind()");
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
	};
	
	@Override
	public void onCreate() {
		Log.d(getClass().getSimpleName(),"onCreate()");
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		Log.d(getClass().getSimpleName(),"onDestroy()");
		serviceHandler.removeCallbacks(myTask);
		serviceHandler = null;
		super.onDestroy();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(getClass().getSimpleName(), "onStart()");
		super.onStart(intent, startId);
		serviceHandler = new Handler();
		serviceHandler.post(myTask);
	}
	
	class Task implements Runnable {
		
		@Override
		public void run() {
			ConnectXivo();
		}
	}
	
	private void ConnectXivo() {
		if (Connection.getInstance().isConnected()) {
			Log.d("SERVICE TEST", "Already connected");
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
						Connection.getInstance().disconnect();
					} catch (ExecutionException e) {
						Connection.getInstance().disconnect();
					} catch (TimeoutException e) {
						Connection.getInstance().disconnect();
					}
				};
			}).start();
			Log.d("SERVICE TEST", "ConnectXivo completed");
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
					
					setLoginPassword();
					Connection connection = Connection.getInstance(login, password, XivoService.this);
					
					InitialListLoader initList = InitialListLoader.init();
					int connectionCode = connection.initialize();
					
					if (connectionCode >= 1){
						return initList.startLoading(getContentResolver(), getResources(), getApplicationContext());
					}
					return connectionCode;
				} else return Constants.NO_NETWORK_AVAILABLE;
			} else return Constants.NO_NETWORK_AVAILABLE;
		}
		
		private void setLoginPassword() {
			settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			loginSettings = getApplication().getSharedPreferences("login_settings", 0);
			if (settings.getBoolean("save_login", true)){
				Log.d("SERVICE TEST", "login settings available");
				login = loginSettings.getString("login","");
				password = loginSettings.getString("password","");
			} else {
				Log.d("SERVICE TEST", "No login and password saved");
			}
		}
		
		protected void onPostExecute(Integer result) {
			
			if (result == Constants.NO_NETWORK_AVAILABLE){
				Toast.makeText(getApplicationContext(), R.string.no_web_connection, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.LOGIN_PASSWORD_ERROR) {
				Toast.makeText(getApplicationContext(), R.string.bad_login_password, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.BAD_HOST){
				Toast.makeText(getApplicationContext(), R.string.bad_host, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.NOT_CTI_SERVER){
				Toast.makeText(getApplicationContext(), R.string.not_cti_server, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.VERSION_MISMATCH) {
				Toast.makeText(getApplicationContext(), R.string.version_mismatch, Toast.LENGTH_LONG).show();
			}
			else if (result == Constants.CTI_SERVER_NOT_SUPPORTED) {
				Toast.makeText(getApplicationContext(), R.string.cti_not_supported
						, Toast.LENGTH_LONG).show();
			}
			else if (result < 1){
				Toast.makeText(getApplicationContext(), R.string.connection_failed
						, Toast.LENGTH_LONG).show();
			}
			else if(result >= 1){
				/**
				 * Parsing and Displaying xlets content
				 */
				Toast.makeText(getApplicationContext(), "Connection established", Toast.LENGTH_LONG).show();
			}
		}
	}
}
