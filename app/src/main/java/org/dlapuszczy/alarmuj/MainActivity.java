package org.dlapuszczy.alarmuj;

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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.dlapuszczy.alarmuj.db.ReportsDbHelper;
import org.dlapuszczy.alarmuj.util.PermissionHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnConnectionFailedListener, ConnectionCallbacks {
    private static final String TAG = "MainActivity";
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
    private CheckBox harvesterCb;
    private CheckBox forwarderCb;
    private CheckBox lumberjacksCb;
    private CheckBox blockedRoadCb;
    private TextView locationTimeTv;
    private TextView accuracyTv;
    private TextView coordinatesTv;


    private void updateCoordinateUi(Location location) {
        coordinatesTv.setText(getString(R.string.your_coordinates) + ": " + getLocationString(location));
        accuracyTv.setText(getString(R.string.accuracy) + ": " + getAccuracyString(location));
        locationTimeTv.setText(getString(R.string.location_time) + ": " + getTimeString(location));
        maybeEnableSendButton();
    }

    private void maybeEnableSendButton() {
        sendButton.setEnabled(lastLocation != null && !getFlags().isEmpty());
    }

    private String getAccuracyString(Location location) {
        if (location.hasAccuracy()) {
            return String.format("%.1f m", location.getAccuracy());
        } else {
            return getString(R.string.unspecified_accuracy);
        }
    }

    private String getTimeString(Location location) {
        long time = location.getTime();
        Date date = new Date(time);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        return formatter.format(date);
    }

    private String getLocationString(Location location) {
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
        setContentView(R.layout.activity_main);

        coordinatesTv = (TextView) findViewById(R.id.coordinates);
        accuracyTv = (TextView) findViewById(R.id.accuracy);
        locationTimeTv = (TextView) findViewById(R.id.location_time);
        harvesterCb = (CheckBox) findViewById(R.id.harvesterAtWorkCb);
        forwarderCb = (CheckBox) findViewById(R.id.forwarderAtWorkCb);
        lumberjacksCb = (CheckBox) findViewById(R.id.lumberjacksAtWorkCb);
        blockedRoadCb = (CheckBox) findViewById(R.id.blockedRoadCb);
        sendButton = (Button) findViewById(R.id.btn_send);

        buildGoogleApiClient();
        dbHelper = new ReportsDbHelper(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        maybeEnableSendButton();
        googleApiClient.connect();

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
        EnumSet<Flag> flags = getFlags();
        enqueueReport(flags, lastLocation);
    }

    public void onCheckboxClicked(View v) {
        maybeEnableSendButton();
    }

    private void enqueueReport(EnumSet<Flag> flags, Location location) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(TAG, "Enqueuing report: " + flags + ", @" + location);
        dbHelper.addReport(db, location, flags);
    }

    private EnumSet<Flag> getFlags() {
        EnumSet<Flag> set = EnumSet.noneOf(Flag.class);


        if (harvesterCb.isChecked()) set.add(Flag.HARVESTER);

        if (forwarderCb.isChecked()) set.add(Flag.FORWARDER);
        if (lumberjacksCb.isChecked()) set.add(Flag.LUMBERJACKS);

        if (blockedRoadCb.isChecked()) set.add(Flag.BLOCKED_ROAD);
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
//                noinspection MissingPermission
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener);
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
