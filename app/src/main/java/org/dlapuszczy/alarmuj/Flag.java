package org.dlapuszczy.alarmuj;

import java.util.EnumSet;
import java.util.Iterator;

/**
 * Created by ohaleck on 13/07/2017.
 */

public enum Flag {
    HARVESTER,
    FORWARDER,
    LUMBERJACKS,
    BLOCKED_ROAD;

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
}
