/*
 * Copyright (c) 2009-2013, Inmite s.r.o. (www.inmite.eu). All rights reserved.
 *
 * This source code can be used only for purposes specified by the given license contract
 * signed by the rightful deputy of Inmite s.r.o. This source code can be used only
 * by the owner of the license.
 *
 * Any disputes arising in respect of this agreement (license) shall be brought
 * before the Municipal Court of Prague.
 */

package carnero.movement.common.remotelog;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import carnero.movement.common.remotelog.model.LogEntry;

public class LogHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "remotelog.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_LOG = "rlog";
    //
    private static SQLiteDatabase sDatabaseRW;
    private static SQLiteDatabase sDatabaseRO;

    public LogHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(getSQL());
        } catch (SQLException e) {
            RemoteLog.e("Failed to create database structure", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing yet
    }

    private synchronized SQLiteDatabase getDatabaseRW() {
        if (sDatabaseRW == null || !sDatabaseRW.isOpen()) {
            sDatabaseRW = getWritableDatabase();
            if (sDatabaseRW.inTransaction()) {
                sDatabaseRW.endTransaction();
            }
        }

        return sDatabaseRW;
    }

    private synchronized SQLiteDatabase getDatabaseRO() {
        if (sDatabaseRO == null || !sDatabaseRO.isOpen()) {
            sDatabaseRO = getReadableDatabase();
        }

        return sDatabaseRO;
    }

    /**
     * Insert error log into database
     *
     * @param log
     * @return id of inserted row
     */
    public synchronized long insertLog(LogEntry log) {
        ContentValues values = new ContentValues();
        values.put(ColumnsLog.TIME, log.time);
        values.put(ColumnsLog.LEVEL, log.level.toInt());
        values.put(ColumnsLog.TAG, log.tag);
        values.put(ColumnsLog.MESSAGE, log.message);

        long result = getDatabaseRW().insertWithOnConflict(
            TABLE_LOG,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        );

        // trim log; 4 hrs
        getDatabaseRW().delete(
            TABLE_LOG,
            ColumnsLog.TIME + " < " + (System.currentTimeMillis() - (4 * 60 * 60 * 1000)),
            null
        );

        return result;
    }

    /**
     * Check if there is any log with given level or worse (higher level)
     *
     * @param minLevel
     * @return true if anything found
     */
    public boolean isThereSomethingBad(FailureLevel minLevel) {
        boolean status = false;
        Cursor cursor = null;

        try {
            cursor = getDatabaseRO().query(
                TABLE_LOG,
                new String[]{ColumnsLog.LEVEL},
                ColumnsLog.LEVEL + " >= " + minLevel.toInt() + " and " + ColumnsLog.TIME + " > " + Prefs.getLastEmail(),
                null, null, null, null,
                "1"
            );

            status = (cursor.getCount() > 0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return status;
    }

    /**
     * Get all saved logs
     *
     * @return list of logs
     */
    public ArrayList<LogEntry> getLogs() {
        ArrayList<LogEntry> logs = new ArrayList<LogEntry>();
        Cursor cursor = null;

        try {
            String[] projection = new String[]{
                ColumnsLog.TIME,
                ColumnsLog.LEVEL,
                ColumnsLog.TAG,
                ColumnsLog.MESSAGE
            };
            cursor = getDatabaseRO().query(
                TABLE_LOG,
                projection,
                null, null, null, null,
                ColumnsLog.TIME + " asc",
                null
            );

            if (cursor.moveToFirst()) {
                int idxTime = cursor.getColumnIndex(ColumnsLog.TIME);
                int idxLevel = cursor.getColumnIndex(ColumnsLog.LEVEL);
                int idxTag = cursor.getColumnIndex(ColumnsLog.TAG);
                int idxMessage = cursor.getColumnIndex(ColumnsLog.MESSAGE);

                LogEntry log;
                do {
                    log = new LogEntry(
                        cursor.getLong(idxTime),
                        FailureLevel.fromInt(cursor.getInt(idxLevel)),
                        cursor.getString(idxTag),
                        cursor.getString(idxMessage)
                    );

                    logs.add(log);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return logs;
    }

    // create database structure

    private static String getSQL() {
        final StringBuilder sql = new StringBuilder();
        sql.append("create table ");
        sql.append(TABLE_LOG);
        sql.append(" (");
        sql.append(ColumnsLog.ID);
        sql.append(" integer primary key autoincrement,");
        sql.append(ColumnsLog.TIME);
        sql.append(" int not null,");
        sql.append(ColumnsLog.LEVEL);
        sql.append(" integer not null,");
        sql.append(ColumnsLog.TAG);
        sql.append(" text not null,");
        sql.append(ColumnsLog.MESSAGE);
        sql.append(" text not null");
        sql.append(");");

        return sql.toString();
    }

    public class ColumnsLog {

        public static final String ID = "_id";
        public static final String TIME = "time"; // integer
        public static final String LEVEL = "level"; // integer
        public static final String TAG = "tag"; // text
        public static final String MESSAGE = "msg"; // text
    }
}
