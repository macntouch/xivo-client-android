package org.xivo.cti;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xivo.cti.message.CallHistoryReply;
import org.xivo.cti.message.CtiMessage;
import org.xivo.cti.message.LoginAck;
import org.xivo.cti.message.LoginCapasAck;
import org.xivo.cti.message.LoginPassAck;
import org.xivo.cti.message.PhoneConfigUpdate;
import org.xivo.cti.message.UserConfigUpdate;
import org.xivo.cti.message.UserIdsList;
import org.xivo.cti.message.UserStatusUpdate;
import org.xivo.cti.message.request.PhoneStatusUpdate;
import org.xivo.cti.model.Action;
import org.xivo.cti.model.CallType;
import org.xivo.cti.model.Capacities;
import org.xivo.cti.model.ObjectType;
import org.xivo.cti.model.PhoneStatus;
import org.xivo.cti.model.Service;
import org.xivo.cti.model.UserStatus;
import org.xivo.cti.model.XiVOCall;
import org.xivo.cti.model.XiVOPreference;
import org.xivo.cti.model.Xlet;
import org.xivo.cti.parser.BooleanParser;

public class MessageParser {

    private static final String FUNCT_LISTID = "listid";
    private static final String FUNCT_UPDATESTATUS = "updatestatus";
    private static final String FUNCT_UPDATECONFIG = "updateconfig";
    private static final String LOGIN_CAPAS = "login_capas";
    private static final String LOGIN_PASS = "login_pass";
    private static final String GETLIST = "getlist";
    private static final String LOGINID = "login_id";
    private static final String HISTORY = "history";

    public CtiMessage parse(JSONObject jsonObject) throws JSONException {
        if (jsonObject == null)
            throw (new IllegalArgumentException("unable to parse null message"));
        String messageClass = jsonObject.getString("class");
        if (messageClass.equals(LOGINID))
            return parseLoginAck(jsonObject);
        else if (messageClass.equals(LOGIN_PASS))
            return parseLoginPassAck(jsonObject);
        else if (messageClass.equals(LOGIN_CAPAS))
            return parseLoginCapasAck(jsonObject);
        else if (messageClass.equals(GETLIST))
            return parseGetList(jsonObject);
        else if (messageClass.equals(HISTORY))
            return parseHistoryReply(jsonObject);
        throw (new IllegalArgumentException("unknown message class"));
    }

    private CtiMessage parseHistoryReply(JSONObject jsonCallHistoryReply) throws JSONException {
        CallHistoryReply callHistoryReply = new CallHistoryReply();
        JSONArray jsonCallHistory = jsonCallHistoryReply.getJSONArray("history");

        for (int i = 0; i < jsonCallHistory.length(); i++) {
            JSONObject jsonCall = jsonCallHistory.getJSONObject(i);
            String callDate = jsonCall.getString("calldate").replace("T", " ");
            callDate = callDate.substring(0, callDate.indexOf('.'));
            long callDuration = Math.round(jsonCall.getDouble("duration"));
            XiVOCall xiVOCall;
            if (jsonCallHistoryReply.getString("mode").equals("0"))
                xiVOCall = new XiVOCall(callDate, callDuration, jsonCall.getString("fullname"), CallType.OUTBOUND);
            else if (jsonCallHistoryReply.getString("mode").equals("1"))
                xiVOCall = new XiVOCall(callDate, callDuration, jsonCall.getString("fullname"), CallType.INBOUND);
            else
                xiVOCall = new XiVOCall(callDate, callDuration, jsonCall.getString("fullname"), CallType.MISSED);
            callHistoryReply.addCall(xiVOCall);
        }

        return callHistoryReply;
    }

    private CtiMessage parseGetList(JSONObject getListJson) throws NumberFormatException, JSONException {
        String function = getListJson.getString("function");
        if (function.equals(FUNCT_UPDATECONFIG))
            return parseConfigUpdate(getListJson);
        if (function.equals(FUNCT_UPDATESTATUS))
            return parseUpdateStatus(getListJson);
        if (function.equals(FUNCT_LISTID))
            return parseUsersIdsList(getListJson);
        throw (new IllegalArgumentException("unknown message class"));
    }

    private CtiMessage parseConfigUpdate(JSONObject jsonConfigUpdate) throws JSONException {
        String listName = jsonConfigUpdate.getString("listname");
        ObjectType objectType = ObjectType.valueOf(listName.toUpperCase());
        switch(objectType) {
            case USERS:
                return parserUserConfigUpdate(jsonConfigUpdate);
            case PHONES:
                return parsePhoneConfigUpdate(jsonConfigUpdate);
            default:
                break;
        }
        return null;
    }

    private CtiMessage parsePhoneConfigUpdate(JSONObject jsonPhoneConfigUpdate) throws JSONException {
        PhoneConfigUpdate phoneConfigUpdate = new PhoneConfigUpdate();
        phoneConfigUpdate.setId(jsonPhoneConfigUpdate.getInt("tid"));
        JSONObject phoneConfigJson = jsonPhoneConfigUpdate.getJSONObject("config");

        if (phoneConfigJson.has("iduserfeatures")) {
            phoneConfigUpdate.setUserId(Integer.valueOf(phoneConfigJson.getInt("iduserfeatures")));
        }
        if (phoneConfigJson.has("number")) {
            phoneConfigUpdate.setNumber(phoneConfigJson.getString("number"));
        }

        return phoneConfigUpdate;
    }
    private CtiMessage parseUsersIdsList(JSONObject userIdsList) throws JSONException {
        UserIdsList usersIdsList = new UserIdsList();
        JSONArray jsonUserIds = userIdsList.getJSONArray("list");
        for (int i = 0; i < jsonUserIds.length();i++) {
            usersIdsList.add(Integer.valueOf((String) jsonUserIds.get(i)));
        }
        return usersIdsList;
    }

    private CtiMessage parseUpdateStatus(JSONObject jsonStatusUpdate) throws JSONException {
        String listName = jsonStatusUpdate.getString("listname");
        ObjectType objectType = ObjectType.valueOf(listName.toUpperCase());
        switch(objectType) {
            case USERS:
                return parseUserUpdateStatus(jsonStatusUpdate);
            case PHONES:
                return parsePhoneStatusUpdate(jsonStatusUpdate);
            default:
                break;
        }
        return null;
    }

    private CtiMessage parsePhoneStatusUpdate(JSONObject jsonPhoneStatusUpdate) throws JSONException {
        PhoneStatusUpdate phoneStatusUpdate =  new PhoneStatusUpdate();
        phoneStatusUpdate.setLineId(Integer.valueOf(jsonPhoneStatusUpdate.getInt("tid")));
        JSONObject jsonStatus = jsonPhoneStatusUpdate.getJSONObject("status");
        phoneStatusUpdate.setHintStatus(jsonStatus.getString("hintstatus"));

        return phoneStatusUpdate;
    }
    private CtiMessage parseUserUpdateStatus(JSONObject userStatusUpdateJson) throws NumberFormatException,
            JSONException {
        UserStatusUpdate userStatusUpdate = new UserStatusUpdate();
        userStatusUpdate.setUserId(Integer.valueOf(userStatusUpdateJson.getString("tid")));
        JSONObject statusJson = userStatusUpdateJson.getJSONObject("status");
        userStatusUpdate.setStatus(statusJson.getString("availstate"));
        return userStatusUpdate;
    }

    private CtiMessage parserUserConfigUpdate(JSONObject userConfigUpdateJson) throws NumberFormatException,
            JSONException {
        UserConfigUpdate userConfigUpdate = new UserConfigUpdate();
        userConfigUpdate.setUserId(Integer.valueOf(userConfigUpdateJson.getString("tid")));

        JSONObject userConfigJson = userConfigUpdateJson.getJSONObject("config");
        if (userConfigJson.has("enablednd")) {
            userConfigUpdate.setDndEnabled(BooleanParser.parse(userConfigJson.getString("enablednd")));
        }
        if (userConfigJson.has("enablerna")) {
            userConfigUpdate.setRnaEnabled(BooleanParser.parse(userConfigJson.getString("enablerna")));
        }
        if (userConfigJson.has("destrna")) {
            userConfigUpdate.setRnaDestination(userConfigJson.getString("destrna"));
        }
        if (userConfigJson.has("enableunc")) {
            userConfigUpdate.setUncEnabled(BooleanParser.parse(userConfigJson.getString("enableunc")));
        }
        if (userConfigJson.has("destunc")) {
            userConfigUpdate.setUncDestination(userConfigJson.getString("destunc"));
        }
        if (userConfigJson.has("enablebusy")) {
            userConfigUpdate.setBusyEnabled(BooleanParser.parse(userConfigJson.getString("enablebusy")));
        }
        if (userConfigJson.has("destbusy")) {
            userConfigUpdate.setBusyDestination(userConfigJson.getString("destbusy"));
        }
        if (userConfigJson.has("firstname")) {
            userConfigUpdate.setFirstName(userConfigJson.getString("firstname"));
        }
        if (userConfigJson.has("lastname")) {
            userConfigUpdate.setLastName(userConfigJson.getString("lastname"));
        }
        if (userConfigJson.has("fullname")) {
            userConfigUpdate.setFullName(userConfigJson.getString("fullname"));
        }
        if (userConfigJson.has("mobilephonenumber")) {
            userConfigUpdate.setMobileNumber(userConfigJson.getString("mobilephonenumber"));
        }
        if (userConfigJson.has("linelist")) {
            JSONArray jsonLines = userConfigJson.getJSONArray("linelist");
            for(int i = 0; i < jsonLines.length();i++) {
                userConfigUpdate.addLineId(Integer.valueOf((String) jsonLines.get(i)));
            }
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
        try {
            capacities.setPreferences(parsePreferences(capacitiesJson.getJSONObject("preferences")));
        } catch (JSONException e) {
        }
        capacities.setUsersStatuses(parseUserStatuses(capacitiesJson.getJSONObject("userstatus")));
        capacities.setServices(parseServices(capacitiesJson.getJSONArray("services")));
        capacities.setPhoneStatuses(parsePhoneStatuses(capacitiesJson.getJSONObject("phonestatus")));
        return capacities;

    }

    protected List<XiVOPreference> parsePreferences(JSONObject jsonPreferences) throws JSONException {
        List<XiVOPreference> xiVOPreferences = new ArrayList<XiVOPreference>();
        @SuppressWarnings("unchecked")
        Iterator<String> keys = jsonPreferences.keys();
        while (keys.hasNext()) {
            String parameter = keys.next();
            XiVOPreference xiVOPreference = new XiVOPreference(parameter, jsonPreferences.getString(parameter));
            xiVOPreferences.add(xiVOPreference);
        }
        return xiVOPreferences;
    }

    protected List<PhoneStatus> parsePhoneStatuses(JSONObject phoneStatusesJson) throws JSONException {
        List<PhoneStatus> phoneStatuses = new ArrayList<PhoneStatus>();
        @SuppressWarnings("unchecked")
        Iterator<String> keys = phoneStatusesJson.keys();
        while (keys.hasNext()) {
            String statusId = keys.next();
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
            String key = keys.next();
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
            String name = actionNames.next();
            Action action = new Action(name, actionsJson.getString(name));
            status.addAction(action);
        }
        return status;
    }

}
