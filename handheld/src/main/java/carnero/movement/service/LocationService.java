package carnero.movement.service;

import java.text.DateFormat;
import java.util.*;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.*;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import carnero.movement.R;
import carnero.movement.common.Constants;
import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.comparator.LocationComparator;
import carnero.movement.db.Helper;
import carnero.movement.db.ModelData;
import carnero.movement.db.ModelDataContainer;
import carnero.movement.receiver.WakeupReceiver;
import carnero.movement.ui.MainActivity;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.mariux.teleport.lib.TeleportClient;
import com.mariux.teleport.lib.TeleportService;

public class LocationService
    extends TeleportService
    implements LocationListener, SensorEventListener {

    private Preferences mPreferences;
    private Helper mHelper;
    private TeleportClient mTeleport;
    private AlarmManager mAlarmManager;
    private PowerManager mPowerManager;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    private NotificationManagerCompat mNotificationManager;
    private PowerManager.WakeLock mWakeLock;
    private Sensor mMotionSensor;
    private MotionListener mMotionListener;
    private final ArrayList<Location> mLocationHistory = new ArrayList<Location>();
    private boolean[] mObtained = new boolean[]{false, false}; // Matches OBTAINED_ constants
    private int mWatchX = 320;
    private int mWatchY = 320;
    private long mLastMotion;
    private long mLastSaveToDB = 0;
    private long mLastSentToWear = 0;
    // counters
    private int mStepsStart;
    private int mStepsSensor;
    private int mSteps;
    private float mDistance;
    private Location mLocation;
    //
    private static final int sLocationTimeThreshold = 5 * 60 * 1000; // 5min
    private static final int sLocationDistanceThreshold = 250; // 250m
    private static final int sLocationTimeThresholdLong = 3 * 60 * 60 * 1000; // 3hr
    private static final int sLocationDistanceThresholdLong = 25; // 25m
    private static final int sMotionWindowStart = 10 * 60 * 1000; // 10 min
    private static final int sMotionWindowEnd = 45 * 60 * 1000; // 45 min
    //
    private static final int OBTAINED_STEPS = 0;
    private static final int OBTAINED_LOCATION = 1;
    //
    public static final String KILL = "kill_service";
    public static final String WAKE = "wake_run_service";

    @Override
    public void onCreate() {
        super.onCreate();

        mPreferences = new Preferences(this);
        mHelper = new Helper(this);
        mAlarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mPowerManager = (PowerManager)getSystemService(POWER_SERVICE);
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mMotionSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mNotificationManager = NotificationManagerCompat.from(this);

        // Load saved values
        mStepsStart = mSteps = mPreferences.getSteps();
        mStepsSensor = mPreferences.getStepsSensor();
        mDistance = mPreferences.getDistance();
        mLocation = mPreferences.getLocation();

        if (mLocation == null) {
            getLastLocation();
        }

        // Initialize
        mTeleport = new TeleportClient(this);
        mTeleport.connect();

        // Fire initial notification & start service
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_notification)
            .setTicker(getString(R.string.notification_ticker))
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(Utils.formatDistance(mDistance) + " | " + mSteps + " steps")
            .setContentIntent(PendingIntent.getActivity(
                LocationService.this,
                1010,
                new Intent(LocationService.this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
            ));

        startForeground(Constants.ID_NOTIFICATION_SERVICE, builder.build());

        // Set alarm for repeating
        final Intent intent = new Intent(this, WakeupReceiver.class);
        intent.putExtra(WAKE, true);

        final PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        mAlarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + (10 * 60 * 1000),
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            alarmIntent
        );

        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra(KILL, false)) {
            RemoteLog.i("Requested kill");

            stopSelf();
        } else if (intent != null && intent.getBooleanExtra(WAKE, false)) {
            RemoteLog.i("Requested wake");

            // Check battery level
            final float battery = Utils.getBatteryLevel();
            if (battery < 20) {
                RemoteLog.i("Wake denied, battery @ " + battery + "%");
            } else {
                resetObtained();

                // Get wake lock
                if (mWakeLock != null && mWakeLock.isHeld()) {
                    mWakeLock.release();
                }

                mWakeLock = mPowerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    ((Object)this).getClass().getSimpleName()
                );
                mWakeLock.acquire();

                // Set timer for releasing wake lock
                final TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            getLastLocation(); // Get last location in case LocationManager didn't fire event
                            handleData(); // Send data we have to refresh Wear

                            mWakeLock.release();
                            mWakeLock = null;

                            RemoteLog.i("Wake lock released (timer)");
                        }
                    }
                };

                final Timer timer = new Timer(true);
                timer.schedule(task, 60 * 1000); // 1 mins

                // Request significant motion to be sure
                requestMotionSensor();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String path = messageEvent.getPath();

        RemoteLog.d("Message received: " + path);

        if (path.startsWith(Constants.PATH_RESOLUTION)) {
            Uri uri = Uri.parse(path);
            String[] resolution = uri.getQuery().split("|");

            mWatchX = Integer.parseInt(resolution[0]);
            mWatchY = Integer.parseInt(resolution[1]);
        }
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        if (mMotionListener != null && mMotionSensor != null) {
            mSensorManager.cancelTriggerSensor(mMotionListener, mMotionSensor);
        }
        mLocationManager.removeUpdates(this);
        mTeleport.disconnect();

        RemoteLog.wtf("Destroying service");

        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            final int steps = (int)event.values[0];

            if (mStepsSensor > steps) { // Device was probably rebooted
                mStepsSensor = 0;
            }

            RemoteLog.d("Received steps: " + (steps - mStepsSensor) + ", real: " + steps);
            mSteps = mStepsStart + (steps - mStepsSensor);

            mPreferences.saveSteps(mSteps);
            mPreferences.saveStepsSensor(steps);

            // Save & send data
            handleData();

            // Notify wake lock (if held)
            setObtained(OBTAINED_STEPS);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // empty
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mLocation == null) {
            RemoteLog.d(
                "Received location from " + location.getProvider() + "..."
                    + " accuracy: " + String.format("%.0f", location.getAccuracy()) + "m"
            );
        } else {
            RemoteLog.d(
                "Received location from " + location.getProvider() + "..."
                    + " accuracy: " + String.format("%.0f", location.getAccuracy()) + "m;"
                    + " distance: " + String.format("%.1f", location.distanceTo(mLocation)) + "m"
            );
        }

        if (location.getAccuracy() > 1000) {
            return;
        }

        boolean distanceThrShort = (mLocation.getTime() + sLocationTimeThreshold) < location.getTime() && mLocation.distanceTo(location) > sLocationDistanceThreshold;
        boolean distanceThrLong = (mLocation.getTime() + sLocationTimeThresholdLong) < location.getTime() && mLocation.distanceTo(location) > sLocationDistanceThresholdLong;

        if (distanceThrShort || distanceThrLong) {
            synchronized (mLocationHistory) {
                mLocationHistory.add(location);
            }

            checkLocation();
        }

        // Notify wake lock (if held)
        setObtained(OBTAINED_LOCATION);
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
            boolean batchMode = mSensorManager.registerListener(
                this,
                stepCounter,
                SensorManager.SENSOR_DELAY_NORMAL,
                20000000 // 20 seconds
            );

            RemoteLog.i("Step counter registered (batch:" + batchMode + ")");
        } else {
            RemoteLog.i("Step counter is not present on this device");
        }

        // Significant motion sensor
        requestMotionSensor();

        // Set listeners for criteria-based provider, and for passive provider
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
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
            RemoteLog.i("Location provider registered: " + bestProvider);
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
            RemoteLog.i("Location provider registered: passive");
        }
    }

    private void getLastLocation() {
        List<String> providers = mLocationManager.getAllProviders();
        Location lastLoc = null;
        for (String provider : providers) {
            Location location = mLocationManager.getLastKnownLocation(provider);
            if (location != null && (lastLoc == null || lastLoc.getTime() < location.getTime())) {
                lastLoc = location;
            }
        }

        if (lastLoc != null) {
            RemoteLog.d("Using last location from " + lastLoc.getProvider());

            onLocationChanged(lastLoc);
        }
    }

    private void checkLocation() {
        synchronized (mLocationHistory) {
            Collections.sort(mLocationHistory, new LocationComparator());

            long start = mLastMotion - sMotionWindowStart;
            long end = mLastMotion + sMotionWindowEnd;
            for (Location location : mLocationHistory) {
                if (mLocation != null && mLocation.getTime() >= location.getTime()) {
                    // Location from history is older than already processed location
                    continue;
                }
                if (location.getTime() < start || location.getTime() > end) {
                    // Location is not withing significant motion window
                    continue;
                }

                if (mLocation != null) {
                    mDistance += mLocation.distanceTo(location);
                }
                mLocation = location;

                // Save status
                if (mDistance > 0) {
                    mPreferences.saveDistance(mDistance);
                }
                mPreferences.saveLocation(mLocation);
            }

            // Clean history
            final ArrayList<Location> drop = new ArrayList<Location>();
            for (Location location : mLocationHistory) {
                if (location.getTime() <= mLocation.getTime()) {
                    drop.add(location);
                } else {
                    break; // It's sorted by time
                }
            }

            for (Location dr : drop) {
                mLocationHistory.remove(dr);
            }
        }

        // Save & send data
        handleData();
    }

    private void resetObtained() {
        for (int i = 0; i < mObtained.length; i++) {
            mObtained[i] = false;
        }
    }

    private void setObtained(int what) {
        mObtained[what] = true;

        boolean all = true;
        for (boolean obtained : mObtained) {
            if (!obtained) {
                all = false;

                break;
            }
        }

        if (all && mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;

            RemoteLog.i("Wake lock released (obtained)");
        }
    }

    private void requestMotionSensor() {
        if (mMotionListener != null && mMotionSensor != null) {
            mSensorManager.cancelTriggerSensor(mMotionListener, mMotionSensor);
        }

        if (mMotionSensor != null) {
            mMotionListener = new MotionListener();
            mSensorManager.requestTriggerSensor(mMotionListener, mMotionSensor);
        } else {
            mMotionListener = null;
        }
    }

    private void handleData() {
        saveToDB();
        sendDataToWear();
        notifyHandheld();
    }

    private void saveToDB() {
        if (mLastSaveToDB > (SystemClock.elapsedRealtime() - (5 * 60 * 1000))) { // Once in 10 mins
            return;
        }

        boolean status = mHelper.saveData(mSteps, mDistance, mLocation);
        if (status) {
            mLastSaveToDB = SystemClock.elapsedRealtime();
        }
    }

    private void sendDataToWear() {
        if (mLastSentToWear > (SystemClock.elapsedRealtime() - (1 * 60 * 1000))) { // Once in 1 min
            return;
        }

        // Summary
        final ArrayList<DataMap> statusList = new ArrayList<DataMap>();

        DataMap statusMap = new DataMap();
        statusMap.putInt("steps", mSteps);
        statusMap.putFloat("distance", mDistance);
        statusMap.putLong("motion", mLastMotion);

        ModelData today = mHelper.getSummaryForDay(0);
        statusMap.putInt("steps_today", today.steps);
        statusMap.putFloat("distance_today", today.distance);

        statusList.add(statusMap);

        // History
        double minDst = Double.MAX_VALUE;
        double maxDst = Double.MIN_VALUE;
        double minStp = Double.MAX_VALUE;
        double maxStp = Double.MIN_VALUE;
        double stepsPrev = -1d;
        double distancePrev = -1d;

        final ModelDataContainer container = mHelper.getDataForDay(0);
        final ArrayList<Double> distanceArray = new ArrayList<Double>();
        final ArrayList<Double> stepsArray = new ArrayList<Double>();

        if (container.previousEntry != null) {
            stepsPrev = container.previousEntry.steps;
            distancePrev = container.previousEntry.distance;
        }

        for (int i = 0; i < container.movements.length; i++) {
            ModelData model = container.movements[i];

            double steps;
            double distance;
            if (model == null) {
                steps = 0;
                distance = 0;
            } else if (stepsPrev == -1f || distancePrev == -1f) {
                stepsPrev = model.steps;
                distancePrev = model.distance;

                continue;
            } else {
                steps = model.steps - stepsPrev;
                distance = model.distance - distancePrev;
                stepsPrev = model.steps;
                distancePrev = model.distance;
            }

            distanceArray.add(distance);
            stepsArray.add(steps);

            minDst = Math.min(minDst, distance);
            maxDst = Math.max(maxDst, distance);
            minStp = Math.min(minStp, steps);
            maxStp = Math.max(maxStp, steps);
        }

        // Normalize data
        double ratio = 1.0f;
        int ratioLine = -1; // steps:0, distance:1
        if (maxStp > maxDst && maxDst > 100) {
            ratio = maxStp / maxDst;
            ratioLine = 0;
        } else if (maxStp < maxDst && maxStp > 100) {
            ratio = maxDst / maxStp;
            ratioLine = 1;
        }

        // Create DataMaps
        final ArrayList<DataMap> stepsList = new ArrayList<DataMap>();
        for (int i = 0; i < stepsArray.size(); i++) {
            DataMap map = new DataMap();
            if (ratioLine == 0) {
                map.putDouble("value", stepsArray.get(i) / ratio);
            } else {
                map.putDouble("value", stepsArray.get(i));
            }

            stepsList.add(map);
        }

        final ArrayList<DataMap> distanceList = new ArrayList<DataMap>();
        for (int i = 0; i < distanceArray.size(); i++) {
            DataMap map = new DataMap();
            if (ratioLine == 1) {
                map.putDouble("value", distanceArray.get(i) / ratio);
            } else {
                map.putDouble("value", distanceArray.get(i));
            }

            distanceList.add(map);
        }

        // Send data
        PutDataMapRequest data = PutDataMapRequest.createWithAutoAppendedId("/status");
        data.getDataMap().putDataMapArrayList("summary", statusList);
        data.getDataMap().putDataMapArrayList("steps", stepsList);
        data.getDataMap().putDataMapArrayList("distance", distanceList);
        syncDataItem(data);

        mLastSentToWear = SystemClock.elapsedRealtime();
    }

    private void notifyHandheld() {
        String text = Utils.formatDistance(mDistance) + " | " + getString(R.string.stats_steps, mSteps);
        text += "\n"
            + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(mLastMotion))
            + " // "
            + mLocationHistory.size();

        final Notification.Builder builder = new Notification.Builder(this)
            .setPriority(Notification.PRIORITY_MIN)
            .setOngoing(true)
            .setWhen(mLocation.getTime())
            .setSmallIcon(R.drawable.ic_notification)
            .setTicker(getString(R.string.notification_ticker))
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setContentIntent(PendingIntent.getActivity(
                LocationService.this,
                1010,
                new Intent(LocationService.this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
            ));

        mNotificationManager.notify(Constants.ID_NOTIFICATION_SERVICE, builder.build());
    }

    // Classes

    private class MotionListener extends TriggerEventListener {

        @Override
        public void onTrigger(TriggerEvent event) {
            mLastMotion = event.timestamp / 1000000; // ns
            requestMotionSensor();

            checkLocation();
        }
    }
}
