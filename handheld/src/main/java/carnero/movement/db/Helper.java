package carnero.movement.db;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.text.format.DateUtils;

import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
import carnero.movement.common.remotelog.RemoteLog;

public class Helper extends SQLiteOpenHelper {

    private static SQLiteDatabase sDatabaseRO;
    private static SQLiteDatabase sDatabaseRW;

    public Helper(Context context) {
        super(context, Structure.name, null, Structure.version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(Structure.getHistoryStructure());
            for (String index : Structure.getStructureIndexes()) {
                db.execSQL(index);
            }
        } catch (SQLException e) {
            RemoteLog.e("Failed to create database");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table " + Structure.Table.History.name);

        onCreate(db);
    }

    private synchronized SQLiteDatabase getDatabaseRO() {
        if (sDatabaseRO == null) {
            sDatabaseRO = getReadableDatabase();
        }

        return sDatabaseRO;
    }

    private synchronized SQLiteDatabase getDatabaseRW() {
        if (sDatabaseRW == null) {
            sDatabaseRW = getWritableDatabase();
            if (sDatabaseRW.inTransaction()) {
                sDatabaseRW.endTransaction();
            }
        }

        return sDatabaseRW;
    }

    public boolean saveData(float steps, float distance, Location location) {
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

        long id = getDatabaseRW().insert(Structure.Table.History.name, null, values);
        if (id >= 0) {
            status = true;
        }

        return status;
    }

    public ModelData getSummaryForDay(int day) {
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

    public ModelData getSummary(long start, long end) {
        Cursor cursor;
        int stepsStart = 0;
        float distanceStart = 0;
        int stepsEnd = -1;
        float distanceEnd = -1;

        // Get last entry from previous day
        cursor = null;
        try {
            cursor = getDatabaseRO().query(
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
            cursor = getDatabaseRO().query(
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

        final ModelData summary = new ModelData();
        summary.steps = stepsEnd - stepsStart;
        summary.distance = distanceEnd - distanceStart;

        return summary;
    }

    public ModelDataContainer getDataForDay(int day) {
        return getDataForDay(day, -1);
    }

    public ModelDataContainer getDataForDay(int day, int intervals) {
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

    public ModelDataContainer getData(long start, long end) {
        return getData(start, end, -1);
    }

    public ModelDataContainer getData(long start, long end, int intervals) {
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

        final ModelDataContainer container = new ModelDataContainer();
        container.movements = new ModelData[intervals];
        container.locations = new ArrayList<ModelLocation>();

        // Get entries for given interval
        Cursor cursor = null;
        try {
            cursor = getDatabaseRO().query(
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

                    ModelData movement = container.movements[interval];
                    if (movement == null) {
                        movement = new ModelData();
                        movement.steps = cursor.getInt(idxSteps);
                        movement.distance = cursor.getFloat(idxDistance);

                        container.movements[interval] = movement;
                    } else {
                        movement.steps = Math.max(movement.steps, cursor.getInt(idxSteps));
                        movement.distance = Math.max(movement.distance, cursor.getFloat(idxDistance));
                    }

                    // Locations
                    if (!cursor.isNull(idxLatitude) && !cursor.isNull(idxLongitude)) {
                        ModelLocation location = new ModelLocation();
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
            cursor = getDatabaseRO().query(
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

                ModelData model = new ModelData();
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
            container.previousEntry = new ModelData();
            container.previousEntry.steps = 0;
            container.previousEntry.distance = 0;
        }

        return container;
    }

    public ArrayList<ModelLocation> getLocationsForDay(int day) {
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

    public ArrayList<ModelLocation> getLocations(long start, long end) {
        final ArrayList<ModelLocation> data = new ArrayList<ModelLocation>();

        Cursor cursor = null;
        try {
            cursor = getDatabaseRO().query(
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
                    ModelLocation model = new ModelLocation();
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
}
