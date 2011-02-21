package com.proformatique.android.xivoclient.service;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.SettingsActivity;

import android.content.Context;

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
