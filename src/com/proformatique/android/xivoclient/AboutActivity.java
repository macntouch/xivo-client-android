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

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class AboutActivity extends XivoActivity {
    
    private final static String TAG = "XiVO About";
    
    private final static double KB = Math.pow(2, 10);
    private final static double MB = Math.pow(2, 20);
    private final static double GB = Math.pow(2, 30);
    private final static double TB = Math.pow(2, 40);
    
    private TextView mBandwidth;
    private TextView mUnit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        super.registerButtons();
        
        mBandwidth = (TextView) findViewById(R.id.bandwidth_received);
        mUnit = (TextView) findViewById(R.id.bandwidth_unit);
    }
    
    @Override
    protected void onBindingComplete() {
        super.onBindingComplete();
        
        long received = -1L;
        
        try {
            if (xivoConnectionService != null)
                received = xivoConnectionService.getReceivedBytes();
        } catch (Exception e) {
            Log.d(TAG, "Could not retrieve bandwidth, using default value");
        }
        
        if (received == -1L) {
            mBandwidth.setText(getString(R.string.unknown));
            mUnit.setText("");
        } else if (received == 0) {
            mBandwidth.setText(Long.toString(received));
            mUnit.setText(R.string.unit_byte);
        } else if (received <= KB) {
            mBandwidth.setText(Long.toString(received));
            mUnit.setText(R.string.unit_byte + "s");
        } else if (received <= MB) {
            mBandwidth.setText(String.format("%.2f", received / KB));
            mUnit.setText(R.string.unit_kb);
        } else if (received <= GB) {
            mBandwidth.setText(String.format("%.2f", received / MB));
            mUnit.setText(R.string.unit_mb);
        } else if (received <= TB) {
            mBandwidth.setText(String.format("%.2f", received / GB));
            mUnit.setText(R.string.unit_gb);
        } else {
            mBandwidth.setText(String.format("%.2f", received / TB));
            mUnit.setText(R.string.unit_tb);
        }
    }
}
