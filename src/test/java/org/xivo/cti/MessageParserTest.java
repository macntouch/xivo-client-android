package org.xivo.cti;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.xivo.cti.message.LoginAck;


public class MessageParserTest {
	MessageParser messageParser;

	@Before public void setUp() {
		messageParser = new MessageParser();
	}

	@Test public void parseLoginAck() throws JSONException {
		JSONObject jsonobject = new JSONObject("{\"class\": \"login_id\",\"sessionid\": \"21UaGDfst7\",\"timenow\": 1361268824.64,\"xivoversion\": \"1.2\"}");
		
		LoginAck loginAck = messageParser.parse(jsonobject);
		
		assertNotNull("unable to decode login ack",loginAck);
		assertEquals("invalid sessionid", "21UaGDfst7",loginAck.sesssionId);
		assertEquals("invalid timenow", 1361268824.64,loginAck.timenow,0.001);
		assertEquals("invalid xivoversion", "1.2",loginAck.xivoversion);
		
	}
	
}
