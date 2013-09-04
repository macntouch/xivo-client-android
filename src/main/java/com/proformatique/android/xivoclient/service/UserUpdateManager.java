package com.proformatique.android.xivoclient.service;

import org.xivo.cti.model.Capacities;
import org.xivo.cti.model.PhoneStatus;
import org.xivo.cti.message.PhoneConfigUpdate;
import org.xivo.cti.message.UserConfigUpdate;
import org.xivo.cti.message.UserStatusUpdate;
import org.xivo.cti.listener.UserUpdateListener;
import org.xivo.cti.message.PhoneStatusUpdate;
import org.xivo.cti.network.XiVOLink;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.dao.CapapresenceProvider;
import com.proformatique.android.xivoclient.dao.UserProvider;
import com.proformatique.android.xivoclient.tools.Constants;

public class UserUpdateManager implements UserUpdateListener {

    private static final String TAG = "XiVO user update";

    private final Service service;
    private long stateId = 0L;
    private XiVOLink xivoLink;
    private Integer userId;
    private Capacities capacities;
    private PhoneStatus userPhoneStatus;

    public UserUpdateManager(Service service) {
        this.service = service;
        this.userPhoneStatus = new PhoneStatus("-1", Constants.DEFAULT_HINT_COLOR, service.getResources()
                .getString(R.string.default_hint_longname));
    }

    public long getStateId() {
        return stateId;
    }

    public String getPhoneStatusColor() {
        return userPhoneStatus.getColor();
    }

    public String getPhoneStatusLongName() {
        return userPhoneStatus.getLongName();
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
        if (userConfigUpdate.getUserId() == this.userId) {
            Log.d(TAG, "user config update : updating full name :" + userConfigUpdate.getFullName());
            Intent iUpdateIdentity = new Intent();
            iUpdateIdentity.setAction(Constants.ACTION_UPDATE_IDENTITY);
            iUpdateIdentity.putExtra("fullname", userConfigUpdate.getFullName());
            Context context = service.getApplicationContext();
            context.sendBroadcast(iUpdateIdentity);
        }
        if (userConfigUpdate.getLineIds().size() > 0) {
            sendGetPhoneConfig(userConfigUpdate.getLineIds().get(0));
            xivoLink.sendGetPhoneStatus(userConfigUpdate.getLineIds().get(0));
        }
        xivoLink.sendGetUserStatus(userConfigUpdate.getUserId());
    }

    @Override
    public void onUserStatusUpdate(UserStatusUpdate userStatusUpdate) {
        long currentStateId;
        Context context = service.getApplicationContext();
        Log.d(TAG, "user status updated " + userStatusUpdate.getUserId() + " satus [" + userStatusUpdate.getStatus()
                + "]");
        String statusName = userStatusUpdate.getStatus();
        Cursor presence = context.getContentResolver().query(CapapresenceProvider.CONTENT_URI,
                new String[] { CapapresenceProvider._ID, CapapresenceProvider.NAME },
                CapapresenceProvider.NAME + " = '" + statusName + "'", null, null);
        presence.moveToFirst();
        currentStateId = presence.getLong(presence.getColumnIndex(CapapresenceProvider._ID));
        presence.close();
        if (userStatusUpdate.getUserId() == this.userId) {
            Log.d(TAG, "My status changed new satus [" + userStatusUpdate.getStatus() + "]");
            stateId = currentStateId;
            Intent iUpdate = new Intent();
            iUpdate.setAction(Constants.ACTION_MY_STATUS_CHANGE);
            iUpdate.putExtra("id", stateId);
            context.sendBroadcast(iUpdate);
        } else {
            String userId = String.valueOf(userStatusUpdate.getUserId());
            long id = UserProvider.getDbUserId(context, userId);
            UserProvider.updatePresence(context, id, userStatusUpdate.getStatus());
            Intent iLoadPresence = new Intent();
            iLoadPresence.setAction(Constants.ACTION_LOAD_USER_LIST);
            context.sendBroadcast(iLoadPresence);

        }

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
        String updatedUserId = phoneConfigUpdate.getUserId().toString();
        long id = UserProvider.getDbUserId(context, updatedUserId);

        Log.d(TAG,
                "Phone config update " + phoneConfigUpdate.getNumber() + " for user : " + phoneConfigUpdate.getUserId()
                        + " internal id " + id);

        ContentValues values = new ContentValues();
        values.put(UserProvider.PHONENUM, phoneConfigUpdate.getNumber());
        values.put(UserProvider.LINEID, phoneConfigUpdate.getId().toString());
        context.getContentResolver().update(Uri.parse(UserProvider.CONTENT_URI + "/" + id), values, null, null);
        Intent iUpdateIntent = new Intent();
        iUpdateIntent.setAction(Constants.ACTION_LOAD_USER_LIST);
        iUpdateIntent.putExtra("id", id);
        context.sendBroadcast(iUpdateIntent);
    }

    @Override
    public void onPhoneStatusUpdate(PhoneStatusUpdate phoneStatusUpdate) {
        Context context = service.getApplicationContext();
        long updatedUserId = UserProvider.getDbUserIdFromLineId(context, phoneStatusUpdate.getLineId().toString());
        long updatedXiVOUserId = UserProvider
                .getXiVOUserIdFromLineId(context, phoneStatusUpdate.getLineId().toString());
        Log.d(TAG,
                "User : [" + updatedUserId + "] XiVO(" + updatedXiVOUserId + ") Phone " + phoneStatusUpdate.getLineId()
                        + " status updated [" + phoneStatusUpdate.getHintStatus() + "]");
        ContentValues values = new ContentValues();
        for (PhoneStatus phoneStatus : capacities.getPhoneStatuses()) {
            if (phoneStatus.getId().equals(phoneStatusUpdate.getHintStatus())) {
                values.put(UserProvider.HINTSTATUS_CODE, phoneStatus.getId());
                values.put(UserProvider.HINTSTATUS_COLOR, phoneStatus.getColor());
                values.put(UserProvider.HINTSTATUS_LONGNAME, phoneStatus.getLongName());
                Log.d(TAG, "    Phone Status [" + phoneStatus.getId() + "] " + phoneStatus.getColor() + " "
                        + phoneStatus.getLongName());
                context.getContentResolver().update(Uri.parse(UserProvider.CONTENT_URI + "/" + updatedUserId), values,
                        null, null);
                if (updatedXiVOUserId == this.userId) {
                    userPhoneStatus = new PhoneStatus(phoneStatus.getId(), phoneStatus.getColor(),
                            phoneStatus.getLongName());
                    Intent i = new Intent();
                    i.setAction(Constants.ACTION_MY_PHONE_CHANGE);
                    i.putExtra("color", phoneStatus.getColor());
                    i.putExtra("longname", phoneStatus.getLongName());
                    context.sendBroadcast(i);
                }
            }
        }
        Intent iUpdateIntent = new Intent();
        iUpdateIntent.setAction(Constants.ACTION_LOAD_USER_LIST);
        iUpdateIntent.putExtra("id", updatedUserId);
        context.sendBroadcast(iUpdateIntent);
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setCapacities(Capacities capacities) {
        this.capacities = capacities;
    }

}
