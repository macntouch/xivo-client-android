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

public class UserProvider extends ContentProvider {
	
	private final static String TAG = "UserProvider";
	
	public final static String PROVIDER_NAME = Constants.PACK + ".user";
	public final static Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/user");
	private static final String CONTENT_TYPE
		= "vnd.android.cursor.dir/vnd.proformatique.xivo.user";
	private static final String CONTENT_ITEM_TYPE
		= "vnd.android.cursor.item/vnd.proformatique.xivo.user";
	
	/*
	 * Columns
	 */
	public static final String _ID = "_id";
	public static final String ASTID = "astid";
	public static final String XIVO_USERID = "xivo_userid";
	public static final String FULLNAME = "fullname";
	public static final String PHONENUM = "phonenum";
	public static final String STATEID = "stateid";
	public static final String STATEID_LONGNAME = "stateid_longname";
	public static final String STATEID_COLOR = "stateid_color";
	public static final String TECHLIST = "techlist";
	public static final String HINTSTATUS_COLOR = "hintstatus_color";
	public static final String HINTSTATUS_CODE = "hintstatus_code";
	public static final String HINTSTATUS_LONGNAME = "hintstatus_longname";
	
	/*
	 * uri matchers
	 */
	private static final int USERS = 1;
	private static final int USER_ID = 2;
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "user", USERS);
		uriMatcher.addURI(PROVIDER_NAME, "user/#", USER_ID);
	}
	
	/*
	 * DB info
	 */
	private SQLiteDatabase xivouserDB;
	private static final String DATABASE_NAME = "xivo_user";
	private static final String DATABASE_TABLE = "user";
	private static final int DATABASE_VERSION = 3;
	private static final String DATABASE_CREATE =
		"create table " + DATABASE_TABLE + " (" +
		_ID + " integer primary key autoincrement, " +
		ASTID + " text not null, " +
		XIVO_USERID + " text not null, " +
		FULLNAME + " text not null, " +
		PHONENUM + " text not null, "+
		STATEID + " text not null, " +
		STATEID_LONGNAME + " text not null, " +
		STATEID_COLOR + " text not null, " +
		TECHLIST + " text not null," +
		HINTSTATUS_COLOR + " text not null, "+
		HINTSTATUS_CODE + " text not null, " +
		HINTSTATUS_LONGNAME + " text not null" +
		");";
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		switch(uriMatcher.match(uri)) {
		case USERS:
			count = xivouserDB.delete(DATABASE_TABLE, selection, selectionArgs);
			break;
		case USER_ID:
			String id = uri.getLastPathSegment();
			count = xivouserDB.delete(DATABASE_TABLE, _ID + " = " + id +
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
		case USERS:
			return CONTENT_TYPE;
		case USER_ID:
			return CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long rowId = xivouserDB.insert(DATABASE_TABLE, "", values);
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
		xivouserDB = dbHelper.getWritableDatabase();
		return xivouserDB != null;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(DATABASE_TABLE);
		
		if (uriMatcher.match(uri) == USER_ID)
			sqlBuilder.appendWhere(_ID + " = " + uri.getLastPathSegment());
		
		Cursor c = sqlBuilder.query(
				xivouserDB, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = 0;
		switch (uriMatcher.match(uri)){
		case USERS:
			count = xivouserDB.update(DATABASE_TABLE, values, selection, selectionArgs);
			break;
		case USER_ID:
			count = xivouserDB.update(
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
}
