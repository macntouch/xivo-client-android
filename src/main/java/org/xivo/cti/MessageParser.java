package org.xivo.cti;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xivo.cti.message.CtiMessage;
import org.xivo.cti.message.LoginAck;
import org.xivo.cti.message.LoginPassAck;

public class MessageParser {
	
	public CtiMessage parse(JSONObject jsonObject) throws JSONException {
		String messageClass = jsonObject.getString("class");
		if (messageClass.equals("login_id"))
			return parseLoginAck(jsonObject);
		else if (messageClass.equals("login_pass")) 
			return parseLoginPassAck(jsonObject);
		throw (new IllegalArgumentException("unknown message class"));
	}

	private CtiMessage parseLoginAck(JSONObject loginAckJson) throws JSONException {
		LoginAck loginAck = new LoginAck();
		loginAck.sesssionId = loginAckJson.getString("sessionid");
		loginAck.timenow = loginAckJson.getDouble("timenow");
		loginAck.xivoversion = loginAckJson.getString("xivoversion");
		return loginAck;
	}

	private CtiMessage parseLoginPassAck(JSONObject loginPassAckJson) throws JSONException {
		LoginPassAck loginPassAck = new LoginPassAck();
		JSONArray capas = loginPassAckJson.getJSONArray("capalist");
		for (int i = 0; i < capas.length(); i++) {
			loginPassAck.capalist.add( (Integer) capas.get(i));
		}
		loginPassAck.timenow = loginPassAckJson.getDouble("timenow");
		loginPassAck.replyId = loginPassAckJson.getLong("replyid");
		return loginPassAck;
	}

}
