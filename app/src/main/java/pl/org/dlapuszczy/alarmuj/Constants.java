package pl.org.dlapuszczy.alarmuj;

/**
 * Created by ohaleck on 21/07/2017.
 */

public interface Constants {
    int SYNC_SERVICE_ID = 303;

    interface ACTION {
        String MAIN_ACTION = "pl.org.dlapuszczy.alarmuj.ACTION.MAIN";
        String START_FOREGROUND = "pl.org.dlapuszczy.action.START_FOREGROUND";
        String STOP = "pl.org.dlapuszczy.action.STOP";

        String CANCEL_SENDING = "pl.org.dlapuszczy.action.CANCEL_SENDING";
    }
}
