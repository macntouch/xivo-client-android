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
import org.xivo.cti.message.UserConfigUpdate;
import org.xivo.cti.message.UserStatusUpdate;
import org.xivo.cti.model.Action;
import org.xivo.cti.model.Capacities;
import org.xivo.cti.model.PhoneStatus;
import org.xivo.cti.model.Service;
import org.xivo.cti.model.UserStatus;
import org.xivo.cti.model.Xlet;

public class MessageParser {

    private static final String LOGIN_CAPAS = "login_capas";
    private static final String LOGIN_PASS = "login_pass";
    private static final String GETLIST = "getlist";
    private static final String LOGINID = "login_id";

    public CtiMessage parse(JSONObject jsonObject) throws JSONException {
        String messageClass = jsonObject.getString("class");
        if (messageClass.equals(LOGINID))
            return parseLoginAck(jsonObject);
        else if (messageClass.equals(LOGIN_PASS))
            return parseLoginPassAck(jsonObject);
        else if (messageClass.equals(LOGIN_CAPAS))
            return parseLoginCapasAck(jsonObject);
        else if (messageClass.equals(GETLIST))
            return parseGetList(jsonObject);
        throw (new IllegalArgumentException("unknown message class"));
    }

    private CtiMessage parseGetList(JSONObject getListJson) throws NumberFormatException, JSONException {
        String function = getListJson.getString("function");
        if (function.equals("updateconfig"))
            return parserUserConfigUpdate(getListJson);
        if (function.equals("updatestatus"))
            return paserUserUpdateStatus(getListJson);
        throw (new IllegalArgumentException("unknown message class"));
    }

    private CtiMessage paserUserUpdateStatus(JSONObject userConfigUpdateJson) throws NumberFormatException,
            JSONException {
        UserStatusUpdate userStatusUpdate = new UserStatusUpdate();
        userStatusUpdate.setUserId(Integer.valueOf(userConfigUpdateJson.getString("tid")));
        JSONObject statusJson = userConfigUpdateJson.getJSONObject("status");
        userStatusUpdate.setStatus(statusJson.getString("availstate"));
        return userStatusUpdate;
    }

    private CtiMessage parserUserConfigUpdate(JSONObject userConfigUpdateJson) throws NumberFormatException,
            JSONException {
        UserConfigUpdate userConfigUpdate = new UserConfigUpdate();
        userConfigUpdate.setUserId(Integer.valueOf(userConfigUpdateJson.getString("tid")));

        JSONObject userConfigJson = userConfigUpdateJson.getJSONObject("config");
        if (userConfigJson.has("enablednd")) {
            userConfigUpdate.setDndEnabled(userConfigJson.getBoolean("enablednd"));
        }
        return userConfigUpdate;
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
            loginPassAck.capalist.add((Integer) capas.get(i));
        }
        loginPassAck.timenow = loginPassAckJson.getDouble("timenow");
        loginPassAck.replyId = loginPassAckJson.getLong("replyid");
        return loginPassAck;
    }

    private CtiMessage parseLoginCapasAck(JSONObject loginCapasAckJson) throws JSONException {
        LoginCapasAck loginCapasAck = new LoginCapasAck();
        loginCapasAck.presence = loginCapasAckJson.getString("presence");
        loginCapasAck.userId = loginCapasAckJson.getString("userid");
        loginCapasAck.applicationName = loginCapasAckJson.getString("appliname");
        loginCapasAck.capacities = parseCapacities(loginCapasAckJson.getJSONObject("capas"));
        loginCapasAck.xlets = parseXlets(loginCapasAckJson.getJSONArray("capaxlets"));
        return loginCapasAck;
    }

    protected List<Xlet> parseXlets(JSONArray xletsJson) throws JSONException {
        List<Xlet> xlets = new ArrayList<Xlet>();

        for (int i = 0; i < xletsJson.length(); i++) {
            int order = 0;
            if (xletsJson.getJSONArray(i).length() > 2) {
                order = Integer.valueOf((xletsJson.getJSONArray(i).getString(2)));
            }
            Xlet xlet = new Xlet(xletsJson.getJSONArray(i).getString(0), xletsJson.getJSONArray(i).getString(1), order);
            xlets.add(xlet);
        }
        return xlets;
    }

    public Capacities parseCapacities(JSONObject capacitiesJson) throws JSONException {
        Capacities capacities = new Capacities();
        capacities.setPreferences(capacitiesJson.getBoolean("preferences"));
        capacities.setUsersStatuses(parseUserStatuses(capacitiesJson.getJSONObject("userstatus")));
        capacities.setServices(parseServices(capacitiesJson.getJSONArray("services")));
        capacities.setPhoneStatuses(parsePhoneStatuses(capacitiesJson.getJSONObject("phonestatus")));
        return capacities;

    }

    protected List<PhoneStatus> parsePhoneStatuses(JSONObject phoneStatusesJson) throws JSONException {
        List<PhoneStatus> phoneStatuses = new ArrayList<PhoneStatus>();
        @SuppressWarnings("unchecked")
        Iterator<String> keys = phoneStatusesJson.keys();
        while (keys.hasNext()) {
            String statusId = (String) keys.next();
            JSONObject phoneStatusJson = phoneStatusesJson.getJSONObject(statusId);
            PhoneStatus phoneStatus = new PhoneStatus(statusId, phoneStatusJson.getString("color"),
                    phoneStatusJson.getString("longname"));
            phoneStatuses.add(phoneStatus);
        }
        return phoneStatuses;
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
        @SuppressWarnings("unchecked")
        Iterator<String> keys = userStatusesJson.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            userStatuses.add(parseUserStatus(key, userStatusesJson.getJSONObject(key)));
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
        @SuppressWarnings("unchecked")
        Iterator<String> actionNames = actionsJson.keys();
        while (actionNames.hasNext()) {
            String name = (String) actionNames.next();
            Action action = new Action(name, actionsJson.getString(name));
            status.addAction(action);
        }
        return status;
    }

}
