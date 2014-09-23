package carnero.movement.db;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.DateUtils;

import carnero.movement.App;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.model.Checkin;
import carnero.movement.model.Location;
import carnero.movement.model.MovementContainer;
import carnero.movement.model.MovementData;

public class Helper extends SQLiteOpenHelper {

    private static SQLiteDatabase sDatabase;
    private static Helper sInstance;

    public static Helper getInstance() {
        if (sInstance == null) {
            sInstance = new Helper(App.get());
        }

        return sInstance;
    }

    private Helper(Context context) {
        super(context, Structure.name, null, Structure.version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(Structure.getHistoryStructure());
            for (String index : Structure.getStructureIndexes()) {
                db.execSQL(index);
            }

            db.execSQL(Structure.getCheckinsStructure());
            for (String index : Structure.getCheckinsIndexes()) {
                db.execSQL(index);
            }
        } catch (SQLException e) {
            RemoteLog.e("Failed to create database");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(Structure.getCheckinsStructure());
            for (String index : Structure.getCheckinsIndexes()) {
                db.execSQL(index);
            }
        }

        if (oldVersion < 3) {
            db.execSQL("drop table " + Structure.Table.Checkins.name);

            db.execSQL(Structure.getCheckinsStructure());
            for (String index : Structure.getCheckinsIndexes()) {
                db.execSQL(index);
            }
        }
    }

    private synchronized SQLiteDatabase getDatabase() {
        if (sDatabase == null) {
            sDatabase = getWritableDatabase();
            if (sDatabase.inTransaction()) {
                sDatabase.endTransaction();
            }
        }

        return sDatabase;
    }

    // Movement

    public boolean saveData(float steps, float distance, android.location.Location location) {
        boolean status = false;

        ContentValues values = new ContentValues();
        values.put(Structure.Table.History.TIME, System.currentTimeMillis());
        values.put(Structure.Table.History.STEPS, steps);
        values.put(Structure.Table.History.DISTANCE, distance);
        if (location != null) {
            values.put(Structure.Table.History.LATITUDE, location.getLatitude());
            values.put(Structure.Table.History.LONGITUDE, location.getLongitude());
            values.put(Structure.Table.History.ACCURACY, location.getAccuracy());
        }

        long id = getDatabase().insert(Structure.Table.History.name, null, values);
        if (id >= 0) {
            status = true;
        }

        return status;
    }

    public MovementData getSummaryForDay(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        calendar.add(Calendar.DAY_OF_MONTH, day);
        long millisStart = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long millisEnd = calendar.getTimeInMillis();

        return getSummary(millisStart, millisEnd);
    }

    public MovementData getSummary(long start, long end) {
        Cursor cursor;
        int stepsStart = 0;
        float distanceStart = 0;
        int stepsEnd = -1;
        float distanceEnd = -1;

        // Get last entry from previous day
        cursor = null;
        try {
            cursor = getDatabase().query(
                Structure.Table.History.name,
                Structure.Table.History.projectionData,
                Structure.Table.History.TIME + " < " + start,
                null, null, null,
                Structure.Table.History.TIME + " desc",
                "1"
            );

            if (cursor.moveToFirst()) {
                int idxSteps = cursor.getColumnIndex(Structure.Table.History.STEPS);
                int idxDistance = cursor.getColumnIndex(Structure.Table.History.DISTANCE);

                stepsStart = cursor.getInt(idxSteps);
                distanceStart = cursor.getFloat(idxDistance);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Get last entry from previous day
        cursor = null;
        try {
            cursor = getDatabase().query(
                Structure.Table.History.name,
                Structure.Table.History.projectionData,
                Structure.Table.History.TIME + " <= " + end,
                null, null, null,
                Structure.Table.History.TIME + " desc",
                "1"
            );

            if (cursor.moveToFirst()) {
                int idxSteps = cursor.getColumnIndex(Structure.Table.History.STEPS);
                int idxDistance = cursor.getColumnIndex(Structure.Table.History.DISTANCE);

                stepsEnd = cursor.getInt(idxSteps);
                distanceEnd = cursor.getFloat(idxDistance);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (stepsEnd == -1 || distanceEnd == -1) {
            return null;
        }

        final MovementData summary = new MovementData();
        summary.steps = stepsEnd - stepsStart;
        summary.distance = distanceEnd - distanceStart;

        return summary;
    }

    public MovementContainer getDataForDay(int day) {
        return getDataForDay(day, -1);
    }

    public MovementContainer getDataForDay(int day, int intervals) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        calendar.add(Calendar.DAY_OF_MONTH, day);
        long millisStart = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long millisEnd = calendar.getTimeInMillis();

        return getData(millisStart, millisEnd, intervals);
    }

    public MovementContainer getData(long start, long end) {
        return getData(start, end, -1);
    }

    public MovementContainer getData(long start, long end, int intervals) {
        long millisInterval;

        if (intervals < 0) {
            int days = (int)((end - start) / DateUtils.DAY_IN_MILLIS);

            if (days <= 1) {
                millisInterval = DateUtils.HOUR_IN_MILLIS;
            } else if (days <= 3) {
                millisInterval = DateUtils.HOUR_IN_MILLIS * 2;
            } else if (days <= 7) {
                millisInterval = DateUtils.HOUR_IN_MILLIS * 4;
            } else {
                millisInterval = DateUtils.HOUR_IN_MILLIS * 8;
            }

            intervals = (int)Math.ceil((end - start) / millisInterval);
        } else {
            millisInterval = (end - start) / intervals;
        }

        long oldest = Long.MAX_VALUE;

        final MovementContainer container = new MovementContainer();
        container.movements = new MovementData[intervals];
        container.locations = new ArrayList<Location>();

        // Get entries for given interval
        Cursor cursor = null;
        try {
            cursor = getDatabase().query(
                Structure.Table.History.name,
                Structure.Table.History.projectionFull,
                Structure.Table.History.TIME + " >= " + start + " and " + Structure.Table.History.TIME + " <= " + end,
                null, null, null,
                Structure.Table.History.TIME + " desc"
            );

            if (cursor.moveToFirst()) {
                int idxTime = cursor.getColumnIndex(Structure.Table.History.TIME);
                int idxSteps = cursor.getColumnIndex(Structure.Table.History.STEPS);
                int idxDistance = cursor.getColumnIndex(Structure.Table.History.DISTANCE);
                int idxLatitude = cursor.getColumnIndex(Structure.Table.History.LATITUDE);
                int idxLongitude = cursor.getColumnIndex(Structure.Table.History.LONGITUDE);
                int idxAccuracy = cursor.getColumnIndex(Structure.Table.History.ACCURACY);

                do {
                    // Movements
                    long time = cursor.getLong(idxTime);
                    if (time < oldest) {
                        oldest = time;
                    }

                    // Oldest is the first interval
                    int interval = intervals - ((int)((end - time) / millisInterval)) - 1;

                    MovementData movement = container.movements[interval];
                    if (movement == null) {
                        movement = new MovementData();
                        movement.steps = cursor.getInt(idxSteps);
                        movement.distance = cursor.getFloat(idxDistance);

                        container.movements[interval] = movement;
                    } else {
                        movement.steps = Math.max(movement.steps, cursor.getInt(idxSteps));
                        movement.distance = Math.max(movement.distance, cursor.getFloat(idxDistance));
                    }

                    // Locations
                    if (!cursor.isNull(idxLatitude) && !cursor.isNull(idxLongitude)) {
                        Location location = new Location();
                        location.time = time;
                        location.latitude = cursor.getDouble(idxLatitude);
                        location.longitude = cursor.getDouble(idxLongitude);
                        location.accuracy = cursor.getDouble(idxAccuracy);

                        container.locations.add(location);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Get oldest entry before given interval
        try {
            cursor = getDatabase().query(
                Structure.Table.History.name,
                Structure.Table.History.projectionData,
                Structure.Table.History.TIME + " < " + oldest,
                null, null, null,
                Structure.Table.History.TIME + " desc",
                "1"
            );

            if (cursor.moveToFirst()) {
                int idxSteps = cursor.getColumnIndex(Structure.Table.History.STEPS);
                int idxDistance = cursor.getColumnIndex(Structure.Table.History.DISTANCE);

                MovementData model = new MovementData();
                model.steps = cursor.getInt(idxSteps);
                model.distance = cursor.getFloat(idxDistance);

                container.previousEntry = model;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (container.previousEntry == null) {
            container.previousEntry = new MovementData();
            container.previousEntry.steps = 0;
            container.previousEntry.distance = 0;
        }

        return container;
    }

    public ArrayList<Location> getLocationsForDay(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        calendar.add(Calendar.DAY_OF_MONTH, day);
        long millisStart = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long millisEnd = calendar.getTimeInMillis();

        return getLocations(millisStart, millisEnd);
    }

    public ArrayList<Location> getLocations(long start, long end) {
        final ArrayList<Location> data = new ArrayList<Location>();

        Cursor cursor = null;
        try {
            cursor = getDatabase().query(
                Structure.Table.History.name,
                Structure.Table.History.projectionLocation,
                Structure.Table.History.TIME + " >= " + start + " and " + Structure.Table.History.TIME + " <= " + end,
                null, null, null,
                Structure.Table.History.TIME + " desc"
            );

            if (cursor.moveToFirst()) {
                int idxTime = cursor.getColumnIndex(Structure.Table.History.TIME);
                int idxLatitude = cursor.getColumnIndex(Structure.Table.History.LATITUDE);
                int idxLongitude = cursor.getColumnIndex(Structure.Table.History.LONGITUDE);
                int idxAccuracy = cursor.getColumnIndex(Structure.Table.History.ACCURACY);

                do {
                    Location model = new Location();
                    model.time = cursor.getLong(idxTime);
                    model.latitude = cursor.getDouble(idxLatitude);
                    model.longitude = cursor.getDouble(idxLongitude);
                    model.accuracy = cursor.getDouble(idxAccuracy);

                    data.add(model);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return data;
    }

    // Foursquare

    public long getLatestCheckinTime() {
        long time = 0;

        Cursor cursor = null;
        try {
            cursor = getDatabase().query(
                Structure.Table.Checkins.name,
                new String[] {Structure.Table.Checkins.CREATED},
                null, null, null, null,
                Structure.Table.Checkins.CREATED + " desc",
                "1"
            );

            if (cursor.moveToFirst()) {
                int idxTime = cursor.getColumnIndex(Structure.Table.Checkins.CREATED);
                time = cursor.getLong(idxTime);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return time;
    }

    public boolean saveCheckin(Checkin checkin) {
        boolean status = false;

        ContentValues values = new ContentValues();
        values.put(Structure.Table.Checkins.CHECKIN_ID, checkin.checkinId);
        values.put(Structure.Table.Checkins.CREATED, checkin.createdAt);
        values.put(Structure.Table.Checkins.LATITUDE, checkin.latitude);
        values.put(Structure.Table.Checkins.LONGITUDE, checkin.longitude);
        values.put(Structure.Table.Checkins.NAME, checkin.name);
        values.put(Structure.Table.Checkins.SHOUT, checkin.shout);
        values.put(Structure.Table.Checkins.ICON_PREFIX, checkin.iconPrefix);
        values.put(Structure.Table.Checkins.ICON_SUFFIX, checkin.iconSuffix);

        long id = getDatabase().insertWithOnConflict(
            Structure.Table.Checkins.name,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        );

        if (id >= 0) {
            status = true;
        }

        return status;
    }
}
