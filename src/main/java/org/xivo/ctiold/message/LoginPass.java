package org.xivo.ctiold.message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginPass {
	private final String claz = "login_pass";
	private final String hashedpassword;

    private static String bytes2String(byte[] bytes) {
        StringBuilder string = new StringBuilder();
        for (byte b: bytes) {
            String hexString = Integer.toHexString(0x00FF & b);
            string.append(hexString.length() == 1 ? "0" + hexString : hexString);
        }
        return string.toString();
    }

	public LoginPass(String password, String sessionId) {
        MessageDigest sha1;
        byte[] sDigest = null;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
            sDigest = sha1.digest((sessionId + ":" + password).getBytes());
        } catch (NoSuchAlgorithmException e) {
        	e.printStackTrace();
        }
        hashedpassword = bytes2String(sDigest);

	}

	public String getClaz() {
		return claz;
	}

	public String getHashedpassword() {
		return hashedpassword;
	}

}
