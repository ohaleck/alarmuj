package pl.org.dlapuszczy.alarmuj;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;

import pl.org.dlapuszczy.alarmuj.Constants.ACTION;
import pl.org.dlapuszczy.alarmuj.db.Report;
import pl.org.dlapuszczy.alarmuj.db.ReportsDbHelper;
import pl.org.dlapuszczy.alarmuj.net.SyncService;
import pl.org.dlapuszczy.alarmuj.util.PermissionCallback;
import pl.org.dlapuszczy.alarmuj.util.PermissionHelper;

public class MainActivity extends AppCompatActivity implements OnConnectionFailedListener, ConnectionCallbacks {
    private final String TAG = "MainActivity";
    private ReportsDbHelper dbHelper;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Button sendButton;


    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "Location received: " + location);
            lastLocation = location;
            updateCoordinateUi(location);
        }
    };
    private TextView locationTimeTv;
    private TextView accuracyTv;
    private TextView coordinatesTv;
    private EnumMap<Flag, CheckBox> checkBoxes = new EnumMap<>(Flag.class);


    private void updateCoordinateUi(Location location) {
        if (location != null) {
            findViewById(pl.org.dlapuszczy.alarmuj.R.id.awaiting_location).setVisibility(View.GONE);
            coordinatesTv.setVisibility(View.VISIBLE);
            accuracyTv.setVisibility(View.VISIBLE);
            locationTimeTv.setVisibility(View.VISIBLE);
            coordinatesTv.setText(getString(pl.org.dlapuszczy.alarmuj.R.string.your_coordinates) + ": " + getLocationString(location));
            accuracyTv.setText(getString(pl.org.dlapuszczy.alarmuj.R.string.accuracy) + ": " + getAccuracyString(location));
            locationTimeTv.setText(getString(pl.org.dlapuszczy.alarmuj.R.string.location_time) + ": " + getTimeString(location));
        } else {
            findViewById(pl.org.dlapuszczy.alarmuj.R.id.awaiting_location).setVisibility(View.VISIBLE);
            coordinatesTv.setVisibility(View.GONE);
            accuracyTv.setVisibility(View.GONE);
            locationTimeTv.setVisibility(View.GONE);
        }
        maybeEnableSendButton();
    }

    private void maybeEnableSendButton() {
        sendButton.setEnabled(lastLocation != null && !getSelectedFlags().isEmpty());
    }

    private String getAccuracyString(Location location) {
        if (location == null) {
            return "";
        }
        if (location.hasAccuracy()) {
            return String.format("%.1f m", location.getAccuracy());
        } else {
            return getString(pl.org.dlapuszczy.alarmuj.R.string.unspecified_accuracy);
        }
    }

    private String getTimeString(Location location) {
        if (location == null) {
            return "";
        }

        long time = location.getTime();
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        return formatter.format(date);
    }

    private String getLocationString(Location location) {
        if (location == null) {
            return "";
        }
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String northSouth = latitude > 0.0 ? "N" : "S";
        String eastWest = longitude > 0.0 ? "E" : "W";
        Locale locale = Locale.getDefault();
        return String.format(locale, "%.8f %s, %.8f %s",
                latitude, northSouth, longitude, eastWest);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(pl.org.dlapuszczy.alarmuj.R.layout.activity_main);
        dbHelper = ReportsDbHelper.getInstance(this);

        coordinatesTv = (TextView) findViewById(pl.org.dlapuszczy.alarmuj.R.id.coordinates);
        accuracyTv = (TextView) findViewById(pl.org.dlapuszczy.alarmuj.R.id.accuracy);
        locationTimeTv = (TextView) findViewById(pl.org.dlapuszczy.alarmuj.R.id.location_time);

        for (Flag flag : Flag.values()) {
            checkBoxes.put(flag, (CheckBox) findViewById(flag.checkboxResId));
        }

        sendButton = (Button) findViewById(pl.org.dlapuszczy.alarmuj.R.id.btn_send);


        buildGoogleApiClient();

//        Intent intent = getIntent();
//        if (intent != null && intent.getAction().equals(CANCEL_SENDING)) {
//            showCancelSendingConfirmDialog();
//        }
    }

//    private void showCancelSendingConfirmDialog() {
//        Log.i(TAG, "Received Cancel Sending Intent");
//        new AlertDialog.Builder(this)
//                .setTitle(R.string.confirm_cancel_sending)
//                .setMessage(R.string.confirm_cancel_sending)
//                .setNegativeButton(R.string.no, new OnClickListener() { // android.R.string.no = "Anuluj"
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                        finish();
//                    }
//                })
//                .setPositiveButton(android.R.string.yes, new OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Intent intent = new Intent(MainActivity.this, SyncService.class);
//                        intent.setAction(STOP);
//                        startService(intent);
//                        finish();
//                    }
//                }).show();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeEnableSendButton();
        googleApiClient.connect();
        if (dbHelper.isReportPending()) {
            startSyncService();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener);
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }

    public void onSendAlertClicked(View v) {
        Log.d(TAG, "Send alert clicked");
        EnumSet<Flag> flags = getSelectedFlags();
        Report report = new Report(lastLocation, flags, System.currentTimeMillis());
        if (enqueueReport(report)) {
            showReportEnqueuedInfo(report);
            resetForm();
        } else {
            showReportEnqueueProblem();
        }
    }

    private void showReportEnqueuedInfo(Report report) {
        Toast.makeText(this, R.string.report_enqueued, Toast.LENGTH_LONG).show();
    }

    private void showReportEnqueueProblem() {
        Toast.makeText(this, R.string.report_enqueueing_problem, Toast.LENGTH_SHORT).show();
    }


    private void resetForm() {
        for (Flag flag : Flag.values()) {
            checkBoxes.get(flag).setChecked(false);
        }
    }

    public void onCheckboxClicked(View v) {
        maybeEnableSendButton();
    }

    private boolean enqueueReport(Report report) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(TAG, "Enqueuing report: " + report.flags + ", @(" + report.latitude + "," + report.longitude + "), accuracy=" + report.accuracy + ", time=" + report.time);
        boolean res = dbHelper.addReport(db, report);
        startSyncService();
        return res;
    }

    private void startSyncService() {
        Intent intent = new Intent(this, SyncService.class);
        intent.setAction(ACTION.START_FOREGROUND);
        startService(intent);
    }

    private EnumSet<Flag> getSelectedFlags() {
        EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
        for (Flag flag : Flag.values()) {
            if (checkBoxes.get(flag).isChecked()) {
                set.add(flag);
            }
        }
        return set;
    }

    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();


    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "Connection failed");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google services connected");
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        PermissionHelper.INSTANCE.doWithPermissions(MainActivity.this, new PermissionCallback() {
            @Override
            public void onResult(boolean result) {
                if (googleApiClient.isConnected()) {
//                noinspection MissingPermission
                    lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
//                noinspection MissingPermission
                    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener);
                } else {
                    Log.e(TAG, "GoogleApiClient not connected!");
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionHelper.INSTANCE.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
