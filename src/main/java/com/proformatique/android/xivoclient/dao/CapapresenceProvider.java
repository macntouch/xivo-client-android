package com.proformatique.android.xivoclient.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.proformatique.android.xivoclient.R;
import com.proformatique.android.xivoclient.tools.Constants;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class CapapresenceProvider extends ContentProvider {
	
	private final static String TAG = "PresenceProvider";
	
	public final static String PROVIDER_NAME = Constants.PACK + ".presence";
	public final static Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/capapresence");
	private static final String CONTENT_TYPE
		= "vnd.android.cursor.dir/vnd.proformatique.xivo.capapresence";
	private static final String CONTENT_ITEM_TYPE
		= "vnd.android.cursor.item/vnd.proformatique.xivo.capapresence";
	
	/*
	 * Columns
	 */
	public static final String _ID = "_id";
	public static final String NAME = "name";
	public static final String COLOR = "color";
	public static final String LONGNAME = "longname";
	public static final String ALLOWED = "allowed";
	
	/*
	 * uri matchers
	 */
	private static final int PRESENCES = 1;
	private static final int PRESENCE_ID = 2;
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "capapresence", PRESENCES);
		uriMatcher.addURI(PROVIDER_NAME, "capapresence/#", PRESENCE_ID);
	}
	
	/*
	 * DB info
	 */
	private SQLiteDatabase capapresenceDB;
	private static final String DATABASE_NAME = "capapresence";
	private static final String DATABASE_TABLE = "capapresence";
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_CREATE =
		"create table " + DATABASE_TABLE + " (" +
		_ID + " integer primary key autoincrement, " +
		NAME + " text not null, " +
		COLOR + " text not null, " +
		LONGNAME + " text not null, " +
		ALLOWED + " integer not null);";
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		switch(uriMatcher.match(uri)) {
		case PRESENCES:
			count = capapresenceDB.delete(DATABASE_TABLE, selection, selectionArgs);
			break;
		case PRESENCE_ID:
			String id = uri.getLastPathSegment();
			count = capapresenceDB.delete(DATABASE_TABLE, _ID + " = " + id +
					(!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
					selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	@Override
	public String getType(Uri uri) {
		switch(uriMatcher.match(uri)) {
		case PRESENCES:
			return CONTENT_TYPE;
		case PRESENCE_ID:
			return CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowId = capapresenceDB.insert(DATABASE_TABLE, "", values);
		if (rowId > 0) {
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		Log.d(TAG, "Failed to insert row: " + uri);
		return null;
	}
	
	@Override
	public boolean onCreate() {
		DBHelper dbHelper = new DBHelper(getContext());
		capapresenceDB = dbHelper.getWritableDatabase();
		return capapresenceDB != null;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(DATABASE_TABLE);
		
		if (uriMatcher.match(uri) == PRESENCE_ID)
			sqlBuilder.appendWhere(_ID + " = " + uri.getLastPathSegment());
		
		Cursor c = sqlBuilder.query(
				capapresenceDB, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		switch (uriMatcher.match(uri)){
		case PRESENCES:
			count = capapresenceDB.update(DATABASE_TABLE, values, selection, selectionArgs);
			break;
		case PRESENCE_ID:
			count = capapresenceDB.update(
					DATABASE_TABLE, values, _ID + " = " + uri.getLastPathSegment() + 
					(!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), 
					selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	private static class DBHelper extends SQLiteOpenHelper {
		
		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}
		
	}
	
	/**
	 * Helper function to get the column _ID of a given stateId
	 * @param context
	 * @param stateid
	 * @return _ID
	 */
	public static long getIndex(Context context, String stateid) {
		long id = -1L;
		Cursor row = context.getContentResolver().query(
				CONTENT_URI, new String[] {_ID, NAME}, NAME + " = '" + stateid + "'", null, null);
		if (row.getCount() > 0) {
			row.moveToFirst();
			id = row.getLong(row.getColumnIndex(_ID));
		}
		row.close();
		return id;
	}
	
	/**
	 * Helper function to get a List of HashMap containing available states
	 */
	public static List<HashMap<String, String>> getStateList(Context context) {
		List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		Cursor states = context.getContentResolver().query(CONTENT_URI,
				new String[] {_ID, NAME, LONGNAME, COLOR}, null, null, null);
		if (states.getCount() > 0) {
			states.moveToFirst();
			do {
				HashMap<String, String> item = new HashMap<String, String>(3);
				item.put("color", states.getString(states.getColumnIndex(COLOR)));
				item.put("stateid", states.getString(states.getColumnIndex(NAME)));
				item.put("longname", states.getString(states.getColumnIndex(LONGNAME)));
				list.add(item);
			} while(states.moveToNext());
		}
		states.close();
		return list;
	}
	
	/**
	 * Returns a state color for a given stateId
	 * @param context
	 * @param stateId
	 * @return color
	 */
	public static String getColor(Context context, String stateId) {
		String color = Constants.DEFAULT_HINT_COLOR;
		Cursor state = context.getContentResolver().query(CONTENT_URI,
				new String[] {NAME, COLOR}, NAME + " = '" + stateId + "'", null, null);
		if (state.getCount() > 0) {
			state.moveToFirst();
			color = state.getString(state.getColumnIndex(COLOR));
		}
		state.close();
		return color;
	}
	
	/**
	 * Returns a longname for a given stateId
	 * @param context
	 * @param stateId
	 * @return
	 */
	public static String getLongname(Context context, String stateId) {
		String longname = context.getString(R.string.default_hint_longname);
		Cursor state = context.getContentResolver().query(CONTENT_URI,
				new String[] {NAME, LONGNAME}, NAME + " = '" + stateId + "'", null, null);
		if (state.getCount() > 0) {
			state.moveToFirst();
			longname = state.getString(state.getColumnIndex(LONGNAME));
		}
		state.close();
		return longname;
	}
}
