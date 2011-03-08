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

import java.util.Arrays;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AboutActivity extends ListActivity {
    
    private String[] items;
    
    private final static double KB = Math.pow(2, 10);
    private final static double MB = Math.pow(2, 20);
    private final static double GB = Math.pow(2, 30);
    private final static double TB = Math.pow(2, 40);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        long received = i.getLongExtra("received_data", -1L);
        setContentView(R.layout.about);
        
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.bandwidth_in_label)).append(" ");
        if (received == -1L) {
            sb.append(getString(R.string.unknown));
        } else if (received == 0) {
            sb.append(received).append(" ").append(getString(R.string.unit_byte));
        } else if (received <= KB) {
            sb.append(received).append(" ").append(getString(R.string.unit_byte)).append("s");
        } else if (received <= MB) {
            sb.append(String.format("%.2f ", received / KB)).append(getString(R.string.unit_kb));
        } else if (received <= GB) {
            sb.append(String.format("%.2f ", received / MB)).append(getString(R.string.unit_mb));
        } else if (received <= TB) {
            sb.append(String.format("%.2f ", received / GB)).append(getString(R.string.unit_gb));
        } else {
            sb.append(String.format("%.2f ", received / TB)).append(getString(R.string.unit_tb));
        }
        
        items = new String[] {
                getString(R.string.credits) + " " + getString(R.string.version),
                getString(R.string.copyright),
                getString(R.string.credits_part1),
                getString(R.string.gpl),
                sb.toString()
        };
        
        this.setListAdapter(new AboutAdapter(Arrays.asList(items)));
    }
    
    private class AboutAdapter extends BaseAdapter {
        
        private List<String> list;
        
        public AboutAdapter(List<String> list) {
            this.list = list;
        }
        
        @Override
        public int getCount() {
            return list == null ? 0 : list.size();
        }
        
        @Override
        public Object getItem(int position) {
            return list.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                LayoutInflater li = getLayoutInflater();
                v = li.inflate(R.layout.about_row, null);
            } else {
                v = convertView;
            }
            TextView text = (TextView) v.findViewById(R.id.about_row_content);
            text.setText(list.get(position));
            return v;
        }
    }
}
