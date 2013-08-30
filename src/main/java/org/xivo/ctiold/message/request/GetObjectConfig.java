package org.xivo.ctiold.message.request;

import org.xivo.ctiold.message.request.GetStatusOrConfig;
import org.xivo.cti.model.ObjectType;

public class GetObjectConfig extends GetStatusOrConfig {

    public GetObjectConfig(ObjectType objectType, Integer objectId) {
        super("updateconfig",objectType,objectId);
    }
}
