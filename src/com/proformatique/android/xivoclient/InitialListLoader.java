/* XiVO Client Android
 * Copyright (C) 2010-2011, Proformatique
 *
 * This file is part of XiVO Client Android.
 *
 * XiVO Client Android is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XiVO Client Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XiVO client Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.proformatique.android.xivoclient;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.Intent;
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
	
	private List<HashMap<String, String>> usersList = new ArrayList<HashMap<String, String>>();
	private List<HashMap<String, String>> historyList = new ArrayList<HashMap<String, String>>();
	private List<String> xletsList = new ArrayList<String>();
	private String xivoId = null;
	private String astId = null;
	private String thisChannelId = null;
	private String peerChannelId = null;
	private HashMap<String, String> capaPresenceState  = new HashMap<String, String>();
	private List<HashMap<String, String>> statusList = new ArrayList<HashMap<String, String>>();
	private HashMap<String, String> featuresEnablednd = new HashMap<String, String>();
	private HashMap<String, String> featuresBusy = new HashMap<String, String>();
	private HashMap<String, String> featuresRna = new HashMap<String, String>();
	private HashMap<String, String> featuresCallrecord = new HashMap<String, String>();
	private HashMap<String, String> featuresIncallfilter = new HashMap<String, String>();
	private HashMap<String, String> featuresUnc = new HashMap<String, String>();
	private HashMap<String, String> featuresEnablevoicemail = new HashMap<String, String>();
	private String xivoUserName;
	private String xivoPhoneNum;
	private String peersPeerChannelId;
	private int[] mwi = new int[3];		// 0 = warning, 1 = nb old messages, 2 = nb new messages
	private String calledNumber = null;
	
	private static InitialListLoader instance;
	
	public static InitialListLoader getInstance(){
		return instance;
	}
	
	private InitialListLoader(){
		super();
	}
	
	public static InitialListLoader init(){
		instance = new InitialListLoader();
		return instance;
	}
	
	public int startLoading(){
		int rCode;
		
		for (String list : lists) {
			rCode = initJsonList(list);
			if (rCode < 1) return rCode;
		}
		return Constants.OK;
	}
	
	@SuppressWarnings("unchecked")
	private int initJsonList(String inputClass) {
		JSONObject jObj = createJsonInputObject(inputClass,"getlist");
		if (jObj!=null){
			try {
				Log.d( LOG_TAG, "Jobj: " + jObj.toString());
				if (Connection.getInstance() != null && Connection.getInstance().getNetworkConnection() != null) {
					PrintStream output = new PrintStream(Connection.getInstance().getNetworkConnection().getOutputStream());
					if (output != null)
						output.println(jObj.toString());
					else
						Log.d(LOG_TAG, "No output stream");
				} else {
					Log.d(LOG_TAG, "No network connection");
				}
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
							JSONObject jObjCurrentState = jObjCurrent.getJSONObject("statedetails");
							
							/**
							 * Feed the useful fields to store in the list
							 */
							String xivoId = jObjCurrent.getString("xivo_userid");
							String userId = jObjCurrent.getString("astid") + "/" + xivoId;
							map.put("xivo_userid", xivoId);
							map.put("fullname", jObjCurrent.getString("fullname"));
							map.put("phonenum", jObjCurrent.getString("phonenum"));
							map.put("stateid", jObjCurrentState.getString("stateid"));
							map.put("stateid_longname", jObjCurrentState.getString("longname"));
							map.put("stateid_color", jObjCurrentState.getString("color"));
							map.put("techlist", jObjCurrent.getJSONArray("techlist").getString(0));
							usersList.add(map);
							// Save voice mail status if it's mine
							if (userId.equals(InitialListLoader.getInstance().getUserId())
									&& jObjCurrent.has("mwi")) {
								JSONArray mwi = jObjCurrent.getJSONArray("mwi");
								for (int j = 0; j < mwi.length(); j++) {
									this.mwi[j] = mwi.getInt(j);
								}
							}
							
							Log.d( LOG_TAG, "map : " + map.toString());
						}
						if (usersList.size() > 1) {
							Collections.sort(usersList, new fullNameComparator());
						}
					}
					
					/**
					 * Loading Phones list
					 */
					else if (inputClass.equals("phones") && ReadLineObject.has("payload")){
						JSONObject jPayloads = ReadLineObject.getJSONObject("payload");
						JSONArray jAllPhones = new JSONArray();
						for (Iterator<String> keyIter = jPayloads.keys(); keyIter.hasNext();) {
							String key = keyIter.next();
							Log.d(LOG_TAG, "Adding " + key + " to jAllPhones");
							jAllPhones.put(jPayloads.getJSONObject(key));
						}
						int nbXivo = jAllPhones.length();
						/**
						 * Use users field "techlist" to search objects in phones list
						 */
						int i=0;
						for (HashMap<String, String> mapUser : usersList) {
							if (!(mapUser.containsKey("techlist"))) {
								Log.d(LOG_TAG, "This user has no phone, skipping");
								Log.d(LOG_TAG, mapUser.toString());
								continue;
							}
							JSONObject jPhone = null;
							for (int j = 0; j < nbXivo; j++) {
								JSONObject xivo = jAllPhones.getJSONObject(j);
								if (xivo.has(mapUser.get("techlist")) == true) {
									jPhone = xivo.getJSONObject(mapUser.get("techlist"));
									break;
								}
							}
							if (jPhone == null) {
								Log.d(LOG_TAG, "No phone for this user, skipping");
								continue;
							}
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
	
	/**
	 * Send a presence status and check it has been enabled by server
	 * 
	 * @param inputClass
	 * @param function
	 * @return
	 */
	@SuppressWarnings("unused")
	private int initJsonString(String inputClass, String function) {
		
		JSONObject jObj = createJsonInputObject(inputClass,"available");
		if (jObj!=null){
			try {
				Log.d( LOG_TAG, "Jobj: " + jObj.toString());
				PrintStream output = new PrintStream(Connection.getInstance().getNetworkConnection().getOutputStream());
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
	
	public String getUserId() {
		return astId+"/"+xivoId;
	}
	
	public String getXivoId() {
		return xivoId;
	}
	
	public String getThisChannelId() {
		return thisChannelId;
	}
	
	public void setThisChannelId(String channelId) {
		this.thisChannelId = channelId;
	}
	
	public String getPeerChannelId() {
		return peerChannelId;
	}
	
	public void setPeerChannelId(String peerChannelId) {
		this.peerChannelId = peerChannelId;
	}
	
	public void setXivoId(String xivoId) {
		this.xivoId = xivoId;
	}
	
	public String getAstId() {
		return astId;
	}
	
	public void setAstId(String astId) {
		this.astId = astId;
	}
	
	public List<HashMap<String, String>> getUsersList() {
		return usersList;
	}
	
	public void setUsersList(List<HashMap<String, String>> usersList) {
		this.usersList = usersList;
	}
	
	public void replaceUsersList(int i, HashMap<String, String> map) {
		this.usersList.set(i, map);
	}
	
	public List<HashMap<String, String>> getHistoryList() {
		return historyList;
	}
	
	public void setHistoryList(List<HashMap<String, String>> historyList) {
		this.historyList = historyList;
	}
	
	public void addHistoryList(HashMap<String, String> map) {
		this.historyList.add(map);
	}
	
	public void clearHistoryList() {
		this.historyList.clear();
	}
	
	@SuppressWarnings("unchecked")
	public void sortHistoryList(){
		Collections.sort(this.historyList, new DateComparator());
	}
	
	public List<String> getXletsList() {
		return xletsList;
	}
	
	public void setXletsList(List<String> xletsList) {
		this.xletsList = xletsList;
	}
	
	public HashMap<String, String> getCapaPresenceState() {
		return capaPresenceState;
	}
	
	public void setCapaPresenceState(HashMap<String, String> capaPresenceState) {
		this.capaPresenceState = capaPresenceState;
	}
	
	public void putCapaPresenceState(String key, String value) {
		this.capaPresenceState.put(key, value);
	}
	
	public List<HashMap<String, String>> getStatusList() {
		return statusList;
	}
	
	public void setStatusList(List<HashMap<String, String>> statusList) {
		this.statusList = statusList;
	}
	
	public void addStatusList(HashMap<String, String> map) {
		this.statusList.add(map);
	}
	
	public HashMap<String, String> getFeaturesEnablednd() {
		return featuresEnablednd;
	}
	
	public void setFeaturesEnablednd(HashMap<String, String> featuresEnablednd) {
		this.featuresEnablednd = featuresEnablednd;
	}
	
	public HashMap<String, String> getFeaturesBusy() {
		return featuresBusy;
	}
	
	public void setFeaturesBusy(HashMap<String, String> featuresBusy) {
		this.featuresBusy = featuresBusy;
	}
	
	public HashMap<String, String> getFeaturesRna() {
		return featuresRna;
	}
	
	public void setFeaturesRna(HashMap<String, String> featuresRna) {
		this.featuresRna = featuresRna;
	}
	
	public HashMap<String, String> getFeaturesCallrecord() {
		return featuresCallrecord;
	}
	
	public void setFeaturesCallrecord(HashMap<String, String> featuresCallrecord) {
		this.featuresCallrecord = featuresCallrecord;
	}
	
	public HashMap<String, String> getFeaturesIncallfilter() {
		return featuresIncallfilter;
	}
	
	public void setFeaturesIncallfilter(HashMap<String, String> featuresIncallfilter) {
		this.featuresIncallfilter = featuresIncallfilter;
	}
	
	public HashMap<String, String> getFeaturesUnc() {
		return featuresUnc;
	}
	
	public void setFeaturesUnc(HashMap<String, String> featuresUnc) {
		this.featuresUnc = featuresUnc;
	}
	
	public HashMap<String, String> getFeaturesEnablevoicemail() {
		return featuresEnablevoicemail;
	}
	
	public void setFeaturesEnablevoicemail(
			HashMap<String, String> featuresEnablevoicemail) {
		this.featuresEnablevoicemail = featuresEnablevoicemail;
	}
	
	@SuppressWarnings("unchecked")
	private class DateComparator implements Comparator
	{
		public int compare(Object obj1, Object obj2)
		{
			SimpleDateFormat sd1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			HashMap<String, String> update1 = (HashMap<String, String>)obj1;
			HashMap<String, String> update2 = (HashMap<String, String>)obj2;
			Date d1 = null, d2 = null;
			try {
				d1 = sd1.parse(update1.get("ts"));
				d2 = sd1.parse(update2.get("ts"));
			} catch (ParseException e) {
				e.printStackTrace();
				return 0;
			}
			return (((d2.getTime()-d1.getTime())>0)?1:-1);
		}
	}
	
	@SuppressWarnings("unchecked")
	private class fullNameComparator implements Comparator
	{
		public int compare(Object obj1, Object obj2)
		{
			HashMap<String, String> update1 = (HashMap<String, String>)obj1;
			HashMap<String, String> update2 = (HashMap<String, String>)obj2;
			return update1.get("fullname").compareToIgnoreCase(update2.get("fullname"));
		}
	}
	
	public void setXivoUserName(String xivoUserName) {
		this.xivoUserName = xivoUserName;
	}
	
	public String getXivoUserName() {
		return xivoUserName;
	}
	
	public void setXivoPhoneNum(String number) {
		this.xivoPhoneNum = number;
	}
	
	public String getXivoPhoneNum() {
		return xivoPhoneNum;
	}
	
	public String getPeersPeerChannelId() {
		return peersPeerChannelId;
	}
	
	public void setPeersPeerChannelId(String peersPeerChannelId) {
		this.peersPeerChannelId = peersPeerChannelId;
	}
	
	/**
	 * Logs the channels to Log.d
	 */
	public void showChannels() {
		Log.d(LOG_TAG, "This channel = " + thisChannelId);
		Log.d(LOG_TAG, "Peer channel = " + peerChannelId);
		Log.d(LOG_TAG, "Peer's peer channel = " + peersPeerChannelId);
	}
	
	/**
	 * Check if the user has a voicemail warning.
	 * @return
	 */
	public boolean hasNewVoicemail() {
		return mwi[0] != 0;
	}
	
	/**
	 * Updates the status of our voicemail
	 * @param warning
	 * @param old
	 * @param newmail
	 */
	public void setMwi(Context context, int warning, int old, int newmail) {
		mwi[0] = warning;
		mwi[1] = old;
		mwi[2] = newmail;
		Intent iVoiceMailUpdate = new Intent();
		iVoiceMailUpdate.setAction(Constants.ACTION_MWI_UPDATE);
		iVoiceMailUpdate.putExtra("mwi", this.mwi);
		context.sendBroadcast(iVoiceMailUpdate);
	}
	
	public void setCalledNumber(String number) {
		this.calledNumber  = number.trim();
	}
	
	public String getCalledNumber() {
		return calledNumber;
	}
}
