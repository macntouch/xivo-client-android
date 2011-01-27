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

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity {
	
	private final static double KB = Math.pow(2, 10);
	private final static double MB = Math.pow(2, 20);
	private final static double GB = Math.pow(2, 30);
	private final static double TB = Math.pow(2, 40);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		
		TextView bandwidth = (TextView) findViewById(R.id.bandwidth_received);
		TextView unit = (TextView) findViewById(R.id.bandwidth_unit);
		
		long received = 0L;
		
		if (Connection.getInstance().isConnected())
			received = Connection.getInstance().getReceivedBytes();
		
		if (received == 0) {
			bandwidth.setText(Long.toString(received));
			unit.setText(R.string.unit_byte);
		} else if (received <= KB) {
			bandwidth.setText(Long.toString(received));
			unit.setText(R.string.unit_byte + "s");
		} else if (received <= MB) {
			bandwidth.setText(String.format("%.2f", received / KB));
			unit.setText(R.string.unit_kb);
		} else if (received <= GB) {
			bandwidth.setText(String.format("%.2f", received / MB));
			unit.setText(R.string.unit_mb);
		} else if (received <= TB) {
			bandwidth.setText(String.format("%.2f", received / GB));
			unit.setText(R.string.unit_gb);
		} else {
			bandwidth.setText(String.format("%.2f", received / TB));
			unit.setText(R.string.unit_tb);
		}
	}
}
