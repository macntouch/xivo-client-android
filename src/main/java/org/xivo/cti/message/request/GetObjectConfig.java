package org.xivo.cti.message.request;

import org.xivo.cti.model.ObjectType;

public class GetObjectConfig extends GetStatusOrConfig {

    public GetObjectConfig(ObjectType objectType, Integer objectId) {
        super("updateconfig",objectType,objectId);
    }
}
