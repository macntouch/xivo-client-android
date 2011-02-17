package com.proformatique.android.xivoclient.service;

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

public class CapaservicesProvider extends ContentProvider {
	
	private final static String TAG = "Services Provider";
	
	public final static String PROVIDER_NAME = Constants.PACK + ".services";
	public final static Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/capaservices");
	private static final String CONTENT_TYPE
		= "vnd.android.cursor.dir/vnd.proformatique.xivo.capaservices";
	private static final String CONTENT_ITEM_TYPE
		= "vnd.android.cursor.item/vnd.proformatique.xivo.capaservices";
	
	/*
	 * Columns
	 */
	public static final String _ID = "_id";
	public static final String SERVICE = "name";
	public static final String ENABLED = "enabled";
	public static final String NUMBER = "number";
	
	/*
	 * uri matchers
	 */
	private static final int SERVICES = 1;
	private static final int SERVICE_ID = 2;
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "capaservices", SERVICES);
		uriMatcher.addURI(PROVIDER_NAME, "capaservices/#", SERVICE_ID);
	}
	
	/*
	 * DB info
	 */
	private SQLiteDatabase capaservicesDB;
	private static final String DATABASE_NAME = "capaservices";
	private static final String DATABASE_TABLE = "capaservices";
	private static final int DATABASE_VERSION = 3;
	private static final String DATABASE_CREATE =
		"create table " + DATABASE_TABLE + " (" +
		_ID + " integer primary key autoincrement, " +
		SERVICE + " text not null, " +
		ENABLED + " integer default 0, " +
		NUMBER  + " text " +
		");";
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		switch(uriMatcher.match(uri)) {
		case SERVICES:
			count = capaservicesDB.delete(DATABASE_TABLE, selection, selectionArgs);
			break;
		case SERVICE_ID:
			String id = uri.getLastPathSegment();
			count = capaservicesDB.delete(DATABASE_TABLE, _ID + " = " + id +
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
		case SERVICES:
			return CONTENT_TYPE;
		case SERVICE_ID:
			return CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowId = capaservicesDB.insert(DATABASE_TABLE, "", values);
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
		capaservicesDB = dbHelper.getWritableDatabase();
		return capaservicesDB != null;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(DATABASE_TABLE);
		
		if (uriMatcher.match(uri) == SERVICE_ID)
			sqlBuilder.appendWhere(_ID + " = " + uri.getLastPathSegment());
		
		Cursor c = sqlBuilder.query(
				capaservicesDB, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = 0;
		switch (uriMatcher.match(uri)){
		case SERVICES:
			count = capaservicesDB.update(DATABASE_TABLE, values, selection, selectionArgs);
			break;
		case SERVICE_ID:
			count = capaservicesDB.update(
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
	
	public static void cursorToString(Cursor entry) {
		Log.d(TAG, "Name: " + entry.getString(entry.getColumnIndex(SERVICE)));
		Log.d(TAG, "Enabled: " + entry.getString(entry.getColumnIndex(ENABLED)));
		Log.d(TAG, "Number: " + entry.getString(entry.getColumnIndex(NUMBER)));
	}
}
