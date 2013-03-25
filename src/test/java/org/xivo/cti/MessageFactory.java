package org.xivo.cti;

import org.json.JSONException;
import org.json.JSONObject;
import org.xivo.cti.message.LoginId;
import org.xivo.cti.message.LoginPass;


public class MessageFactory {
	
    public JSONObject createLoginId(String username){
    	LoginId loginId = new LoginId(username);
    	JSONObject jsonLoginId = new JSONObject(loginId);
    	try {
			jsonLoginId.accumulate("class",  jsonLoginId.get("claz"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	jsonLoginId.remove("claz");
		return jsonLoginId;
    }

	public JSONObject createLoginPass(String password, String sessionId) {
		LoginPass loginPass = new LoginPass(password, sessionId);
		
		return null;
	}

}
