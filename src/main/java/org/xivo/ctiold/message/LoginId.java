package org.xivo.ctiold.message;

public class LoginId {
	private final String claz =  "login_id";
	private final String company =  "default";
	private final String ident;
	private final String userlogin;
	private final String version =  "9999";
	private final String xivoversion = "1.2";


	public LoginId(String username, String identity) {
		userlogin = username;
		ident = identity;
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
