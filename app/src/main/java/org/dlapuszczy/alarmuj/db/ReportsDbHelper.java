package org.dlapuszczy.alarmuj.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.provider.BaseColumns;
import android.util.Log;

import org.dlapuszczy.alarmuj.Flag;

import java.util.EnumSet;

/**
 * Created by ohaleck on 14/07/2017.
 */

public class ReportsDbHelper extends SQLiteOpenHelper {


    private static final String TAG = "ReportsDbHelper";

    public static class Report implements BaseColumns {
        public static final String TABLE_NAME = "reports";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String ACCURACY = "accuracy";
        public static final String FLAGS = "flags";
        public static final String TIME = "time";
    }

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "Reports.db";
    private static final String SQL_CREATE_DB = "CREATE TABLE " + Report.TABLE_NAME + " (" +
            Report._ID + " INTEGER PRIMARY KEY," +
            Report.LATITUDE + " NUMERIC," +
            Report.LONGITUDE + " NUMERIC," +
            Report.ACCURACY + " NUMERIC," +
            Report.FLAGS + " TEXT," +
            Report.TIME + " INTEGER" +
            ")";
    private static final String SQL_DELETE_DB = "DROP TABLE IF EXISTS " + Report.TABLE_NAME;

    public ReportsDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_DB);
        onCreate(db);
    }

    public void addReport(SQLiteDatabase db, Location location, EnumSet<Flag> flags) {
        if (location == null) {
            Log.d(TAG, "Location is null");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(Report.LATITUDE, location.getLatitude());
        values.put(Report.LONGITUDE, location.getLongitude());
        values.put(Report.ACCURACY, location.getAccuracy());
        values.put(Report.FLAGS, Flag.toString(flags));
        values.put(Report.TIME, System.currentTimeMillis());
        db.insert(Report.TABLE_NAME, null, values);
    }
}
