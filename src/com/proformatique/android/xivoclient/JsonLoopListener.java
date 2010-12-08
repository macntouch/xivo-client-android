package com.proformatique.android.xivoclient;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

import com.proformatique.android.xivoclient.InitialListLoader.fullNameComparator;
import com.proformatique.android.xivoclient.tools.Constants;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
	public static boolean cancel = false;
	private static JsonLoopListener instance;

	public static JsonLoopListener getInstance(Context context) {
        if (null == instance) {
            instance = new JsonLoopListener(context);
        } else if (cancel == true) {
        	instance.startJsonListener();
        }

        return instance;
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
			jObj.accumulate("userid", InitialListLoader.initialListLoader.astId+"/"+
					InitialListLoader.initialListLoader.xivoId);
			
			PrintStream output = new PrintStream(
					Connection.getInstance().networkConnection.getOutputStream());
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
			jObj.accumulate("peer", InitialListLoader.initialListLoader.astId+"/"+
					InitialListLoader.initialListLoader.xivoId);
			jObj.accumulate("size",elementsNumber);
			jObj.accumulate("mode",mode);
			jObj.accumulate("morerecentthan",sIso.format(c1.getTime()));
			
			PrintStream output = new PrintStream(
					Connection.getInstance().networkConnection.getOutputStream());
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
    		private String jSonObj;

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
       			}
       		} 
       	};

        thread = new Thread() {
        	public void run() {
           		int i = 0;
					while(i < 1) {
						
						if (cancel) break;
						
						try {
							
							JSONObject jObjCurrent = Connection.getInstance().readData();
							String classRec = "";
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
								
								updateUserList(InitialListLoader.initialListLoader.usersList, map, "presence");

								handler.sendEmptyMessage(1);
							}

							if (classRec.equals("phones")) {
								HashMap<String, String> map = new HashMap<String, String>();

								JSONObject jStatus = jObjCurrent.getJSONObject("status");
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
								updateUserList(InitialListLoader.initialListLoader.usersList, map, "phone");
								handler.sendEmptyMessage(1);
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
									InitialListLoader.initialListLoader.historyList.add(map);
								}
								
								/**
								 * Sorting list
								 */
								if (InitialListLoader.initialListLoader.historyList.size()!=0){
									Collections.sort(InitialListLoader.initialListLoader.historyList, new DateComparator());
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
									
									InitialListLoader.initialListLoader.featuresEnablednd = map;
								}

								feature = "callrecord";
								if (jObj.has(feature)){
									HashMap<String, String> map = new HashMap<String, String>(); 
									jObjFeature = jObj.getJSONObject(feature);
									map.put("enabled", jObjFeature.getString("enabled"));
									
									InitialListLoader.initialListLoader.featuresCallrecord = map;
								}

								feature = "incallfilter";
								if (jObj.has(feature)){
									HashMap<String, String> map = new HashMap<String, String>(); 
									jObjFeature = jObj.getJSONObject(feature);
									map.put("enabled", jObjFeature.getString("enabled"));
									
									InitialListLoader.initialListLoader.featuresIncallfilter = map;
								}

								feature = "enablevoicemail";
								if (jObj.has(feature)){
									HashMap<String, String> map = new HashMap<String, String>(); 
									jObjFeature = jObj.getJSONObject(feature);
									map.put("enabled", jObjFeature.getString("enabled"));
									
									InitialListLoader.initialListLoader.featuresEnablevoicemail = map;
								}

								feature = "busy";
								if (jObj.has(feature)||jObj.has("enable"+feature)){
									HashMap<String, String> map = new HashMap<String, String>(); 
									if (jObj.has(feature)) jObjFeature = jObj.getJSONObject(feature);
									else if (jObj.has("enable"+feature)) jObjFeature = jObj.getJSONObject("enable"+feature);
									map.put("enabled", jObjFeature.getString("enabled"));
									map.put("number", jObjFeature.getString("number"));
									
									InitialListLoader.initialListLoader.featuresBusy = map;
								}

								feature = "rna";
								if (jObj.has(feature)||jObj.has("enable"+feature)){
									HashMap<String, String> map = new HashMap<String, String>(); 
									if (jObj.has(feature)) jObjFeature = jObj.getJSONObject(feature);
									else if (jObj.has("enable"+feature)) jObjFeature = jObj.getJSONObject("enable"+feature);
									map.put("enabled", jObjFeature.getString("enabled"));
									map.put("number", jObjFeature.getString("number"));
									
									InitialListLoader.initialListLoader.featuresRna.clear();
									InitialListLoader.initialListLoader.featuresRna = map;
								}

								feature = "unc";
								if (jObj.has(feature)||jObj.has("enable"+feature)){
									HashMap<String, String> map = new HashMap<String, String>(); 
									if (jObj.has(feature)) jObjFeature = jObj.getJSONObject(feature);
									else if (jObj.has("enable"+feature)) jObjFeature = jObj.getJSONObject("enable"+feature);
									map.put("enabled", jObjFeature.getString("enabled"));
									map.put("number", jObjFeature.getString("number"));
									
									InitialListLoader.initialListLoader.featuresUnc = map;
								}

								handler.sendEmptyMessage(4);
							}

           				} catch (NullPointerException e) {
           					cancel = true;
           					handler.sendEmptyMessage(Constants.JSON_POPULATE_ERROR);
           				} catch (IOException e) {
           					cancel = true;
           					handler.sendEmptyMessage(Constants.NO_NETWORK_AVAILABLE);
						} catch (JSONException e) {
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
	    	if (usersMap.get("xivo_userid").equals(map.get("xivo_userid"))){
	    		if (typeMaj.equals("presence")){
		    		usersMap.put("stateid", map.get("stateid"));
		    		usersMap.put("stateid_longname", map.get("stateid_longname"));
		    		usersMap.put("stateid_color", map.get("stateid_color"));
		    		
		    		usersList.set(i, usersMap);
		    		break;
	    		}
	    		if (typeMaj.equals("phone")){
					if (map.get("xivo_userid").equals(InitialListLoader.initialListLoader.xivoId)){
						InitialListLoader.initialListLoader.capaPresenceState.put("hintstatus_color", map.get("hintstatus_color"));
						InitialListLoader.initialListLoader.capaPresenceState.put("hintstatus_code", map.get("hintstatus_code"));
						InitialListLoader.initialListLoader.capaPresenceState.put("hintstatus_longname", map.get("hintstatus_longname"));
						handler.sendEmptyMessage(2);
					}

		    		usersMap.put("hintstatus_color", map.get("hintstatus_color"));
		    		usersMap.put("hintstatus_code", map.get("hintstatus_code"));
		    		usersMap.put("hintstatus_longname", map.get("hintstatus_longname"));
		    		usersList.set(i, usersMap);
		    		break;
	    		}
	    	}
	    	
	    }

	}
	
	class DateComparator implements Comparator
	{
	    @SuppressWarnings("unchecked")
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


}
