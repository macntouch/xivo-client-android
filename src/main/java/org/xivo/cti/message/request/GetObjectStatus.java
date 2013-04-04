package org.xivo.cti.message.request;

import org.xivo.cti.model.ObjectType;

public class GetObjectStatus extends GetStatusOrConfig {

    public GetObjectStatus(ObjectType objectType, Integer objectId) {
        super("updatestatus",objectType,objectId);
    }
}
