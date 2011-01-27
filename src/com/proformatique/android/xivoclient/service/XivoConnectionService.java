package com.proformatique.android.xivoclient.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import com.proformatique.android.xivoclient.tools.Constants;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class XivoConnectionService extends Service {
    
    private static final String TAG = "XiVO connection service";
    
    private SharedPreferences prefs = null;
    private Socket networkConnection = null;
    private BufferedReader inputBuffer = null;
    private long bytesReceived = 0L;
    
    /**
     * Implementation of the methods between the service and the activities
     */
    private final IXivoConnectionService.Stub binder = new IXivoConnectionService.Stub() {
        
        @Override
        public int connect() throws RemoteException {
            int res = connectToServer();
            switch(res) {
            case Constants.BAD_HOST:
                Log.d(TAG, "Connection failed, bad host");
                return res;
            case Constants.NOT_CTI_SERVER:
                Log.d(TAG, "This host/port is not a CTI server");
                return res;
            case Constants.CONNECTION_OK:
                Log.d(TAG, "Connected to the server");
                return res;
            }
            return res;
        }
        
        @Override
        public void disconnect() throws RemoteException {
        }
        
        @Override
        public boolean isLoggedIn() throws RemoteException {
            return false;
        }
        
        @Override
        public void logOff() throws RemoteException {
        }
        
        @Override
        public void login() throws RemoteException {
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
    
    @Override
    public void onStart(Intent i, int startId) {
        super.onStart(i, startId);
        Log.d(TAG, "XiVO connection service started");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * Starts a network connection with the server
     * @return connection status
     */
    private int connectToServer() {
        int port = Integer.parseInt(prefs.getString("server_port", "5003"));
        String host = prefs.getString("server_adress", "");
        
        try {
            Log.d(TAG, "Connecting to " + host + " " + port);
            networkConnection = new Socket(host, port);
            bytesReceived = 0L;
            inputBuffer = new BufferedReader(
                    new InputStreamReader(networkConnection.getInputStream()));
            
            String firstLine;
            while ((firstLine = getLine()) != null) {
                if (firstLine.contains("XiVO CTI Server")) {
                    return Constants.CONNECTION_OK;
                }
            }
            return Constants.NOT_CTI_SERVER;
        } catch (UnknownHostException e) {
            Log.d(TAG, "Unknown host " + host);
            return Constants.BAD_HOST;
        } catch (IOException e) {
            return Constants.BAD_HOST;
        }
    }
    
    /**
     * Reads the next incoming line and increment the byte counter and returns the line
     * @throws IOException 
     */
    private String getLine() throws IOException {
        if (inputBuffer == null)
            return null;
        
        String line = inputBuffer.readLine();
        if (line != null) {
            bytesReceived += line.getBytes().length;
        }
        return line;
    }
}
