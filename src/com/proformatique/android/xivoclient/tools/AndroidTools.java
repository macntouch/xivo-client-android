package com.proformatique.android.xivoclient.tools;

import android.content.Context;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Collection of helper functions to get around the Android API
 * 
 * functions should be removed the a new version of the official API implements
 * them.
 * 
 */
public class AndroidTools {
    
    private final static String TAG = "XiVO android tools";
    
    private AndroidTools() {
    };
    
    /**
     * Hang-up the phone
     * 
     * This is not part of the official API, it may not work on some phones or
     * could be removed from future versions of Android. The return value should
     * always be checked.
     * 
     * @param context
     * @return true if successful false if failed
     */
    public static boolean hangup(Context context) {
        Log.d(TAG, "Trying to hangup");
        try {
            // Get the private getITelephone method from the TelephoneManager
            // class
            Method iTelephonyGetter = TelephonyManager.class.getDeclaredMethod("getITelephony",
                    null);
            // Make the method "instance" accessible
            if (iTelephonyGetter != null) {
                iTelephonyGetter.setAccessible(true);
            } else {
                Log.d(TAG, "The getITelephony method does not seem to exist for this build");
                return false;
            }
            // Get an instance of our iTelephony object
            com.android.internal.telephony.ITelephony iTelephonyInstance = 
                (com.android.internal.telephony.ITelephony) iTelephonyGetter
                    .invoke((TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE),
                            null);
            // Call it
            if (iTelephonyInstance != null) {
                return iTelephonyInstance.endCall();
            } else {
                Log.d(TAG, "Could not retrieve the Telephony interface");
                return false;
            }
        } catch (SecurityException e) {
            Log.d(TAG, "Security exception while accessing the TelephonyManager. "
                    + "Add CALL_PHONE to your manifest");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "The getITelephony method does not seem to exist for this build");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "A method signature changed. Check the endCall and getITelephony params");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.d(TAG, "The called method is private, make sure you change it's accessibility");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.d(TAG, "This TelephonyManager cannot use endCall");
            e.printStackTrace();
        } catch (RemoteException e) {
            Log.d(TAG, "Cannot reach the TelephonyManager");
            e.printStackTrace();
        }
        return false;
    }
}
