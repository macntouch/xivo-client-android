package org.xivo.ctiold.message.request;

import org.xivo.ctiold.message.CtiEvent;
import org.xivo.ctiold.message.CtiMessage;
import org.xivo.ctiold.message.UserUpdateListener;

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
