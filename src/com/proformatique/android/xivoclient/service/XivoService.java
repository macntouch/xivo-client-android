package com.proformatique.android.xivoclient.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
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
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(getClass().getSimpleName(), "onBind()");
		return xivoServiceStub;
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
		serviceHandler.postDelayed(myTask, 2000L);
	}
	
	class Task implements Runnable {
		
		@Override
		public void run() {
			// Random values are changed here for testing
			// The hard work should be implemented here.
			changed = changed == true ? false : true;
			serviceHandler.postDelayed(this,1000L);
			Log.i(getClass().getSimpleName(), "Task running");
		}
		
	}
	
}
