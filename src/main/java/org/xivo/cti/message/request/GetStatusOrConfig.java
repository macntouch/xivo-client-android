package org.xivo.cti.message.request;

import org.xivo.cti.model.ObjectType;

public class GetStatusOrConfig  extends GetConfig {

    private final Integer objectId;

    public GetStatusOrConfig(String function,ObjectType objectType, Integer objectId) {
        super(function,objectType.toString());
        this.objectId = objectId;
    }

    public Integer getObjectId() {
        return objectId;
    }

}
