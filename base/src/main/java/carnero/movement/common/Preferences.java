package carnero.movement.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

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
    private static final String PREF_GRAPH_ABS = "graph_type";
    private static final String PREF_BACKUP = "db_backup";
    private static final String PREF_FOURSQUARE_TOKEN = "fsq_token";

    public Preferences() {
        mPrefs = Application.get().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /* Total steps */

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

    /* Last output of step counter */

    public void saveStepsSensor(int steps) {
        mPrefs.edit()
            .putInt(PREF_STEPS_SENSOR, steps)
            .apply();
    }

    public int getStepsSensor() {
        return mPrefs.getInt(PREF_STEPS_SENSOR, 0);
    }

    /* Last known location */

    public void saveLocation(Location location) {
        if (!mPrefs.contains(PREF_FIRST)) {
            mPrefs.edit()
                .putLong(PREF_FIRST, System.currentTimeMillis())
                .putFloat(PREF_LATITUDE, (float)location.getLatitude())
                .putFloat(PREF_LONGITUDE, (float)location.getLongitude())
                .putFloat(PREF_ACCURACY, location.getAccuracy())
                .putLong(PREF_TIME, location.getTime())
                .apply();
        } else {
            mPrefs.edit()
                .putFloat(PREF_LATITUDE, (float)location.getLatitude())
                .putFloat(PREF_LONGITUDE, (float)location.getLongitude())
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

    /* Total distance */

    public void saveDistance(float distance) {
        mPrefs.edit()
            .putFloat(PREF_DISTANCE, distance)
            .apply();
    }

    public float getDistance() {
        return mPrefs.getFloat(PREF_DISTANCE, 0f);
    }

    /* Type of graph (incremental, differential) */

    public void setGraphType(boolean absolute) {
        mPrefs.edit()
            .putBoolean(PREF_GRAPH_ABS, absolute)
            .apply();
    }

    public boolean getGraphType() {
        return mPrefs.getBoolean(PREF_GRAPH_ABS, false);
    }

    /* Last DB backup */

    public void saveLastBackup(long time) {
        mPrefs.edit()
            .putLong(PREF_BACKUP, time)
            .apply();
    }

    public long getLastBackup() {
        return mPrefs.getLong(PREF_BACKUP, 0l);
    }

    /* Foursquare token */

    public boolean hasFoursquareToken() {
        return mPrefs.contains(PREF_FOURSQUARE_TOKEN);
    }

    public void saveFoursquareToken(String token) {
        mPrefs.edit()
            .putString(PREF_FOURSQUARE_TOKEN, token)
            .apply();
    }

    public String getFoursquareToken() {
        return mPrefs.getString(PREF_FOURSQUARE_TOKEN, null);
    }
}
