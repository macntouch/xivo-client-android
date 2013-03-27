package org.xivo.cti;

import org.json.JSONException;
import org.json.JSONObject;
import org.xivo.cti.message.LoginCapas;
import org.xivo.cti.message.LoginId;
import org.xivo.cti.message.LoginPass;


public class MessageFactory {
	private static String KEY_COMMANDID = "commandid";
	private static String KEY_CLASS = "class";
	private static String KEY_CLAZ = "claz";
	
	private static long commandId = 0;
	
	
	private JSONObject replaceClass(JSONObject message) {
    	try {
    		message.accumulate(KEY_CLASS,  message.get("claz"));
    		message.accumulate(KEY_COMMANDID, commandId++);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	message.remove(KEY_CLAZ);
    	return message;
	}
	
    public JSONObject createLoginId(String username){
    	LoginId loginId = new LoginId(username);
    	JSONObject jsonLoginId = new JSONObject(loginId);
    	return replaceClass(jsonLoginId);
    }

	public JSONObject createLoginPass(String password, String sessionId) {
		LoginPass loginPass = new LoginPass(password, sessionId);
		
		JSONObject jsonLoginPass = new JSONObject(loginPass);
		return replaceClass(jsonLoginPass);
	}

	public JSONObject createLoginCapas(int capaId) {
		LoginCapas loginCapas = new LoginCapas(capaId);
		
		return replaceClass(new JSONObject(loginCapas));
	}

}
