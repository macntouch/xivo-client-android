package org.xivo.cti.message;

public class UserConfigUpdate extends CtiMessage implements CtiEvent<UserUpdateListener> {
    int userId;
    boolean dndEnabled;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public void setDndEnabled(boolean dndEnabled) {
        this.dndEnabled = dndEnabled;
    }

    public boolean isDndEnabled() {
        return dndEnabled;
    }

    @Override
    public void notify(UserUpdateListener listener) {
        listener.onUserConfigUpdate(this);
        
    }

}
