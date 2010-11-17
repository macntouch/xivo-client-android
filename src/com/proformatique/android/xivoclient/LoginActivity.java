package com.proformatique.android.xivoclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.menu_settings:
            menuSettings();
            return true;
        case R.id.menu_exit:
            menuExit();
            return true;
        case R.id.menu_about:
            menuAbout();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

	private void menuAbout() {
		Intent defineIntent = new Intent(this, AboutActivity.class);
		startActivity(defineIntent);
	}

	private void menuExit() {
		finish();
	}

	private void menuSettings() {
		Intent defineIntent = new Intent(this, SettingsActivity.class);
		startActivity(defineIntent);
		
	}
	
    public void clickOnButtonOk(View v) {
    	EditText eLogin = (EditText) findViewById(R.id.login); 
    	EditText ePassword = (EditText) findViewById(R.id.password); 
    	
		Connection connection = new Connection(eLogin.getText().toString(),
				ePassword.getText().toString(), this);
		
		int connectionCode = connection.initialize();
		
		if (connectionCode < 1){
			Toast.makeText(this, R.string.connection_failed
					, Toast.LENGTH_LONG).show();
		}
		else{
			Toast.makeText(this, R.string.connection_ok
					, Toast.LENGTH_LONG).show();
			// intent vers tabhost
		}
			
		
    }
	
}