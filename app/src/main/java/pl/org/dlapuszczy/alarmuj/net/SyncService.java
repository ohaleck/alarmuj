package pl.org.dlapuszczy.alarmuj.net;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import pl.org.dlapuszczy.alarmuj.Constants.ACTION;
import pl.org.dlapuszczy.alarmuj.MainActivity;
import pl.org.dlapuszczy.alarmuj.R;
import pl.org.dlapuszczy.alarmuj.db.ReportsDbHelper;

import static pl.org.dlapuszczy.alarmuj.Constants.SYNC_SERVICE_ID;

/**
 * Created by ohaleck on 19/07/2017.
 */

// http://www.tutorialsface.com/2015/09/simple-android-foreground-service-example/

public class SyncService extends Service implements ACTION {
    private static final String TAG = "SyncService";
    private ReportsDbHelper dbHelper;
    private boolean shouldSync = true;
    private NotificationManager notificationManager;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = ReportsDbHelper.getInstance(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    // TODO make notification reflect pending reports:
    // TODO https://developer.android.com/training/notify-user/managing.html

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(START_FOREGROUND)) {
                Log.i(TAG, "Received Start Foreground Intent ");
                startForeground(SYNC_SERVICE_ID, createNotification(true));
                maybeEnableSync();
            } else if (intent.getAction().equals(STOP)) {
                Log.i(TAG, "Received Stop Foreground Intent");
                notifyStoping();
                stop();
            } else if (intent.getAction().equals(CANCEL_SENDING)) {
                Log.i(TAG, "Received Cancel Sending Intent");
                notifyCancelled();
                stopSync();
            }
        }

        return Service.START_STICKY;
    }

    private void notifyCancelled() {
        // TODO
    }

    private void notifyStoping() {
        // TODO
        startForeground(SYNC_SERVICE_ID, createNotification(false));
    }

    private void stopSync() {
        shouldSync = false;
    }

    private void maybeEnableSync() {
//        SQLiteDatabase writableDatabase = dbHelper.getWritableDatabase();
        if (shouldSync && dbHelper.isReportPending() && isNetworkAvailable()) {
            Log.d(TAG, "Reports pending");
            new ConsumerThread(this.getApplicationContext()).start();

        } else {
            Log.d(TAG, "No reports pending");
            stop();
        }

    }

    private void stop() {
        stopForeground(false);
        stopSelf();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private Notification createNotification(boolean ongoing) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent cancelIntent = new Intent(this, SyncService.class);
        cancelIntent.setAction(ACTION.CANCEL_SENDING);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent cancelPendingIntent = PendingIntent.getService(this, 0, cancelIntent, 0);

        Builder builder = new Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.sending_reports))
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing);
        if (ongoing) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel_sending), cancelPendingIntent);
        }
        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "In onDestroy");
        Toast.makeText(this, "Service Destroyed!", Toast.LENGTH_SHORT).show();
    }
}
