package carnero.movement.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;

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

    public ArrayList<Model> getData() {
        final ArrayList<Model> data = new ArrayList<Model>();

        Cursor cursor = null;
        try {
            cursor = getDatabaseRO().query(
                    Structure.Table.History.name,
                    Structure.Table.History.projection,
                    null, null, null, null,
                    Structure.Table.History.TIME + " asc"
            );

            if (cursor.moveToFirst()) {
                int idxID = cursor.getColumnIndex(Structure.Table.History.ID);
                int idxTime = cursor.getColumnIndex(Structure.Table.History.TIME);
                int idxSteps = cursor.getColumnIndex(Structure.Table.History.STEPS);
                int idxDistance = cursor.getColumnIndex(Structure.Table.History.DISTANCE);
                int idxLatitude = cursor.getColumnIndex(Structure.Table.History.LATITUDE);
                int idxLongitude = cursor.getColumnIndex(Structure.Table.History.LONGITUDE);
                int idxAccuracy = cursor.getColumnIndex(Structure.Table.History.ACCURACY);

                do {
                    Model model = new Model();
                    model.id = cursor.getLong(idxID);
                    model.time = cursor.getLong(idxTime);
                    model.steps = cursor.getInt(idxSteps);
                    model.distance = cursor.getFloat(idxDistance);
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
