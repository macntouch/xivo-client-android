package org.xivo.cti;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.xivo.cti.message.CtiMessage;
import org.xivo.cti.message.LoginAck;
import org.xivo.cti.message.LoginPassAck;


public class MessageParserTest {
	MessageParser messageParser;

	@Before public void setUp() {
		messageParser = new MessageParser();
	}

	@Test public void parseLoginAck() throws JSONException {
		JSONObject jsonobject = new JSONObject("{\"class\": \"login_id\",\"sessionid\": \"21UaGDfst7\",\"timenow\": 1361268824.64,\"xivoversion\": \"1.2\"}");
		
		LoginAck loginAck = (LoginAck) messageParser.parse(jsonobject);
		
		assertNotNull("unable to decode login ack",loginAck);
		assertEquals("Can't sessionid", "21UaGDfst7",loginAck.sesssionId);
		assertEquals("Can't timenow", 1361268824.64,loginAck.timenow,0.001);
		assertEquals("Can't xivoversion", "1.2",loginAck.xivoversion);
		
	}
	
	@Test public void parseLoginPasswordAck() throws JSONException {
		JSONObject jsonObject = new JSONObject("{\"capalist\": [2],\"class\": \"login_pass\",\"replyid\": 1646064863,\"timenow\": 1361268824.68}");
		
		LoginPassAck loginPassAck = (LoginPassAck) messageParser.parse(jsonObject);
		assertNotNull("unable to decode login ack",loginPassAck);
		assertEquals("Can't decode capalist", Integer.valueOf(2), loginPassAck.capalist.get(0));		
		assertEquals("Can't decode timenow", 1361268824.68,loginPassAck.timenow,0.001);
		assertEquals("Can't decode replyid", 1646064863, loginPassAck.replyId);
	}

	@Test(expected=IllegalArgumentException.class)
	public void parseUnknowMessage() throws JSONException {
		JSONObject jsonObject = new JSONObject("{\"capalist\": [2],\"class\": \"unexisting_message_class\",\"replyid\": 1646064863,\"timenow\": 1361268824.68}");
		
		messageParser.parse(jsonObject);

	}
}
