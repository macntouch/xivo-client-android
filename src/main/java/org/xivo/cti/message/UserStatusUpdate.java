package org.xivo.cti.message;

public class UserStatusUpdate extends CtiMessage implements CtiEvent<UserUpdateListener>{
    int userId;
    String status;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public void notify(UserUpdateListener listener) {
       listener.onUserStatusUpdate(this);
        
    }

}
