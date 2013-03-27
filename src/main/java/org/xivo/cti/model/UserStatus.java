package org.xivo.cti.model;

import java.util.ArrayList;
import java.util.List;

public class UserStatus {
	private final String name;
	private String color;
	private String longName;
	private List<String> allowed = new ArrayList<String>();
	private List<Action> actions = new ArrayList<Action>();
	
	public UserStatus(String name) {
		this.name = name;
	}


	public String getName() {
		return name;
	}


	public void setColor(String color) {
		this.color = color;
	}


	public String getColor() {
		return color;
	}


	public String getLongName() {
		return longName;
	}


	public void setLongName(String longName) {
		this.longName = longName;
	}


	public boolean isAllowed(String status) {
		return allowed.contains(status);
	}


	public List<Action> getActions() {
		return actions;
	}


	public void allow(String statusName) {
		allowed.add(statusName);
	}


	public void addAction(Action action) {
		actions.add(action);
	}

}
