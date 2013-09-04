package com.proformatique.android.xivoclient.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.proformatique.android.xivoclient.SettingsActivity;

public class JsonParserHelper {

    /**
     * Searches a phone update message and returns the comm that contains the
     * user's mobile number as calleridnum
     * 
     * @param context
     * @param line
     * @return comm
     */
    public static JSONObject getMyComm(Context context, JSONObject line) {
        JSONObject comm = null;
        String myNumber = SettingsActivity.getMobileNumber(context);
        String key = null;
        try {
            JSONObject comms = line.getJSONObject("status").getJSONObject("comms");
            for (@SuppressWarnings("unchecked")
            Iterator<String> iter = comms.keys(); iter.hasNext();) {
                key = iter.next();
                if (comms.getJSONObject(key).getString("calleridnum").equals(myNumber)) {
                    return comms.getJSONObject(key);
                }
            }
        } catch (JSONException e) {
            return null;
        }
        return comm;
    }

    /**
     * Returns a list of comms containing the user's mobile number as
     * calleridnum in a of List<JSONObject> returns an empty List when no comms
     * match the number
     * 
     * @param context
     * @param line
     *            -- JSON line
     * @return List
     */
    public static List<JSONObject> getMyComms(Context context, JSONObject line) {
        try {
            JSONObject jComms = line.getJSONObject("status").getJSONObject("comms");
            List<JSONObject> commList = new ArrayList<JSONObject>(jComms.length());
            String number = SettingsActivity.getMobileNumber(context);
            String key = null;
            @SuppressWarnings("unchecked")
            Iterator<String> iter = jComms.keys();
            while (iter.hasNext()) {
                key = iter.next();
                if (jComms.getJSONObject(key).getString("calleridnum").equals(number)) {
                    commList.add(jComms.getJSONObject(key));
                }
            }
            return commList;
        } catch (JSONException e) {
            return new ArrayList<JSONObject>();
        }
    }

    /**
     * Returns the status of a comm
     * 
     * @param comm
     * @return status
     */
    public static String getChannelStatus(JSONObject comm) {
        try {
            return comm.getString("status");
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * Checks if the channels in this comm matches the supplied thisChannel and
     * peerChannel
     * 
     * @param comm
     * @param thisChannel
     * @param peerChannel
     * @return true if they match
     */
    public static boolean channelsMatch(JSONObject comm, String thisChannel, String peerChannel) {
        try {
            return comm.getString("thischannel").equals(thisChannel)
                    && comm.getString("peerchannel").equals(peerChannel);
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Parses a phone update and retuns the calleridnum
     * 
     * @param line
     * @return calleridnum or ""
     * @throws JSONException
     */
    public static String getCallerIdNum(JSONObject line) throws JSONException {
        if (line.has("status") && line.getJSONObject("status").has("comms")) {
            JSONObject comms = line.getJSONObject("status").getJSONObject("comms");
            String key = null;
            for (@SuppressWarnings("unchecked")
            Iterator<String> iter = comms.keys(); iter.hasNext();) {
                key = iter.next();
                if (comms.getJSONObject(key).has("calleridnum")) {
                    return comms.getJSONObject(key).getString("calleridnum");
                }
            }
        }
        return "";
    }
}
