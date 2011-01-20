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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.proformatique.android.xivoclient;
package com.proformatique.android.xivoclient.service;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.proformatique.android.xivoclient.tools.Constants;

/**
 * This class implements an infinite loop that receives Json events from CTI server.
 * When an event occurs, it is detected and lists of users, phones, etc are updated,
 * then a broadcast intent is sent to inform Activities that an update is available
 *  
 * @author cquaquin
 *
 */
public class JsonLoopListener {
	
	Context context;
	Thread thread;
	Handler handler;
	protected String LOG_TAG = "JSONLOOP";
	private static boolean cancel = false;
	private static JsonLoopListener instance;
	
	// Keep a copy of useMobile and mobileNumber since the config can't be
	// accessed from other threads
	private static boolean useMobile;
	private static String mobileNumber;
	
	public static JsonLoopListener getInstance(Context context) {
		useMobile = SettingsActivity.getUseMobile(context);
		if (useMobile) {
			mobileNumber = SettingsActivity.getMobileNumber(context);
		}
		if (null == instance) {
			instance = new JsonLoopListener(context);
		} else if (cancel == true) {
			instance.startJsonListener();
			instance.sendListRefresh();
		}
		
		return instance;
	}
	
	public static void setUseMobile(boolean newValue) {
		useMobile = newValue;
	}
	
	public static void setMobileNumber(String number) {
		mobileNumber = number;
	}
	
	private JsonLoopListener(Context context) {
		this.context = context;
		startJsonListener();
		sendListRefresh();
	}
	
	private void sendListRefresh() {
		new Thread(new Runnable() {
			public void run() {
				sendFeaturesListRefresh();
				InitialListLoader.getInstance().clearHistoryList();
				sendListHistoRefresh("0","10");
				sendListHistoRefresh("1","10");
				sendListHistoRefresh("2","10");
			}
		}).start();
	}
	
	private void sendFeaturesListRefresh(){
		JSONObject jObj = new JSONObject();
		
		try {
			jObj.accumulate("direction", Constants.XIVO_SERVER);
			jObj.accumulate("class","featuresget");
			jObj.accumulate("userid", InitialListLoader.getInstance().getUserId());
			
			PrintStream output = new PrintStream(
					Connection.getInstance(context).getNetworkConnection().getOutputStream());
			output.println(jObj.toString());
			Log.d( LOG_TAG , "Client : "+jObj.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendListHistoRefresh(String mode, String elementsNumber){
		JSONObject jObj = new JSONObject();
		
		try {
			SimpleDateFormat sIso = new SimpleDateFormat("yyyy-MM-dd");
			Date dDay = new Date();
			Calendar c1 = new GregorianCalendar();
			c1.setTime(dDay);
			c1.add(Calendar.DAY_OF_MONTH, -30);
			
			jObj.accumulate("direction", Constants.XIVO_SERVER);
			jObj.accumulate("class","history");
			jObj.accumulate("peer", InitialListLoader.getInstance().getUserId());
			jObj.accumulate("size",elementsNumber);
			jObj.accumulate("mode",mode);
			jObj.accumulate("morerecentthan",sIso.format(c1.getTime()));
			
			PrintStream output = new PrintStream(
					Connection.getInstance(context).getNetworkConnection().getOutputStream());
			output.println(jObj.toString());
			Log.d( LOG_TAG , "Client : "+jObj.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Permanent Listener for incoming JSON lines reading
	 */
	private void startJsonListener(){
		cancel = false;
		handler = new Handler() {
			
			public void handleMessage(Message msg) {
				switch(msg.what) {
				case 1:
					/**
					 * Send  a broadcast intent to all Broadcast Receiver 
					 * that listen this action --> inform Activities that a list is updated
					 */
					Log.d( LOG_TAG , "Send Broadcast "+msg.what);
					Intent i = new Intent();
					i.setAction(Constants.ACTION_LOAD_USER_LIST);
					context.sendBroadcast(i);
					break;
				case 2:
					/**
					 * Send  a broadcast intent to all Broadcast Receiver 
					 * that listen this action --> inform Activities that the user's status phone
					 * is updated
					 */
					Log.d( LOG_TAG , "Send Broadcast "+msg.what);
					Intent i2 = new Intent();
					i2.setAction(Constants.ACTION_LOAD_PHONE_STATUS);
					context.sendBroadcast(i2);
					break;
				case 3:
					/**
					 * Send  a broadcast intent to all Broadcast Receiver 
					 * that listen this action --> inform Activities that the call history
					 * is updated
					 */
					Log.d( LOG_TAG , "Send Broadcast "+msg.what);
					Intent i3 = new Intent();
					i3.setAction(Constants.ACTION_LOAD_HISTORY_LIST);
					context.sendBroadcast(i3);
					break;
				case 4:
					/**
					 * Send  a broadcast intent to all Broadcast Receiver 
					 * that listen this action --> inform Activities that the features list
					 * is updated
					 */
					Log.d( LOG_TAG , "Send Broadcast "+msg.what);
					Intent i4 = new Intent();
					i4.setAction(Constants.ACTION_LOAD_FEATURES);
					context.sendBroadcast(i4);
					break;
				case Constants.NO_NETWORK_AVAILABLE:
					Intent iNoNetwork = new Intent();
					iNoNetwork.setAction(Constants.ACTION_DISCONNECT);
					context.sendBroadcast(iNoNetwork);
					break;
				case Constants.JSON_POPULATE_ERROR:
					Intent iJsonError = new Intent();
					iJsonError.setAction(Constants.ACTION_DISCONNECT);
					context.sendBroadcast(iJsonError);
					break;
				case Constants.FORCED_DISCONNECT:
					Log.d(LOG_TAG, "Send Broadcast Forced disconnection");
					Intent iForcedDisconnection = new Intent();
					iForcedDisconnection.setAction(Constants.ACTION_FORCED_DISCONNECT);
					context.sendBroadcast(iForcedDisconnection);
					break;
				}
			} 
		};
		
		thread = new Thread() {
			public void run() {
				int i = 0;
				while(i < 1) {
					
					if (cancel) break;
					
					try {
						
						JSONObject jObjCurrent = Connection.getInstance(context).readData();
						String classRec = "";
						@SuppressWarnings("unused")
						String functionRec = "";
						
						if (jObjCurrent.has("class"))
							classRec = (String) jObjCurrent.get("class");
						
						if (jObjCurrent.has("function"))
							functionRec = (String) jObjCurrent.get("function");
						
						if (classRec.equals("presence")) {
							HashMap<String, String> map = new HashMap<String, String>();
							JSONObject jObjCurrentState = jObjCurrent.getJSONObject("capapresence").getJSONObject("state");
							
							map.put("xivo_userid", jObjCurrent.getString("xivo_userid"));
							map.put("stateid", jObjCurrentState.getString("stateid"));
							map.put("stateid_longname", jObjCurrentState.getString("longname"));
							map.put("stateid_color", jObjCurrentState.getString("color"));
							
							updateUserList(InitialListLoader.getInstance().getUsersList(), map, "presence");
							
							handler.sendEmptyMessage(1);
						}
						
						if (classRec.equals("phones")) {
							HashMap<String, String> map = new HashMap<String, String>();
							
							if (jObjCurrent.has("status")) {
								JSONObject jStatus = jObjCurrent.getJSONObject("status");
								updatePhoneChannelStatus(jStatus);
							
								JSONObject jHintStatus = jStatus.getJSONObject("hintstatus");
								map.put("xivo_userid", jStatus.getString("id"));
								if (jHintStatus.has("code")){
									map.put("hintstatus_color", jHintStatus.getString("color"));
									map.put("hintstatus_code", jHintStatus.getString("code"));
									map.put("hintstatus_longname", jHintStatus.getString("longname"));
								}
								else {
									map.put("hintstatus_color", "");
									map.put("hintstatus_code", "");
									map.put("hintstatus_longname", "");
								}
								updateUserList(InitialListLoader.getInstance().getUsersList(), map, "phone");
								handler.sendEmptyMessage(1);
							}
						}
						
						/**
						 * Loading History of calls list
						 */
						if (classRec.equals("history")){
							JSONArray jArr = jObjCurrent.getJSONArray("payload");
							int len = jArr.length();
							for(int j = 0; j < len; j++){
								HashMap<String, String> map = new HashMap<String, String>();
								JSONObject jObjCurrentList = jArr.getJSONObject(j);
								
								/**
								 * Feed the useful fields in a map to store in the list
								 */
								map.put("duration", jObjCurrentList.getString("duration"));
								map.put("termin", jObjCurrentList.getString("termin"));
								map.put("direction", jObjCurrentList.getString("direction"));
								map.put("fullname", jObjCurrentList.getString("fullname"));
								map.put("ts", jObjCurrentList.getString("ts"));
								InitialListLoader.getInstance().addHistoryList(map);
							}
							
							/**
							 * Sorting list
							 */
							if (InitialListLoader.getInstance().getHistoryList().size()!=0){
								InitialListLoader.getInstance().sortHistoryList();
							}
							
							handler.sendEmptyMessage(3);
						}
						
						/**
						 * Loading Features list
						 */
						if (classRec.equals("features")){
							JSONObject jObj = jObjCurrent.getJSONObject("payload");
							JSONObject jObjFeature = null;
							String feature = "";
							
							feature = "enablednd";
							if (jObj.has(feature)){
								HashMap<String, String> map = new HashMap<String, String>(); 
								jObjFeature = jObj.getJSONObject(feature);
								map.put("enabled", jObjFeature.getString("enabled"));
								
								InitialListLoader.getInstance().setFeaturesEnablednd(map);
							}
							
							feature = "callrecord";
							if (jObj.has(feature)){
								HashMap<String, String> map = new HashMap<String, String>(); 
								jObjFeature = jObj.getJSONObject(feature);
								map.put("enabled", jObjFeature.getString("enabled"));
								
								InitialListLoader.getInstance().setFeaturesCallrecord(map);
							}
							
							feature = "incallfilter";
							if (jObj.has(feature)){
								HashMap<String, String> map = new HashMap<String, String>(); 
								jObjFeature = jObj.getJSONObject(feature);
								map.put("enabled", jObjFeature.getString("enabled"));
								
								InitialListLoader.getInstance().setFeaturesIncallfilter(map);
							}
							
							feature = "enablevoicemail";
							if (jObj.has(feature)){
								HashMap<String, String> map = new HashMap<String, String>(); 
								jObjFeature = jObj.getJSONObject(feature);
								map.put("enabled", jObjFeature.getString("enabled"));
								
								InitialListLoader.getInstance().setFeaturesEnablevoicemail(map);
							}
							
							feature = "busy";
							if (jObj.has(feature)||jObj.has("enable"+feature)){
								HashMap<String, String> map = new HashMap<String, String>(); 
								if (jObj.has(feature)) jObjFeature = jObj.getJSONObject(feature);
								else if (jObj.has("enable"+feature)) jObjFeature = jObj.getJSONObject("enable"+feature);
								map.put("enabled", jObjFeature.getString("enabled"));
								map.put("number", jObjFeature.getString("number"));
								
								InitialListLoader.getInstance().setFeaturesBusy(map);
							}
							
							feature = "rna";
							if (jObj.has(feature)||jObj.has("enable"+feature)){
								HashMap<String, String> map = new HashMap<String, String>(); 
								if (jObj.has(feature)) jObjFeature = jObj.getJSONObject(feature);
								else if (jObj.has("enable"+feature)) jObjFeature = jObj.getJSONObject("enable"+feature);
								map.put("enabled", jObjFeature.getString("enabled"));
								map.put("number", jObjFeature.getString("number"));
								
								InitialListLoader.getInstance().setFeaturesRna(map);
							}
							
							feature = "unc";
							if (jObj.has(feature)||jObj.has("enable"+feature)){
								HashMap<String, String> map = new HashMap<String, String>(); 
								if (jObj.has(feature)) jObjFeature = jObj.getJSONObject(feature);
								else if (jObj.has("enable"+feature)) jObjFeature = jObj.getJSONObject("enable"+feature);
								map.put("enabled", jObjFeature.getString("enabled"));
								map.put("number", jObjFeature.getString("number"));
								
								InitialListLoader.getInstance().setFeaturesUnc(map);
							}
							
							handler.sendEmptyMessage(4);
						}
						
						if (classRec.equals("disconnect")) {
							if (jObjCurrent.has("type") && jObjCurrent.get("type").equals("force")) {
								handler.sendEmptyMessage(Constants.FORCED_DISCONNECT);
							}
						}
						
					} catch (NullPointerException e) {
						Log.e(LOG_TAG, e.toString());
						e.printStackTrace();
						cancel = true;
						handler.sendEmptyMessage(Constants.JSON_POPULATE_ERROR);
					} catch (IOException e) {
						Log.e(LOG_TAG, "IOException");
						cancel = true;
						handler.sendEmptyMessage(Constants.NO_NETWORK_AVAILABLE);
					} catch (JSONException e) {
						Log.e(LOG_TAG, e.toString());
						cancel = true;
						handler.sendEmptyMessage(Constants.JSON_POPULATE_ERROR);
					}
				}
			};
		};
		thread.start();
	}
	
	protected void updateUserList(List<HashMap<String,String>> usersList, HashMap<String,String> map, String typeMaj) {
		int len = usersList.size();
		for (int i = 0; i<len; i++){
			HashMap<String,String> usersMap = usersList.get(i);
			if (usersMap.containsKey("xivo_userid") && usersMap.get("xivo_userid").equals(map.get("xivo_userid"))){
				if (typeMaj.equals("presence")){
					usersMap.put("stateid", map.get("stateid"));
					usersMap.put("stateid_longname", map.get("stateid_longname"));
					usersMap.put("stateid_color", map.get("stateid_color"));
					InitialListLoader.getInstance().replaceUsersList(i, usersMap);
					break;
				}
				if (typeMaj.equals("phone")){
					if (map.get("xivo_userid").equals(InitialListLoader.getInstance().getXivoId())){
						InitialListLoader.getInstance().putCapaPresenceState("hintstatus_color", map.get("hintstatus_color"));
						InitialListLoader.getInstance().putCapaPresenceState("hintstatus_code", map.get("hintstatus_code"));
						InitialListLoader.getInstance().putCapaPresenceState("hintstatus_longname", map.get("hintstatus_longname"));
						handler.sendEmptyMessage(2);
					}
					
					usersMap.put("hintstatus_color", map.get("hintstatus_color"));
					usersMap.put("hintstatus_code", map.get("hintstatus_code"));
					usersMap.put("hintstatus_longname", map.get("hintstatus_longname"));
					InitialListLoader.getInstance().replaceUsersList(i, usersMap);
					break;
				}
			}
		}
	}
	
	private void updatePhoneChannelStatus(JSONObject jStatus) throws JSONException {
		JSONArray comms = jStatus.getJSONObject("comms").names();
		if (comms == null)
			return;
		for (int j = 0; j < comms.length(); j++) {
			JSONObject comm = jStatus.getJSONObject("comms")
					.getJSONObject(comms.getString(j));
			if (comm != null && comm.has("calleridname") && comm.has("status")) {
				if (useMobile)
					parseCommMobile(comm);
				else
					parseComm(comm);
				InitialListLoader.getInstance().showChannels();
			}
		}
	}
	
	private void parseComm(JSONObject comm) throws JSONException {
		Log.d(LOG_TAG, "parseComm " + comm.toString());
		String thisChannel = comm.has("thischannel") ? comm.getString("thischannel") : null;
		
		if (thisChannel != null) {
			String myNum = InitialListLoader.getInstance().getXivoPhoneNum();
			String status = comm.getString("status");
			String peerChannel = comm.has("peerchannel") ? comm.getString("peerchannel") : null;
			if (myNum != null && thisChannel.contains(myNum)) {
				if (status.equals("linked-caller")) {
					sendOnThePhoneIntent();
					InitialListLoader.getInstance().setThisChannelId(thisChannel);
					if (peerChannel != null)
						InitialListLoader.getInstance().setPeerChannelId(peerChannel);
					else
						InitialListLoader.getInstance().setPeerChannelId(null);
				} else if (status.equals("unlinked-caller") || status.equals("hangup")) {
					resetChannels();
				}
			} else if (myNum != null && status.equals("linked-caller") && peerChannel != null
					&& peerChannel.contains(myNum)) {
				InitialListLoader l = InitialListLoader.getInstance();
				l.setPeersPeerChannelId(comm.getString("peerchannel"));
				l.setThisChannelId(comm.getString("peerchannel"));
				l.setPeerChannelId(comm.getString("thischannel"));
				sendOnThePhoneIntent();
			}
		}
	}
	
	/**
	 * Parses the comm part of an incomming phone status update.
	 * This method is valid only when using a mobile number.
	 * 
	 * @param comm
	 * @throws JSONException
	 */
	private void parseCommMobile(JSONObject comm) throws JSONException {
		Log.d(LOG_TAG, "Parsing comm (mobile): " + comm.toString());
		String status = comm.getString("status");
		String thisChannel = comm.has("thischannel") ? comm.getString("thischannel") : null;
		String peerChannel = comm.has("peerchannel") ? comm.getString("peerchannel") : null;
		int linenum = comm.has("linenum") ? comm.getInt("linenum") : 0;
		
		if (linenum == 1 && peerChannel != null && peerChannel.contains(mobileNumber)
				&& status.equals("ringing")) {
			sendOnThePhoneIntent();
			InitialListLoader l = InitialListLoader.getInstance();
			l.setThisChannelId(peerChannel);
			l.setPeerChannelId(thisChannel);
		} else if (linenum == 2 && status.equals("linked-caller") && thisChannel != null 
				&& thisChannel.equals(InitialListLoader.getInstance().getPeerChannelId())) {
			sendOnThePhoneIntent();
			InitialListLoader.getInstance().setPeersPeerChannelId(peerChannel);
		} else if ((status.equals("unlinked-caller") || status.equals("hangup"))
				&& (peerChannel != null && peerChannel.contains(mobileNumber))) {
			resetChannels();
		}
	}
	
	/**
	 * Sets channels to null and send and hangup broadcast
	 */
	private void resetChannels() {
		Intent iHangup = new Intent();
		iHangup.setAction(Constants.ACTION_HANGUP);
		context.sendBroadcast(iHangup);
		
		InitialListLoader l = InitialListLoader.getInstance();
		l.setThisChannelId(null);
		l.setPeerChannelId(null);
		l.setPeersPeerChannelId(null);
	}
	
	private void sendOnThePhoneIntent() {
		Intent iOffhook = new Intent();
		iOffhook.setAction(Constants.ACTION_OFFHOOK);
		context.sendBroadcast(iOffhook);
	}
	
	public static boolean isCancel() {
		return cancel;
	}
	
	public static void setCancel(boolean cancel) {
		JsonLoopListener.cancel = cancel;
	}
}
