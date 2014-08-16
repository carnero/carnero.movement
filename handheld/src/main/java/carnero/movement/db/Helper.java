package carnero.movement.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import carnero.movement.common.Constants;

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
			Log.e(Constants.TAG, "Failed to create database");
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

    public ModelData[] getDataToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long millisStart = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long millisEnd = calendar.getTimeInMillis();

        return getData(millisStart, millisEnd);
    }

    public ModelData[] getData(int days) {
        long millisEnd = System.currentTimeMillis();
        long millisStart = millisEnd - (DateUtils.DAY_IN_MILLIS * days);

        return getData(millisStart, millisEnd);
    }

    public ModelData[] getData(long start, long end) {
        long millisInterval;

        int days = (int) ((end - start) / DateUtils.DAY_IN_MILLIS);
        if (days <= 1) {
            millisInterval = DateUtils.HOUR_IN_MILLIS;
        } else if (days <= 3) {
            millisInterval = DateUtils.HOUR_IN_MILLIS * 2;
        } else if (days <= 7) {
            millisInterval = DateUtils.HOUR_IN_MILLIS * 4;
        } else {
            millisInterval = DateUtils.HOUR_IN_MILLIS * 8;
        }

        int intervals = (int) Math.ceil((end - start) / millisInterval);
        final ModelData[] data = new ModelData[intervals + 1];

        Cursor cursor = null;
        try {
            cursor = getDatabaseRO().query(
                    Structure.Table.History.name,
                    Structure.Table.History.projectionData,
                    Structure.Table.History.TIME + " >= " + start + " and " + Structure.Table.History.TIME + " <= " + end,
                    null, null, null,
                    Structure.Table.History.TIME + " desc"
            );

            if (cursor.moveToFirst()) {
                int idxTime = cursor.getColumnIndex(Structure.Table.History.TIME);
                int idxSteps = cursor.getColumnIndex(Structure.Table.History.STEPS);
                int idxDistance = cursor.getColumnIndex(Structure.Table.History.DISTANCE);

                do {
                    long time = cursor.getLong(idxTime);
                    int interval = intervals - ((int) ((end - time) / millisInterval)); // Oldest is the first interval

                    ModelData model = data[interval];
                    if (model == null) {
                        model = new ModelData();
                        model.steps = cursor.getInt(idxSteps);
                        model.distance = cursor.getFloat(idxDistance);

                        data[interval] = model;
                    } else {
                        model.steps = Math.max(model.steps, cursor.getInt(idxSteps));
                        model.distance = Math.max(model.distance, cursor.getFloat(idxDistance));
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return data;
    }

    public ArrayList<ModelLocation> getLocationsToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long millisStart = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long millisEnd = calendar.getTimeInMillis();

        return getLocations(millisStart, millisEnd);
    }

    public ArrayList<ModelLocation> getLocations(int days) {
        long millisEnd = System.currentTimeMillis();
        long millisStart = millisEnd - (DateUtils.DAY_IN_MILLIS * days);

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
