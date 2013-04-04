package org.xivo.cti.message;

import java.util.ArrayList;
import java.util.List;

public class UserConfigUpdate extends CtiMessage implements CtiEvent<UserUpdateListener> {
    private int userId;
    private boolean dndEnabled;
    private boolean rnaEnabled;
    private String rnaDestination;
    private boolean uncEnabled;
    private String UncDestination;
    private boolean busyEnabled;
    private String busyDestination;
    private String firstName;
    private String lastName;
    private String fullName;
    private String mobileNumber;
    private List<Integer> lineIds;

    public UserConfigUpdate() {
        lineIds = new ArrayList<Integer>();
    }
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

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isUncEnabled() {
        return uncEnabled;
    }

    public void setUncEnabled(boolean uncEnabled) {
        this.uncEnabled = uncEnabled;
    }

    public String getUncDestination() {
        return UncDestination;
    }

    public void setUncDestination(String uncDestination) {
        UncDestination = uncDestination;
    }

    public boolean isBusyEnabled() {
        return busyEnabled;
    }

    public void setBusyEnabled(boolean busyEnabled) {
        this.busyEnabled = busyEnabled;
    }

    public String getBusyDestination() {
        return busyDestination;
    }

    public void setBusyDestination(String busyDestination) {
        this.busyDestination = busyDestination;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public List<Integer> getLineIds() {
        return lineIds;
    }
    public void addLineId(Integer id) {
        lineIds.add(id);
    }

}
