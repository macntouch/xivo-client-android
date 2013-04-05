package org.xivo.cti.message.request;

import org.xivo.cti.message.CtiEvent;
import org.xivo.cti.message.CtiMessage;
import org.xivo.cti.message.UserUpdateListener;

public class PhoneStatusUpdate extends CtiMessage implements CtiEvent<UserUpdateListener>{
    private Integer lineId;
    private String hintStatus;

    public Integer getLineId() {
        return lineId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public String getHintStatus() {
        return hintStatus;
    }

    public void setHintStatus(String hintStatus) {
        this.hintStatus = hintStatus;
    }

    @Override
    public void notify(UserUpdateListener listener) {
        listener.onUserStatusUpdate(this);

    }

}
