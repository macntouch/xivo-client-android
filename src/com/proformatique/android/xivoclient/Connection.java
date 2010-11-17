package com.proformatique.android.xivoclient;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
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
	String sessionId;
	JSONObject jCapa;
	
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
	 * @return error or success code
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
			System.out.println("Client: " + jLogin.toString());
			PrintStream output = new PrintStream(networkConnection.getOutputStream());
			output.println(jLogin.toString());
		} catch (IOException e) {
			return Constants.NO_NETWORK_AVAILABLE;
		}
		
		ReadLineObject = readJsonObjectCTI();
		
		try {
		    if (ReadLineObject.getString("class").equals(Constants.XIVO_LOGIN_OK)){

				/**
				 * Second step : check that password is allowed on server
				 */
				int codePassword = passwordCTI(ReadLineObject);
				
				/**
				 * Third step : send configuration options on server
				 */
				if (codePassword > 0) {
					ReadLineObject = readJsonObjectCTI();
					if (ReadLineObject.getString("class").equals(Constants.XIVO_PASSWORD_OK))
					{
						int codeCapas = capasCTI();
						if (codeCapas > 0) {
							ReadLineObject = readJsonObjectCTI();
							
							if (ReadLineObject.getString("class").equals(Constants.XIVO_LOGIN_CAPAS_OK)){
								jCapa = ReadLineObject;
								System.out.println(jCapa.length());
								return Constants.CONNECTION_OK;
							}
						}
					}
				}
			}
		    
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return Constants.LOGIN_KO;
	}

	/**
	 * Perform a read action on the stream from CTI server
	 * @return JSON object retrieved
	 */
	private JSONObject readJsonObjectCTI() {
		JSONObject ReadLineObject;
		
		try {
			while ((responseLine = input.readLine()) != null) {
				try {
					ReadLineObject = new JSONObject(responseLine);
					System.out.println("Server: " + responseLine);
					return ReadLineObject;
				}
				catch (Exception e) {
					e.printStackTrace();

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}

	private int passwordCTI(JSONObject jsonSessionRead) throws JSONException {
		byte[] sDigest = null;
		sessionId = jsonSessionRead.getString("sessionid");
		JSONObject jsonPasswordAuthent = new JSONObject();
		
		/**
		 * Encrypt password for communication with algorithm SHA1
		 */
		MessageDigest sha1;
		try {
			sha1 = MessageDigest.getInstance("SHA1");
			sDigest = sha1.digest((sessionId+":"+password).getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		jsonPasswordAuthent.accumulate("class", "login_pass");
		jsonPasswordAuthent.accumulate("hashedpassword", bytes2String(sDigest));
		PrintStream output;
		try {
			output = new PrintStream(networkConnection.getOutputStream());
			output.println(jsonPasswordAuthent.toString());
		} catch (IOException e) {
			return Constants.NO_NETWORK_AVAILABLE;
		}

		return Constants.CONNECTION_OK;
	}

	private int capasCTI() throws JSONException {
		JSONObject jsonCapas = new JSONObject();
		
		jsonCapas.accumulate("class", "login_capas");
		jsonCapas.accumulate("agentlogin", "now");
		jsonCapas.accumulate("capaid", "client");
		jsonCapas.accumulate("lastconnwins", "false");
		jsonCapas.accumulate("loginkind", "agent");
		jsonCapas.accumulate("phonenumber", "101");
		jsonCapas.accumulate("state", "");
		
		PrintStream output;
		try {
			output = new PrintStream(networkConnection.getOutputStream());
			output.println(jsonCapas.toString());
		} catch (IOException e) {
			return Constants.NO_NETWORK_AVAILABLE;
		}

		return Constants.CONNECTION_OK;
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
