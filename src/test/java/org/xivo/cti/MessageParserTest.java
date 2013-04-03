package org.xivo.cti;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.xivo.cti.message.CallHistoryReply;
import org.xivo.cti.message.LoginAck;
import org.xivo.cti.message.LoginCapasAck;
import org.xivo.cti.message.LoginPassAck;
import org.xivo.cti.message.UserConfigUpdate;
import org.xivo.cti.message.UserIdsList;
import org.xivo.cti.message.UserStatusUpdate;
import org.xivo.cti.model.CallType;
import org.xivo.cti.model.Capacities;
import org.xivo.cti.model.PhoneStatus;
import org.xivo.cti.model.Service;
import org.xivo.cti.model.UserStatus;
import org.xivo.cti.model.XiVOCall;
import org.xivo.cti.model.XiVOPreference;
import org.xivo.cti.model.Xlet;

public class MessageParserTest {
    MessageParser messageParser;

    @Before
    public void setUp() {
        messageParser = new MessageParser();
    }

    @Test
    public void getUsersList() throws JSONException {
        JSONObject jsonUsersList = new JSONObject("{\"function\": \"listid\", " +
                                                    "\"listname\": \"users\", " +
                                                    "\"class\": \"getlist\"," +
                                                    "\"tipbxid\": \"xivo\", " +
                                                    "\"list\": [\"11\", \"25\", \"12\", \"14\"], " +
                                                    "\"timenow\": 1364919114.1 " +
                                                    "}");
        UserIdsList usersList = (UserIdsList) messageParser.parse(jsonUsersList);
        assertNotNull("unable to decode users list",usersList);
        assertThat("list not decoded",usersList.getUserIds(),hasItem(new Integer(25)));

    }
    /*
    {"function": "updateconfig", 
    "listname": "users", 
    "tipbxid": "xivo", 
    "timenow": 1364975899.38, "tid": "9", 
    "config": {"enablednd": 0, "destrna": "", "enablerna": 0, "firstname": "Alice", 
    "profileclient": null, "lastname": "Johnson", 
    "enableunc": 0, "destbusy": "", "enablebusy": 0, 
    "voicemailid": null, "incallfilter": 0, "destunc": "", 
    "enablevoicemail": 0, "enablexfer": 1, 
    "fullname": "Alice Johnson", "agentid": null, "enableclient": 0, "linelist": ["5"], "mobilephonenumber": ""}, "class": "getlist"}
    */
    @Test
    public void parseUserConfigUpdate() throws JSONException {
        JSONObject userConfigUpdateJson = new JSONObject("{" + "\"class\": \"getlist\","
                + "\"function\": \"updateconfig\", " + "\"listname\": \"users\", " + "\"tipbxid\": \"xivo\","
                + "\"timenow\": 1361440830.99," + "\"tid\": \"3\"," + 
                "\"config\": {" +
                    "\"enablednd\": true, \"destrna\": \"4561\",\"enablerna\": 1, " +
                    "\"firstname\": \"Alice\", \"lastname\": \"Johnson\", \"fullname\": \"Alice Johnson\"}" + "}");

        UserConfigUpdate userConfigUpdate = (UserConfigUpdate) messageParser.parse(userConfigUpdateJson);
        assertNotNull("unable de decode user configuration update", userConfigUpdate);
        assertEquals("unable to decode user id ", 3, userConfigUpdate.getUserId());
        assertTrue("unable to decode dnd enabled", userConfigUpdate.isDndEnabled());
        assertTrue("unable to decode rna enabled", userConfigUpdate.isRnaEnabled());
        assertEquals("unable to decode rna destination","4561",userConfigUpdate.getRnaDestination());
        assertEquals("unable to decode first name","Alice",userConfigUpdate.getFirstName());
        assertEquals("unable to decode last name","Johnson",userConfigUpdate.getLastName());
        assertEquals("unable to decode full name","Alice Johnson",userConfigUpdate.getFullName());
    }

    @Test
    public void parseUserUpdateEmptyConfig() throws JSONException {
        JSONObject userConfigUpdateJson = new JSONObject("{" + "\"class\": \"getlist\","
                + "\"function\": \"updateconfig\", " + "\"listname\": \"users\", " + "\"tipbxid\": \"xivo\","
                + "\"timenow\": 1361440830.99," + "\"tid\": \"3\"," + "\"config\": {}" + "}");
        UserConfigUpdate userConfigUpdate = (UserConfigUpdate) messageParser.parse(userConfigUpdateJson);
        assertNotNull("unable de decode user configuration update", userConfigUpdate);
        assertEquals("unable to decode user id ", 3, userConfigUpdate.getUserId());
        assertFalse("unable to decode dnd enabled", userConfigUpdate.isDndEnabled());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseUnknowGetListMessage() throws JSONException {
        JSONObject jsonObject = new JSONObject(
                "{\"capalist\": [2],\"class\": \"getlist\",\"replyid\": 1646064863,\"timenow\": 1361268824.68, \"function\": \"unkonwn\"}");
        messageParser.parse(jsonObject);
    }

    @Test
    public void parseCallHistoryOutbound() throws JSONException {
        JSONObject callHistoryJson = new JSONObject("{\"class\": \"history\"," + "\"history\": ["
                + "{\"calldate\": \"2013-03-29T08:44:35.273998\", \"duration\": 0.148765, \"fullname\": \"*844201\"},"
                + "{\"calldate\": \"2013-03-28T16:56:48.071213\", \"duration\": 58.834744, \"fullname\": \"41400\"}],"
                + "\"mode\": 0, " + "\"replyid\": 529422441, " + "\"timenow\": 1364571477.33}");
        XiVOCall xivoCall = new XiVOCall("2013-03-28 16:56:48", 59, "41400", CallType.OUTBOUND);
        CallHistoryReply callHistoryReply = (CallHistoryReply) messageParser.parse(callHistoryJson);

        assertNotNull("cannot parse history", callHistoryReply);

        assertThat("cannot parse call history", callHistoryReply.getCallHistory(), hasItem(xivoCall));
    }

    @Test
    public void parseCallHistoryInbound() throws JSONException {
        JSONObject callHistoryJson = new JSONObject("{\"class\": \"history\"," + "\"history\": ["
                + "{\"calldate\": \"2013-03-29T08:44:35.273998\", \"duration\": 0.148765, \"fullname\": \"*844201\"},"
                + "{\"calldate\": \"2013-03-28T16:56:48.071213\", \"duration\": 58.834744, \"fullname\": \"41400\"}],"
                + "\"mode\": 1, " + "\"replyid\": 529422441, " + "\"timenow\": 1364571477.33}");
        XiVOCall xivoCall = new XiVOCall("2013-03-28 16:56:48", 59, "41400", CallType.INBOUND);
        CallHistoryReply callHistoryReply = (CallHistoryReply) messageParser.parse(callHistoryJson);

        assertNotNull("cannot parse history", callHistoryReply);

        assertThat("cannot parse call history", callHistoryReply.getCallHistory(), hasItem(xivoCall));
    }

    @Test
    public void parseCallHistoryMissed() throws JSONException {
        JSONObject callHistoryJson = new JSONObject("{\"class\": \"history\"," + "\"history\": ["
                + "{\"calldate\": \"2013-03-29T08:44:35.273998\", \"duration\": 0.148765, \"fullname\": \"*844201\"},"
                + "{\"calldate\": \"2013-03-28T16:56:48.071213\", \"duration\": 58.834744, \"fullname\": \"41400\"}],"
                + "\"mode\": 2, " + "\"replyid\": 529422441, " + "\"timenow\": 1364571477.33}");
        XiVOCall xivoCall = new XiVOCall("2013-03-28 16:56:48", 59, "41400", CallType.MISSED);
        CallHistoryReply callHistoryReply = (CallHistoryReply) messageParser.parse(callHistoryJson);

        assertNotNull("cannot parse history", callHistoryReply);

        assertThat("cannot parse call history", callHistoryReply.getCallHistory(), hasItem(xivoCall));
    }

    @Test
    public void paserUserSatusUpdate() throws JSONException {
        JSONObject userStatusUpdateJson = new JSONObject("{" + "\"class\": \"getlist\","
                + "\"function\": \"updatestatus\", " + "\"listname\": \"users\", " + "\"tipbxid\": \"xivo\","
                + "\"timenow\": 1361440830.99," + "\"tid\": \"7\"," + "\"status\": {\"availstate\": available}" + "}");

        UserStatusUpdate userStatusUpdate = (UserStatusUpdate) messageParser.parse(userStatusUpdateJson);

        assertNotNull("unable to decode user status update", userStatusUpdate);
        assertEquals("unable to decode user id ", 7, userStatusUpdate.getUserId());
        assertEquals("unable to decode user status", "available", userStatusUpdate.getStatus());
    }

    @Test
    public void parseLoginAck() throws JSONException {
        JSONObject jsonobject = new JSONObject("{\"class\": \"login_id\",\"sessionid\": \"21UaGDfst7\","
                + "\"timenow\": 1361268824.64,\"xivoversion\": \"1.2\"}");

        LoginAck loginAck = (LoginAck) messageParser.parse(jsonobject);

        assertNotNull("unable to decode login ack", loginAck);
        assertEquals("Can't sessionid", "21UaGDfst7", loginAck.sesssionId);
        assertEquals("Can't timenow", 1361268824.64, loginAck.timenow, 0.001);
        assertEquals("Can't xivoversion", "1.2", loginAck.xivoversion);

    }

    @Test
    public void parseLoginPasswordAck() throws JSONException {
        JSONObject jsonObject = new JSONObject("{\"capalist\": [2],\"class\": \"login_pass\","
                + "\"replyid\": 1646064863,\"timenow\": 1361268824.68}");

        LoginPassAck loginPassAck = (LoginPassAck) messageParser.parse(jsonObject);
        assertNotNull("unable to decode login ack", loginPassAck);
        assertEquals("Can't decode capalist", Integer.valueOf(2), loginPassAck.capalist.get(0));
        assertEquals("Can't decode timenow", 1361268824.68, loginPassAck.timenow, 0.001);
        assertEquals("Can't decode replyid", 1646064863, loginPassAck.replyId);
    }

    @Test
    public void parseLoginCapas() throws JSONException {
        JSONObject jsonObject = new JSONObject(
                "{\"class\": \"login_capas\",\"presence\": \"available\", "
                        + "\"userid\": \"3\", \"ipbxid\": \"xivo\",\"appliname\": \"Client\","
                        + " \"timenow\": 1364373405.73,"
                        + "\"replyid\": 2, "
                        + " \"capas\": {"
                        + "\"regcommands\": {},"
                        + "\"ipbxcommands\": {},"
                        + "\"preferences\":  {\"xlet.identity.logagent\": \"1\", \"xlet.identity.pauseagent\": \"1\"},"
                        + " \"userstatus\": {"
                        + "\"available\": {\"color\": \"#08FD20\","
                        + " \"allowed\": [\"available\", \"away\",\"outtolunch\", \"donotdisturb\", \"berightback\"],"
                        + " \"actions\": {\"enablednd\": \"false\"}, \"longname\": \"Disponible\"},"
                        + "\"disconnected\": {\"color\": \"#202020\", \"actions\": {\"agentlogoff\": \"\"}, \"longname\": \"D\u00e9connect\u00e9\"}, "
                        + "\"outtolunch\": {\"color\": \"#001AFF\", "
                        + "\"allowed\": [\"available\", \"away\", \"outtolunch\", \"donotdisturb\", \"berightback\"],"
                        + " \"actions\": {\"enablednd\": \"false\"}, \"longname\": \"Parti Manger\"}"
                        + "},"
                        + "\"services\": [\"enablednd\", \"fwdrna\"], "
                        + "\"phonestatus\": {  "
                        + "\"16\": {\"color\": \"#F7FF05\", \"longname\": \"En Attente\"}, "
                        + "\"1\": {\"color\": \"#FF032D\", \"longname\": \"En ligne OU appelle\"}, "
                        + "\"0\": {\"color\": \"#0DFF25\", \"longname\": \"Disponible\"}, "
                        + "\"8\": {\"color\": \"#1B0AFF\", \"longname\": \"Sonne\"}"
                        + "},"
                        + "}, "
                        + "\"capaxlets\": ["
                        + "[\"identity\", \"grid\"], [\"search\", \"tab\"], [\"customerinfo\", \"tab\", \"1\"], "
                        + "[\"fax\", \"tab\", \"2\"], [\"dial\", \"grid\", \"2\"], [\"tabber\", \"grid\", \"3\"], "
                        + "[\"history\", \"tab\", \"3\"], [\"remotedirectory\", \"tab\", \"4\"], "
                        + "[\"features\", \"tab\", \"5\"], [\"mylocaldir\", \"tab\", \"6\"], [\"conference\", \"tab\", \"7\"]"
                        + "], " + "}");
        LoginCapasAck loginCapasAck = (LoginCapasAck) messageParser.parse(jsonObject);
        assertNotNull("unable to decode login capas ack", loginCapasAck);
        assertEquals("unable to decode presence", loginCapasAck.presence, "available");
        assertEquals("unable to decode user id", loginCapasAck.userId, "3");
        assertEquals("unable to decode application name", loginCapasAck.applicationName, "Client");
        assertNotNull("unable to decode capacitied", loginCapasAck.capacities);
        assertNotNull("unable to decode xlets", loginCapasAck.xlets);

    }

    @Test
    public void parseXlets() throws JSONException {
        JSONArray xletsJson = new JSONArray(
                "[[\"identity\", \"grid\"], [\"search\", \"tab\"], [\"customerinfo\", \"tab\", \"1\"], "
                        + "[\"fax\", \"tab\", \"2\"], [\"dial\", \"grid\", \"2\"], [\"tabber\", \"grid\", \"3\"], "
                        + "[\"history\", \"tab\", \"3\"], [\"remotedirectory\", \"tab\", \"4\"], "
                        + "[\"features\", \"tab\", \"5\"], [\"mylocaldir\", \"tab\", \"6\"], [\"conference\", \"tab\", \"7\"]]");
        List<Xlet> xlets = messageParser.parseXlets(xletsJson);

        assertNotNull("unable to decode xlets");

        Xlet xlet = new Xlet("fax", "tab", 2);

        assertThat(xlets, hasItem(xlet));
    }

    @Test
    public void parseCapacities() throws JSONException {
        JSONObject jsonObject = new JSONObject("{\"regcommands\": {}, "
                + "\"preferences\": {\"xlet.identity.logagent\": \"1\", \"xlet.identity.pauseagent\": \"1\"},"
                + " \"userstatus\": {" + "\"available\": {\"color\": \"#08FD20\","
                + " \"allowed\": [\"available\", \"away\",\"outtolunch\", \"donotdisturb\", \"berightback\"],"
                + " \"actions\": {\"enablednd\": \"false\"}, \"longname\": \"Disponible\"},"
                + "\"disconnected\": {\"color\": \"#202020\", "
                + "\"actions\": {\"agentlogoff\": \"\"}, \"longname\": \"D\u00e9connect\u00e9\"}}, "
                + "\"services\": [\"enablednd\", \"fwdunc\"], "
                + "\"phonestatus\": {  \"16\": {\"color\": \"#F7FF05\", \"longname\": \"En Attente\"}, "
                + "\"8\": {\"color\": \"#1B0AFF\", \"longname\": \"Sonne\"}}, " + "\"ipbxcommands\": {}" + "}");

        Capacities capacities = messageParser.parseCapacities(jsonObject);
        assertNotNull("unable to decode capacities", capacities);
        assertNotNull("unable to decode capacity preferences", capacities.getPreferences());
        assertNotNull("unable to decode userstatus", capacities.getUsersStatuses());
        assertNotNull("unable to decode services", capacities.getServices());
        assertNotNull("unable to decode phone statuses", capacities.getPhoneStatuses());

    }

    @Test
    public void parseCapacitiesNoPreferences() throws JSONException {
        JSONObject jsonObject = new JSONObject("{\"regcommands\": {}, " + "\"preferences\": false,"
                + " \"userstatus\": {" + "\"available\": {\"color\": \"#08FD20\","
                + " \"allowed\": [\"available\", \"away\",\"outtolunch\", \"donotdisturb\", \"berightback\"],"
                + " \"actions\": {\"enablednd\": \"false\"}, \"longname\": \"Disponible\"},"
                + "\"disconnected\": {\"color\": \"#202020\", "
                + "\"actions\": {\"agentlogoff\": \"\"}, \"longname\": \"D\u00e9connect\u00e9\"}}, "
                + "\"services\": [\"enablednd\", \"fwdunc\"], "
                + "\"phonestatus\": {  \"16\": {\"color\": \"#F7FF05\", \"longname\": \"En Attente\"}, "
                + "\"8\": {\"color\": \"#1B0AFF\", \"longname\": \"Sonne\"}}, " + "\"ipbxcommands\": {}" + "}");

        Capacities capacities = messageParser.parseCapacities(jsonObject);
        assertNotNull("unable to decode capacities", capacities);
        assertNotNull("unable to decode capacity preferences", capacities.getPreferences());
    }

    @Test
    public void parsePreferences() throws JSONException {
        JSONObject jsonPreferences = new JSONObject(
                "{\"xlet.identity.logagent\": \"1\", \"xlet.identity.pauseagent\": \"0\"}");

        List<XiVOPreference> preferences = messageParser.parsePreferences(jsonPreferences);

        assertNotNull("unable to decode preferences", preferences);
        XiVOPreference xivoPreference = new XiVOPreference("xlet.identity.pauseagent", "0");
        assertThat("unable to decode xivo preferences", preferences, hasItem(xivoPreference));
    }

    @Test
    public void testParsePhoneStatuses() throws JSONException {
        JSONObject phoneStatusesJson = new JSONObject(
                "{\"16\": {\"color\": \"#F7FF05\", \"longname\": \"En Attente\"}, "
                        + "\"1\": {\"color\": \"#FF032D\", \"longname\": \"En ligne OU appelle\"}, "
                        + "\"0\": {\"color\": \"#0DFF25\", \"longname\": \"Disponible\"}, "
                        + "\"8\": {\"color\": \"#1B0AFF\", \"longname\": \"Sonne\"}}, ");

        List<PhoneStatus> phoneStatuses = messageParser.parsePhoneStatuses(phoneStatusesJson);
        assertNotNull("phone statuses not decoded", phoneStatuses);
        PhoneStatus phoneStatus = new PhoneStatus("1", "#FF032D", "En ligne OU appelle");
        assertThat(phoneStatuses, hasItem(phoneStatus));
    }

    @Test
    public void testParseUserStatuses() throws JSONException {
        JSONObject userStatusesJson = new JSONObject(
                "{"
                        + "\"available\": {\"color\": \"#08FD20\","
                        + " \"allowed\": [\"available\", \"away\",\"outtolunch\", \"donotdisturb\", \"berightback\"],"
                        + " \"actions\": {\"enablednd\": \"false\"}, \"longname\": \"Disponible\"},"
                        + "\"outtolunch\": {\"color\": \"#001AFF\", \"allowed\": [\"available\", \"away\", \"outtolunch\", \"donotdisturb\", \"berightback\"],"
                        + " \"actions\": {\"enablednd\": \"false\"}, \"longname\": \"Parti Manger\"}}");

        List<UserStatus> userStatuses = messageParser.parseUserStatuses(userStatusesJson);
        assertEquals("all statuses not decoded", 2, userStatuses.size());
        assertEquals("status not decoded", "outtolunch", userStatuses.get(0).getName());
        assertEquals("status not decoded", "available", userStatuses.get(1).getName());

    }

    @Test
    public void parseUserStatus() throws JSONException {
        JSONObject userStatusJson = new JSONObject("{\"color\": \"#08FD20\","
                + " \"allowed\": [\"available\", \"away\",\"outtolunch\", \"donotdisturb\", \"berightback\"], "
                + "\"actions\": {\"enablednd\": \"false\"}, \"longname\": \"Disponible\"}");

        UserStatus userStatus = messageParser.parseUserStatus("available", userStatusJson);
        assertNotNull("unable to decode user status", userStatus);
        assertEquals("invalid name", "available", userStatus.getName());
        assertEquals("unable to decode color", "#08FD20", userStatus.getColor());
        assertEquals("unable to decode longname", "Disponible", userStatus.getLongName());
        assertTrue("unable to decode allowed", userStatus.isAllowed("available"));
        assertEquals("unable to decode action name", "enablednd", userStatus.getActions().get(0).getName());
        assertEquals("unable to decode action parameter", "false", userStatus.getActions().get(0).getParameters());

    }

    @Test
    public void parseUserStatusNothingAllowed() throws JSONException {
        JSONObject userStatusJson = new JSONObject("{\"color\": \"#08FD20\","
                + "\"actions\": {\"enablednd\": \"false\"}, \"longname\": \"Disponible\"}");
        try {
            messageParser.parseUserStatus("available", userStatusJson);
        } catch (JSONException e) {
            fail("allowed is optional");
        }

    }

    @Test
    public void testServices() throws JSONException {
        JSONArray servicesJson = new JSONArray("[\"enablednd\", \"fwdunc\"]");
        List<Service> services = messageParser.parseServices(servicesJson);

        assertThat(services, hasItem(new Service("enablednd")));
        assertThat(services, hasItem(new Service("fwdunc")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseUnknowMessage() throws JSONException {
        JSONObject jsonObject = new JSONObject(
                "{\"capalist\": [2],\"class\": \"unexisting_message_class\",\"replyid\": 1646064863,\"timenow\": 1361268824.68}");

        messageParser.parse(jsonObject);

    }

    @Test(expected = IllegalArgumentException.class)
    public void parseNullMessage() throws JSONException {
        messageParser.parse(null);
    }
}
