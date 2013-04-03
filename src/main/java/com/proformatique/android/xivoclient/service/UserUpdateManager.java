package com.proformatique.android.xivoclient.service;

import org.xivo.cti.message.UserConfigUpdate;
import org.xivo.cti.message.UserStatusUpdate;
import org.xivo.cti.message.UserUpdateListener;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.proformatique.android.xivoclient.tools.Constants;

public class UserUpdateManager implements UserUpdateListener {

    private static final String TAG = "XiVO user update";

    private Service service;
    private long stateId = 0L;

    public UserUpdateManager(Service service) {
        this.service = service;
    }
    
    @Override
    public void onUserConfigUpdate(UserConfigUpdate userConfigUpdate) {
        Log.d(TAG,"user config update : " +userConfigUpdate.getUserId());

    }

    @Override
    public void onUserStatusUpdate(UserStatusUpdate userStatusUpdate) {
        Context context = service.getApplicationContext();
        Log.d(TAG, "user status updated " + userStatusUpdate.getUserId() + " satus [" + userStatusUpdate.getStatus()
                + "]");
        String statusName = userStatusUpdate.getStatus();
        Cursor presence = context.getContentResolver().query(CapapresenceProvider.CONTENT_URI,
                new String[] { CapapresenceProvider._ID, CapapresenceProvider.NAME },
                CapapresenceProvider.NAME + " = '" + statusName + "'", null, null);
        presence.moveToFirst();
        stateId = presence.getLong(presence.getColumnIndex(CapapresenceProvider._ID));
        presence.close();
        Intent iUpdate = new Intent();
        iUpdate.setAction(Constants.ACTION_MY_STATUS_CHANGE);
        iUpdate.putExtra("id", stateId);
        context.sendBroadcast(iUpdate);

    }

    public long getStateId() {
        return stateId;
    }

}
