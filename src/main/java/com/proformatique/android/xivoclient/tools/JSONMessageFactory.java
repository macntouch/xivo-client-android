package com.proformatique.android.xivoclient.tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class JSONMessageFactory {
    
    private final static String TAG = "JSONMessageFactory";
    
    /**
     * Cannot be instanciated
     */
    private JSONMessageFactory() {};
    
    public static JSONObject getJsonFeaturesRefresh(String astid, String userId) {
        JSONObject obj = new JSONObject();
        try {
            obj.accumulate("direction", Constants.XIVO_SERVER);
            obj.accumulate("class","featuresget");
            obj.accumulate("userid", astid + "/" + userId);
        } catch (JSONException e) {
            return null;
        }
        return obj;
    }
    
    /**
     * Parses a phone update and check if it matches a given astId and xivoId
     * @param line
     * @param astId
     * @param xivoId
     * @return
     */
    public static boolean checkIdMatch(JSONObject line, String astId, String xivoId) {
        try {
            return line.getString("astid").equals(astId)
                    && line.getJSONObject("status").getString("id").equals(xivoId);
        } catch (JSONException e) {
            return false;
        }
    }
    
    /**
     * Create a json object to change the users current state
     * @param stateId
     * @return
     */
    public static JSONObject getJsonState(String stateId) {
        JSONObject jObj = new JSONObject();
        try {
            jObj.accumulate("direction", Constants.XIVO_SERVER);
            jObj.accumulate("class", "availstate");
            jObj.accumulate("availstate", stateId);
        } catch (JSONException e) { }
        return jObj;
    }
    
    public static JSONObject getJsonHistoRefresh(String astid, String userId, String mode, String size) {
        
        SimpleDateFormat sIso = new SimpleDateFormat("yyyy-MM-dd");
        Date dDay = new Date();
        Calendar c1 = new GregorianCalendar();
        c1.setTime(dDay);
        c1.add(Calendar.DAY_OF_MONTH, -30);
        
        JSONObject jObj = new JSONObject();
        try {
            jObj.accumulate("direction", Constants.XIVO_SERVER);
            jObj.accumulate("class","history");
            jObj.accumulate("peer", astid + "/" + userId);
            jObj.accumulate("size",size);
            jObj.accumulate("mode",mode);
            jObj.accumulate("morerecentthan",sIso.format(c1.getTime()));
        } catch (JSONException e) { }
        return jObj;
    }

    public static JSONObject createJsonFeaturePut(String astid, String xivoid, String feature, String value, String phone) {
        JSONObject jObj = new JSONObject();
        try {
            jObj.accumulate("direction", Constants.XIVO_SERVER);
            jObj.accumulate("class", "featuresput");
            jObj.accumulate("userid", astid + "/" + xivoid);
            jObj.accumulate("function", feature);
            jObj.accumulate("value", value);
            if (phone != null)
                jObj.accumulate("destination", phone);
        } catch (JSONException e) { }
        return jObj;
    }
    
    /**
     * Create an hangup JSON object
     * "{
     *   "class": "ipbxcommand",
     *   "details": {
     *     "channelids": "chan:xivo/17:SIP/4002-00000755",
     *     "command": "hangup"
     *   },
     *   "direction": "xivoserver"
     * }" 
     * @return
     */
    public static JSONObject createJsonHangupObject(Context context, String source) {
        JSONObject j = new JSONObject();
        JSONObject details = new JSONObject();
        try {
            details.accumulate("command", "hangup");
            details.accumulate("channelids", source);
            j.accumulate("class", "ipbxcommand");
            j.accumulate("direction", Constants.XIVO_SERVER);
            j.accumulate("details", details);
            return j;
        } catch (JSONException e) {
            Log.e(TAG, "JSONException");
            e.printStackTrace();
            return null;
        }
    }
    
    public static JSONObject createJsonTransfer(String type, String src, String dest) {
        JSONObject jsonTransfer = new JSONObject();
        try {
            jsonTransfer.accumulate("direction", Constants.XIVO_SERVER);
            jsonTransfer.accumulate("class", type);
            jsonTransfer.accumulate("source", src);
            jsonTransfer.accumulate("destination", "ext:" + dest);
            return jsonTransfer;
        } catch (JSONException e) {
            return null;
        }
    }
}
