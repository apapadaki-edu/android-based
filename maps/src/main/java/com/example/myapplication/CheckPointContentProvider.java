package com.example.myapplication;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

public class CheckPointContentProvider extends ContentProvider {
    static final String PROVIDER_NAME = "com.example.myapplication.CheckPointContentProvider";
    static final String URL = "content://" + PROVIDER_NAME + "/checkpoints";
    static final Uri CONTENT_URI = Uri.parse(URL);

    static final String _ID = "_id";
    static final String lat = "latitude";
    static final String lon = "longitude";
    static final String address = "address";
    static final String city = "city";
    static final String country = "country";
    static final String postalCode = "postal_code";
    static final String knownName = "known_name";

    private static HashMap<String, String> CHECKPOINTS_PROJECTION_MAP;

    static final int CHECKPOINTS = 0;
    static final int CHECKPOINT_ID = 1;

    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "checkpoints", CHECKPOINTS);
        uriMatcher.addURI(PROVIDER_NAME, "checkpoints/#", CHECKPOINT_ID);
    }

    /**
     * Database specific constant declarations
     */

    private SQLiteDatabase db;
    static final String DATABASE_NAME = "Checkpoints.db";
    static final String CHECKPOINTS_TABLE_NAME = "checkpoints";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_DB_TABLE =
            " CREATE TABLE " + CHECKPOINTS_TABLE_NAME +
                    " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " latitude NUMERIC NOT NULL, " +
                    " longitude NUMERIC NOT NULL, "+
                    " address TEXT, "+
                    " city TEXT, "+
                    " country TEXT,"+
                    " postal_code TEXT, "+
                    " known_name TEXT);";

    /**
     * Helper class that actually creates and manages
     * the provider's underlying data repository.
     */

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " +  CHECKPOINTS_TABLE_NAME);
            onCreate(db);
        }
    }


    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        /**
         * Create a write able database which will trigger its
         * creation if it doesn't already exist.
         */

        db = dbHelper.getWritableDatabase();
        return (db == null)? false:true;

    }

    @Override
    public Cursor query(Uri uri, String[] projection,
                        String selection,String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(CHECKPOINTS_TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            case CHECKPOINTS:
                qb.setProjectionMap(CHECKPOINTS_PROJECTION_MAP);
                break;

            case CHECKPOINT_ID:
                qb.appendWhere( _ID + "=" + uri.getPathSegments().get(1));
                break;

            default:
        }

        if (sortOrder == null || sortOrder == ""){
            sortOrder = _ID;
        }

        Cursor c = qb.query(db,	projection,	selection,
                selectionArgs,null, null, sortOrder);
        /**
         * register to watch a content URI for changes
         */
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            /**
             * Get all checkpoints
             */
            case CHECKPOINTS:
                return "vnd.android.cursor.dir/vnd.example.checkpoints";
            /**
             * Get a particular checkpoint
             */
            case CHECKPOINT_ID:
                return "vnd.android.cursor.item/vnd.example.checkpoints";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }


    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        Log.i("inInsert", "");
        /**
         * Add a new student record
         */
        long rowID = db.insert(	CHECKPOINTS_TABLE_NAME, "", values);

        /**
         * If record is added successfully
         */
        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }

        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)){
            case CHECKPOINTS:
                count = db.delete(CHECKPOINTS_TABLE_NAME, selection, selectionArgs);
                break;

            case CHECKPOINT_ID:
                String id = uri.getPathSegments().get(1);
                count = db.delete( CHECKPOINTS_TABLE_NAME, _ID +  " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;    }

    @Override
    public int update(Uri uri, ContentValues values,
                      String selection, String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case CHECKPOINTS:
                count = db.update(CHECKPOINTS_TABLE_NAME, values, selection, selectionArgs);
                break;

            case CHECKPOINT_ID:
                count = db.update(CHECKPOINTS_TABLE_NAME, values,
                        _ID + " = " + uri.getPathSegments().get(1) +
                                (!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri );
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
