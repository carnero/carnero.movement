package carnero.movement.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import carnero.movement.App;

public class Preferences {

    private SharedPreferences mPrefs;
    //
    private static final String FILE = "movement.prefs";
    private static final String PREF_FIRST = "first";
    private static final String PREF_STEPS = "steps";
    private static final String PREF_STEPS_SENSOR = "steps_last";
    private static final String PREF_DISTANCE = "distance";
    private static final String PREF_LATITUDE = "loc_latitude";
    private static final String PREF_LONGITUDE = "loc_longitude";
    private static final String PREF_ACCURACY = "loc_accuracy";
    private static final String PREF_TIME = "loc_time";

    public Preferences(Context context) {
        mPrefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public void saveSteps(int steps) {
        if (!mPrefs.contains(PREF_FIRST)) {
            mPrefs.edit()
                    .putLong(PREF_FIRST, System.currentTimeMillis())
                    .putInt(PREF_STEPS, steps)
                    .apply();
        } else {
            mPrefs.edit()
                    .putInt(PREF_STEPS, steps)
                    .apply();
        }
    }

    public int getSteps() {
        return mPrefs.getInt(PREF_STEPS, 0);
    }

    public void saveStepsSensor(int steps) {
        mPrefs.edit()
                .putInt(PREF_STEPS_SENSOR, steps)
                .apply();
    }

    public int getStepsSensor() {
        return mPrefs.getInt(PREF_STEPS_SENSOR, 0);
    }

    public void saveLocation(Location location) {
        if (!mPrefs.contains(PREF_FIRST)) {
            mPrefs.edit()
                    .putLong(PREF_FIRST, System.currentTimeMillis())
                    .putFloat(PREF_LATITUDE, (float) location.getLatitude())
                    .putFloat(PREF_LONGITUDE, (float) location.getLongitude())
                    .putFloat(PREF_ACCURACY, location.getAccuracy())
                    .putLong(PREF_TIME, location.getTime())
                    .apply();
        } else {
            mPrefs.edit()
                    .putFloat(PREF_LATITUDE, (float) location.getLatitude())
                    .putFloat(PREF_LONGITUDE, (float) location.getLongitude())
                    .putFloat(PREF_ACCURACY, location.getAccuracy())
                    .putLong(PREF_TIME, location.getTime())
                    .apply();
        }
    }

    public Location getLocation() {
        if (!mPrefs.contains(PREF_LATITUDE) || !mPrefs.contains(PREF_LONGITUDE)) {
            return null;
        }

        final Location location = new Location("HISTORY");
        location.setLatitude(mPrefs.getFloat(PREF_LATITUDE, 0f));
        location.setLongitude(mPrefs.getFloat(PREF_LONGITUDE, 0f));
        location.setAccuracy(mPrefs.getFloat(PREF_ACCURACY, 0f));
        location.setTime(mPrefs.getLong(PREF_TIME, 0l));

        return location;
    }

    public void saveDistance(float distance) {
        mPrefs.edit()
                .putFloat(PREF_DISTANCE, distance)
                .apply();
    }

    public float getDistance() {
        return mPrefs.getFloat(PREF_DISTANCE, 0f);
    }
}
