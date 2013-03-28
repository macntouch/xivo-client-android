package org.xivo.cti;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class MessageFactoryTest {
	private MessageFactory messageFactory;

	@Before
	public void setUp() throws Exception {
		messageFactory = new MessageFactory();
	}

	@Test
	public void class_setup() {
		JSONObject jsonObject = messageFactory.createLoginId("username","ident");
		assertTrue("class not initialized",jsonObject.has("class"));
		assertTrue("no command id",jsonObject.has("commandid"));
	}
	
	@Test
	public void createLoginId() throws JSONException {
	    JSONObject jsonLoginId = messageFactory.createLoginId("hoghn","ident-1234");
        assertEquals("invalid class",jsonLoginId.get("class"),"login_id");
        assertEquals("invalid ident",jsonLoginId.get("ident"),"ident-1234");
	    
	}
	
	@Test
	public void createLoginCapas() throws JSONException {
		JSONObject message = messageFactory.createLoginCapas(3);
		assertEquals("invalid class",message.get("class"),"login_capas");
		assertEquals("invalid loginkind",message.get("loginkind"),"user");
		assertEquals("invalid capaid",message.get("capaid"),3);
		assertEquals("invalid lastconnwins",message.get("lastconnwins"),false);
		assertEquals("invalid state",message.get("state"),"available");
	}
	
}
