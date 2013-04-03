package org.xivo.cti.message.request;

public class GetConfig {
    private static final String USERS = "users";
    private static final String XIVO = "xivo";
    private static final String GETLIST = "getlist";

    private final String claz = GETLIST;
    private final String tipBxid = XIVO;
    private final String listName = USERS;
    private final String function;
    
    public GetConfig(String function) {
        this.function = function;
    }

    public String getClaz() {
        return claz;
    }

    public String getTipBxid() {
        return tipBxid;
    }

    public String getListName() {
        return listName;
    }

    public String getFunction() {
        return function;
    }

}
