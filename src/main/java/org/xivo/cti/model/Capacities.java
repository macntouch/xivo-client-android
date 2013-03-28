package org.xivo.cti.model;

import java.util.List;

public class Capacities {

	private boolean preferences;
	private List<UserStatus> usersStatuses;
	private List<Service> services;
	private List<PhoneStatus> phoneStatuses;
	
	public boolean isPreferences() {
		return preferences;
	}
	public void setPreferences(boolean preferences) {
		this.preferences = preferences;
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
}
