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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

import com.proformatique.android.xivoclient.service.CapaxletsProvider;
import com.proformatique.android.xivoclient.tools.Constants;
import com.proformatique.android.xivoclient.xlets.XletContactSearch;
import com.proformatique.android.xivoclient.xlets.XletDialer;
import com.proformatique.android.xivoclient.xlets.XletHisto;
import com.proformatique.android.xivoclient.xlets.XletServices;

public class HomeActivity extends XivoActivity implements OnItemClickListener {
    
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
        xletsAdapter = new XletsAdapter();
        grid.setAdapter(xletsAdapter);
        grid.setOnItemClickListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_LOAD_XLETS);
        registerReceiver(receiver, filter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "onResume");
        startInCallScreenKiller(this);
        xletsAdapter.notifyDataSetChanged();
    }
    
    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "DESTROY");
        stopInCallScreenKiller(this);
        if (!SettingsActivity.getKeepRunning(this)) {
            Log.d(LOG_TAG, "Stoping XiVO connection service");
            stopXivoConnectionService();
            Connection.INSTANCE.releaseService();
        }
        unregisterReceiver(receiver);
        super.onDestroy();
    }
    
    /**
     * A runnable to be used from non UI thread to update the Grid Use
     * handler.post(upgradeGrid)
     */
    final Runnable updateGrid = new Runnable() {
        
        public void run() {
            if (xletsAdapter != null) {
                xletsAdapter.notifyDataSetChanged();
            }
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
            Uri allXlets = CapaxletsProvider.CONTENT_URI;
            Cursor c = managedQuery(allXlets, null, null, null, null);
            availXlets = new ArrayList<String>(c.getCount());
            if (c.moveToFirst()) {
                do {
                    String incomingXlet = c.getString(c.getColumnIndex(CapaxletsProvider.XLET));
                    int index;
                    if ((index = incomingXlet.indexOf("-")) != -1) {
                        incomingXlet = incomingXlet.substring(0, index);
                    }
                    // Only add xlets that are implemented
                    if (implementedXlets.contains(incomingXlet))
                        availXlets.add(incomingXlet);
                } while (c.moveToNext());
            }
            c.close();
            handler.post(updateGrid);
        }
        
        @Override
        public int getCount() {
            return availXlets == null ? 0 : availXlets.size();
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
            
            if (position >= availXlets.size()) {
                Log.d(LOG_TAG, "Tried to acces an xlet over the array size");
            } else if (availXlets.get(position).equals("dial")) {
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
            } else {
                tv.setText("...");
                iv.setImageResource(R.drawable.icon);
            }
            return v;
        }
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
