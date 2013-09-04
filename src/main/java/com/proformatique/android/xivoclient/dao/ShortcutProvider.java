package com.proformatique.android.xivoclient.dao;

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

/**
 * Content provider to manage shortcuts added to the XiVO client
 * 
 */
public class ShortcutProvider extends ContentProvider {
    
    private final static String TAG = "XiVO shortcuts";
    
    public final static String PROVIDER_NAME = Constants.PACK + ".shortcut";
    public final static Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/shortcut");
    private static final String CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.proformatique.xivo.shortcut";
    private static final String CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.proformatique.xivo.shortcut";
    
    /*
     * Columns
     */
    public static final String _ID = "_id";
    public static final String NAME = "name";
    public static final String PACKAGE = "package";
    
    /*
     * uri matchers
     */
    private static final int SHORTCUTS = 1;
    private static final int SHORTCUT_ID = 2;
    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "shortcut", SHORTCUTS);
        uriMatcher.addURI(PROVIDER_NAME, "shortcut/#", SHORTCUT_ID);
    }
    
    /*
     * DB info
     */
    private SQLiteDatabase xivoShortcutDB;
    private static final String DATABASE_NAME = "xivoshortcut";
    private static final String DATABASE_TABLE = "shortcut";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_CREATE = "create table " + DATABASE_TABLE + " (" + _ID
            + " integer primary key autoincrement, " + NAME + " text not null, " + PACKAGE
            + " text not null);";
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
        case SHORTCUTS:
            count = xivoShortcutDB.delete(DATABASE_TABLE, selection, selectionArgs);
            break;
        case SHORTCUT_ID:
            String id = uri.getLastPathSegment();
            count = xivoShortcutDB.delete(DATABASE_TABLE, _ID + " = " + id
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
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
        switch (uriMatcher.match(uri)) {
        case SHORTCUTS:
            return CONTENT_TYPE;
        case SHORTCUT_ID:
            return CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowId = xivoShortcutDB.insert(DATABASE_TABLE, "", values);
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
        xivoShortcutDB = dbHelper.getWritableDatabase();
        return xivoShortcutDB != null;
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(DATABASE_TABLE);
        
        if (uriMatcher.match(uri) == SHORTCUT_ID)
            sqlBuilder.appendWhere(_ID + " = " + uri.getLastPathSegment());
        
        Cursor c = sqlBuilder.query(xivoShortcutDB, projection, selection, selectionArgs, null,
                null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
        case SHORTCUTS:
            count = xivoShortcutDB.update(DATABASE_TABLE, values, selection, selectionArgs);
            break;
        case SHORTCUT_ID:
            count = xivoShortcutDB.update(DATABASE_TABLE, values, _ID + " = "
                    + uri.getLastPathSegment()
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
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
