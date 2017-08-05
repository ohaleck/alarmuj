package pl.org.dlapuszczy.alarmuj;

import android.util.Log;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Created by ohaleck on 13/07/2017.
 */

public enum Flag {
    HARVESTER(R.id.harvesterAtWorkCb),
    FORWARDER(R.id.forwarderAtWorkCb),
    LUMBERJACKS(R.id.lumberjacksAtWorkCb),
    BLOCKED_ROAD(R.id.blockedRoadCb),
    HARVESTED_WOOD(R.id.harvestedWoodCb),
    LOGGING_SITE(R.id.loggingSiteCb);

    private static final Pattern LIST_SPLIT_PATTERN = Pattern.compile("[\\s*,\\s*]");
    private static final String TAG = "Flag";
    public final int checkboxResId;

    Flag(int checkboxResId) {
        this.checkboxResId = checkboxResId;
    }

    public static String toString(EnumSet<Flag> flags) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<Flag> iter = flags.iterator(); iter.hasNext(); ) {
            Flag flag = iter.next();
            sb.append(flag);
            if (iter.hasNext()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    public static EnumSet<Flag> parseList(String listString) {
        EnumSet<Flag> enumSet = EnumSet.noneOf(Flag.class);
        if (listString == null || listString.isEmpty()) {
            return enumSet;
        } else {
            String[] flagStrings = LIST_SPLIT_PATTERN.split(listString);
            for (String string : flagStrings) {
                try {
                    enumSet.add(Flag.valueOf(string));
                } catch (IllegalArgumentException | NullPointerException e) {
                    Log.w(TAG, "Unknown or null flag string: " + e);
                }
            }
            return enumSet;
        }
    }
}
