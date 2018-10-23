package com.example.android.popularmoviesstageone;

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

import java.util.HashMap;

public class MoviesProvider extends ContentProvider {

    static final String PROVIDER_NAME = "com.example.android.provider.popularmoviesstageone";
    static final String URL = "content://" + PROVIDER_NAME + "/favourites";
    static final Uri CONTENT_URI = Uri.parse(URL);

    static final String _ID = "_id";
    static final String TITLE = "title";
    static final String SYNOPSIS = "synopsis";
    static final String USER_RATING = "user_rating";
    static final String RELEASE_DATE = "release_date";
    static final String POSTER = "poster";

    private static HashMap<String, String> FAVOURITES_PROJECTION_MAP;

    static final int FAVOURITES = 1;
    static final int FAVOURITE_ID = 2;

    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "favourites", FAVOURITES);
        uriMatcher.addURI(PROVIDER_NAME, "favourites/#", FAVOURITE_ID);
    }

    private SQLiteDatabase db;
    static final String DATABASE_NAME = "Movies";
    static final  String FAVOURITES_TABLE_NAME = "favourites";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_DB_TABLE = "CREATE TABLE " + FAVOURITES_TABLE_NAME +
            " (_id INTEGER PRIMARY KEY, " + " title TEXT NOT NULL, " +
            " synopsis TEXT NOT NULL, " +  " user_rating TEXT NOT NULL, " +
            " release_date TEXT NOT NULL, " + "poster BLOB);";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + FAVOURITES_TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return (db == null)? false:true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = db.insert(FAVOURITES_TABLE_NAME, "", values);

        if(rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        else {
            Uri _uri = Uri.parse("Duplicate");
            return _uri;
        }

    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(FAVOURITES_TABLE_NAME);

        switch(uriMatcher.match(uri)) {
            case FAVOURITES:
                qb.setProjectionMap(FAVOURITES_PROJECTION_MAP);
                break;

            case FAVOURITE_ID:
                qb.appendWhere( _ID + "=" + uri.getPathSegments().get(1));
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (sortOrder == null || sortOrder == "") {
            sortOrder = TITLE;
        }
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case FAVOURITES:
                count = db.delete(FAVOURITES_TABLE_NAME, selection, selectionArgs);
                break;

            case FAVOURITE_ID:
                String id = uri.getPathSegments().get(1);
                count = db.delete(FAVOURITES_TABLE_NAME, _ID +  " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case FAVOURITES:
                count = db.update(FAVOURITES_TABLE_NAME, values, selection, selectionArgs);
                break;

            case FAVOURITE_ID:
                count = db.update(FAVOURITES_TABLE_NAME, values, _ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri );
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch(uriMatcher.match(uri)) {
            case FAVOURITES:
                return "vnd.android.cursor.dir/vnd.example.favourites";

            case FAVOURITE_ID:
                return "vnd.android.cursor.item/vnd.example.favourites";

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}