package org.xivo.ctiold.message;

public class LoginCapas {
	private final String claz =  "login_capas";
	private final String loginkind = "user";
	private final int capaid;
	private final boolean lastconnwins = false;
	private final String state = "available";

	public LoginCapas(int capaId) {
		this.capaid = capaId;
	}

	public String getClaz() {
		return claz;
	}

	public String getLoginkind() {
		return loginkind;
	}

	public int getCapaid() {
		return capaid;
	}

	public boolean isLastconnwins() {
		return lastconnwins;
	}

	public String getState() {
		return state;
	}

}
