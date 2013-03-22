package org.xivo.cti;

import org.json.JSONException;
import org.json.JSONObject;
import org.xivo.cti.message.LoginAck;

public class MessageParser {

	public LoginAck parse(JSONObject jsonobject) throws JSONException {
		LoginAck loginAck = new LoginAck();
		loginAck.sesssionId = jsonobject.getString("sessionid");
		loginAck.timenow = jsonobject.getDouble("timenow");
		loginAck.xivoversion = jsonobject.getString("xivoversion");
		return loginAck;
	}

}
