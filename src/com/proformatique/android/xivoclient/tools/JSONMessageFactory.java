package com.proformatique.android.xivoclient.tools;

import org.json.JSONException;
import org.json.JSONObject;

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
}
