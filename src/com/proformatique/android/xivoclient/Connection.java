package com.proformatique.android.xivoclient;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.tools.Constants;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Class of Connection and authentication on Xivo CTI server
 * 
 * @author cquaquin
 *
 */
public class Connection {

	String serverAdress;
	int serverPort;
	String login;
	String password;
	Activity callingActivity;
	SharedPreferences settings;
	Socket networkConnection;
	DataInputStream input;
	String responseLine;
	
	public Connection(String login, String password,
			Activity callingActivity) {
		super();
		this.login = login;
		this.password = password;
		this.callingActivity = callingActivity;
		this.settings = PreferenceManager.getDefaultSharedPreferences(callingActivity);
		this.serverAdress = this.settings.getString("server_adress", "");
		this.serverPort = Integer.parseInt(this.settings.getString("server_port", "5003"));

	}

	/**
	 * Perform network connection with Xivo CTI server
	 * 
	 * @return error code
	 */
	public int initialize() {
		
		try {
			networkConnection = new Socket(serverAdress, serverPort);

			input = new DataInputStream(networkConnection.getInputStream());
            String responseLine;
            
			while ((responseLine = input.readLine()) != null) {

                   if (responseLine.contains("XiVO CTI Server")) {
                	   return loginCTI();
                   }
               }
			return Constants.NOT_CTI_SERVER;
			
		} catch (UnknownHostException e) {
			return Constants.BAD_HOST;
		} catch (IOException e) {
			return Constants.NO_NETWORK_AVAILABLE;
		}
		
	}
	
	/**
	 * Perform authentication by Json array on Xivo CTI server
	 * 
	 * @return error code
	 */
	private int loginCTI(){
		
		JSONObject ReadLineObject;
		
		/**
		 * Creating first Json login array
		 */
		JSONObject jLogin = new JSONObject();
		try {
			jLogin.accumulate("class","login_id");
			jLogin.accumulate("company", Constants.XIVO_CONTEXT);
			jLogin.accumulate("ident","undef@X11-LE");
			jLogin.accumulate("userid",login);
			jLogin.accumulate("version",Constants.XIVO_LOGIN_VERSION);
			jLogin.accumulate("xivoversion",Constants.XIVO_VERSION);
			
		} catch (JSONException e) {
			return Constants.JSON_POPULATE_ERROR;
		}
		
		/**
		 * First step : check that login is allowed on server
		 */
		try {
			PrintStream output = new PrintStream(networkConnection.getOutputStream());
			output.println(jLogin.toString());
		} catch (IOException e) {
			return Constants.NO_NETWORK_AVAILABLE;
		}
		
		try {
			while ((responseLine = input.readLine()) != null) {
				try {
					ReadLineObject = new JSONObject(responseLine);
					System.out.println("Server: " + responseLine);
					if (ReadLineObject.getString("class").equals(Constants.XIVO_LOGIN_OK)){

						/**
						 * Second step : check that password is allowed on server
						 */
						int codePassword = passwordCTI(ReadLineObject);
						
						/**
						 * Third step : send configuration options on server
						 */
						if (codePassword > 1) {
							return capasCTI();
						}
					}
					else return Constants.XIVO_LOGIN_KO;
				}
				catch (Exception e) {
					e.printStackTrace();
					return Constants.NO_NETWORK_AVAILABLE;
				}
					
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		
		return Constants.CONNECTION_OK;
	}

	private int capasCTI() {
		// TODO Auto-generated method stub
		return 0;
	}

	private int passwordCTI(JSONObject jsonSessionRead) throws JSONException {
		byte[] sDigest = null;
		String sessionId = jsonSessionRead.getString("sessionid");
		JSONObject jsonPasswordAuthent = new JSONObject();
		
		MessageDigest sha1;
		try {
			sha1 = MessageDigest.getInstance("SHA1");
			sDigest = sha1.digest((sessionId+":"+password).getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		jsonPasswordAuthent.accumulate("class", "login_pass");
		jsonPasswordAuthent.accumulate("hashedpassword", bytes2String(sDigest));

		return 0;
	}

	private static String bytes2String(byte[] bytes) {
        StringBuilder string = new StringBuilder();
        for (byte b: bytes) {
                String hexString = Integer.toHexString(0x00FF & b);
                string.append(hexString.length() == 1 ? "0" + hexString : hexString);
        }
        return string.toString();
	}

}
