package pl.org.dlapuszczy.alarmuj.db;

import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.provider.BaseColumns;

import java.util.EnumSet;
import java.util.HashMap;

import pl.org.dlapuszczy.alarmuj.Flag;

/**
 * Created by ohaleck on 29/07/2017.
 */
public class Report implements BaseColumns {
    public static final String TABLE_NAME = "reports";


    public static final String ID = "_id"; // integer
    public static final String LATITUDE = "latitude"; // decimal
    public static final String LONGITUDE = "longitude"; // decimal
    public static final String ACCURACY = "accuracy"; // decimal
    public static final String FLAGS = "flags"; // string
    public static final String TIME = "time"; // long
    public static final String TIME_SENT = "time_sent"; // long
    public static final String SERIAL = "serial_no"; // string

    public static final String[] COLUMNS = new String[]{ID, LATITUDE, LONGITUDE, ACCURACY, FLAGS, TIME, TIME_SENT};

    public final EnumSet<Flag> flags;
    public final long time;
    public final double latitude;
    public final double longitude;
    public final double accuracy;

    public Report(Location location, EnumSet<Flag> flags, long time) {
        this(location.getLatitude(), location.getLongitude(), location.getAccuracy(), flags, time);
    }

    public Report(double latitude, double longitude, double accuracy, EnumSet<Flag> flags, long time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.flags = flags;
        this.time = time;
    }

    public static Report fromCursor(Cursor cursor) {
        return new Report(
                cursor.getDouble(cursor.getColumnIndex(LATITUDE)),
                cursor.getDouble(cursor.getColumnIndex(LONGITUDE)),
                cursor.getDouble(cursor.getColumnIndex(ACCURACY)),
                Flag.parseList(cursor.getString(cursor.getColumnIndex(FLAGS))),
                cursor.getLong(cursor.getColumnIndex(TIME))
        );
    }

    public HashMap<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(LATITUDE, latitude);
        map.put(LONGITUDE, longitude);
        map.put(ACCURACY, accuracy);
        map.put(FLAGS, Flag.toString(flags));
        map.put(TIME, time);
        map.put(SERIAL, Build.SERIAL);
        return map;
    }
}
