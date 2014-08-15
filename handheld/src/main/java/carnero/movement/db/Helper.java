package carnero.movement.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

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
}
