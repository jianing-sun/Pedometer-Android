package jianingsun.mypedometer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jianingsun on 2018-02-10.
 */

public class myPedometerDatabase extends SQLiteOpenHelper {

    private final static String DB_NAME = "steps";
    private final static int DB_VERSION = 2;

    private static myPedometerDatabase instance;
    private static final AtomicInteger counter = new AtomicInteger();

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + DB_NAME + "(date INTEGER, steps INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            db.execSQL("CREATE TABLE " + DB_NAME + "2 (date INTEGER, steps INTEGER)");
            db.execSQL("INSERT INTO " + DB_NAME + "2 (date, steps) SELECT date, steps FROM " +
            DB_NAME);
            db.execSQL("DROP TABLE " + DB_NAME);
            db.execSQL("ALTER TABLE " + DB_NAME + "2 RENAME TO " + DB_NAME + "");
        }
    }

    private myPedometerDatabase(final Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // synchronized keyword ensures that a method can be invoked
    // by only one thread at a time (thread-safe)
    public static synchronized myPedometerDatabase getInstance(final Context c) {
        // if there isn't a database instance, create one
        if (instance == null) {
            instance = new myPedometerDatabase(c.getApplicationContext());
        }
        // increment by one for the current value
        counter.incrementAndGet();
        return instance;
    }

    @Override
    public void close() {
        if (counter.decrementAndGet() == 0) {
            super.close();
        }
    }

    public Cursor query(final String[] columns, final String selection,
                        final String[] selectionArgs, final String groupBy,
                        final String having, final String orderBy, final String limit) {

        return getReadableDatabase()
                .query(DB_NAME, columns, selection, selectionArgs, groupBy, having, orderBy, limit);

    }

    public void insertNewDay(long date, int steps) {
        getWritableDatabase().beginTransaction();
        try {
            // query a date
            Cursor c = getReadableDatabase().query(DB_NAME, new String[]{"date"}, "date = ?",
                    new String[]{String.valueOf(date)}, null, null, null);
            if (c.getCount() == 0 && steps >= 0) {

                addToLastEntry(steps);

                ContentValues values = new ContentValues();
                values.put("date", date);
                values.put("steps", -steps);
                getWritableDatabase().insert(DB_NAME, null, values);
            }
            c.close();
            getWritableDatabase().setTransactionSuccessful();
        } finally {
            getWritableDatabase().endTransaction();
        }
    }

    public void addToLastEntry(int steps) {
        if (steps > 0) {
            getWritableDatabase().execSQL("UPDATE " + DB_NAME + " SET steps = steps + " + steps +
            " WHERE date = (SELECT MAX(date) FROM " + DB_NAME + ")");
        }
    }

    public int getTotalWithoutToday() {
        Cursor c = getReadableDatabase()
                .query(DB_NAME, new String[]{"SUM(steps)"}, "steps > 0 AND date > 0 AND date < ?",
                        new String[]{String.valueOf(Util.getToday())}, null, null, null);
        c.moveToFirst();
        int re = c.getInt(0);
        c.close();
        return re;
    }

    // TODO: get the maximum of steps walked in one day

    // TODO: get the maximum of steps walked in one day and the date that happend





    public int getSteps(final long date) {
        Cursor c = getReadableDatabase().query(DB_NAME, new String[]{"Steps"}, "date = ?",
                new String[]{String.valueOf(date)}, null, null, null);
        c.moveToFirst();
        int re;
        if (c.getCount() == 0) {
            re = Integer.MIN_VALUE;
        } else {
            re = c.getInt(0);
        }
        c.close();
        return re;
    }

    public int getDaysWithoutToday() {
        Cursor c = getReadableDatabase()
                .query(DB_NAME, new String[]{"COUNT(*)"}, "steps > ? AND date < ? AND date > 0",
                        new String[]{String.valueOf(0), String.valueOf(Util.getToday())}, null,
                        null, null);
        c.moveToFirst();
        int re = c.getInt(0);
        c.close();
        return re < 0 ? 0 : re;
    }

    public int getDays() {
        int re = this.getDaysWithoutToday() + 1;
        return re;
    }


    public void saveCurrentSteps(int steps) {

        ContentValues values = new ContentValues();
        values.put("steps", steps);
        if (getWritableDatabase().update(DB_NAME, values, "date = -1", null) == 0) {
            values.put("date", -1);
            getWritableDatabase().insert(DB_NAME, null, values);
        }

    }

    public int getCurrentSteps() {
        int re = getSteps(-1);
        return re == Integer.MIN_VALUE ? 0 : re;
    }

}



































