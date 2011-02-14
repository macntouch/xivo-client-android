package com.proformatique.android.xivoclient.tools;

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
}
