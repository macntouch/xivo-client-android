package com.proformatique.android.xivoclient.service;

import java.util.List;

import org.xivo.cti.listener.CallHistoryListener;
import org.xivo.cti.model.XiVOCall;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.proformatique.android.xivoclient.dao.HistoryProvider;
import com.proformatique.android.xivoclient.tools.Constants;

public class CallHistoryManager implements CallHistoryListener {

    private final Service service;

    public CallHistoryManager(Service service) {
        this.service = service;
    }

    @Override
    public void onCallHistoryUpdated(List<XiVOCall> callHistory) {
        Context context = service.getApplicationContext();
        for (XiVOCall xiVOCall : callHistory) {
            ContentValues values = new ContentValues();
            values.put(HistoryProvider.DURATION, xiVOCall.getDuration());
            values.put(HistoryProvider.TERMIN, "termin");
            values.put(HistoryProvider.DIRECTION, xiVOCall.getCallType().toString());
            values.put(HistoryProvider.FULLNAME, xiVOCall.getFullName());
            values.put(HistoryProvider.TS, xiVOCall.getCallDate());
            context.getContentResolver().insert(HistoryProvider.CONTENT_URI, values);
            values.clear();
        }
        Intent iLoadHistory = new Intent();
        iLoadHistory.setAction(Constants.ACTION_LOAD_HISTORY_LIST);
        context.sendBroadcast(iLoadHistory);        
    }

}
