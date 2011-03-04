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

package com.proformatique.android.xivoclient.xlets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.SettingsActivity;
import com.proformatique.android.xivoclient.XivoActivity;
import com.proformatique.android.xivoclient.tools.AndroidTools;
import com.proformatique.android.xivoclient.tools.Constants;

public class XletDialer extends XivoActivity {
    
    private static final String LOG_TAG = "XiVO Dialer";
    private final int VM_DISABLED_FILTER = 0xff555555;
    
    private EditText phoneNumber;
    private ImageButton dialButton;
    private IncomingReceiver receiver;
    private Dialog dialog;
    
    private CallTask callTask = null;
    private PhoneStateListener phoneStateListener = null;
    private TelephonyManager telephonyManager = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.xlet_dialer);
        
        phoneNumber = (EditText) findViewById(R.id.number);
        dialButton = (ImageButton) findViewById(R.id.dialButton);
        
        refreshHangupButton();
        
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            phoneNumber.setText(bundle.getString("numToCall"));
            if (!bundle.getBoolean("edit")) {
                callTask = new CallTask();
                callTask.execute();
            }
        }
        
        phoneStateListener = new PhoneStateListener() {
            
            public void onCallStateChanged(int state, String incomingNumber) {
                refreshHangupButton();
            }
        };
        
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        
        receiver = new IncomingReceiver();
        
        /**
         * Register a BroadcastReceiver for Intent action that trigger a call
         */
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_OFFHOOK);
        filter.addAction(Constants.ACTION_MWI_UPDATE);
        filter.addAction(Constants.ACTION_CALL_PROGRESS);
        filter.addAction(Constants.ACTION_ONGOING_CALL);
        registerReceiver(receiver, new IntentFilter(filter));
        
        registerButtons();
    }
    
    @Override
    protected void onBindingComplete() {
        try {
            newVoiceMail(xivoConnectionService.hasNewVoiceMail());
        } catch (RemoteException e) {
            newVoiceMail(false);
        }
        refreshHangupButton();
        super.onBindingComplete();
    }
    
    private boolean isServiceReady() {
        try {
            return xivoConnectionService != null && xivoConnectionService.isAuthenticated();
        } catch (RemoteException e) {
            return false;
        }
    }
    
    private void newVoiceMail(final boolean status) {
        ImageButton vm_button = (ImageButton) findViewById(R.id.voicemailButton);
        vm_button.setEnabled(true);
        if (status) {
            vm_button.setColorFilter(null);
        } else {
            vm_button.setColorFilter(VM_DISABLED_FILTER, PorterDuff.Mode.SRC_ATOP);
        }
    }
    
    /**
     * Hang up a call using the most appropriate method
     */
    private void hangup() {
        if (SettingsActivity.getUseMobile(this)) {
            if (isMobileOffHook()) {
                if (AndroidTools.hangup(this)) return;
            } else {
                return;
            }
        }
        try {
            if (xivoConnectionService != null && xivoConnectionService.isAuthenticated()) {
                if (xivoConnectionService.isOnThePhone()) {
                    xivoConnectionService.hangup();
                }
                return;
            }
        } catch (RemoteException e) {
            // Handled with other failure after the catch
        }
        Log.d(LOG_TAG, "Service not ready to hang-up");
        Toast.makeText(this, getString(R.string.service_not_ready), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Starts a new CallTask
     */
    private void call() {
        if (isServiceReady()) {
            callTask = new CallTask();
            callTask.execute();
        } else {
            Toast.makeText(this, getString(R.string.service_not_ready), Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, "Calling before the service is ready, aborting");
        }
    }
    
    private void transfer() {
        if (isServiceReady()) {
            try {
                xivoConnectionService.transfer(phoneNumber.getText().toString());
                return;
            } catch (RemoteException e) {
                // Checked by isServiceReady
                e.printStackTrace();
            }
        }
        Toast.makeText(this, getString(R.string.service_not_ready), Toast.LENGTH_SHORT).show();
    }
    
    private void atxfer() {
        if (isServiceReady()) {
            try {
                xivoConnectionService.atxfer(phoneNumber.getText().toString());
                return;
            } catch (RemoteException e) {
                // Checked by isServiceReady
                e.printStackTrace();
            }
        }
        Toast.makeText(this, getString(R.string.service_not_ready), Toast.LENGTH_SHORT).show();
    }
    
    public void clickOnCall(View v) {
        if (phoneNumber.getText().toString().equals("")) {
            hangup();
        } else {
            callOrTransfer();
        }
    }
    
    private void callOrTransfer() {
        // Not on the phone, call
        if (SettingsActivity.getUseMobile(this)) {
            if (!isMobileOffHook()) {
                call();
                return;
            }
        } else {
            try {
                if (isServiceReady() && !xivoConnectionService.isOnThePhone()) {
                    call();
                    return;
                }
            } catch (RemoteException e) {
                Toast.makeText(this, getString(R.string.service_not_ready),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // On the phone
        if (!(isServiceReady())) {
            Log.d(LOG_TAG, "Trying to call or transfer before the service is ready");
            Toast.makeText(this, getString(R.string.service_not_ready), Toast.LENGTH_SHORT).show();
            return;
        }
        
        final String num = phoneNumber.getText().toString();
        final String[] menu = new String[] {
                String.format(getString(R.string.transfer_number), num),
                String.format(getString(R.string.atxfer_number), num),
                getString(R.string.hangup), getString(R.string.cancel_label) };
        new AlertDialog.Builder(this).setTitle(getString(R.string.context_action))
                .setItems(menu, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                        case 0:
                            transfer();
                            break;
                        case 1:
                            atxfer();
                            break;
                        case 2:
                            hangup();
                            break;
                        default:
                            Log.d(LOG_TAG, "Canceling");
                            break;
                        }
                    }
                }).show();
    }
    
    /**
     * Set the status of the phone to update the dial button
     * 
     * @param offHook
     */
    public void setPhoneOffHook(final boolean offHook) {
        if (offHook) {
            dialButton.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_dial_action_hangup));
        } else {
            dialButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_dial_action_call));
            ((EditText) findViewById(R.id.number)).setEnabled(true);
            if (dialog != null)
                dialog.dismiss();
        }
    }
    
    /**
     * Check if our phone is Off hook and if we have active channels
     */
    private void refreshHangupButton() {
        if (SettingsActivity.getUseMobile(this)) {
            setPhoneOffHook(isMobileOffHook());
        } else {
            try {
                setPhoneOffHook(xivoConnectionService != null
                        && xivoConnectionService.hasChannels());
            } catch (RemoteException e) {
                setPhoneOffHook(false);
            }
        }
    }
    
    /**
     * Check the phone status returns true if it's off hook
     * 
     * @return
     */
    private boolean isMobileOffHook() {
        if (telephonyManager == null)
            return false;
        switch (telephonyManager.getCallState()) {
        case TelephonyManager.CALL_STATE_OFFHOOK:
        case TelephonyManager.CALL_STATE_RINGING:
            return true;
        default:
            return false;
        }
    }
    
    /**
     * Creating a AsyncTask to run call process
     * 
     * @author cquaquin
     */
    private class CallTask extends AsyncTask<Void, Integer, Integer>
            implements android.content.DialogInterface.OnCancelListener {
        
        public String progress = null;
        private TextView text = null;
        public boolean completeOrCancel = false;
        
        @Override
        protected void onPreExecute() {
            
            progress = getString(R.string.calling, phoneNumber.getText().toString());
            completeOrCancel = false;
            
            phoneNumber.setEnabled(false);
            dialog = new Dialog(XletDialer.this);
            
            dialog.setContentView(R.layout.xlet_dialer_call);
            dialog.setTitle(R.string.calling_title);
            dialog.setOnCancelListener(this);
            
            text = (TextView) dialog.findViewById(R.id.call_message);
            text.setText(progress);
            
            dialog.show();
            
            super.onPreExecute();
        }
        
        @Override
        protected Integer doInBackground(Void... params) {
            int result = Constants.OK;
            try {
                while (xivoConnectionService == null) timer(100);
                xivoConnectionService.call(phoneNumber.getText().toString().replace("(", "")
                        .replace(")", "").replace("-", "").trim());
                while (!completeOrCancel) timer(200);
            } catch (RemoteException e) {
                result = Constants.REMOTE_EXCEPTION;
            }
            return result;
        }
        
        private void timer(int milliseconds) {
            try {
                synchronized (this) {
                    this.wait(milliseconds);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            text.setText(progress);
            super.onProgressUpdate(values);
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            phoneNumber.setEnabled(true);
            switch (result) {
            case Constants.OK:
                break;
            case Constants.REMOTE_EXCEPTION:
                showToast(R.string.remote_exception);
                break;
            }
        }
        
        private void showToast(int messageId) {
            Toast.makeText(XletDialer.this, getString(messageId), Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onCancel(DialogInterface dialog) {
            Log.d(LOG_TAG, "Cancelling the call");
        }
    }
    
    /**
     * BroadcastReceiver, intercept Intents
     * 
     * @author cquaquin
     * 
     */
    private class IncomingReceiver extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Constants.ACTION_OFFHOOK)) {
                Log.d(LOG_TAG, "OffHook action received");
                refreshHangupButton();
                phoneNumber.setEnabled(true);
                phoneNumber.setText("");
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
            } else if (action.equals(Constants.ACTION_MWI_UPDATE)) {
                Log.d(LOG_TAG, "MWI update received");
                int[] mwi = intent.getExtras().getIntArray("mwi");
                newVoiceMail(mwi[0] == 1);
            } else if (action.equals(Constants.ACTION_CALL_PROGRESS)) {
                final String status = intent.getStringExtra("status");
                final String code = intent.getStringExtra("code");
                refreshHangupButton();
                if (code.equals(Constants.CALLING_STATUS_CODE)) {
                    if (dialog != null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                    phoneNumber.setEnabled(true);
                    phoneNumber.setText("");
                }
                if (callTask != null) {
                    if (Integer.parseInt(code) == Constants.HINTSTATUS_AVAILABLE_CODE) {
                        callTask.completeOrCancel = true;
                    }
                    callTask.progress = status;
                    callTask.onProgressUpdate();
                }
            } else if (action.equals(Constants.ACTION_ONGOING_CALL)) {
                refreshHangupButton();
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
                phoneNumber.setEnabled(true);
                phoneNumber.setText("");
            } else if (action.equals(Constants.ACTION_MY_PHONE_CHANGE)) {
                if (intent.getStringExtra("code").equals(Constants.AVAILABLE_STATUS_CODE)) {
                    refreshHangupButton();
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
    
    public void clickOnDelete(View v) {
        keyPressed(KeyEvent.KEYCODE_DEL);
    }
    
    private void keyPressed(int keyCode) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        phoneNumber.onKeyDown(keyCode, event);
    }
    
    public void clickVoiceMail(View v) {
        if (SettingsActivity.getUseMobile(this)) {
            Toast.makeText(this, "Not available when using your mobile number.", Toast.LENGTH_LONG)
                    .show();
        } else {
            ((EditText) findViewById(R.id.number)).setText("*98");
            new CallTask().execute();
        }
    }
    
    @Override
    protected void onResume() {
        if (phoneNumber != null && phoneNumber.getText().toString().equals("")) {
            phoneNumber.setText(SettingsActivity.getLastDialerValue(this));
        }
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        if (phoneNumber != null) {
            phoneNumber.setEnabled(true);
            SettingsActivity.setLastDialerValue(this, phoneNumber.getText().toString());
        }
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        super.onDestroy();
    }
    
    @Override
    protected void setUiEnabled(boolean state) {
        super.setUiEnabled(state);
        if (dialButton != null) {
            dialButton.setEnabled(state);
        }
        if (phoneNumber != null) {
            phoneNumber.setEnabled(state);
        }
    }
}
