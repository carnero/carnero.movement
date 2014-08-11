package carnero.movement.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

public class LocationService extends TeleportService implements LocationListener {

    private Preferences mPreferences;
    private TeleportClient mTeleport;
    private LocationManager mLocationManager;
    private NotificationManagerCompat mNotificationManager;
    private float mDistance;
    private Location mLastLocation;
    private int mWatchX = 320;
    private int mWatchY = 320;
    //
    private static final int sLocationTimeThreshold = 5 * 60 * 1000; // 5min
    private static final int sLocationDistanceThreshold = 250; // 250m
    //
    public static final String KILL = "kill_service";

    @Override
    public void onCreate() {
        super.onCreate();

        mPreferences = new Preferences(this);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mNotificationManager = NotificationManagerCompat.from(this);

        mDistance = mPreferences.getDistance();
        mLastLocation = mPreferences.getLocation();

        if (mLastLocation == null) {
            getLastLocation();
        }

        final String distanceStr;
        if (mDistance > 1600) {
            distanceStr = String.format(Locale.getDefault(), "%.1f", (mDistance / 1000f)) + " km";
        } else {
            distanceStr = String.format(Locale.getDefault(), "%.1f", mDistance) + " m";
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
    public void onLocationChanged(Location location) {
        if (mLastLocation == null) {
            Log.d(Constants.TAG, "Received location: " + location.getAccuracy());
        } else {
            Log.d(Constants.TAG, "Received location: " + location.getAccuracy() + ", " + location.distanceTo(mLastLocation) + "m");
        }

        if (location.getAccuracy() > 1600) {
            return;
        }

        if (mLastLocation == null) {
            mLastLocation = location;

            // Save first location
            mPreferences.saveLocation(mLastLocation);

            Log.d(Constants.TAG, "Distance: " + mDistance + "m // Last source: " + location.getProvider() + " (first)");

            // Send to wear
            sendDataToWear(mDistance, mLastLocation);
            notifyHandheld(mDistance, mLastLocation);
        } else if ((mLastLocation.getTime() + sLocationTimeThreshold) < location.getTime()
                && mLastLocation.distanceTo(location) > sLocationDistanceThreshold) {
            mDistance += mLastLocation.distanceTo(location);
            mLastLocation = location;

            // Save status
            if (mDistance > 0) {
                mPreferences.saveDistance(mDistance);
            }
            if (mLastLocation != null) {
                mPreferences.saveLocation(mLastLocation);
            }

            Log.d(Constants.TAG, "Distance: " + mDistance + "m // Last source: " + location.getProvider());

            // Send to wear
            sendDataToWear(mDistance, mLastLocation);
            notifyHandheld(mDistance, mLastLocation);
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

    private void sendDataToWear(float distance, Location location) {
        DataMap map = new DataMap();
        map.putFloat("distance", distance);
        map.putDouble("latitude", location.getLatitude());
        map.putDouble("longitude", location.getLongitude());
        if (location.hasAltitude()) {
            map.putDouble("altitude", location.getAltitude());
        }
        map.putDouble("accuracy", location.getAccuracy());
        map.putLong("time", location.getTime());

        ArrayList<DataMap> mapList = new ArrayList<DataMap>();
        mapList.add(map);

        PutDataMapRequest data = PutDataMapRequest.createWithAutoAppendedId("/status");
        data.getDataMap().putDataMapArrayList("status", mapList);
        syncDataItem(data);
    }

    private void notifyHandheld(float distance, Location location) {
        final String distanceStr;
        if (distance > 1600) {
            distanceStr = String.format(Locale.getDefault(), "%.1f", (distance / 1000f)) + " km";
        } else {
            distanceStr = String.format(Locale.getDefault(), "%.1f", distance) + " m";
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setWhen(location.getTime())
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
