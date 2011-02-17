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

package com.proformatique.android.xivoclient.service;

import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
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
	
	// Keep a copy of useMobile and mobileNumber since the config can't be
	// accessed from other threads
	private static boolean useMobile;
	private static String mobileNumber;
	
	public static void setUseMobile(boolean newValue) {
		useMobile = newValue;
	}
	
	public static void setMobileNumber(String number) {
		mobileNumber = number;
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
				//InitialListLoader.getInstance().showChannels();
			}
		}
	}
	
	private void parseComm(JSONObject comm) throws JSONException {
		Log.d(LOG_TAG, "parseComm " + comm.toString());
		String thisChannel = comm.has("thischannel") ? comm.getString("thischannel") : null;
		
		if (thisChannel != null) {
			String myNum = "";
			String status = comm.getString("status");
			String peerChannel = comm.has("peerchannel") ? comm.getString("peerchannel") : null;
			if (myNum != null && thisChannel.contains(myNum)) {
				if (status.equals("linked-caller")) {
					sendOnThePhoneIntent();
					/*
					InitialListLoader.getInstance().setThisChannelId(thisChannel);
					if (peerChannel != null)
						InitialListLoader.getInstance().setPeerChannelId(peerChannel);
					else
						InitialListLoader.getInstance().setPeerChannelId(null);
						*/
				} else if (status.equals("unlinked-caller") || status.equals("hangup")) {
					resetChannels();
				}
			} else if (myNum != null && status.equals("linked-caller") && peerChannel != null
					&& peerChannel.contains(myNum)) {
				/*
				InitialListLoader l = InitialListLoader.getInstance();
				l.setPeersPeerChannelId(comm.getString("peerchannel"));
				l.setThisChannelId(comm.getString("peerchannel"));
				l.setPeerChannelId(comm.getString("thischannel"));
				*/
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
		String calleridnum = comm.has("calleridnum") ? comm.getString("calleridnum")  : null;
		int linenum = comm.has("linenum") ? comm.getInt("linenum") : 0;
		
		if (linenum == 1 && calleridnum != null && calleridnum.equals(mobileNumber)
				&& status.equals("ringing")) {
			sendOnThePhoneIntent();
			/*
			InitialListLoader l = InitialListLoader.getInstance();
			l.setPeerChannelId(thisChannel);
			l.setPeersPeerChannelId(peerChannel);
			*/
		} else if (linenum == 2 && status.equals("linked-caller") && thisChannel != null 
				/*&& thisChannel.equals(InitialListLoader.getInstance().getPeerChannelId())*/) {
			sendOnThePhoneIntent();
			//InitialListLoader.getInstance().setPeersPeerChannelId(peerChannel);
		} else if ((status.equals("unlinked-caller") || status.equals("hangup"))
				&& (peerChannel != null && peerChannel.contains(mobileNumber)
				|| calleridnum != null && calleridnum.equals(mobileNumber))) {
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
		/*
		InitialListLoader l = InitialListLoader.getInstance();
		l.setThisChannelId(null);
		l.setPeerChannelId(null);
		l.setPeersPeerChannelId(null);
		*/
	}
	
	private void sendOnThePhoneIntent() {
		Intent iOffhook = new Intent();
		iOffhook.setAction(Constants.ACTION_OFFHOOK);
		context.sendBroadcast(iOffhook);
	}
}
