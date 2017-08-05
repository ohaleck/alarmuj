package pl.org.dlapuszczy.alarmuj.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by ohaleck on 18/07/2017.
 */

public class PermissionHelper {
    public static final PermissionHelper INSTANCE = new PermissionHelper();
    private static final int LOCATION_PERMISSION_REQUEST = 2342;
    private final Queue<PermissionCallback> waitingForPermissionQueue = new ConcurrentLinkedQueue<>();

    private boolean runtimePermissionGranted;

    public void doWithPermissions(Activity activity, PermissionCallback permissionCallback) {
        if (ContextCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            permissionCallback.onResult(true);
        } else {
            waitingForPermissionQueue.add(permissionCallback);
            ActivityCompat.requestPermissions(activity, new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PERMISSION_GRANTED) {
                        runtimePermissionGranted = false;
                        break;
                    }
                    runtimePermissionGranted = true;
                }
            }
            executeQueuedCallbacks();
        }
    }

    private void executeQueuedCallbacks() {
        PermissionCallback callback;
        while ((callback = waitingForPermissionQueue.poll()) != null) {
            callback.onResult(runtimePermissionGranted);
        }

    }
}
