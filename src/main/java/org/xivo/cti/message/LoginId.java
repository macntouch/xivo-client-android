package org.xivo.cti.message;

public class LoginId {
	private final String claz =  "login_id";
	private final String company =  "default";
	private final String ident = "X11-LE-24079";
	private final String userlogin;
	private final String version =  "9999";
	private final String xivoversion = "1.2";


	public LoginId(String username) {
		userlogin = username;
	}


	public String getClaz() {
		return claz;
	}


	public String getCompany() {
		return company;
	}


	public String getIdent() {
		return ident;
	}


	public String getUserlogin() {
		return userlogin;
	}


	public String getVersion() {
		return version;
	}


	public String getXivoversion() {
		return xivoversion;
	}

}
