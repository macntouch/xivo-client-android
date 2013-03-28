package org.xivo.cti;

import org.json.JSONException;
import org.json.JSONObject;
import org.xivo.cti.message.LoginCapas;
import org.xivo.cti.message.LoginId;
import org.xivo.cti.message.LoginPass;

public class MessageFactory {
    private static String KEY_COMMANDID = "commandid";
    private static String KEY_CLASS = "class";
    private static String KEY_CLAZ = "claz";

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

}
