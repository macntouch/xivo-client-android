package com.proformatique.android.xivoclient;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import com.proformatique.android.xivoclient.xlets.XletIdentity;

public class XletsContainerTabActivity extends TabActivity {
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.xlets_container);

	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    intent = new Intent().setClass(this, XletIdentity.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("identity").setIndicator("Identity").setContent(intent);
	    tabHost.addTab(spec);

	    spec = tabHost.newTabSpec("xlet2").setIndicator("Xlet 2").setContent(intent);
        tabHost.addTab(spec);

	    spec = tabHost.newTabSpec("xlet3").setIndicator("Xlet 3").setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
        
//        tabHost.getTabWidget().getChildAt(0).getLayoutParams().height = ;


	}
}
