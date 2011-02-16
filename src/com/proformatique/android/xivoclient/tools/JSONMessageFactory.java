package com.proformatique.android.xivoclient.tools;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.SettingsActivity;

import android.content.Context;

public class JSONMessageFactory {
    /**
     * Cannot be instanciated
     */
    private JSONMessageFactory() {};
    
    public static JSONObject getJsonClassFunction(String inputClass, String function) {
        JSONObject obj = new JSONObject();
        try {
            obj.accumulate("direction", Constants.XIVO_SERVER);
            obj.accumulate("class", inputClass);
            obj.accumulate("function", function);
        } catch (JSONException e) {
            return null;
        }
        return obj;
    }
    
    public static JSONObject getJsonFeaturesRefresh(String userId) {
        JSONObject obj = new JSONObject();
        try {
            obj.accumulate("direction", Constants.XIVO_SERVER);
            obj.accumulate("class","featuresget");
            obj.accumulate("userid", userId);
        } catch (JSONException e) {
            return null;
        }
        return obj;
    }
    
    public static JSONObject getJsonLogin(Context context) {
        JSONObject obj = new JSONObject();
        try {
            obj.accumulate("class","login_id");
            obj.accumulate("company", SettingsActivity.getXivoContext(context));
            obj.accumulate("ident","android-" + android.os.Build.VERSION.RELEASE);
            obj.accumulate("userid", SettingsActivity.getLogin(context));
            obj.accumulate("version",Constants.XIVO_LOGIN_VERSION);
            obj.accumulate("xivoversion",Constants.XIVO_VERSION);
        } catch (JSONException e) {
            return null;
        }
        return obj;
    }
    
    /**
     * Prepare the Json string for calling process
     * 
     * @param inputClass
     * @param phoneNumberSrc
     * @param phoneNumberDest
     * @return
     */
    public static JSONObject getJsonCallingObject(String inputClass, String phoneNumberSrc,
            String phoneNumberDest) {
        
        JSONObject jObj = new JSONObject();
        String phoneSrc;
        
        if (phoneNumberSrc == null)
            phoneSrc = "user:special:me";
        else if (phoneNumberSrc.equals(""))
            phoneSrc = "user:special:me";
        else
            phoneSrc = "ext:"+phoneNumberSrc;
        
        try {
            jObj.accumulate("direction", Constants.XIVO_SERVER);
            jObj.accumulate("class",inputClass);
            jObj.accumulate("source", phoneSrc);
            jObj.accumulate("destination", "ext:"+phoneNumberDest);
            return jObj;
        } catch (JSONException e) {
            return null;
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
            for (Iterator<String> iter = comms.keys(); iter.hasNext(); )
                return comms.getJSONObject(iter.toString()).getString("calleridnum");
        } catch (JSONException e) {
            return "";
        }
        return "";
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
    
    public static JSONObject getKeepAlive() {
        JSONObject jObj = new JSONObject();
        try {
            jObj.accumulate("direction", Constants.XIVO_SERVER);
            jObj.accumulate("class", "keepalive");
        } catch (JSONException e) { }
        return jObj;
    }
}
