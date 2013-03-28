package org.xivo.cti.model;

public class PhoneStatus {
	private final String id;
	private final String color;
	private final String longName;

	public PhoneStatus(String id, String color, String longName) {
		this.id = id;
		this.color = color;
		this.longName = longName;
	}

	
	public String getId() {
		return id;
	}


	public String getColor() {
		return color;
	}


	public String getLongName() {
		return longName;
	}


	@Override
	public boolean equals(Object obj) {
		PhoneStatus phoneStatus = (PhoneStatus) obj;
		return (phoneStatus.getId().equals(this.id)
				&& phoneStatus.getColor().equals(this.color)
				&& phoneStatus.getLongName().equals(this.longName));
	}
	
	@Override
	public String toString() {
		return id + ":{"+color+","+longName+"}";
	}
}
