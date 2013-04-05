package org.xivo.cti;

import org.json.JSONException;
import org.json.JSONObject;
import org.xivo.cti.message.LoginCapas;
import org.xivo.cti.message.LoginId;
import org.xivo.cti.message.LoginPass;
import org.xivo.cti.message.request.GetConfig;
import org.xivo.cti.message.request.GetObjectConfig;
import org.xivo.cti.message.request.GetObjectStatus;
import org.xivo.cti.message.request.GetUsersList;
import org.xivo.cti.model.ObjectType;

public class MessageFactory {
    private static String KEY_COMMANDID = "commandid";

    private static long commandId = 1;

    private JSONObject addFields(JSONObject message) {
        try {
            message.accumulate(KEY_COMMANDID, commandId++);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message;
    }

    public JSONObject createLoginId(String username,String identity) {
        LoginId loginId = new LoginId(username,identity);
        JSONObject jsonLoginId = new JSONObject();
        try {
            jsonLoginId.accumulate("class", loginId.getClaz());
            jsonLoginId.accumulate("company", loginId.getCompany());
            jsonLoginId.accumulate("ident", loginId.getIdent());
            jsonLoginId.accumulate("userlogin", loginId.getUserlogin());
            jsonLoginId.accumulate("version", loginId.getVersion());
            jsonLoginId.accumulate("xivoversion", loginId.getXivoversion());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return addFields(jsonLoginId);
    }

    public JSONObject createLoginPass(String password, String sessionId) {
        LoginPass loginPass = new LoginPass(password, sessionId);

        JSONObject jsonLoginPass = new JSONObject();

        try {
            jsonLoginPass.accumulate("class", loginPass.getClaz());
            jsonLoginPass.accumulate("hashedpassword", loginPass.getHashedpassword());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return addFields(jsonLoginPass);
    }

    public JSONObject createLoginCapas(int capaId) {
        LoginCapas loginCapas = new LoginCapas(capaId);

        JSONObject jsonLoginCapas = new JSONObject();

        try {
            jsonLoginCapas.accumulate("class", loginCapas.getClaz());
            jsonLoginCapas.accumulate("capaid", loginCapas.getCapaid());
            jsonLoginCapas.accumulate("loginkind", loginCapas.getLoginkind());
            jsonLoginCapas.accumulate("state", loginCapas.getState());
            jsonLoginCapas.accumulate("lastconnwins", loginCapas.isLastconnwins());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return addFields(jsonLoginCapas);
    }

    public JSONObject createGetUsersList() {
        GetUsersList getUsersList = new GetUsersList();
        JSONObject jsonGetUsersList = new JSONObject();
        jsonGetUsersList = createGetConfig(getUsersList);

        return  jsonGetUsersList;
    }

    public JSONObject createGetUserConfig(Integer userId) {
        GetObjectConfig getUserConfig = new GetObjectConfig(ObjectType.USERS,userId);
        JSONObject jsonGetUserConfig = createGetConfig(getUserConfig);

        try {
            jsonGetUserConfig.accumulate("tid", userId.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonGetUserConfig;
    }

    protected JSONObject createGetConfig(GetConfig getConfig) {
        JSONObject jsonGetConfig = new JSONObject();
        try {
            jsonGetConfig.accumulate("class",getConfig.getClaz());
            jsonGetConfig.accumulate("tipbxid",getConfig.getTipBxid());
            jsonGetConfig.accumulate("listname",getConfig.getListName());
            jsonGetConfig.accumulate("function",getConfig.getFunction());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return addFields(jsonGetConfig);
    }

    public JSONObject createGetUserStatus(Integer userId) {
        GetObjectStatus getUserStatus = new GetObjectStatus(ObjectType.USERS,userId);
        JSONObject jsonGetUserStatus = createGetConfig(getUserStatus);
        try {
            jsonGetUserStatus.accumulate("tid", getUserStatus.getObjectId().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonGetUserStatus;
    }

    public JSONObject createGetPhoneConfig(Integer lineId) {
        GetObjectConfig getPhoneConfig = new GetObjectConfig(ObjectType.PHONES,lineId);
        JSONObject jsonGetUserConfig = createGetConfig(getPhoneConfig);

        try {
            jsonGetUserConfig.accumulate("tid", getPhoneConfig.getObjectId().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonGetUserConfig;
    }

    public JSONObject createGetPhoneStatus(Integer lineId) {
        GetObjectStatus getPhoneStatus = new GetObjectStatus(ObjectType.PHONES,lineId);
        JSONObject jsonGetPhoneStatus = createGetConfig(getPhoneStatus);
        try {
            jsonGetPhoneStatus.accumulate("tid", getPhoneStatus.getObjectId().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonGetPhoneStatus;
    }
}
