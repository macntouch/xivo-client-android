package org.xivo.cti.model;

public class Service {
	private final String name;

	public Service(String name) {
		this.name = name;
	}
	
		public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Service : " + name;
	}
	
	@Override
	public boolean equals(Object obj) {
		return ((Service) obj).getName().equals(this.name);
	}

}
