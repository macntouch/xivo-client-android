package org.xivo.cti.model;

public class Action {
	private final String name;
	private final String parameters;

	public Action(String name, String parameters) {
		this.name = name;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public String getParameters() {
		return parameters;
	}

}
