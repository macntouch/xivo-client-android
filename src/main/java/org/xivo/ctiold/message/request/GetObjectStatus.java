package org.xivo.ctiold.message.request;

import org.xivo.ctiold.message.request.GetStatusOrConfig;
import org.xivo.cti.model.ObjectType;

public class GetObjectStatus extends GetStatusOrConfig {

    public GetObjectStatus(ObjectType objectType, Integer objectId) {
        super("updatestatus",objectType,objectId);
    }
}
