/**
 * DbAdaptor for WiFiGPSLocation
 *
 * Copyright (C) 2010 Hossein Falaki
 */
package edu.ucla.cens.wifigpslocation;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.location.Location;

import java.util.HashMap;

/**
 * Simple  database access helper class. 
 * Interfaces with the SQLite database to store WiFi cache objects.
 *
 * @author Hossein Falaki
 */
public class DbAdaptor 
{


    private static final String VER = WiFiGPSLocationService.VER;

    public static final String KEY_ROWID = "_id";
    public static final String KEY_TIME = "recordtime";
    public static final String KEY_COUNT = "recordcount";
    public static final String KEY_SIGNATURE = "recordsign";
    public static final String KEY_LAT = "loclat";
    public static final String KEY_LON = "loclon";
    public static final String KEY_ACC = "locacc";
    public static final String KEY_PROVIDER = "locprovider";
    public static final String KEY_HASLOC = "haslocation";


    public static final int NO  = 0;
    public static final int YES = 1;




    private static final String TAG = "WiFiGPSDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /** Database creation sql statement */
    private static final String DATABASE_CREATE =
            "create table wifigps (_id integer primary key "
           + "autoincrement, recordtime integer not null, " 
           + "recordcount integer not null,"
           + "recordsign text not null,"
           + "loclat real,"
           + "loclon real,"
           + "locacc real,"
           + "locprovider text,"
           + "haslocation integer not null"
           + ");";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "wifigps";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper 
    {

        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
                int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS wifigps");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx       the Context within which to work
     */
    public DbAdaptor(Context ctx) 
    {
        this.mCtx = ctx;
    }

    /**
     * Open the database.
     * If it cannot be opened, try to create a new instance of the
     * database. If it cannot be created, throw an exception to signal
     * the failure.
     * 
     * @return this         (self reference, allowing this to be
     *                      chained in an initialization call)
     * @throws SQLException if the database could be neither opened or
     *                      created
     */
    public DbAdaptor open() throws SQLException 
    {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    /**
      * Closes the database.
      */
    public void close() 
    {
        mDbHelper.close();
    }


    /**
     * Writes the given cache in the database. 
     * It deletes the previous content of the database before
     * writing the new cache.
     * 
     * @param   mCurCache       current WiFi to GPS cache
     */
    public void syncDb(HashMap<String, GPSInfo> mCurCache) 
    {

        int dbCount = mDb.delete(DATABASE_TABLE, "1", null);
        Log.i(TAG, "Deleted database content: " + dbCount + " rows.");

        GPSInfo gpsInfo;
        double lat, lon, acc;
        int count;
        long time;
        String provider;

        ContentValues initialValues;


        for (String key : mCurCache.keySet())
        {
            gpsInfo =  mCurCache.get(key);
            if (gpsInfo == null)
                continue;


            initialValues = new ContentValues();

            if (gpsInfo.loc != null)
            {
                lat = gpsInfo.loc.getLatitude();
                lon = gpsInfo.loc.getLongitude();
                acc = gpsInfo.loc.getAccuracy();
                provider = gpsInfo.loc.getProvider();

                initialValues.put(KEY_LAT, lat);
                initialValues.put(KEY_LON, lon);
                initialValues.put(KEY_ACC, acc);
                initialValues.put(KEY_PROVIDER, provider);
                initialValues.put(KEY_HASLOC, YES);

            }
            else
            {
                initialValues.put(KEY_HASLOC, NO);
            }


            count = gpsInfo.count;
            time = gpsInfo.time;

            initialValues.put(KEY_TIME, time);
            initialValues.put(KEY_COUNT, count);
            initialValues.put(KEY_SIGNATURE, key);

            mDb.insert(DATABASE_TABLE, null, initialValues);

            Log.i(TAG, "Saved " + gpsInfo.toString());
        }


    }

    /**
     * Deletes the entry with the given rowId
     * 
     * @param rowId         id of datarecord to delete
     * @return              true if deleted, false otherwise
     */
    public boolean deleteEntry(long rowId) 
    {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID 
                + "=" + rowId, null) > 0;
    }

    /**
     * Returns a Cursor over the list of all datarecords in the database
     * 
     * @return              Cursor over all notes
     */
    public Cursor fetchAllEntries() 
    {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID,
            KEY_TIME, KEY_COUNT, KEY_SIGNATURE, KEY_LAT,KEY_LON,
               KEY_ACC, KEY_PROVIDER, KEY_HASLOC},
                null, null, null, null, null);
    }


}
