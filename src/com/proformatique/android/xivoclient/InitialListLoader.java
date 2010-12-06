package com.proformatique.android.xivoclient;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.proformatique.android.xivoclient.tools.Constants;

/**
 * This class is a useful lists provider for all class in the app
 * The lists are all initially loaded just after connection to CTI server
 * 
 * @author cquaquin
 *
 */
public class InitialListLoader {

	private static final String LOG_TAG = "LOAD_LISTS";
	
	/**
	 * Reference available lists
	 * WARNING : Let the users list before the others. 
	 * 			 Phones list need users list to be loaded
	 */
	String[] lists = new String[] { "users", "phones"};

	public List<HashMap<String, String>> usersList = new ArrayList<HashMap<String, String>>();
	public List<HashMap<String, String>> historyList = new ArrayList<HashMap<String, String>>();
	public List<HashMap<String, String>> phonesList = new ArrayList<HashMap<String, String>>();
	public List<String> xletsList = new ArrayList<String>();
	public String xivoId = null;
	public String astId = null;
	public HashMap<String, String> capaPresenceState  = new HashMap<String, String>();
	public List<HashMap<String, String>> statusList = new ArrayList<HashMap<String, String>>();
	
	public static InitialListLoader initialListLoader;
	
	public InitialListLoader(){
		initialListLoader = this;
	}
	
	public int startLoading(){
		int rCode;
		
		for (String list : lists) {
			rCode = initJsonList(list);
			if (rCode < 1) return rCode;
		}
		
		return Constants.OK;
	}

	private int initJsonList(String inputClass) {
		JSONObject jObj = createJsonInputObject(inputClass,"getlist");
		if (jObj!=null){
			try {
				Log.d( LOG_TAG, "Jobj: " + jObj.toString());
				PrintStream output = new PrintStream(Connection.getInstance().networkConnection.getOutputStream());
				output.println(jObj.toString());
			} catch (IOException e) {
				return Constants.NO_NETWORK_AVAILABLE;
			}
			
			JSONObject ReadLineObject = Connection.getInstance().readJsonObjectCTI(inputClass);
			if (ReadLineObject!=null){

				try {
					
					/**
					 * Loading Users list
					 */
					if (inputClass.equals("users")){
						JSONArray jArr = ReadLineObject.getJSONArray("payload");
						int len = jArr.length();

						for(int i = 0; i < len; i++){
							HashMap<String, String> map = new HashMap<String, String>();
							JSONObject jObjCurrent = jArr.getJSONObject(i);
						
							/**
							 * Feed the useful fields to store in the list
							 */
							map.put("xivo_userid", jObjCurrent.getString("xivo_userid"));
							map.put("fullname", jObjCurrent.getString("fullname"));
							map.put("phonenum", jObjCurrent.getString("phonenum"));
							map.put("stateid", jObjCurrent.getJSONObject("statedetails").getString("stateid"));
							map.put("stateid_longname", jObjCurrent.getJSONObject("statedetails").getString("longname"));
							map.put("techlist", jObjCurrent.getJSONArray("techlist").getString(0));
							usersList.add(map);

							Log.d( LOG_TAG, "map : " + map.toString());
						}
						/**
						 * Sorting list
						 */
						if (usersList.size()!=0){
							Collections.sort(usersList, new fullNameComparator());
						}
					}
					
					/**
					 * Loading Phones list
					 */
					else if (inputClass.equals("phones")){
						JSONObject jAllPhones = ReadLineObject.getJSONObject("payload").getJSONObject(astId);
						/**
						 * Use users field "techlist" to search objects in phones list
						 */
						int i=0;
						for (HashMap<String, String> mapUser : usersList) {
							JSONObject jPhone = jAllPhones.getJSONObject(mapUser.get("techlist"));
							/**
							 * "Real" phone number is retrieved from phones list
							 */
							mapUser.put("phonenum", jPhone.getString("number"));
							try {
								JSONObject jPhoneStatus = jPhone.getJSONObject("hintstatus");
								mapUser.put("hintstatus_color", jPhoneStatus.getString("color"));
								mapUser.put("hintstatus_code", jPhoneStatus.getString("code"));
								mapUser.put("hintstatus_longname", jPhoneStatus.getString("longname"));
							} catch (JSONException e) {
								Log.d( LOG_TAG, "No Phones status : "+ jPhone.toString());
								mapUser.put("hintstatus_color", "");
								mapUser.put("hintstatus_code", "");
								mapUser.put("hintstatus_longname", "");
							}
							if (mapUser.get("xivo_userid").equals(xivoId)){
								capaPresenceState.put("phonenum", mapUser.get("phonenum"));
								capaPresenceState.put("hintstatus_color", mapUser.get("hintstatus_color"));
								capaPresenceState.put("hintstatus_code", mapUser.get("hintstatus_code"));
								capaPresenceState.put("hintstatus_longname", mapUser.get("hintstatus_longname"));
							}
							usersList.set(i, mapUser);
							i++;
						}
					}
				
				} catch (JSONException e) {
					e.printStackTrace();
					return Constants.JSON_POPULATE_ERROR;
				}
			}
		}

		return Constants.OK;
	}
	
	class fullNameComparator implements Comparator
	{
	    @SuppressWarnings("unchecked")
		public int compare(Object obj1, Object obj2)
	    {
	        HashMap<String, String> update1 = (HashMap<String, String>)obj1;
	        HashMap<String, String> update2 = (HashMap<String, String>)obj2;
	        return update1.get("fullname").compareTo(update2.get("fullname"));
	    }
	}


	/**
	 * Send a presence status and check it has been enabled by server
	 * 
	 * @param inputClass
	 * @param function
	 * @return
	 */
	private int initJsonString(String inputClass, String function) {

		JSONObject jObj = createJsonInputObject(inputClass,"available");
		if (jObj!=null){
			try {
				Log.d( LOG_TAG, "Jobj: " + jObj.toString());
				PrintStream output = new PrintStream(Connection.getInstance().networkConnection.getOutputStream());
				output.println(jObj.toString());
			} catch (IOException e) {
				return Constants.NO_NETWORK_AVAILABLE;
			}
			
			JSONObject jObjCurrent = Connection.getInstance().readJsonObjectCTI("presence");
			if (jObjCurrent!=null){

				try {
						HashMap<String, String> map = new HashMap<String, String>();

						if (inputClass.equals("users")){
							map.put("xivo_userid", jObjCurrent.getString("xivo_userid"));
						}
						
						Log.d( LOG_TAG, "map : " + map.toString());
				
				} catch (JSONException e) {
					e.printStackTrace();
					return Constants.JSON_POPULATE_ERROR;
				}
			}
		}

		return Constants.OK;
	}
	
	
	private JSONObject createJsonInputObject(String inputClass, String function) {
		JSONObject jObj = new JSONObject();
		
		try {
			jObj.accumulate("direction", Constants.XIVO_SERVER);
			jObj.accumulate("class",inputClass);
			jObj.accumulate("function", function);
			
			return jObj;
		} catch (JSONException e) {
			return null;
		}
	}
	

	
}
