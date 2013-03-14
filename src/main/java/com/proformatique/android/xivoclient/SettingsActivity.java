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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.proformatique.android.xivoclient.service.ShortcutProvider;
import com.proformatique.android.xivoclient.tools.AndroidTools;
import com.proformatique.android.xivoclient.tools.Constants;

public class SettingsActivity extends PreferenceActivity {
    
    private final static String TAG = "XiVO settings";
    
    private final static String USE_MOBILE_OPTION = "use_mobile_number";
    private final static String START_ON_BOOT = "start_on_boot";
    private final static String ALWAYS_CONNECTED = "always_connected";
    private final static boolean USE_MOBILE_DEFAULT = false;
    private static final String MOBILE_PHONE_NUMBER = "mobile_number";
    private static final String DEFAULT_MOBILE_PHONE_NUMBER = "";
    private static final String LAST_DIALER_VALUE = "last_dialer_value";
    
    SharedPreferences settingsPrefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        settingsPrefs = getPreferenceManager().getSharedPreferences();
        addPreferencesFromResource(R.xml.settings);
        
        if (settingsPrefs.getBoolean("use_fullscreen", false)) {
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        
        /**
         * Init value for mobile number
         */
        if (settingsPrefs.getString("mobile_number", "").equals("")) {
            
            /**
             * TODO : Check that default value is visible when no data exists in
             * EditText field
             */
            TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(
                    Context.TELEPHONY_SERVICE);
            String mobileNumber = tMgr.getLine1Number();
            SharedPreferences.Editor editor = settingsPrefs.edit();
            editor.putString("mobile_number", mobileNumber);
            
            editor.commit();
        }
        
        /**
         * This Listener will trigger when users disable the "save_login"
         * parameter, so the app can erase previously saved login and password
         * 
         */
        settingsPrefs
                .registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
                    
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                            String key) {
                        
                        if (key.equals("save_login")) {
                            Boolean saveLogin = sharedPreferences.getBoolean(key, true);
                            
                            if (!saveLogin) {
                                
                                SharedPreferences loginSettings;
                                loginSettings = getSharedPreferences("login_settings", 0);
                                
                                SharedPreferences.Editor editor = loginSettings.edit();
                                
                                editor.putString("login", "");
                                editor.putString("password", "");
                                editor.commit();
                                
                            }
                        }
                        Intent prefChanged = new Intent();
                        prefChanged.setAction(Constants.ACTION_SETTINGS_CHANGE);
                        sendBroadcast(prefChanged);
                    }
                });
    }
    
    /**
     * Returns the use_mobile_number preference value.
     * Check if the use_mobile_number is true or false and make sure a mobile number has been set
     * if use_mobile_number is true.
     * 
     * @param context
     * @return true if use_mobile_number is set and a mobile number has been set else false
     * @throws NullPointerException if context is null
     */
    public static boolean getUseMobile(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(USE_MOBILE_OPTION, USE_MOBILE_DEFAULT)
                && prefs.getString(MOBILE_PHONE_NUMBER, "").equals("") == false;
    }
    
    /**
     * Returns the mobile phone number or null if use_mobile_number is not true
     * 
     * @param context
     * @return
     */
    public static String getMobileNumber(Context context) {
        if (getUseMobile(context)) {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(
                    MOBILE_PHONE_NUMBER, DEFAULT_MOBILE_PHONE_NUMBER);
        } else {
            return null;
        }
    }
    
    /**
     * Returns the default context
     */
    public static String getXivoContext(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("context",
                Constants.XIVO_CONTEXT);
    }
    
    /**
     * Returns the Xivo login
     */
    public static String getLogin(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("login", "");
    }
    
    public static boolean getStartOnBoot(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(START_ON_BOOT,
                false);
    }
    
    public static boolean getAlwaysConnected(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(ALWAYS_CONNECTED,
                false);
    }
    
    /**
     * Returns the list of shortcuts
     * 
     * @param context
     * @return
     */
    public static List<HashMap<String, ?>> getShortcuts(Context context) {
        Cursor c = context.getContentResolver().query(ShortcutProvider.CONTENT_URI, null, null,
                null, ShortcutProvider.NAME);
        int size = c != null ? c.getCount() : 0;
        if (size > 0) {
            List<HashMap<String, ?>> list = new ArrayList<HashMap<String, ?>>(size);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                HashMap<String, Object> item = new HashMap<String, Object>();
                String packageName = c.getString(c.getColumnIndex(ShortcutProvider.PACKAGE));
                item.put("package", packageName);
                item.put("name", c.getString(c.getColumnIndex(ShortcutProvider.NAME)));
                item.put("icon", AndroidTools.getPackageIcon(context, packageName));
                list.add(item);
                c.moveToNext();
            }
            c.close();
            return list;
        }
        c.close();
        return null;
    }
    
    /**
     * Returns the number of shortcuts available
     * 
     * @param context
     * @return
     */
    public static int getNbShortcuts(Context context) {
        Cursor c = context.getContentResolver().query(ShortcutProvider.CONTENT_URI, null, null,
                null, null);
        int res = c != null ? c.getCount() : 0;
        c.close();
        return res;
    }
    
    /**
     * Adds a shortcut to ShortcutProvider for persistence
     * 
     * @param context
     * @param packageName
     */
    public static void addShortcut(Context context, String packageName) {
        Log.d(TAG, "Adding the following shortcut " + packageName);
        if (hasShortcut(context, packageName)) {
            Toast.makeText(context, context.getString(R.string.shortcut_already_present),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        ContentValues values = new ContentValues();
        values.put(ShortcutProvider.NAME, AndroidTools.getPackageLabel(context, packageName));
        values.put(ShortcutProvider.PACKAGE, packageName);
        context.getContentResolver().insert(ShortcutProvider.CONTENT_URI, values);
        values.clear();
    }
    
    /**
     * Removes a shortcut from the ShortcutProvider
     * 
     * @param context
     * @param packageName
     */
    public static void removeShortcut(Context context, String packageName) {
        Log.d(TAG, "Deleting the following shortcut: " + packageName);
        context.getContentResolver().delete(ShortcutProvider.CONTENT_URI,
                ShortcutProvider.PACKAGE + " = '" + packageName + "'", null);
    }
    
    /**
     * Check if the ShortcutProvider already contains a given package
     * 
     * @param context
     * @param packageName
     * @return
     */
    private static boolean hasShortcut(Context context, String packageName) {
        Cursor c = context.getContentResolver().query(ShortcutProvider.CONTENT_URI, null,
                ShortcutProvider.PACKAGE + " = '" + packageName + "'", null, null);
        boolean res = c.getCount() > 0;
        c.close();
        return res;
    }
    
    /**
     * Saves the last number that was on the dialer to restore it on the next
     * onResume
     * 
     * @param context
     * @param number
     */
    public static void setLastDialerValue(Context context, String number) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(LAST_DIALER_VALUE,
                number).commit();
    }
    
    /**
     * Retrieves the last dialer phone number that was present on the
     * phoneNumber field of the dialer when onPause was called
     * 
     * @param context
     * @return number
     */
    public static String getLastDialerValue(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(LAST_DIALER_VALUE,
                "");
    }
}
