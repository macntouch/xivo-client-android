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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.proformatique.android.xivoclient.service.CapaxletsProvider;
import com.proformatique.android.xivoclient.tools.AndroidTools;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.xlets.XletContactSearch;
import com.proformatique.android.xivoclient.xlets.XletDialer;
import com.proformatique.android.xivoclient.xlets.XletHisto;
import com.proformatique.android.xivoclient.xlets.XletServices;

public class HomeActivity extends XivoActivity
    implements OnItemClickListener, OnItemLongClickListener {
    
    /**
     * Constants
     */
    private static final String LOG_TAG = "XiVO Home";
    
    /**
     * UI
     */
    private GridView grid;
    private Handler handler = new Handler();
    
    /*
     * xlets
     */
    private List<String> availXlets = null;
    private List<HashMap<String, ?>> shortcuts = null;
    private List<String> implementedXlets = null;
    private XletsAdapter xletsAdapter = null;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_LOAD_XLETS)) {
                xletsAdapter.updateAvailableXlets();
            }
        }
    };
    private XletObserver observer = null;
    
    /**
     * Activity life cycle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler(new FileDumpExceptionHandler());
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "onCreate");
        setContentView(R.layout.home_activity);
        super.registerButtons(); // Set onClickListeners for the XivoActivity
        
        /*
         * Setup the grid, it's adapter, observer and listener
         */
        grid = (GridView) findViewById(R.id.grid);
        grid.setOnItemClickListener(this);
        grid.setOnItemLongClickListener(this);
        grid.setEmptyView(findViewById(android.R.id.empty));
        
        observer = new XletObserver();
        getContentResolver().registerContentObserver(CapaxletsProvider.CONTENT_URI, true, observer);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_LOAD_XLETS);
        registerReceiver(receiver, filter);
        
        handler.post(updateGrid);
    }
    
    @Override
    public void onBackPressed() {
        if (!SettingsActivity.getAlwaysConnected(this)) {
            if (xivoConnectionService != null) {
                try {
                    xivoConnectionService.disconnect();
                } catch (RemoteException e) {
                    Log.d(LOG_TAG, "Remote exception onBackPressed");
                    Toast.makeText(this, R.string.disconnect_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }
        super.onBackPressed();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "onResume");
        startInCallScreenKiller(this);
        handler.post(updateGrid);
    }
    
    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "DESTROY");
        stopInCallScreenKiller(this);
        unregisterReceiver(receiver);
        getContentResolver().unregisterContentObserver(observer);
        super.onDestroy();
    }
    
    /**
     * A runnable to be used from non UI thread to update the Grid Use
     * handler.post(upgradeGrid)
     */
    final Runnable updateGrid = new Runnable() {
        
        public void run() {
            if (xletsAdapter == null) {
                xletsAdapter = new XletsAdapter();
                grid.setAdapter(xletsAdapter);
            }
            xletsAdapter.updateAvailableXlets();
            xletsAdapter.notifyDataSetChanged();
        }
    };
    
    /**
     * InCallScreenKiller service lifecycle
     */
    public static void startInCallScreenKiller(Context context) {
        Intent inCallScreenKillerIntent = new Intent();
        inCallScreenKillerIntent.setClassName(Constants.PACK, InCallScreenKiller.class.getName());
        context.startService(inCallScreenKillerIntent);
        Log.d(LOG_TAG, "InCallScreenKiller started");
    }
    
    public static void stopInCallScreenKiller(Context context) {
        Intent inCallScreenKillerIntent = new Intent();
        inCallScreenKillerIntent.setClassName(Constants.PACK, InCallScreenKiller.class.getName());
        context.stopService(inCallScreenKillerIntent);
        Log.d(LOG_TAG, "InCallScreenKilled stopped");
    }
    
    private class XletsAdapter extends BaseAdapter {
        
        public XletsAdapter() {
            // Add more xlets here
            implementedXlets = new ArrayList<String>(1);
            implementedXlets.add("dial");
            implementedXlets.add("search");
            implementedXlets.add("history");
            implementedXlets.add("features");
            
            updateAvailableXlets();
        }
        
        /**
         * Retrieves the list of xlets from the content provider
         */
        public void updateAvailableXlets() {
            shortcuts = SettingsActivity.getShortcuts(HomeActivity.this);
            if (shortcuts != null)
                Log.d(LOG_TAG, "Shortcuts size: " + shortcuts.size());
            else
                Log.d(LOG_TAG, "No shortcuts");
            Uri allXlets = CapaxletsProvider.CONTENT_URI;
            Cursor c = managedQuery(allXlets, null, null, null, null);
            availXlets = new ArrayList<String>(c.getCount());
            if (c.moveToFirst()) {
                do {
                    String incomingXlet = c.getString(c.getColumnIndex(CapaxletsProvider.XLET));
                    Log.d(LOG_TAG,"xlet ..." + incomingXlet);
                    // Only add xlets that are implemented
                    if (implementedXlets.contains(incomingXlet))
                        availXlets.add(incomingXlet);
                } while (c.moveToNext());
            }
            c.close();
        }
        
        @Override
        public int getCount() {
            return (availXlets == null ? 0 : availXlets.size())
                    + SettingsActivity.getNbShortcuts(HomeActivity.this) + 1;
        }
        
        @Override
        public Object getItem(int position) {
            return null;
        }
        
        @Override
        public long getItemId(int position) {
            return 0;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                LayoutInflater li = getLayoutInflater();
                v = li.inflate(R.layout.icon, null);
            } else {
                v = convertView;
            }
            TextView tv = (TextView) v.findViewById(R.id.icon_text);
            ImageView iv = (ImageView) v.findViewById(R.id.icon_image);
            
            if (position < availXlets.size()) {
                if (availXlets.get(position).equals("dial")) {
                    tv.setText(getString(R.string.dialer_btn_lbl));
                    iv.setImageResource(R.drawable.ic_menu_call);
                } else if (availXlets.get(position).equals("search")) {
                    tv.setText(getString(R.string.userslist_btn_lbl));
                    iv.setImageResource(R.drawable.ic_menu_friendslist);
                } else if (availXlets.get(position).equals("history")) {
                    tv.setText(getString(R.string.history_btn_lbl));
                    iv.setImageResource(R.drawable.ic_menu_recent_history);
                } else if (availXlets.get(position).equals("features")) {
                    tv.setText(getString(R.string.service_btn_lbl));
                    iv.setImageResource(R.drawable.ic_menu_manage);
                }
            } else if (shortcuts != null && position < shortcuts.size() + availXlets.size()) {
                int index = position - availXlets.size();
                if (index < 0  || index >= shortcuts.size()) return v;
                try {
                    tv.setText((String) shortcuts.get(index).get("name"));
                    iv.setImageDrawable((Drawable) shortcuts.get(index).get("icon"));
                } catch (ClassCastException e) {
                    Log.d(LOG_TAG, "Class cast exception on shortcut at position " + position);
                }
            } else {
                tv.setText("");
                iv.setImageResource(R.drawable.empty);
            }
            return v;
        }
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        launch(position);
    }
    
    /**
     * An item has been selected on the list
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        final String[] menuItems = getMenuItems(position);
        new AlertDialog.Builder(this)
        .setTitle(getString(R.string.context_action))
        .setItems(menuItems,
                new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selected = menuItems[which];
                        if (selected.equals(getString(R.string.add_menu_title))) {
                            addLauncher();
                        } else if (selected.equals(getString(R.string.launch_menu_title))) {
                            launch(position);
                        } else if (selected.equals(getString(R.string.del_menu_title))) {
                            delLauncher(position);
                        }
                    }
                }).show();
        return true;
    }
    
    private void addLauncher() {
        startActivityForResult(
                new Intent(this, ApplicationPicker.class), Constants.CODE_ADD_APPLICATION);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "Activity result received");
        if (requestCode == Constants.CODE_ADD_APPLICATION && resultCode == RESULT_OK) {
            String packageName = data.getStringExtra("package");
            if (packageName != null && !packageName.equals("")) {
                SettingsActivity.addShortcut(this, packageName);
                handler.post(updateGrid);
            }
        }
    }
    
    private void delLauncher(int position) {
        int shortcutIndex = position - availXlets.size();
        if (shortcuts != null) {
            String packageName = (String) shortcuts.get(shortcutIndex).get("package");
            SettingsActivity.removeShortcut(this, packageName);
            handler.post(updateGrid);
        }
    }
    
    private void launch(int position) {
        Log.d(LOG_TAG, "Launch " + position);
        if (position >= availXlets.size() + SettingsActivity.getNbShortcuts(this)) {
            Log.d(LOG_TAG, "Empty item, skipping");
            return;
        } else if (position >= availXlets.size() && shortcuts != null) {
            int index = position - availXlets.size();
            try {
                String packageName = (String) shortcuts.get(index).get("package");
                AndroidTools.startApp(this, packageName);
            } catch (ClassCastException e) {
                Log.d(LOG_TAG, "Failled to start the selected application, package is not a string");
            } catch (NullPointerException e) {
                Log.d(LOG_TAG, "Failed to start the selected application, null shortcuts");
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.uninstalled_error, Toast.LENGTH_LONG).show();
            }
        } else {
            String choice = availXlets.get(position);
            Log.d(LOG_TAG, "Clicked " + choice);
            if (choice.equals("dial")) {
                Intent i = new Intent(this, XletDialer.class);
                startActivity(i);
            } else if (choice.equals("features")) {
                Intent i = new Intent(this, XletServices.class);
                startActivity(i);
            } else if (choice.equals("search")) {
                Intent i = new Intent(this, XletContactSearch.class);
                startActivity(i);
            } else if (choice.equals("history")) {
                startActivity(new Intent(this, XletHisto.class));
            } else {
                Log.d(LOG_TAG, "Unhandled click");
            }
        }
    }
    
    /**
     * Returns an array of Strings containing the long click menu
     * @param position
     * @return menu items
     */
    private String[] getMenuItems(int position) {
        int menuSize = 1;
        final int nbShortcut = SettingsActivity.getNbShortcuts(this);
        if (position < availXlets.size() + nbShortcut) menuSize++;
        if (position >= availXlets.size() && position < availXlets.size() + nbShortcut) menuSize++;
        final String[] menuAdd = {
                getString(R.string.add_menu_title)};
        final String[] menuAddLaunch = {
                getString(R.string.launch_menu_title),
                getString(R.string.add_menu_title)};
        final String[] menuAddLaunchDel = {
                getString(R.string.launch_menu_title),
                getString(R.string.add_menu_title),
                getString(R.string.del_menu_title)};
        switch(menuSize) {
        case 2:
            return menuAddLaunch;
        case 3:
            return menuAddLaunchDel;
        default:
            return menuAdd;
        }
    }
    
    private class XletObserver extends android.database.ContentObserver {
        
        public XletObserver() {
            super(null);
        }
        
        @Override
        public void onChange(boolean selfChange) {
            Log.d(LOG_TAG, "Xlets changed");
            super.onChange(selfChange);
            handler.post(updateGrid);
        }
    }
}
