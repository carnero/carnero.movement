package carnero.movement.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.mariux.teleport.lib.TeleportClient;
import com.mariux.teleport.lib.TeleportService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import carnero.movement.R;
import carnero.movement.common.Constants;
import carnero.movement.common.Preferences;
import carnero.movement.ui.MainActivity;

public class LocationService extends TeleportService implements LocationListener, SensorEventListener {

    private Preferences mPreferences;
    private TeleportClient mTeleport;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    private NotificationManagerCompat mNotificationManager;
    private int mWatchX = 320;
    private int mWatchY = 320;
    // counters
    private int mStepsStart;
    private int mSteps;
    private float mDistance;
    private Location mLocation;
    //
    private static final int sLocationTimeThreshold = 5 * 60 * 1000; // 5min
    private static final int sLocationDistanceThreshold = 250; // 250m
    private static final int sLocationTimeThresholdLong = 3 * 60 * 60 * 1000; // 3hr
    private static final int sLocationDistanceThresholdLong = 25; // 25m
    //
    public static final String KILL = "kill_service";

    @Override
    public void onCreate() {
        super.onCreate();

        mPreferences = new Preferences(this);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mNotificationManager = NotificationManagerCompat.from(this);

        mStepsStart = mSteps = mPreferences.getSteps();
        mDistance = mPreferences.getDistance();
        mLocation = mPreferences.getLocation();

        if (mLocation == null) {
            getLastLocation();
        }

        final String distanceStr;
        if (mDistance > 1600) {
            distanceStr = String.format(Locale.getDefault(), "%.1f", (mDistance / 1000f)) + " km | " + mSteps + " steps";
        } else {
            distanceStr = String.format(Locale.getDefault(), "%.1f", mDistance) + " m | " + mSteps + " steps";
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(getString(R.string.notification_ticker))
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(distanceStr)
                .setContentIntent(PendingIntent.getActivity(
                        LocationService.this,
                        1010,
                        new Intent(LocationService.this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ));

        startForeground(Constants.ID_NOTIFICATION_SERVICE, builder.build());

        mTeleport = new TeleportClient(this);
        mTeleport.connect();

        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra(KILL, false)) {
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String path = messageEvent.getPath();

        Log.d(Constants.TAG, "Message received: " + path);

        if (path.startsWith(Constants.PATH_RESOLUTION)) {
            Uri uri = Uri.parse(path);
            String[] resolution = uri.getQuery().split("|");

            mWatchX = Integer.parseInt(resolution[0]);
            mWatchY = Integer.parseInt(resolution[1]);
        }
    }

    @Override
    public void onDestroy() {
        mLocationManager.removeUpdates(this);
        mTeleport.disconnect();

        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            mSteps = mStepsStart + (int) event.values[0];

            mPreferences.saveSteps(mSteps);

            if ((mSteps % 100) == 0) {
                // Send to wear
                sendDataToWear();
                notifyHandheld();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // empty
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mLocation == null) {
            Log.d(Constants.TAG, "Received location: " + location.getAccuracy());
        } else {
            Log.d(Constants.TAG, "Received location: " + location.getAccuracy() + ", " + location.distanceTo(mLocation) + "m");
        }

        if (location.getAccuracy() > 1600) {
            return;
        }

        if (mLocation == null) {
            mLocation = location;

            // Save first location
            mPreferences.saveLocation(mLocation);

            // Send to wear
            sendDataToWear();
            notifyHandheld();
        } else if (
                ((mLocation.getTime() + sLocationTimeThreshold) < location.getTime() && mLocation.distanceTo(location) > sLocationDistanceThreshold)
                || ((mLocation.getTime() + sLocationTimeThresholdLong) < location.getTime() && mLocation.distanceTo(location) > sLocationDistanceThresholdLong)
                ) {
            mDistance += mLocation.distanceTo(location);
            mLocation = location;

            // Save status
            if (mDistance > 0) {
                mPreferences.saveDistance(mDistance);
            }
            if (mLocation != null) {
                mPreferences.saveLocation(mLocation);
            }

            // Send to wear
            sendDataToWear();
            notifyHandheld();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        // unused
    }

    @Override
    public void onProviderDisabled(String provider) {
        // unused
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // unused
    }

    private void init() {
        mTeleport.sendMessage(Constants.PATH_RESOLUTION, null);

        // Set listener for step counter
        Sensor stepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepCounter != null) {
            mSensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Set listeners for criteria-based provider, and for passive provider
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_MEDIUM);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setCostAllowed(true);

        final String bestProvider = mLocationManager.getBestProvider(criteria, true);
        if (bestProvider != null) {
            mLocationManager.requestLocationUpdates(
                    bestProvider,
                    sLocationTimeThreshold,
                    sLocationDistanceThreshold,
                    this
            );
        }

        boolean isPassiveBest = LocationManager.PASSIVE_PROVIDER.equals(bestProvider);
        boolean isPassiveAvail = mLocationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
        if (!isPassiveBest && isPassiveAvail) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER,
                    sLocationTimeThreshold,
                    sLocationDistanceThreshold,
                    this
            );
        }
    }

    private void getLastLocation() {
        List<String> providers = mLocationManager.getAllProviders();
        Location lastLoc = null;
        for (String provider : providers) {
            Location location = mLocationManager.getLastKnownLocation(provider);
            if (lastLoc == null || lastLoc.getTime() < location.getTime()) {
                lastLoc = location;
            }
        }

        if (lastLoc != null) {
            Log.d(Constants.TAG, "Using last location");

            onLocationChanged(lastLoc);
        }
    }

    private void sendDataToWear() {
        DataMap map = new DataMap();
        map.putInt("steps", mSteps);
        map.putFloat("distance", mDistance);
        map.putDouble("latitude", mLocation.getLatitude());
        map.putDouble("longitude", mLocation.getLongitude());
        if (mLocation.hasAltitude()) {
            map.putDouble("altitude", mLocation.getAltitude());
        }
        map.putDouble("accuracy", mLocation.getAccuracy());
        map.putLong("time", mLocation.getTime());

        ArrayList<DataMap> mapList = new ArrayList<DataMap>();
        mapList.add(map);

        PutDataMapRequest data = PutDataMapRequest.createWithAutoAppendedId("/status");
        data.getDataMap().putDataMapArrayList("status", mapList);
        syncDataItem(data);
    }

    private void notifyHandheld() {
        final String distanceStr;
        if (mDistance > 1600) {
            distanceStr = String.format(Locale.getDefault(), "%.1f", (mDistance / 1000f)) + " km | " + mSteps + " steps";
        } else {
            distanceStr = String.format(Locale.getDefault(), "%.1f", mDistance) + " m | " + mSteps + " steps";
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setWhen(mLocation.getTime())
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(getString(R.string.notification_ticker))
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(distanceStr)
                .setContentIntent(PendingIntent.getActivity(
                        LocationService.this,
                        1010,
                        new Intent(LocationService.this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ));

        mNotificationManager.notify(Constants.ID_NOTIFICATION_SERVICE, builder.build());
    }
}
