package com.example;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Advanced Raw SQLite Database Helper for the Fitness Assistant app.
 * Demonstrates advanced SQL capabilities:
 * 1. CRUD operations on UserProfile & WorkoutLog.
 * 2. SQLite Views (workout_summary) utilizing JOIN, GROUP BY, and HAVING.
 * 3. Subqueries for calculating Personal Bests.
 * 4. Indexes on frequently queried columns (body_part).
 * 5. SQLite Triggers to auto-synchronize User Profile current_weight upon weight_history insertions.
 */
public class GymDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "GymDatabaseHelper";
    private static final String DATABASE_NAME = "gym_fitness.db";
    private static final int DATABASE_VERSION = 2;

    // Singleton instance
    private static GymDatabaseHelper instance;

    // Table: user_profile
    public static final String TABLE_USER_PROFILE = "user_profile";
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_NAME = "name";
    public static final String COL_USER_HEIGHT = "height";
    public static final String COL_USER_CURRENT_WEIGHT = "current_weight";
    public static final String COL_USER_TARGET_WEIGHT = "target_weight";
    public static final String COL_USER_CREATED_AT = "created_at";
    public static final String COL_USER_UPDATED_AT = "updated_at";

    // Table: exercises
    public static final String TABLE_EXERCISES = "exercises";
    public static final String COL_EX_ID = "id";
    public static final String COL_EX_TITLE = "title";
    public static final String COL_EX_BODY_PART = "body_part";
    public static final String COL_EX_EQUIPMENT = "equipment";
    public static final String COL_EX_DIFFICULTY = "difficulty";
    public static final String COL_EX_INSTRUCTIONS = "instructions";
    public static final String COL_EX_YOUTUBE_URL = "youtube_url";

    // Table: workout_logs
    public static final String TABLE_WORKOUT_LOGS = "workout_logs";
    public static final String COL_LOG_ID = "id";
    public static final String COL_LOG_USER_ID = "user_id";
    public static final String COL_LOG_EXERCISE_ID = "exercise_id";
    public static final String COL_LOG_DATE = "date";
    public static final String COL_LOG_SETS = "sets";
    public static final String COL_LOG_REPS = "reps";
    public static final String COL_LOG_WEIGHT = "weight";
    public static final String COL_LOG_NOTES = "notes";

    // Table: weight_history
    public static final String TABLE_WEIGHT_HISTORY = "weight_history";
    public static final String COL_WH_ID = "id";
    public static final String COL_WH_USER_ID = "user_id";
    public static final String COL_WH_WEIGHT = "weight";
    public static final String COL_WH_DATE = "date";
    public static final String COL_WH_NOTES = "notes";

    // Table: progress_photos
    public static final String TABLE_PROGRESS_PHOTOS = "progress_photos";
    public static final String COL_PHOTO_ID = "id";
    public static final String COL_PHOTO_USER_ID = "user_id";
    public static final String COL_PHOTO_PATH = "image_path";
    public static final String COL_PHOTO_DATE = "date";
    public static final String COL_PHOTO_NOTES = "notes";
    public static final String COL_PHOTO_WEIGHT = "weight";

    // View: workout_summary
    public static final String VIEW_WORKOUT_SUMMARY = "workout_summary";

    public static synchronized GymDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new GymDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private GymDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Create user_profile Table
        String createUserProfileTable = "CREATE TABLE " + TABLE_USER_PROFILE + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_NAME + " TEXT NOT NULL, " +
                COL_USER_HEIGHT + " REAL NOT NULL, " +
                COL_USER_CURRENT_WEIGHT + " REAL NOT NULL, " +
                COL_USER_TARGET_WEIGHT + " REAL, " +
                COL_USER_CREATED_AT + " TEXT, " +
                COL_USER_UPDATED_AT + " TEXT" +
                ");";
        db.execSQL(createUserProfileTable);

        // 2. Create exercises Table
        String createExercisesTable = "CREATE TABLE " + TABLE_EXERCISES + " (" +
                COL_EX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EX_TITLE + " TEXT NOT NULL, " +
                COL_EX_BODY_PART + " TEXT NOT NULL, " +
                COL_EX_EQUIPMENT + " TEXT NOT NULL, " +
                COL_EX_DIFFICULTY + " TEXT NOT NULL, " +
                COL_EX_INSTRUCTIONS + " TEXT NOT NULL, " +
                COL_EX_YOUTUBE_URL + " TEXT NOT NULL" +
                ");";
        db.execSQL(createExercisesTable);

        // 3. Create Index on exercises.body_part for rapid sorting and filtering
        String createIndexBodyPart = "CREATE INDEX IF NOT EXISTS idx_exercises_body_part ON " +
                TABLE_EXERCISES + " (" + COL_EX_BODY_PART + ");";
        db.execSQL(createIndexBodyPart);

        // 4. Create workout_logs Table
        String createWorkoutLogsTable = "CREATE TABLE " + TABLE_WORKOUT_LOGS + " (" +
                COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LOG_USER_ID + " INTEGER NOT NULL DEFAULT 1, " +
                COL_LOG_EXERCISE_ID + " INTEGER NOT NULL, " +
                COL_LOG_DATE + " TEXT NOT NULL, " +
                COL_LOG_SETS + " INTEGER NOT NULL, " +
                COL_LOG_REPS + " INTEGER NOT NULL, " +
                COL_LOG_WEIGHT + " REAL NOT NULL, " +
                COL_LOG_NOTES + " TEXT, " +
                "FOREIGN KEY (" + COL_LOG_EXERCISE_ID + ") REFERENCES " + TABLE_EXERCISES + "(" + COL_EX_ID + ") ON DELETE CASCADE" +
                ");";
        db.execSQL(createWorkoutLogsTable);

        // 5. Create weight_history Table
        String createWeightHistoryTable = "CREATE TABLE " + TABLE_WEIGHT_HISTORY + " (" +
                COL_WH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_WH_USER_ID + " INTEGER NOT NULL DEFAULT 1, " +
                COL_WH_WEIGHT + " REAL NOT NULL, " +
                COL_WH_DATE + " TEXT NOT NULL, " +
                COL_WH_NOTES + " TEXT" +
                ");";
        db.execSQL(createWeightHistoryTable);

        // 6. Create progress_photos Table
        String createProgressPhotosTable = "CREATE TABLE " + TABLE_PROGRESS_PHOTOS + " (" +
                COL_PHOTO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PHOTO_USER_ID + " INTEGER NOT NULL DEFAULT 1, " +
                COL_PHOTO_PATH + " TEXT NOT NULL, " +
                COL_PHOTO_DATE + " TEXT NOT NULL, " +
                COL_PHOTO_NOTES + " TEXT, " +
                COL_PHOTO_WEIGHT + " REAL" +
                ");";
        db.execSQL(createProgressPhotosTable);

        // 7. Create SQLite TRIGGER to automatically update user_profile.current_weight
        // whenever a new row is inserted into weight_history
        String createWeightTrigger = "CREATE TRIGGER IF NOT EXISTS trg_update_user_current_weight " +
                "AFTER INSERT ON " + TABLE_WEIGHT_HISTORY + " " +
                "BEGIN " +
                "  UPDATE " + TABLE_USER_PROFILE + " " +
                "  SET " + COL_USER_CURRENT_WEIGHT + " = NEW." + COL_WH_WEIGHT + ", " +
                "      " + COL_USER_UPDATED_AT + " = NEW." + COL_WH_DATE + " " +
                "  WHERE " + COL_USER_ID + " = NEW." + COL_WH_USER_ID + "; " +
                "END;";
        db.execSQL(createWeightTrigger);

        // 8. Create workout_summary VIEW using JOIN, GROUP BY, and HAVING
        String createWorkoutSummaryView = "CREATE VIEW IF NOT EXISTS " + VIEW_WORKOUT_SUMMARY + " AS " +
                "SELECT " +
                "  wl." + COL_LOG_DATE + " AS workout_date, " +
                "  COUNT(DISTINCT wl." + COL_LOG_EXERCISE_ID + ") AS exercises_count, " +
                "  SUM(wl." + COL_LOG_SETS + ") AS total_sets, " +
                "  SUM(wl." + COL_LOG_SETS + " * wl." + COL_LOG_REPS + ") AS total_reps, " +
                "  SUM(wl." + COL_LOG_SETS + " * wl." + COL_LOG_REPS + " * wl." + COL_LOG_WEIGHT + ") AS total_volume, " +
                "  GROUP_CONCAT(DISTINCT e." + COL_EX_TITLE + ") AS exercise_names " +
                "FROM " + TABLE_WORKOUT_LOGS + " wl " +
                "INNER JOIN " + TABLE_EXERCISES + " e ON wl." + COL_LOG_EXERCISE_ID + " = e." + COL_EX_ID + " " +
                "GROUP BY wl." + COL_LOG_DATE + " " +
                "HAVING total_volume > 0;";
        db.execSQL(createWorkoutSummaryView);

        Log.d(TAG, "GymDatabaseHelper schema created successfully.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP VIEW IF EXISTS " + VIEW_WORKOUT_SUMMARY);
        db.execSQL("DROP TRIGGER IF EXISTS trg_update_user_current_weight");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROGRESS_PHOTOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHT_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKOUT_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXERCISES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_PROFILE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    // ==========================================
    // 1. USER PROFILE CRUD OPERATIONS
    // ==========================================

    /**
     * Create / Insert a new user profile.
     */
    public long insertUserProfile(UserProfile profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_NAME, profile.getName());
        cv.put(COL_USER_HEIGHT, profile.getHeight());
        cv.put(COL_USER_CURRENT_WEIGHT, profile.getCurrentWeight());
        cv.put(COL_USER_TARGET_WEIGHT, profile.getTargetWeight());
        cv.put(COL_USER_CREATED_AT, profile.getCreatedAt());
        cv.put(COL_USER_UPDATED_AT, profile.getUpdatedAt());
        long id = db.insert(TABLE_USER_PROFILE, null, cv);
        profile.setId(id);
        return id;
    }

    /**
     * Read the primary user profile. Returns null if not configured yet.
     */
    public UserProfile getUserProfile() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER_PROFILE, null, null, null, null, null, COL_USER_ID + " ASC", "1");
        UserProfile profile = null;
        if (cursor != null && cursor.moveToFirst()) {
            profile = new UserProfile(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_USER_HEIGHT)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_USER_CURRENT_WEIGHT)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_USER_TARGET_WEIGHT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CREATED_AT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_UPDATED_AT))
            );
            cursor.close();
        }
        return profile;
    }

    public UserProfile getPrimaryUserProfile() {
        UserProfile profile = getUserProfile();
        if (profile == null) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            profile = new UserProfile(1, "Alex Rivera", 178.0, 74.5, 72.0, today, today);
            long id = insertUserProfile(profile);
            profile.setId(id);
            insertWeightEntry(new WeightHistory(0, id > 0 ? id : 1, 74.5, today, "Initial baseline weigh-in"));
        }
        return profile;
    }

    /**
     * Update an existing user profile.
     */
    public int updateUserProfile(UserProfile profile) {
        if (profile == null) return 0;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_NAME, profile.getName());
        cv.put(COL_USER_HEIGHT, profile.getHeight());
        cv.put(COL_USER_CURRENT_WEIGHT, profile.getCurrentWeight());
        cv.put(COL_USER_TARGET_WEIGHT, profile.getTargetWeight());
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        cv.put(COL_USER_UPDATED_AT, today);

        int rows = 0;
        if (profile.getId() > 0) {
            rows = db.update(TABLE_USER_PROFILE, cv, COL_USER_ID + " = ?", new String[]{String.valueOf(profile.getId())});
        }
        if (rows == 0) {
            rows = db.update(TABLE_USER_PROFILE, cv, null, null);
        }
        if (rows == 0) {
            long newId = insertUserProfile(profile);
            profile.setId(newId);
            rows = (newId > 0) ? 1 : 0;
        }
        return rows;
    }

    /**
     * Save user profile and log weight entry to trigger SQLite sync.
     */
    public void saveOrUpdateUserProfile(UserProfile profile, boolean recordWeightHistory) {
        updateUserProfile(profile);
        if (recordWeightHistory) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            insertWeightEntry(new WeightHistory(0, profile.getId() > 0 ? profile.getId() : 1, profile.getCurrentWeightKg(), today, "Profile biometrics update"));
        }
    }

    /**
     * Delete a user profile.
     */
    public int deleteUserProfile(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_USER_PROFILE, COL_USER_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // ==========================================
    // 2. WORKOUT LOG CRUD OPERATIONS
    // ==========================================

    /**
     * Create / Insert a workout log entry.
     */
    public long insertWorkoutLog(WorkoutLog log) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LOG_USER_ID, log.getUserId());
        cv.put(COL_LOG_EXERCISE_ID, log.getExerciseId());
        cv.put(COL_LOG_DATE, log.getDate());
        cv.put(COL_LOG_SETS, log.getSets());
        cv.put(COL_LOG_REPS, log.getReps());
        cv.put(COL_LOG_WEIGHT, log.getWeight());
        cv.put(COL_LOG_NOTES, log.getNotes());
        long id = db.insert(TABLE_WORKOUT_LOGS, null, cv);
        log.setId(id);
        return id;
    }

    /**
     * Read all workout logs with joined exercise title.
     */
    public List<WorkoutLog> getAllWorkoutLogs() {
        List<WorkoutLog> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT wl." + COL_LOG_ID + ", wl." + COL_LOG_USER_ID + ", wl." + COL_LOG_EXERCISE_ID + ", " +
                "e." + COL_EX_TITLE + " AS exercise_title, wl." + COL_LOG_DATE + ", wl." + COL_LOG_SETS + ", " +
                "wl." + COL_LOG_REPS + ", wl." + COL_LOG_WEIGHT + ", wl." + COL_LOG_NOTES + " " +
                "FROM " + TABLE_WORKOUT_LOGS + " wl " +
                "JOIN " + TABLE_EXERCISES + " e ON wl." + COL_LOG_EXERCISE_ID + " = e." + COL_EX_ID + " " +
                "ORDER BY wl." + COL_LOG_DATE + " DESC, wl." + COL_LOG_ID + " DESC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                WorkoutLog log = new WorkoutLog(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_USER_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_EXERCISE_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow("exercise_title")),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_DATE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_SETS)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_REPS)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_WEIGHT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_NOTES))
                );
                list.add(log);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    /**
     * Read a single workout log by ID.
     */
    public WorkoutLog getWorkoutLogById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT wl." + COL_LOG_ID + ", wl." + COL_LOG_USER_ID + ", wl." + COL_LOG_EXERCISE_ID + ", " +
                "e." + COL_EX_TITLE + " AS exercise_title, wl." + COL_LOG_DATE + ", wl." + COL_LOG_SETS + ", " +
                "wl." + COL_LOG_REPS + ", wl." + COL_LOG_WEIGHT + ", wl." + COL_LOG_NOTES + " " +
                "FROM " + TABLE_WORKOUT_LOGS + " wl " +
                "JOIN " + TABLE_EXERCISES + " e ON wl." + COL_LOG_EXERCISE_ID + " = e." + COL_EX_ID + " " +
                "WHERE wl." + COL_LOG_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});
        WorkoutLog log = null;
        if (cursor != null && cursor.moveToFirst()) {
            log = new WorkoutLog(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_USER_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_EXERCISE_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow("exercise_title")),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_DATE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_SETS)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_REPS)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_WEIGHT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_NOTES))
            );
            cursor.close();
        }
        return log;
    }

    /**
     * Update a workout log.
     */
    public int updateWorkoutLog(WorkoutLog log) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LOG_EXERCISE_ID, log.getExerciseId());
        cv.put(COL_LOG_DATE, log.getDate());
        cv.put(COL_LOG_SETS, log.getSets());
        cv.put(COL_LOG_REPS, log.getReps());
        cv.put(COL_LOG_WEIGHT, log.getWeight());
        cv.put(COL_LOG_NOTES, log.getNotes());
        return db.update(TABLE_WORKOUT_LOGS, cv, COL_LOG_ID + " = ?", new String[]{String.valueOf(log.getId())});
    }

    /**
     * Delete a workout log.
     */
    public int deleteWorkoutLog(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WORKOUT_LOGS, COL_LOG_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // ==========================================
    // 3. WEIGHT HISTORY & TRIGGER VERIFICATION
    // ==========================================

    /**
     * Insert a weight measurement into weight_history.
     * Note: This immediately fires the SQLite TRIGGER 'trg_update_user_current_weight'
     * which updates user_profile.current_weight!
     */
    public long insertWeightEntry(double weight, String date, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_WH_USER_ID, 1);
        cv.put(COL_WH_WEIGHT, weight);
        cv.put(COL_WH_DATE, date);
        cv.put(COL_WH_NOTES, notes);
        return db.insert(TABLE_WEIGHT_HISTORY, null, cv);
    }

    public long insertWeightEntry(WeightHistory entry) {
        if (entry == null) return -1;
        return insertWeightEntry(entry.getWeight(), entry.getDate(), entry.getNotes());
    }

    /**
     * Read all recorded weight history entries.
     */
    public List<WeightHistory> getAllWeightHistory() {
        List<WeightHistory> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WEIGHT_HISTORY, null, null, null, null, null, COL_WH_DATE + " DESC, " + COL_WH_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                WeightHistory item = new WeightHistory(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_WH_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_WH_USER_ID)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_WH_WEIGHT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_WH_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_WH_NOTES))
                );
                list.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<WeightHistory> getWeightHistory() {
        return getAllWeightHistory();
    }

    public List<WeightHistory> getWeightHistory(long userId) {
        return getAllWeightHistory();
    }

    public int deleteWeightEntry(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WEIGHT_HISTORY, COL_WH_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // ==========================================
    // 4. PROGRESS PHOTOS CRUD
    // ==========================================

    public long insertProgressPhoto(ProgressPhoto photo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PHOTO_USER_ID, photo.getUserId());
        cv.put(COL_PHOTO_PATH, photo.getImagePath());
        cv.put(COL_PHOTO_DATE, photo.getDate());
        cv.put(COL_PHOTO_NOTES, photo.getNotes());
        cv.put(COL_PHOTO_WEIGHT, photo.getWeight());
        long id = db.insert(TABLE_PROGRESS_PHOTOS, null, cv);
        photo.setId(id);
        return id;
    }

    public List<ProgressPhoto> getAllProgressPhotos() {
        List<ProgressPhoto> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PROGRESS_PHOTOS, null, null, null, null, null, COL_PHOTO_DATE + " DESC, " + COL_PHOTO_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                ProgressPhoto item = new ProgressPhoto(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_PHOTO_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_PHOTO_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_PATH)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_NOTES)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PHOTO_WEIGHT))
                );
                list.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<ProgressPhoto> getAllProgressPhotos(long userId) {
        return getAllProgressPhotos();
    }

    public int deleteProgressPhoto(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PROGRESS_PHOTOS, COL_PHOTO_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // ==========================================
    // 5. EXERCISES DATABASE OPERATIONS
    // ==========================================

    public int getExerciseCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EXERCISES, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public long insertExercise(Exercise ex) {
        if (ex == null) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if (ex.getId() > 0) {
            cv.put(COL_EX_ID, ex.getId());
        }
        cv.put(COL_EX_TITLE, ex.getTitle());
        cv.put(COL_EX_BODY_PART, ex.getBodyPart());
        cv.put(COL_EX_EQUIPMENT, ex.getEquipment());
        cv.put(COL_EX_DIFFICULTY, ex.getDifficulty());
        cv.put(COL_EX_INSTRUCTIONS, ex.getInstructions());
        cv.put(COL_EX_YOUTUBE_URL, ex.getYoutubeUrl());
        long id = db.insertWithOnConflict(TABLE_EXERCISES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (id > 0) {
            ex.setId(id);
        }
        return id;
    }

    public void bulkInsertExercises(List<Exercise> exercises) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Exercise ex : exercises) {
                ContentValues cv = new ContentValues();
                if (ex.getId() > 0) {
                    cv.put(COL_EX_ID, ex.getId());
                }
                cv.put(COL_EX_TITLE, ex.getTitle());
                cv.put(COL_EX_BODY_PART, ex.getBodyPart());
                cv.put(COL_EX_EQUIPMENT, ex.getEquipment());
                cv.put(COL_EX_DIFFICULTY, ex.getDifficulty());
                cv.put(COL_EX_INSTRUCTIONS, ex.getInstructions());
                cv.put(COL_EX_YOUTUBE_URL, ex.getYoutubeUrl());
                db.insertWithOnConflict(TABLE_EXERCISES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Exercise> getAllExercises() {
        return queryExercises(null, null);
    }

    public List<Exercise> getExercisesByBodyPart(String bodyPart) {
        return queryExercises(COL_EX_BODY_PART + " = ?", new String[]{bodyPart});
    }

    public List<Exercise> searchExercises(String query, String bodyPartFilter) {
        if (bodyPartFilter != null && !bodyPartFilter.equalsIgnoreCase("All")) {
            return queryExercises(COL_EX_BODY_PART + " = ? AND " + COL_EX_TITLE + " LIKE ?",
                    new String[]{bodyPartFilter, "%" + query + "%"});
        } else {
            return queryExercises(COL_EX_TITLE + " LIKE ?", new String[]{"%" + query + "%"});
        }
    }

    public Exercise getExerciseById(long id) {
        List<Exercise> list = queryExercises(COL_EX_ID + " = ?", new String[]{String.valueOf(id)});
        return list.isEmpty() ? null : list.get(0);
    }

    public List<String> getDistinctBodyParts() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COL_EX_BODY_PART + " FROM " + TABLE_EXERCISES + " ORDER BY " + COL_EX_BODY_PART + " ASC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public double getPersonalBestForExercise(long exerciseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT MAX(" + COL_LOG_WEIGHT + ") FROM " + TABLE_WORKOUT_LOGS + " WHERE " + COL_LOG_EXERCISE_ID + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(exerciseId)});
        double maxWeight = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            maxWeight = cursor.getDouble(0);
            cursor.close();
        }
        return maxWeight;
    }

    private List<Exercise> queryExercises(String selection, String[] selectionArgs) {
        List<Exercise> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXERCISES, null, selection, selectionArgs, null, null, COL_EX_TITLE + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Exercise ex = new Exercise(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_EX_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EX_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EX_BODY_PART)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EX_EQUIPMENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EX_DIFFICULTY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EX_INSTRUCTIONS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EX_YOUTUBE_URL))
                );
                list.add(ex);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    // ==========================================
    // 6. ADVANCED SQL: VIEW (JOIN, GROUP BY, HAVING)
    // ==========================================

    /**
     * Queries the workout_summary SQLite VIEW created in onCreate().
     * The VIEW executes:
     * - JOIN between workout_logs and exercises
     * - GROUP BY date
     * - HAVING total_volume > 0
     */
    public List<WorkoutSummary> getRecentWorkoutSummaries(int limit) {
        List<WorkoutSummary> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT workout_date, exercises_count, total_sets, total_reps, total_volume, exercise_names " +
                "FROM " + VIEW_WORKOUT_SUMMARY + " " +
                "ORDER BY workout_date DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                WorkoutSummary ws = new WorkoutSummary(
                        cursor.getString(cursor.getColumnIndexOrThrow("workout_date")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("exercises_count")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("total_sets")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("total_reps")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("total_volume")),
                        cursor.getString(cursor.getColumnIndexOrThrow("exercise_names"))
                );
                list.add(ws);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<WorkoutSummary> getRecentWorkoutSummaries() {
        return getRecentWorkoutSummaries(10);
    }

    /**
     * Aggregates total volume of all completed workouts.
     */
    public double getTotalAllTimeVolume() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(total_volume) FROM " + VIEW_WORKOUT_SUMMARY, null);
        double sum = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            sum = cursor.getDouble(0);
            cursor.close();
        }
        return sum;
    }

    // ==========================================
    // 7. ADVANCED SQL: SUBQUERIES FOR PERSONAL BEST
    // ==========================================

    /**
     * Demonstrates an SQL SUBQUERY to find the user's top personal best lifts
     * where each exercise's maximum logged weight is isolated by a correlated subquery:
     * SELECT e.title, wl.weight, wl.reps, wl.date
     * FROM workout_logs wl
     * JOIN exercises e ON wl.exercise_id = e.id
     * WHERE wl.weight = (SELECT MAX(sub.weight) FROM workout_logs sub WHERE sub.exercise_id = wl.exercise_id)
     */
    public List<PersonalBest> getTopPersonalBests(int limit) {
        List<PersonalBest> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT e." + COL_EX_TITLE + " AS exercise_title, wl." + COL_LOG_WEIGHT + " AS max_weight, " +
                "wl." + COL_LOG_REPS + " AS reps, wl." + COL_LOG_DATE + " AS date " +
                "FROM " + TABLE_WORKOUT_LOGS + " wl " +
                "INNER JOIN " + TABLE_EXERCISES + " e ON wl." + COL_LOG_EXERCISE_ID + " = e." + COL_EX_ID + " " +
                "WHERE wl." + COL_LOG_WEIGHT + " = (" +
                "  SELECT MAX(sub." + COL_LOG_WEIGHT + ") " +
                "  FROM " + TABLE_WORKOUT_LOGS + " sub " +
                "  WHERE sub." + COL_LOG_EXERCISE_ID + " = wl." + COL_LOG_EXERCISE_ID +
                ") " +
                "GROUP BY e." + COL_EX_ID + " " +
                "ORDER BY max_weight DESC LIMIT " + limit;

        Cursor cursor = db.rawQuery(sql, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                PersonalBest pb = new PersonalBest(
                        cursor.getString(cursor.getColumnIndexOrThrow("exercise_title")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("max_weight")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("reps")),
                        cursor.getString(cursor.getColumnIndexOrThrow("date"))
                );
                list.add(pb);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    /**
     * Subquery to find the single all-time heaviest lift recorded by the user.
     */
    public PersonalBest getOverallHeaviestLift() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT e." + COL_EX_TITLE + " AS exercise_title, wl." + COL_LOG_WEIGHT + " AS max_weight, " +
                "wl." + COL_LOG_REPS + " AS reps, wl." + COL_LOG_DATE + " AS date " +
                "FROM " + TABLE_WORKOUT_LOGS + " wl " +
                "INNER JOIN " + TABLE_EXERCISES + " e ON wl." + COL_LOG_EXERCISE_ID + " = e." + COL_EX_ID + " " +
                "WHERE wl." + COL_LOG_WEIGHT + " = (SELECT MAX(" + COL_LOG_WEIGHT + ") FROM " + TABLE_WORKOUT_LOGS + ") " +
                "LIMIT 1";

        Cursor cursor = db.rawQuery(sql, null);
        PersonalBest pb = null;
        if (cursor != null && cursor.moveToFirst()) {
            pb = new PersonalBest(
                    cursor.getString(cursor.getColumnIndexOrThrow("exercise_title")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("max_weight")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("reps")),
                    cursor.getString(cursor.getColumnIndexOrThrow("date"))
            );
            cursor.close();
        }
        return pb;
    }

    public WorkoutLog getOverallPersonalBestLift() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT wl." + COL_LOG_ID + ", wl." + COL_LOG_USER_ID + ", wl." + COL_LOG_EXERCISE_ID + ", " +
                "e." + COL_EX_TITLE + " AS exercise_title, wl." + COL_LOG_DATE + ", wl." + COL_LOG_SETS + ", " +
                "wl." + COL_LOG_REPS + ", wl." + COL_LOG_WEIGHT + ", wl." + COL_LOG_NOTES + " " +
                "FROM " + TABLE_WORKOUT_LOGS + " wl " +
                "INNER JOIN " + TABLE_EXERCISES + " e ON wl." + COL_LOG_EXERCISE_ID + " = e." + COL_EX_ID + " " +
                "WHERE wl." + COL_LOG_WEIGHT + " = (SELECT MAX(" + COL_LOG_WEIGHT + ") FROM " + TABLE_WORKOUT_LOGS + ") " +
                "LIMIT 1";

        Cursor cursor = db.rawQuery(sql, null);
        WorkoutLog log = null;
        if (cursor != null && cursor.moveToFirst()) {
            log = new WorkoutLog(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_USER_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_EXERCISE_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow("exercise_title")),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_DATE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_SETS)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_REPS)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_WEIGHT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_NOTES))
            );
            cursor.close();
        }
        return log;
    }

    /**
     * Seeds initial profile and starter workouts if database is freshly created.
     */
    public void seedInitialUserDataIfEmpty() {
        if (getUserProfile() == null) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            UserProfile initialProfile = new UserProfile(
                    1,
                    "Alex Rivera",
                    180.0,
                    75.8,
                    74.0,
                    today,
                    today
            );
            insertUserProfile(initialProfile);

            // Record starting weight in weight_history
            insertWeightEntry(76.5, "2026-08-20", "Starting bodyweight baseline");
            insertWeightEntry(76.0, "2026-08-28", "End of week 1");
            insertWeightEntry(75.8, today, "Current weighed in morning");

            // Seed sample workouts if exercises exist
            if (getExerciseCount() > 0) {
                insertWorkoutLog(new WorkoutLog(1, 1, 1, "Barbell Bench Press", today, 4, 10, 80.0, "Smooth pause reps"));
                insertWorkoutLog(new WorkoutLog(2, 1, 24, "Romanian Deadlift", today, 3, 8, 100.0, "Hamstring stretch"));
                insertWorkoutLog(new WorkoutLog(3, 1, 21, "Barbell Back Squat", "2026-09-03", 5, 5, 110.0, "Heavy sets"));
                insertWorkoutLog(new WorkoutLog(4, 1, 33, "Overhead Press (OHP)", "2026-09-03", 4, 8, 55.0, "Strict form"));
            }
        }
    }
}
