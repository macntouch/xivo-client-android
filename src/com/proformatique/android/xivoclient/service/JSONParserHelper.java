package com.proformatique.android.xivoclient.service;

import android.content.Context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.SettingsActivity;

public class JSONParserHelper {
	
	/**
	 * Searches a phone update message and returns the comm that contains the user's mobile number
	 * as calleridnum
	 * @param context
	 * @param line
	 * @return comm
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject getMyComm(Context context, JSONObject line) {
		JSONObject comm = null;
		String myNumber = SettingsActivity.getMobileNumber(context);
		String key = null;
		try {
			JSONObject comms = line.getJSONObject("status").getJSONObject("comms");
			for (Iterator<String> iter = comms.keys(); iter.hasNext(); ) {
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
     * Returns a list of comms containing the user's mobile number as calleridnum in a of
     * List<JSONObject> returns an empty List when no comms match the number
     * @param context
     * @param line -- JSON line
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
                if (jComms.getJSONObject(key).getString("calledidnum").equals(number)) {
                    commList.add(jComms.getJSONObject(key));
                }
            }
            return commList;
        } catch (JSONException e) {
            return new ArrayList<JSONObject>();
        }
    }
    
	/**
	 * Parses a phone update and retuns the calleridnum
	 * @param line
	 * @return calleridnum or ""
	 */
	@SuppressWarnings("unchecked")
	public static String getCalledIdNum(JSONObject line) {
		try {
			JSONObject comms = line.getJSONObject("status").getJSONObject("comms");
			String key = null;
			for (Iterator<String> iter = comms.keys(); iter.hasNext(); )
				key = iter.next();
			return comms.getJSONObject(key).getString("calleridnum");
		} catch (JSONException e) {
			return "";
		}
	}
}
