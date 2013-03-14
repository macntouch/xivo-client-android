package com.proformatique.android.xivoclient.tools;

import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

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
    }
    
    /**
     * Shows an Activity over the Android dialer when receiving a call.
     * 
     * @param context
     * @param cls
     *            - The activity to start
     * @param wait
     *            - Time to wait before starting the new activity after
     *            answering
     */
    public static void showOverDialer(Context context, Class<?> cls, long wait) {
        Intent i = new Intent(context, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (wait > 0L) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Log.d(TAG, "Interrupted before killing hiding the Android dialer");
                e.printStackTrace();
            }
        }
        final KeyguardManager.KeyguardLock kmkl = ((KeyguardManager) context
                .getSystemService(Context.KEYGUARD_SERVICE)).newKeyguardLock("kCaller");
        kmkl.disableKeyguard();
        context.startActivity(i);
    }
    
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
        }
        return false;
    }
    
    /**
     * Returns the label of a package
     * 
     * @param context
     * @param packageName
     * @return Application name or null
     */
    public static String getPackageLabel(Context context, String packageName) {
        if (packageName == null || packageName.equals(""))
            return null;
        final PackageManager pm = context.getPackageManager();
        Intent launchIntent = new Intent(Intent.ACTION_MAIN, null);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = pm.queryIntentActivities(launchIntent, 0);
        for (ResolveInfo info : apps) {
            if (info.activityInfo.packageName.equals(packageName)) {
                return (String) info.loadLabel(pm);
            }
        }
        return null;
    }
    
    /**
     * Returns the icon of a package
     * 
     * @param context
     * @param packageName
     * @return The icon or null
     */
    public static Drawable getPackageIcon(Context context, String packageName) {
        if (packageName == null || packageName.equals(""))
            return null;
        final PackageManager pm = context.getPackageManager();
        Intent launchIntent = new Intent(Intent.ACTION_MAIN, null);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = pm.queryIntentActivities(launchIntent, 0);
        for (ResolveInfo info : apps) {
            if (info.activityInfo.packageName.equals(packageName)) {
                return (Drawable) info.loadIcon(pm);
            }
        }
        return null;
    }
    
    /**
     * Start another application
     * 
     * @param context
     * @param packageName
     * @throws ActivityNotFoundException
     */
    public static void startApp(Context context, String packageName)
            throws ActivityNotFoundException {
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            Intent starter = pm.getLaunchIntentForPackage(packageName);
            if (starter != null) {
                context.startActivity(starter);
            } else {
                throw new ActivityNotFoundException("Null intent while starting activity");
            }
        } else {
            throw new ActivityNotFoundException("Failed to retrieve the package manager");
        }
    }
}
