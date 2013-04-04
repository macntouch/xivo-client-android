package org.xivo.cti.message.request;

import org.xivo.cti.model.ObjectType;

public class GetObjectConfig extends GetConfig {

    private final Integer objectId;

    public GetObjectConfig(ObjectType objectType, Integer objectId) {
        super("updateconfig",objectType.toString());
        this.objectId = objectId;
    }

    public Integer getObjectId() {
        return objectId;
    }

}
