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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.proformatique.android.xivoclient.service.IXivoConnectionService;
import com.proformatique.android.xivoclient.tools.AndroidTools;
import com.proformatique.android.xivoclient.xlets.XletDialer;

/**
 * This service will return the the XletsContainerTabActivity after receiving a
 * call. The goal is to be able to use the XiVO client after receiving a call
 * back to do transfers or other operations.
 * 
 */
public class InCallScreenKiller extends Service {
    
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private final static String LOG_TAG = "InCallScreenKiller";
    protected IXivoConnectionService xivoConnectionService = null;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            
            /**
             * Waits 2 seconds after answering a call and launch the
             * XletsContainerTabActivity
             */
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                Log.d(LOG_TAG, "onCallStateChanged called");
                try {
                    if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        if (xivoConnectionService != null && xivoConnectionService.killDialer()) {
                            AndroidTools.showOverDialer(getApplicationContext(), XletDialer.class,
                                    2000);
                        } else {
                            Log.d(LOG_TAG, "Not killing this dialer.");
                        }
                    }
                } catch (RemoteException e) {
                    Log.d(LOG_TAG, "Remote exception");
                }
            }
        };
        // Start listening
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    
    @Override
    public void onDestroy() {
        // Stop listening
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onDestroy();
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        xivoConnectionService = Connection.INSTANCE.getConnection(getApplicationContext());
        if (xivoConnectionService == null) {
            AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
                private long DELAY = 100;
                private int MAX_WAIT = 50;
                @Override
                protected Integer doInBackground(Void... params) {
                    int i = 0;
                    do {
                        try {
                            synchronized (this) {
                                wait(DELAY);
                            }
                        } catch (InterruptedException e) {
                            Log.d(LOG_TAG, "Interrupted while waiting for a binding");
                            e.printStackTrace();
                        }
                        xivoConnectionService
                                = Connection.INSTANCE.getConnection(getApplicationContext());
                        i++;
                    } while (xivoConnectionService == null && i < MAX_WAIT);
                    return 0;
                }
            };
            task.execute();
        }
    }
}
