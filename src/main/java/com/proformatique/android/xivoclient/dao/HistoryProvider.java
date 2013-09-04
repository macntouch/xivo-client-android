package com.proformatique.android.xivoclient.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class HistoryProvider extends ContentProvider {
	
	private final static String TAG = "History provider";
	
	public final static String PROVIDER_NAME = Constants.PACK + ".history";
	public final static Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/history");
	private static final String CONTENT_TYPE
		= "vnd.android.cursor.dir/vnd.proformatique.xivo.history";
	private static final String CONTENT_ITEM_TYPE
		= "vnd.android.cursor.item/vnd.proformatique.xivo.history";
	
	/*
	 * Columns
	 */
	public static final String _ID = "_id";
	public static final String DURATION = "duration";
	public static final String TERMIN = "termin";
	public static final String DIRECTION = "direction";
	public static final String FULLNAME = "fullname";
	public static final String TS = "ts";
	
	/*
	 * uri matchers
	 */
	private static final int HISTORIES = 1;
	private static final int HISTORY_ID = 2;
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "history", HISTORIES);
		uriMatcher.addURI(PROVIDER_NAME, "history/#", HISTORY_ID);
	}
	
	/*
	 * DB info
	 */
	private SQLiteDatabase xivoHistoryDB;
	private static final String DATABASE_NAME = "xivo_history";
	private static final String DATABASE_TABLE = "history";
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_CREATE =
		"create table " + DATABASE_TABLE + " (" +
		_ID + " integer primary key autoincrement, " +
		DURATION + " text not null, " +
		TERMIN + " text not null, " +
		FULLNAME + " text not null, " +
		TS + " text not null, " +
		DIRECTION + " text not null" +
		");";
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		switch(uriMatcher.match(uri)) {
		case HISTORIES:
			count = xivoHistoryDB.delete(DATABASE_TABLE, selection, selectionArgs);
			break;
		case HISTORY_ID:
			String id = uri.getLastPathSegment();
			count = xivoHistoryDB.delete(DATABASE_TABLE, _ID + " = " + id +
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
		case HISTORIES:
			return CONTENT_TYPE;
		case HISTORY_ID:
			return CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowId = xivoHistoryDB.insert(DATABASE_TABLE, "", values);
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
		xivoHistoryDB = dbHelper.getWritableDatabase();
		return xivoHistoryDB != null;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(DATABASE_TABLE);
		
		if (uriMatcher.match(uri) == HISTORY_ID)
			sqlBuilder.appendWhere(_ID + " = " + uri.getLastPathSegment());
		
		Cursor c = sqlBuilder.query(
				xivoHistoryDB, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		switch (uriMatcher.match(uri)){
		case HISTORIES:
			count = xivoHistoryDB.update(DATABASE_TABLE, values, selection, selectionArgs);
			break;
		case HISTORY_ID:
			count = xivoHistoryDB.update(
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
	 * Returns a list of hashmap of the history entries
	 * @param context
	 * @return list
	 */
	public static List<HashMap<String, String>> getList(Context context) {
		List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		Cursor history = context.getContentResolver().query(CONTENT_URI, null, null, null, null);
		if (history.getCount() > 0) {
			int iDur = history.getColumnIndex(DURATION);
			int iTs = history.getColumnIndex(TS);
			int iDir = history.getColumnIndex(DIRECTION);
			int iTermin = history.getColumnIndex(TERMIN);
			int iFullname = history.getColumnIndex(FULLNAME);
			history.moveToFirst();
			do {
				HashMap<String, String> item = new HashMap<String, String>();
				item.put("duration", history.getString(iDur));
				item.put("direction", history.getString(iDir));
				item.put("ts", history.getString(iTs));
				item.put("termin", history.getString(iTermin));
				item.put("fullname", history.getString(iFullname));
				list.add(item);
			} while(history.moveToNext());
		}
		history.close();
		return list;
	}
}
