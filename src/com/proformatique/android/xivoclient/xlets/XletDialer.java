package com.proformatique.android.xivoclient.xlets;

import java.io.IOException;
import java.io.PrintStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.Connection;
import com.proformatique.android.xivoclient.JsonLoopListener;
import com.proformatique.android.xivoclient.LoginActivity;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.XletsContainerTabActivity;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.xlets.XletContactSearch.IncomingReceiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class XletDialer extends Activity implements XletInterface{

	private static final String LOG_TAG = "XLET DIALER";
	EditText phoneNumber;
	IncomingReceiver receiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.xlet_dialer);
		phoneNumber = (EditText) findViewById(R.id.number);
		
		receiver = new IncomingReceiver();

		/**
		 *  Register a BroadcastReceiver for Intent action that trigger a call
		 */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_XLET_DIAL_CALL);
        registerReceiver(receiver, new IntentFilter(filter));

	}
	
	public void clickOnCall(View v) {
    	new CallJsonTask().execute();
    }
    
	/**
	 * Creating a AsyncTask to run call process
	 * @author cquaquin
	 */
	 private class CallJsonTask extends AsyncTask<Void, Integer, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {

	    	/**
	    	 * If the user enabled "use_mobile_number" setting, the call takes
	    	 * the mobile number for source phone. 
	    	 */
	    	String mobileNumber = "";
	    	Boolean useMobile;
	    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(XletDialer.this);
	       	useMobile = settings.getBoolean("use_mobile_number",false);
	       	
	       	if (useMobile) 
	       		mobileNumber = settings.getString("mobile_number","");
	    	
	    	/**
	    	 * Creating Call Json object
	    	 */
	    	JSONObject jCalling = createJsonCallingObject("originate", mobileNumber, 
	    			phoneNumber.getText().toString());
			try {
				Log.d( LOG_TAG, "jCalling: " + jCalling.toString());
				PrintStream output = new PrintStream(Connection.getInstance().networkConnection.getOutputStream());
				output.println(jCalling.toString());

				return Constants.OK; 
				
			} catch (IOException e) {
				return Constants.NO_NETWORK_AVAILABLE;
			}
	    	
		}

		@Override
		protected void onPostExecute(Integer result) {
			
			if (result == Constants.OK)
				Toast.makeText(XletDialer.this, R.string.call_ok
						, Toast.LENGTH_LONG).show();
			else Toast.makeText(XletDialer.this, R.string.no_web_connection
					, Toast.LENGTH_LONG).show();

		}

	 }
    
	 /**
	  * Prepare the Json string for calling process
	  * 
	  * @param inputClass
	  * @param phoneNumberSrc
	  * @param phoneNumberDest
	  * @return
	  */
	private JSONObject createJsonCallingObject(String inputClass, 
			String phoneNumberSrc,
			String phoneNumberDest) {
		
		JSONObject jObj = new JSONObject();
		String phoneSrc;
		
		if (phoneNumberSrc == null)
			phoneSrc = "user:special:me";
		else if (phoneNumberSrc.equals(""))
			phoneSrc = "user:special:me";
		else phoneSrc = "ext:"+phoneNumberSrc;
		
		try {
			jObj.accumulate("direction", Constants.XIVO_SERVER);
			jObj.accumulate("class",inputClass);
			jObj.accumulate("source", phoneSrc);
			jObj.accumulate("destination", "ext:"+phoneNumberDest);
			
			return jObj;
		} catch (JSONException e) {
			return null;
		}
	}

	/**
	 * BroadcastReceiver, intercept Intents with action ACTION_XLET_DIAL_CALL
	 * to perform a call
	 * @author cquaquin
	 *
	 */
	public class IncomingReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(Constants.ACTION_XLET_DIAL_CALL)) {
	        	Log.d( LOG_TAG , "Received Broadcast ");
				Bundle extra = intent.getExtras();
		
				if (extra != null){
					XletsContainerTabActivity parentAct;
					phoneNumber.setText(extra.getString("numToCall"));
					parentAct = (XletsContainerTabActivity)XletDialer.this.getParent();
					parentAct.switchTab(0);
					
					new CallJsonTask().execute();
				}

	        }
		}
	}

    public void clickOn1(View v) {
    	phoneNumber.append("1");
    }

    public void clickOn2(View v) {
    	phoneNumber.append("2");
    }
	
    public void clickOn3(View v) {
    	phoneNumber.append("3");
    }

    public void clickOn4(View v) {
    	phoneNumber.append("4");
    }

    public void clickOn5(View v) {
    	phoneNumber.append("5");
    }

    public void clickOn6(View v) {
    	phoneNumber.append("6");
    }

    public void clickOn7(View v) {
    	phoneNumber.append("7");
    }

    public void clickOn8(View v) {
    	phoneNumber.append("8");
    }

    public void clickOn9(View v) {
    	phoneNumber.append("9");
    }

    public void clickOn0(View v) {
    	phoneNumber.append("0");
    }

    public void clickOnStar(View v) {
    	phoneNumber.append("*");
    }

    public void clickOnSharp(View v) {
    	phoneNumber.append("#");
    }

    protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}

}
