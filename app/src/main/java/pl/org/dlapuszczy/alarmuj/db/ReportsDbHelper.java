package pl.org.dlapuszczy.alarmuj.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import pl.org.dlapuszczy.alarmuj.Flag;

/**
 * Created by ohaleck on 14/07/2017.
 */

public class ReportsDbHelper extends SQLiteOpenHelper {
    ;
    private static ReportsDbHelper instance;

    public static synchronized ReportsDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ReportsDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private static final String TAG = "ReportsDbHelper";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "Reports.db";
    private static final String SQL_CREATE_DB = "CREATE TABLE " + Report.TABLE_NAME + " (" +
            Report._ID + " INTEGER PRIMARY KEY," +
            Report.LATITUDE + " NUMERIC," +
            Report.LONGITUDE + " NUMERIC," +
            Report.ACCURACY + " NUMERIC," +
            Report.FLAGS + " TEXT," +
            Report.TIME + " INTEGER," +
            Report.TIME_SENT + " INTEGER" +
            ")";
    private static final String SQL_DELETE_DB = "DROP TABLE IF EXISTS " + Report.TABLE_NAME;

    private ReportsDbHelper(Context context) {
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

    public boolean addReport(SQLiteDatabase db, Report report) {
        if (report == null) {
            Log.d(TAG, "Report is null");
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(Report.LATITUDE, report.latitude);
        values.put(Report.LONGITUDE, report.longitude);
        values.put(Report.ACCURACY, report.accuracy);
        values.put(Report.FLAGS, Flag.toString(report.flags));
        values.put(Report.TIME, report.time);
        values.put(Report.TIME_SENT, -1);
        long rowId = db.insert(Report.TABLE_NAME, null, values);
        return rowId != -1;
    }

    public boolean isReportPending() {
        long numEntries = DatabaseUtils.queryNumEntries(getReadableDatabase(), Report.TABLE_NAME, Report.TIME_SENT + "=?", new String[]{"-1"});
        return numEntries > 0L;
    }

    public Cursor getPendingReports() {
        return getReadableDatabase().query(Report.TABLE_NAME, Report.COLUMNS, Report.TIME_SENT + "=?", new String[]{"-1"}, null, null, Report.TIME);
    }
}
