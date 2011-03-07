package com.proformatique.android.xivoclient;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Displays a list of installed activities. The selected application's package
 * is returned as a result
 * 
 */
public class ApplicationPicker extends Activity implements OnItemClickListener {
    
    private final static String TAG = "App Picker";
    
    private List<ResolveInfo> apps = null;
    private ApplicationAdapter adapter = null;
    
    private ListView lv = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_picker);
        
        lv = (ListView) findViewById(R.id.app_list);
        
        initList();
        
        adapter = new ApplicationAdapter(apps);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
    
    /**
     * Initialize the application list
     */
    private void initList() {
        Intent launchIntent = new Intent(Intent.ACTION_MAIN, null);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        apps = getPackageManager().queryIntentActivities(launchIntent, 0);
    }
    
    private class ApplicationAdapter implements ListAdapter {
        
        private List<ResolveInfo> items;
        
        public ApplicationAdapter(List<ResolveInfo> list) {
            items = list;
        }
        
        /**
         * All applications can be picked from the list
         */
        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }
        
        /**
         * The list doesn't contain any separator
         */
        @Override
        public boolean isEnabled(int position) {
            return true;
        }
        
        @Override
        public int getCount() {
            return items.size();
        }
        
        @Override
        public Object getItem(int position) {
            return items.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public int getItemViewType(int position) {
            return R.layout.app_picker_items;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                final LayoutInflater inflater = LayoutInflater.from(ApplicationPicker.this);
                v = inflater.inflate(R.layout.app_picker_items, null);
            } else {
                v = convertView;
            }
            if (v == null) {
                Log.e(TAG, "Could not inflate an application item");
                return null;
            }
            ImageView icon = (ImageView) v.findViewById(R.id.app_icon);
            TextView name = (TextView) v.findViewById(R.id.app_name);
            
            if (icon == null || name == null) {
                Log.e(TAG, "Could not retrieve icon and/or label");
                return v;
            }
            
            name.setText(items.get(position).loadLabel(getPackageManager()));
            icon.setImageDrawable(items.get(position).loadIcon(getPackageManager()));
            
            return v;
        }
        
        @Override
        public int getViewTypeCount() {
            return 1;
        }
        
        /**
         * Ids may change if the content of the list is updated
         */
        @Override
        public boolean hasStableIds() {
            return false;
        }
        
        @Override
        public boolean isEmpty() {
            return items.size() == 0;
        }
        
        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            // TODO Auto-generated method stub
        }
        
        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            // TODO Auto-generated method stub
        }
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setResult(RESULT_OK, new Intent().putExtra("package",
                apps.get(position).activityInfo.packageName));
        finish();
    }
}
