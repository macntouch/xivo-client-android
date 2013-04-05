package com.proformatique.android.xivoclient.service;

import org.xivo.cti.message.PhoneConfigUpdate;
import org.xivo.cti.message.UserConfigUpdate;
import org.xivo.cti.message.UserStatusUpdate;
import org.xivo.cti.message.UserUpdateListener;
import org.xivo.cti.message.request.PhoneStatusUpdate;
import org.xivo.cti.network.XiVOLink;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.tools.Constants;

public class UserUpdateManager implements UserUpdateListener {

    private static final String TAG = "XiVO user update";

    private final Service service;
    private long stateId = 0L;
    private XiVOLink xivoLink;
    private Integer userId;

    public UserUpdateManager(Service service) {
        this.service = service;
    }

    @Override
    public void onUserConfigUpdate(UserConfigUpdate userConfigUpdate) {
        if (userConfigUpdate.getFullName() == null) {
            Log.d(TAG, "user config update : " + userConfigUpdate.getUserId() + " null full name");
            return;
        }
        Log.d(TAG, "user config update : " + userConfigUpdate.getUserId() + " " + userConfigUpdate.getFullName());
        ContentValues user = new ContentValues();
        user.put(UserProvider.ASTID, userConfigUpdate.getUserId());
        user.put(UserProvider.XIVO_USERID, userConfigUpdate.getUserId());
        user.put(UserProvider.FULLNAME, userConfigUpdate.getFullName());
        user.put(UserProvider.PHONENUM, "....");
        user.put(UserProvider.STATEID, "disconnected");
        user.put(UserProvider.STATEID_LONGNAME, "disconnected");
        user.put(UserProvider.STATEID_COLOR, "#202020");
        user.put(UserProvider.TECHLIST, "techlist");
        user.put(UserProvider.HINTSTATUS_COLOR, Constants.DEFAULT_HINT_COLOR);
        user.put(UserProvider.HINTSTATUS_CODE, Constants.DEFAULT_HINT_CODE);
        user.put(UserProvider.HINTSTATUS_LONGNAME,
                service.getApplicationContext().getString(R.string.default_hint_longname));
        service.getApplicationContext().getContentResolver().insert(UserProvider.CONTENT_URI, user);
        user.clear();
        if (userConfigUpdate.getLineIds().size() > 0) {
            sendGetPhoneConfig(userConfigUpdate.getLineIds().get(0));
            xivoLink.sendGetPhoneStatus(userConfigUpdate.getLineIds().get(0));
        }
        xivoLink.sendGetUserStatus(userConfigUpdate.getUserId());
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
        if (userStatusUpdate.getUserId() == this.userId) {
            Intent iUpdate = new Intent();
            iUpdate.setAction(Constants.ACTION_MY_STATUS_CHANGE);
            iUpdate.putExtra("id", stateId);
            context.sendBroadcast(iUpdate);
        }
        else {
            String userId = String.valueOf(userStatusUpdate.getUserId());
            long id = UserProvider.getUserId(context,userId,userId);
            UserProvider.updatePresence(context, id, userStatusUpdate.getStatus());
            Intent iLoadPresence = new Intent();
            iLoadPresence.setAction(Constants.ACTION_LOAD_USER_LIST);
            context.sendBroadcast(iLoadPresence);

        }

    }

    public long getStateId() {
        return stateId;
    }

    private void sendGetPhoneConfig(Integer lineId) {
        xivoLink.sendGetPhoneConfig(lineId);
    }

    public void setXivoLink(XiVOLink xivoLink) {
        this.xivoLink = xivoLink;
    }

    @Override
    public void onPhoneConfigUpdate(PhoneConfigUpdate phoneConfigUpdate) {
        Context context = service.getApplicationContext();
        String userId = phoneConfigUpdate.getUserId().toString();
        long id = UserProvider.getUserId(context,userId,userId);

        Log.d(TAG,"Phone config update " + phoneConfigUpdate.getNumber()+ " for user : " + phoneConfigUpdate.getUserId() + " internal id "+id);

        ContentValues values = new ContentValues();
        values.put(UserProvider.PHONENUM, phoneConfigUpdate.getNumber());
        context.getContentResolver().update(Uri.parse(UserProvider.CONTENT_URI + "/" + id), values, null, null);
        Intent iUpdateIntent = new Intent();
        iUpdateIntent.setAction(Constants.ACTION_LOAD_USER_LIST);
        iUpdateIntent.putExtra("id", id);
        context.sendBroadcast(iUpdateIntent);
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public void onUserStatusUpdate(PhoneStatusUpdate phoneStatusUpdate) {
        Log.d(TAG,"Phone"+phoneStatusUpdate.getLineId()+ "status updated "+phoneStatusUpdate.getHintStatus());

    }

}
