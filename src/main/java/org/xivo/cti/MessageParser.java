package org.xivo.cti;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xivo.cti.message.CtiMessage;
import org.xivo.cti.message.LoginAck;
import org.xivo.cti.message.LoginCapasAck;
import org.xivo.cti.message.LoginPassAck;
import org.xivo.cti.model.Action;
import org.xivo.cti.model.Capacities;
import org.xivo.cti.model.Service;
import org.xivo.cti.model.UserStatus;

public class MessageParser {
	
	public CtiMessage parse(JSONObject jsonObject) throws JSONException {
		String messageClass = jsonObject.getString("class");
		if (messageClass.equals("login_id"))
			return parseLoginAck(jsonObject);
		else if (messageClass.equals("login_pass")) 
			return parseLoginPassAck(jsonObject);
		else if (messageClass.equals("login_capas"))
			return parseLoginCapasAck(jsonObject);
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
	
	private CtiMessage parseLoginCapasAck(JSONObject loginCapasAckJson) throws JSONException  {
		LoginCapasAck loginCapasAck = new LoginCapasAck();
		loginCapasAck.presence = loginCapasAckJson.getString("presence");
		loginCapasAck.userId = loginCapasAckJson.getString("userid");
		loginCapasAck.applicationName = loginCapasAckJson.getString("appliname");
		loginCapasAck.capacities = parseCapacities(loginCapasAckJson.getJSONObject("capas"));
		return loginCapasAck;
	}
	
	public Capacities parseCapacities(JSONObject capacitiesJson) throws JSONException {
		Capacities capacities = new Capacities();
		
		capacities.setPreferences(capacitiesJson.getBoolean("preferences"));
		
		capacities.setUsersStatuses(parseUserStatuses(capacitiesJson.getJSONObject("userstatus")));
		
		capacities.setServices(parseServices(capacitiesJson.getJSONArray("services")));
		return capacities;
		
	}

	public List<Service> parseServices(JSONArray servicesJson) throws JSONException {
		List<Service> services = new ArrayList<Service>();
		
		for (int i = 0; i < servicesJson.length(); i++) {
			services.add(new Service(servicesJson.get(i).toString()));
		}

		
		return services;
	}

	public List<UserStatus> parseUserStatuses(JSONObject userStatusesJson) throws JSONException {
		List<UserStatus> userStatuses = new ArrayList<UserStatus>();
		Iterator<String> keys = userStatusesJson.keys();
		while(keys.hasNext()) {
			String key = (String) keys.next();
			userStatuses.add(parseUserStatus(key,userStatusesJson.getJSONObject(key)));
		}
		return userStatuses;
	}

	public UserStatus parseUserStatus(String key, JSONObject userStatusJson) throws JSONException {
		UserStatus status = new UserStatus(key);
		status.setColor(userStatusJson.getString("color"));
		status.setLongName(userStatusJson.getString("longname"));
		if (userStatusJson.has("allowed")) {
			JSONArray allowedJson = userStatusJson.getJSONArray("allowed");
			for (int i = 0; i < allowedJson.length(); i++) {
				status.allow(allowedJson.get(i).toString());
			}
		}
		JSONObject actionsJson = userStatusJson.getJSONObject("actions");
		Iterator<String> actionNames = actionsJson.keys();
		while(actionNames.hasNext()) {
			String name = (String) actionNames.next();
			Action action = new Action(name,actionsJson.getString(name));
			status.addAction(action);
		}
		return status;
	}

}
