package org.xivo.cti.message;

public class UserConfigUpdate extends CtiMessage implements CtiEvent<UserUpdateListener> {
    private int userId;
    private boolean dndEnabled;
    private boolean rnaEnabled;
    private String rnaDestination;
    private String firstName;

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

    public void setRnaEnabled(boolean rnaEnabled) {
        this.rnaEnabled = rnaEnabled;
    }

    public boolean isRnaEnabled() {
        return rnaEnabled;
    }

    public String getRnaDestination() {
        return rnaDestination;
    }

    public void setRnaDestination(String rnaDestination) {
        this.rnaDestination = rnaDestination;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

}
