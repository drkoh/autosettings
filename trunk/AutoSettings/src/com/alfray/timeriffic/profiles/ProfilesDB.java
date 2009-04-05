/*
 * Copyright 2008 (c) ralfoide gmail com, 2008
 * Project: timeriffic
 * License: GPL version 3 or any later version
 */

package com.alfray.timeriffic.profiles;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

//-----------------------------------------------

/*
 * Debug Tip; to view content of database, use:
 * $ cd /cygdrive/c/.../android-sdk_..._windows/tools
 * $ ./adb shell 'echo ".dump" | sqlite3 data/data/com.alfray.timeriffic/databases/games.db'
 */

/**
 * Helper to access the profiles database.
 * <p/>
 * The interface is similar to a {@link ContentProvider}, which should make it
 * easy to use only later.
 */
public class ProfilesDB {
    
    private static final String TAG = "Tmrfc-ProfilesDB";
    private static final boolean DEBUG = true;

    private static final String PROFILES_TABLE = "profiles";
    private static final String DB_NAME = "profiles.db";
    private static final int DB_VERSION = 1 * 100 + 1; // major*100 + minor

    private SQLiteDatabase mDb;
    private DatabaseHelper mDbHelper;

    private Context mContext;

    // ----------------------------------

    /** Call this after creating this object. */
    public boolean onCreate(Context context) {
        mContext = context;
        mDbHelper = new DatabaseHelper(context, DB_NAME, DB_VERSION);
        mDb = mDbHelper.getWritableDatabase();
        boolean created = mDb != null;
        return created;
    }

    /** Call this when the database is no longer needed. */
    public void onDestroy() {
        mContext = null;
        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
        }
    }

    // ----------------------------------

    /**
     * @see SQLiteDatabase#beginTransaction()
     */
    public void beginTransaction() {
        mDb.beginTransaction();
    }

    /**
     * @see SQLiteDatabase#setTransactionSuccessful()
     */
    public void setTransactionSuccessful() {
        mDb.setTransactionSuccessful();
    }

    /**
     * @see SQLiteDatabase#endTransaction()
     */
    public void endTransaction() {
        mDb.endTransaction();
    }

    // ----------------------------------
    
    public long getProfileIdForRowId(long row_id) {
        try {
            SQLiteStatement sql = mDb.compileStatement(
                    String.format("SELECT %s FROM %s WHERE %s=%d;",
                            Columns.PROFILE_ID,
                            PROFILES_TABLE,
                            Columns._ID, row_id));
            return sql.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            // no profiles
            return 0;
        }
    }
    
    /**
     * Returns the max profile index (not id!).
     * 
     * If maxProfileIndex == 0, returns the absolute max profile index,
     * i.e. the very last one.
     * If maxProfileIndex > 0, returns the max profile index that is smaller
     * than the given index (so basically the profile just before the one
     * given).
     * Returns 0 if there is no such index.
     */
    public long getMaxProfileIndex(long maxProfileIndex) {
        try {
            String testMaxProfId = "";
            if (maxProfileIndex > 0) {
                testMaxProfId = String.format("AND %s<%d",
                        Columns.PROFILE_ID, maxProfileIndex << Columns.PROFILE_SHIFT);
            }
            // e.g. SELECT MAX(prof_id) FROM profiles WHERE type=1 [ AND prof_id < 65536 ]
            SQLiteStatement sql = mDb.compileStatement(
                    String.format("SELECT MAX(%s) FROM %s WHERE %s=%d %s;",
                            Columns.PROFILE_ID,
                            PROFILES_TABLE,
                            Columns.TYPE, Columns.TYPE_IS_PROFILE,
                            testMaxProfId));

            return sql.simpleQueryForLong() >> Columns.PROFILE_SHIFT;
        } catch (SQLiteDoneException e) {
            // no profiles
            return 0;
        }
    }

    /**
     * Returns the min action index (not id!) that is greater than the
     * requested minActionIndex.
     * <p/>
     * So for a given index (including 0), returns the next one used.
     * If there's no such index (i.e. not one used after the given index)
     * returns -1.
     */
    public long getMinActionIndex(long profileIndex, long minActionIndex) {
        try {
            long pid = (profileIndex << Columns.PROFILE_SHIFT) + minActionIndex;
            long maxPid = (profileIndex + 1) << Columns.PROFILE_SHIFT;

            // e.g. SELECT MIN(prof_id) FROM profiles WHERE type=2 AND prof_id > 32768+256 AND prof_id < 65536
            SQLiteStatement sql = mDb.compileStatement(
                    String.format("SELECT MIN(%s) FROM %s WHERE %s=%d AND %s>%d AND %s<%d;",
                            Columns.PROFILE_ID,
                            PROFILES_TABLE,
                            Columns.TYPE, Columns.TYPE_IS_TIMED_ACTION,
                            Columns.PROFILE_ID, pid, 
                            Columns.PROFILE_ID, maxPid));

            long result = sql.simpleQueryForLong();
            if (result > pid && result < maxPid) return result & Columns.ACTION_MASK;
        } catch (SQLiteDoneException e) {
            // no actions
        }
        return -1;
    }

    // ----------------------------------

    /**
     * Inserts a new profile before the given profile index.
     * If beforeProfileIndex is <= 0, insert at the end.
     * 
     * @return the profile index (not the row id)
     */
    public long insertProfile(long beforeProfileIndex,
            String title, boolean isEnabled) {

        beginTransaction();
        try {
            long index = getMaxProfileIndex(beforeProfileIndex);
            if (beforeProfileIndex <= 0) {
                long max = Long.MAX_VALUE >> Columns.PROFILE_SHIFT;
                if (index >= max - 1) {
                    // TODO repack
                    throw new UnsupportedOperationException("Profile index at maximum.");
                } else if (index < max - Columns.PROFILE_GAP) {
                    index += Columns.PROFILE_GAP;
                } else {
                    index += (max - index) / 2;
                }
            } else {
                if (index == beforeProfileIndex - 1) {
                    // TODO repack
                    throw new UnsupportedOperationException("No space left to insert profile before profile.");
                } else {
                    index = (index + beforeProfileIndex) / 2; // get middle offset
                }
            }
            
            long id = index << Columns.PROFILE_SHIFT;
            
            ContentValues values = new ContentValues(2);
            values.put(Columns.PROFILE_ID, id);
            values.put(Columns.TYPE, Columns.TYPE_IS_PROFILE);
            values.put(Columns.DESCRIPTION, title);
            values.put(Columns.IS_ENABLED, isEnabled);
            
            id = mDb.insert(PROFILES_TABLE, Columns.TYPE, values);

            if (DEBUG) Log.d(TAG, String.format("Insert profile: %d => row %d", index, id));

            if (id < 0) throw new SQLException("insert profile row failed");
            setTransactionSuccessful();
            return index;
        } finally {
            endTransaction();
        }
    }

    /**
     * Inserts a new action for the given profile index.
     * If afterActionIndex is == 0, inserts at the beginning of these actions.
     * 
     * @return the action index (not the row id)
     */
    public long insertTimedAction(long profileIndex,
            long afterActionIndex,
            boolean isActive,
            int hourMin,
            int days,
            String actions,
            long nextMs) {

        beginTransaction();
        try {
            long pid = profileIndex << Columns.PROFILE_SHIFT;

            long index = getMinActionIndex(profileIndex, afterActionIndex);

            if (index < 0) index = Columns.ACTION_MASK - 1;
            
            if (index - afterActionIndex < 2) {
                // TODO repack
                throw new UnsupportedOperationException("No space left to insert action.");
            }

            index = (index + afterActionIndex) / 2; // get middle offset
            
            pid += index;

            String description = TimedActionUtils.computeDescription(
                    mContext, hourMin, days, actions);
            
            ContentValues values = new ContentValues(2);

            values.put(Columns.TYPE, Columns.TYPE_IS_TIMED_ACTION);
            values.put(Columns.PROFILE_ID, pid);
            values.put(Columns.DESCRIPTION, description);
            values.put(Columns.IS_ENABLED, isActive);
            values.put(Columns.HOUR_MIN, hourMin);
            values.put(Columns.DAYS, days);
            values.put(Columns.ACTIONS, actions);
            values.put(Columns.NEXT_MS, nextMs);
            
            long id = mDb.insert(PROFILES_TABLE, Columns.TYPE, values);

            if (DEBUG) Log.d(TAG, String.format("Insert profile %d, action: %d => row %d", profileIndex, index, id));

            if (id < 0) throw new SQLException("insert action row failed");
            setTransactionSuccessful();
            return index;
        } finally {
            endTransaction();
        }
    }

    // ----------------------------------

    /** id is used if >= 0 */
    public Cursor query(long id,
            String[] projection,
            String selection, 
            String[] selectionArgs, 
            String sortOrder) {
    	
    	SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    	qb.setTables(PROFILES_TABLE);

    	if (id >= 0) {
        	qb.appendWhere(String.format("%s=%d", Columns._ID, id));
        }
        
        if (sortOrder == null || sortOrder.length() == 0) sortOrder = Columns.DEFAULT_SORT_ORDER;
        
        Cursor c = qb.query(mDb, projection, selection, selectionArgs,
        		null, // groupBy
        		null, // having,
        		sortOrder);
        return c;
    }

    // ----------------------------------

    /**
     * @param name Profile name to update, if not null.
     * @param isEnabled Profile is enable flag to update.
     * @return Number of rows affected. 1 on success, 0 on failure.
     */
    public int updateProfile(long prof_id, String name, boolean isEnabled) {

        String where = String.format("%s=%d AND %s=%d",
                Columns.TYPE, Columns.TYPE_IS_PROFILE,
                Columns.PROFILE_ID, prof_id);

        ContentValues cv = new ContentValues();
        if (name != null) cv.put(Columns.DESCRIPTION, name);
        cv.put(Columns.IS_ENABLED, isEnabled);
        
        beginTransaction();
        try {
            int count = mDb.update(PROFILES_TABLE, cv, where, null);
            setTransactionSuccessful();
            return count;
        } finally {
            endTransaction();
        }
    }

    /**
     * @param isEnabled Timed action is enable flag to update.
     * @return Number of rows affected. 1 on success, 0 on failure.
     */
    public int updateTimedAction(long action_id, boolean isEnabled) {

        String where = String.format("%s=%d AND %s=%d",
                Columns.TYPE, Columns.TYPE_IS_TIMED_ACTION,
                Columns.PROFILE_ID, action_id);

        ContentValues cv = new ContentValues();
        cv.put(Columns.IS_ENABLED, isEnabled);
        
        beginTransaction();
        try {
            int count = mDb.update(PROFILES_TABLE, cv, where, null);
            setTransactionSuccessful();
            return count;
        } finally {
            endTransaction();
        }
    }

    /**
     * @param name Timed action description to update, if not null.
     * @return Number of rows affected. 1 on success, 0 on failure.
     */
    public int updateTimedAction(long action_id, int hourMin, int days,
            String actions, String description) {

        String where = String.format("%s=%d AND %s=%d",
                Columns.TYPE, Columns.TYPE_IS_TIMED_ACTION,
                Columns.PROFILE_ID, action_id);

        ContentValues cv = new ContentValues();
        cv.put(Columns.HOUR_MIN, hourMin);
        cv.put(Columns.DAYS, days);
        cv.put(Columns.ACTIONS, actions);
        if (description != null) cv.put(Columns.DESCRIPTION, description);
        
        beginTransaction();
        try {
            int count = mDb.update(PROFILES_TABLE, cv, where, null);
            setTransactionSuccessful();
            return count;
        } finally {
            endTransaction();
        }
    }

    // ----------------------------------

    /** 
     * @param row_id The SQL row id, NOT the prof_id
     * @return The number of deleted rows, >= 1 on success, 0 on failure.
     */
    public int deleteProfile(long row_id) {
        
        beginTransaction();
        try {
            long pid = getProfileIdForRowId(row_id);
            if (pid == 0) throw new InvalidParameterException("No profile id for this row id.");

            pid = pid & (~Columns.ACTION_MASK);
            
            // DELETE FROM profiles WHERE prof_id >= 65536 AND prof_id < 65536+65535
            String where = String.format("%s>=%d AND %s<%d",
                    Columns.PROFILE_ID, pid,
                    Columns.PROFILE_ID, pid + Columns.ACTION_MASK);
            
            int count = mDb.delete(PROFILES_TABLE, where, null);
            setTransactionSuccessful();
            return count;
        } finally {
            endTransaction();
        }
    }

    /** 
     * @param row_id The SQL row id, NOT the prof_id
     * @return The number of deleted rows, 1 on success, 0 on failure.
     */
    public int deleteAction(long row_id) {
        
        beginTransaction();
        try {
            // DELETE FROM profiles WHERE TYPE=2 AND _id=65537
            String where = String.format("%s=%d AND %s=%d",
                    Columns.TYPE, Columns.TYPE_IS_TIMED_ACTION,
                    Columns._ID, row_id);
            
            int count = mDb.delete(PROFILES_TABLE, where, null);
            setTransactionSuccessful();
            return count;
        } finally {
            endTransaction();
        }
    }

    
    // ----------------------------------    

    /** Convenience helper to open/create/update the database */
    private class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context,
                String db_name,
                int version) {
			super(context, db_name, null /* cursor factory */, version);
		}

		@Override
        public void onCreate(SQLiteDatabase db) {
            SQLiteDatabase old_mDb = mDb;
            mDb = db;
            onResetTables();
            initDefaultProfiles();
            mDb = old_mDb;
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, String.format("Upgrading database from version %1$d to %2$d.",
                    oldVersion, newVersion));
            db.execSQL("DROP TABLE IF EXISTS " + PROFILES_TABLE);
            onCreate(db);
        }
        
        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            // pass
        }        
    }

    /**
     * Called by {@link DatabaseHelper} to reset the tables.
     */
    private void onResetTables() {
        // hand over that chocolate and nobody gets hurt!
        mDb.execSQL(String.format("DROP TABLE IF EXISTS %s;", PROFILES_TABLE));
        
        mDb.execSQL(String.format("CREATE TABLE %s "
                + "(%s INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "%s INTEGER, "
                + "%s TEXT, "
                + "%s INTEGER, "
                + "%s INTEGER, "
                + "%s INTEGER, "
                + "%s INTEGER, "
                + "%s TEXT, "
                + "%s INTEGER);" ,
                PROFILES_TABLE,
                Columns._ID,
                Columns.TYPE,
                Columns.DESCRIPTION,
                Columns.IS_ENABLED,
                Columns.PROFILE_ID,
                Columns.HOUR_MIN,
                Columns.DAYS,
                Columns.ACTIONS,
                Columns.NEXT_MS));
    }

    /**
     * Called by {@link DatabaseHelper} when the database has just been
     * created to initialize it with initial data. It's safe to use
     * {@link ProfilesDB#insertProfile(String, boolean)} or
     * {@link ProfilesDB#insertTimedAction(String, boolean, int, int, String, long)}
     * at that point.
     */
    private void initDefaultProfiles() {
        long pindex = insertProfile(0, "Weekdaze", true /*isEnabled*/);
        long action = insertTimedAction(pindex, 0,
                true,               //isActive
                7*60+0,             //hourMin
                Columns.MONDAY + Columns.TUESDAY + Columns.WEDNESDAY + Columns.THURSDAY,
                "RR,VV,W0",            //actions
                0                   //nextMs
                );
        insertTimedAction(pindex, action,
                false,              //isActive
                20*60+0,             //hourMin
                Columns.MONDAY + Columns.TUESDAY + Columns.WEDNESDAY + Columns.THURSDAY,
                "RM,VV",            //actions
                0                   //nextMs
                );

        pindex = insertProfile(0, "Party Time", true /*isEnabled*/);
        action = insertTimedAction(pindex, 0,
                false,              //isActive
                9*60+0,             //hourMin
                Columns.FRIDAY + Columns.SATURDAY,
                "RR",               //actions
                0                   //nextMs
                );
        insertTimedAction(pindex, action,
                false,               //isActive
                22*60+0,             //hourMin
                Columns.FRIDAY + Columns.SATURDAY,
                "RM,VV",            //actions
                0                   //nextMs
                );

        pindex = insertProfile(0, "Sleeping-In", true /*isEnabled*/);
        action = insertTimedAction(pindex, 0,
                false,               //isActive
                10*60+30,            //hourMin
                Columns.SUNDAY,
                "RR,W1",               //actions
                0                   //nextMs
                );
        insertTimedAction(pindex, action,
                false,               //isActive
                21*60+0,             //hourMin
                Columns.SUNDAY,
                "RM,VV",            //actions
                0                   //nextMs
                );
    }

    /**
     * Some simple profiles for me
     */
    private void initRalfProfiles() {
        long pindex = insertProfile(0, "Ralf", true /*isEnabled*/);
        long action = insertTimedAction(pindex, 0,
                true,               //isActive
                9*60+0,             //hourMin
                Columns.MONDAY + Columns.TUESDAY + Columns.WEDNESDAY + Columns.THURSDAY + Columns.FRIDAY + Columns.SATURDAY + Columns.SUNDAY,
                "RR,VV,B75",            //actions
                0                   //nextMs
                );
        insertTimedAction(pindex, action,
                false,              //isActive
                21*60+0,             //hourMin
                Columns.MONDAY + Columns.TUESDAY + Columns.WEDNESDAY + Columns.THURSDAY + Columns.FRIDAY + Columns.SATURDAY + Columns.SUNDAY,
                "RM,VN,B5",            //actions
                0                   //nextMs
                );
    }

    // --------------

    /**
     * Labels of the reset profiles choices.
     * Default is index 0.
     */
    public String[] getResetLabels() {
        return new String[] {
            "Fun Profiles",
            "Empty Profiles",
            "Ralf Profiles"
        };
    }

    /**
     * Reset profiles according to choices.
     * 
     * @param labelIndex An index from the {@link #getResetLabels()} array.
     */
    public void resetProfiles(int labelIndex) {

        if (DEBUG) Log.d(TAG, "Reset profiles: " + Integer.toString(labelIndex));
        
        beginTransaction();
        try {
            // empty tables
            onResetTables();
        
            switch(labelIndex) {
            case 0:
                initDefaultProfiles();
                break;
            case 1:
                // empty profiles list, already done.
                // pass
                break;
            case 2:
                initRalfProfiles();
                break;
            }

            setTransactionSuccessful();
        } finally {
            endTransaction();
        }
    }

    // --------------

    public void removeAllActionExecFlags() {
        // generates WHERE type=2 (aka action) AND enable=1
        String where = String.format("%s=%d AND %s=%d",
                Columns.TYPE, Columns.TYPE_IS_TIMED_ACTION,
                Columns.IS_ENABLED, 1);

        ContentValues values = new ContentValues(1);
        values.put(Columns.IS_ENABLED, false);

        beginTransaction();
        try {
            mDb.update(
                    PROFILES_TABLE, // table
                    values,         // values
                    where,          // whereClause
                    null            // whereArgs
                    );

            setTransactionSuccessful();
        } finally {
            endTransaction();
        }
    }

    /**
     * Returns the list of all enabled profiles.
     * This is a list of profiles indexes.
     * Can return an empty list, but not null.
     */
    public long[] getEnabledProfiles() {

        // generates WHERE type=1 (aka profile) AND enable=1
        String where = String.format("%s=%d AND %s=%d",
                Columns.TYPE, Columns.TYPE_IS_PROFILE,
                Columns.IS_ENABLED, 1);

        Cursor c = null;
        try {
            c = mDb.query(
                    PROFILES_TABLE,                         // table
                    new String[] { Columns.PROFILE_ID },    // columns
                    where,                                  // selection
                    null,                                   // selectionArgs
                    null,                                   // groupBy
                    null,                                   // having
                    null                                    // orderBy
                    );
            
            int profIdColIndex = c.getColumnIndexOrThrow(Columns.PROFILE_ID);
            
            long[] indexes = new long[c.getCount()];
            
            if (c.moveToFirst()) {
                int i = 0;
                do {
                    indexes[i++] = c.getLong(profIdColIndex) >> Columns.PROFILE_SHIFT;
                } while (c.moveToNext());
            }
            
            return indexes;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Returns the list of timed actions that should be activated "now",
     * as defined by the given day and hour/minute and limited to the
     * given list of profiles.
     * <p/>
     * Returns a list of action prof_id (ids, not indexes) or null.
     * prof_indexes is a list of profile indexes (not ids) that are currently
     * enabled.
     * <p/>
     * Synopsis:
     * - If there are no profiles activated, abort. That is we support prof_ids
     *   being an empty list.
     * - Selects actions which action_day & given_day != 0 (bitfield match)
     * - Selects all actions for that day, independently of the hourMin.
     *   The hourMin check is done after on the result.
     * - Can return an empty list, but not null.
     */
    public ArrayList<ActionInfo> getDayActivableActions(int hourMin, int day, long[] prof_indexes) {
        if (prof_indexes.length < 1) return null;

        StringBuilder profList = new StringBuilder();
        for (long prof_index : prof_indexes) {
            if (profList.length() > 0) profList.append(",");
            profList.append(Long.toString(prof_index));
        }
        
        // generates WHERE type=2 (aka action) 
        //           AND hourMin <= targetHourMin
        //           AND days & MASK != 0
        //           AND prof_id >> SHIFT IN (profList)
        String where = String.format("%s=%d AND %s <= %d AND %s & %d != 0 AND %s >> %d IN (%s)",
                Columns.TYPE, Columns.TYPE_IS_TIMED_ACTION,
                Columns.HOUR_MIN, hourMin,
                Columns.DAYS, day,
                Columns.PROFILE_ID, Columns.PROFILE_SHIFT, profList);

        // ORDER BY hourMin DESC
        String orderBy = String.format("%s DESC", Columns.HOUR_MIN);

        if (DEBUG) Log.d(TAG, "Get actions: WHERE " + where + " ORDER BY " + orderBy);

        
        Cursor c = null;
        try {
            c = mDb.query(
                    PROFILES_TABLE,                         // table
                    new String[] { Columns._ID, Columns.HOUR_MIN, Columns.ACTIONS },    // columns
                    where,                                  // selection
                    null,                                   // selectionArgs
                    null,                                   // groupBy
                    null,                                   // having
                    orderBy
                    );

            int rowIdColIndex = c.getColumnIndexOrThrow(Columns._ID);
            int hourMinColIndex = c.getColumnIndexOrThrow(Columns.HOUR_MIN);
            int actionsColInfo = c.getColumnIndexOrThrow(Columns.ACTIONS);

            // Above we got the list of all actions for the requested day
            // that happen before the requested hourMin, in descending time
            // order, e.g. the most recent action is first in the list.
            //
            // We want to return the first action found. There might be more
            // than one action with the same time, so return them all.
            
            ArrayList<ActionInfo> infos = new ArrayList<ActionInfo>();
            if (c.moveToFirst()) {
                int currHourMin = c.getInt(hourMinColIndex);
                do {
                    infos.add(new ActionInfo(c.getLong(rowIdColIndex),
                            c.getString(actionsColInfo)));

                    if (DEBUG) Log.d(TAG, String.format("ActivableAction: day %d, hourMin %04d", day, currHourMin));
                        
                } while (c.moveToNext() && c.getInt(hourMinColIndex) == currHourMin);
            }

            return infos;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Invokes {@link #getDayActivableActions(int, int, long[])} for the current
     * day. If nothing is found, look at the 6 previous days to see if we can
     * find an action.
     */
    public ArrayList<ActionInfo> getWeekActivableActions(int hourMin, int day, long[] prof_indexes) {
        ArrayList<ActionInfo> actions = null;

        // Look for the last enabled action for day.
        // If none was found, loop up to 6 days before and check the last
        // action before 24:00.
        for (int k = 0; k < 7; k++) {
            actions = getDayActivableActions(hourMin, day, prof_indexes);

            if (actions != null && actions.size() > 0) {
                break;
            }

            // Switch to previous day and loop from monday to sunday as needed.
            day = day >> 1;
            if (day == 0) day = Columns.SUNDAY;
            
            // Look for anything "before the end of the day". Since we
            // want to match 23:59 we need to add one minute thus 24h00
            // is our target.
            hourMin = 24*60 + 0;
        }
        
        return actions;
    }

    /**
     * Given a day and an hourMin time, try to find the first even that happens
     * after that timestamp. If nothing if found on the day, look up to 6 days
     * ahead.
     * 
     * prof_indexes is a list of profile indexes (not ids) that are currently
     * enabled.
     * 
     * @return The number of minutes from the given timestamp to the next event
     *  or 0 if there's no such event ("now" is not a valid next event)
     */
    public int getWeekNextEvent(int hourMin, int day, long[] prof_indexes) {
        // First try to find something today
        int found = getDayNextEvent(hourMin, day, prof_indexes);
        if (found > hourMin) {
            return found - hourMin;
        }
        
        // Otherwise look for the 6 days of events
        int minutes = 24*60 - hourMin;
        for(int k = 1; k < 7; k++, minutes += 24*60) {
            // Switch to next day. Loop from sunday back to monday.
            day = day << 1;
            if (day > Columns.SUNDAY) day = Columns.MONDAY;
            
            found = getDayNextEvent(-1 /*One minute before 00:00*/, day, prof_indexes);
            if (found >= 0) {
                return minutes + found;
            }
        }
        
        return 0;
    }

    /**
     * Given a day and an hourMin time, try to find the first even that happens
     * after that timestamp on the singular day.
     * 
     * prof_indexes is a list of profile indexes (not ids) that are currently
     * enabled.
     * 
     * @return The hourMin of the event found (hourMin..23:59) or -1 if nothing found.
     *  If the return value is not -1, it is guaranteed to be greater than the
     *  given hourMin since we look for event *past* this time.
     */
    private int getDayNextEvent(int hourMin, int day, long[] prof_indexes) {
        if (prof_indexes.length < 1) return -1;

        StringBuilder profList = new StringBuilder();
        for (long prof_index : prof_indexes) {
            if (profList.length() > 0) profList.append(",");
            profList.append(Long.toString(prof_index));
        }
        
        // generates WHERE type=2 (aka action) 
        //           AND hourMin > targetHourMin
        //           AND days & MASK != 0
        //           AND prof_id >> SHIFT IN (profList)
        String where = String.format("%s=%d AND %s > %d AND %s & %d != 0 AND %s >> %d IN (%s)",
                Columns.TYPE, Columns.TYPE_IS_TIMED_ACTION,
                Columns.HOUR_MIN, hourMin,
                Columns.DAYS, day,
                Columns.PROFILE_ID, Columns.PROFILE_SHIFT, profList);

        // ORDER BY hourMin ASC
        String orderBy = String.format("%s ASC", Columns.HOUR_MIN);

        // LIMIT 1 (we only want the first result)
        String limit = "1";
        
        if (DEBUG) Log.d(TAG, "Get actions: WHERE " + where + " ORDER BY " + orderBy + " LIMIT " + limit);

        
        Cursor c = null;
        try {
            c = mDb.query(
                    PROFILES_TABLE,                         // table
                    new String[] { Columns._ID, Columns.HOUR_MIN, Columns.ACTIONS },    // columns
                    where,                                  // selection
                    null,                                   // selectionArgs
                    null,                                   // groupBy
                    null,                                   // having
                    orderBy,
                    limit
                    );

            int hourMinColIndex = c.getColumnIndexOrThrow(Columns.HOUR_MIN);

            if (c.moveToFirst()) {
                hourMin = c.getInt(hourMinColIndex);

                if (DEBUG) Log.d(TAG, String.format("NextEvent: day %d, hourMin %04d", day, hourMin));
                
                return hourMin;
            }
        } finally {
            if (c != null) c.close();
        }

        return -1;
    }


    /**
     * Struct that describes a Timed Action returned by
     * {@link ProfilesDB#getDayActivableActions(int, int, long[])}
     */
    public static class ActionInfo {

        public final long mRowId;
        public final String mActions;

        public ActionInfo(long rowId, String actions) {
            mRowId = rowId;
            mActions = actions;
        }
        
        @Override
        public String toString() {
            return String.format("Action<#.%d @%04d: %s>", mRowId, mActions);
        }
    }

    /**
     * Mark all actions as enabled.
     */
    public void markActionsEnabled(ArrayList<ActionInfo> actions) {

        StringBuilder rowList = new StringBuilder();
        for (ActionInfo info : actions) {
            if (rowList.length() > 0) rowList.append(",");
            rowList.append(Long.toString(info.mRowId));
        }

        // generates WHERE type=2 (aka action) AND _id in (profList)
        String where = String.format("%s=%d AND %s IN (%s)",
                Columns.TYPE, Columns.TYPE_IS_TIMED_ACTION,
                Columns._ID, rowList);

        if (DEBUG) Log.d(TAG, "Mark actions: WHERE " + where);
        
        ContentValues values = new ContentValues(1);
        values.put(Columns.IS_ENABLED, true);

        beginTransaction();
        try {
            mDb.update(
                    PROFILES_TABLE, // table
                    values,         // values
                    where,          // whereClause
                    null            // whereArgs
                    );

            setTransactionSuccessful();
        } finally {
            endTransaction();
        }
    }

}
