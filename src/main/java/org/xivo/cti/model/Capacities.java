package org.xivo.cti.model;

import java.util.ArrayList;
import java.util.List;

public class Capacities {

    private List<UserStatus> usersStatuses;
    private List<Service> services;
    private List<PhoneStatus> phoneStatuses;
    private List<XiVOPreference> preferences;

    public Capacities() {
        preferences = new ArrayList<XiVOPreference>();
    }

    public List<UserStatus> getUsersStatuses() {
        return usersStatuses;
    }

    public void setUsersStatuses(List<UserStatus> usersStatuses) {
        this.usersStatuses = usersStatuses;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public List<PhoneStatus> getPhoneStatuses() {
        return phoneStatuses;
    }

    public void setPhoneStatuses(List<PhoneStatus> phoneStatuses) {
        this.phoneStatuses = phoneStatuses;
    }

    public List<XiVOPreference> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<XiVOPreference> preferences) {
        this.preferences = preferences;
    }

}
