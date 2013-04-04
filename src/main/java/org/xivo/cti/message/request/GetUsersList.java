package org.xivo.cti.message.request;

import org.xivo.cti.model.ObjectType;


public class GetUsersList extends GetConfig {
    private static final String LISTID = "listid";

    public GetUsersList() {
        super(LISTID,ObjectType.USERS.toString());
    }

}
