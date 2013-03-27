package org.xivo.cti;

import static org.junit.Assert.*;

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
		JSONObject jsonObject = messageFactory.createLoginId("username");
		assertTrue("class not initialized",jsonObject.has("class"));
		assertFalse("older claz key not removed",jsonObject.has("claz"));
		assertTrue("no command id",jsonObject.has("commandid"));
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
