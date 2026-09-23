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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    public static final String COL_USER_PROFILE_IMAGE = "profile_image_path";

    // Table: exercises
    public static final String TABLE_EXERCISES = "exercises";
    public static final String COL_EX_ID = "id";
    public static final String COL_EX_TITLE = "title";
    public static final String COL_EX_BODY_PART = "body_part";
    public static final String COL_EX_EQUIPMENT = "equipment";
    public static final String COL_EX_DIFFICULTY = "difficulty";
    public static final String COL_EX_INSTRUCTIONS = "instructions";
    public static final String COL_EX_YOUTUBE_URL = "youtube_url";
    public static final String COL_EX_TYPE = "exercise_type";
    public static final String COL_EX_RATING = "rating";
    public static final String COL_EX_VIDEO_TITLE = "video_title";
    public static final String COL_EX_YOUTUBE_CHANNEL = "youtube_channel";
    public static final String COL_EX_VIDEO_MATCH = "video_match";
    public static final String COL_EX_IS_FAVORITE = "is_favorite";

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
    public static final String COL_LOG_SPLIT_NAME = "split_name";

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

    // Table: user_accounts
    public static final String TABLE_USER_ACCOUNTS = "user_accounts";
    public static final String COL_ACC_ID = "id";
    public static final String COL_ACC_NAME = "name";
    public static final String COL_ACC_IDENTIFIER = "identifier";
    public static final String COL_ACC_PASSWORD = "password";
    public static final String COL_ACC_CREATED_AT = "created_at";

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
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Seamless migration for existing installations without duplicate column errors
        try {
            if (isColumnMissing(db, TABLE_USER_PROFILE, COL_USER_PROFILE_IMAGE)) {
                db.execSQL("ALTER TABLE " + TABLE_USER_PROFILE + " ADD COLUMN " + COL_USER_PROFILE_IMAGE + " TEXT;");
            }
        } catch (Exception ignored) {}
        try {
            if (isColumnMissing(db, TABLE_WORKOUT_LOGS, COL_LOG_SPLIT_NAME)) {
                db.execSQL("ALTER TABLE " + TABLE_WORKOUT_LOGS + " ADD COLUMN " + COL_LOG_SPLIT_NAME + " TEXT;");
            }
        } catch (Exception ignored) {}
        try {
            if (isColumnMissing(db, TABLE_EXERCISES, COL_EX_TYPE)) {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_TYPE + " TEXT;");
            }
            if (isColumnMissing(db, TABLE_EXERCISES, COL_EX_RATING)) {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_RATING + " REAL DEFAULT 4.8;");
            }
            if (isColumnMissing(db, TABLE_EXERCISES, COL_EX_VIDEO_TITLE)) {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_VIDEO_TITLE + " TEXT;");
            }
            if (isColumnMissing(db, TABLE_EXERCISES, COL_EX_YOUTUBE_CHANNEL)) {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_YOUTUBE_CHANNEL + " TEXT;");
            }
            if (isColumnMissing(db, TABLE_EXERCISES, COL_EX_VIDEO_MATCH)) {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_VIDEO_MATCH + " TEXT;");
            }
            if (isColumnMissing(db, TABLE_EXERCISES, COL_EX_IS_FAVORITE)) {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_IS_FAVORITE + " INTEGER DEFAULT 0;");
            }
        } catch (Exception ignored) {}

        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER_ACCOUNTS + " (" +
                    COL_ACC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_ACC_NAME + " TEXT NOT NULL, " +
                    COL_ACC_IDENTIFIER + " TEXT NOT NULL UNIQUE, " +
                    COL_ACC_PASSWORD + " TEXT NOT NULL, " +
                    COL_ACC_CREATED_AT + " TEXT NOT NULL" +
                    ");");
        } catch (Exception ignored) {}
    }

    private boolean isColumnMissing(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if (columnName.equalsIgnoreCase(cursor.getString(nameIndex))) {
                            return false; // column already exists
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return true; // column is missing
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
                COL_USER_UPDATED_AT + " TEXT, " +
                COL_USER_PROFILE_IMAGE + " TEXT" +
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
                COL_EX_YOUTUBE_URL + " TEXT NOT NULL, " +
                COL_EX_TYPE + " TEXT, " +
                COL_EX_RATING + " REAL DEFAULT 4.8, " +
                COL_EX_VIDEO_TITLE + " TEXT, " +
                COL_EX_YOUTUBE_CHANNEL + " TEXT, " +
                COL_EX_VIDEO_MATCH + " TEXT, " +
                COL_EX_IS_FAVORITE + " INTEGER DEFAULT 0" +
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
                COL_LOG_SPLIT_NAME + " TEXT, " +
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

        // 9. Create user_accounts Table
        String createUserAccountsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_USER_ACCOUNTS + " (" +
                COL_ACC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ACC_NAME + " TEXT NOT NULL, " +
                COL_ACC_IDENTIFIER + " TEXT NOT NULL UNIQUE, " +
                COL_ACC_PASSWORD + " TEXT NOT NULL, " +
                COL_ACC_CREATED_AT + " TEXT NOT NULL" +
                ");";
        db.execSQL(createUserAccountsTable);

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
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_ACCOUNTS);
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
        cv.put(COL_USER_PROFILE_IMAGE, profile.getProfileImagePath());
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
            int imageColIdx = cursor.getColumnIndex(COL_USER_PROFILE_IMAGE);
            String imagePath = (imageColIdx != -1) ? cursor.getString(imageColIdx) : null;

            profile = new UserProfile(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_USER_HEIGHT)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_USER_CURRENT_WEIGHT)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_USER_TARGET_WEIGHT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CREATED_AT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_UPDATED_AT)),
                    imagePath
            );
            cursor.close();
        }
        return profile;
    }

    public UserProfile getPrimaryUserProfile() {
        UserProfile profile = getUserProfile();
        if (profile == null) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            profile = new UserProfile(1, "Alex Rivera", 178.0, 74.5, 72.0, today, today, null);
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
        cv.put(COL_USER_PROFILE_IMAGE, profile.getProfileImagePath());
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
        cv.put(COL_LOG_SPLIT_NAME, log.getSplitName());
        long id = db.insert(TABLE_WORKOUT_LOGS, null, cv);
        log.setId(id);
        return id;
    }

    /**
     * Inserts a complete workout session with a designated split name and all constituent exercises.
     */
    public long logWorkoutSession(String splitName, String date, List<Exercise> exercises, String notes) {
        if (exercises == null || exercises.isEmpty()) return 0;
        SQLiteDatabase db = this.getWritableDatabase();
        long lastId = 0;
        db.beginTransaction();
        try {
            for (Exercise ex : exercises) {
                ContentValues cv = new ContentValues();
                cv.put(COL_LOG_USER_ID, 1);
                cv.put(COL_LOG_EXERCISE_ID, ex.getId());
                cv.put(COL_LOG_DATE, date);
                cv.put(COL_LOG_SETS, 3);
                cv.put(COL_LOG_REPS, 10);
                cv.put(COL_LOG_WEIGHT, 0.0);
                cv.put(COL_LOG_NOTES, notes);
                cv.put(COL_LOG_SPLIT_NAME, splitName != null && !splitName.isEmpty() ? splitName : "Workout Split");
                lastId = db.insert(TABLE_WORKOUT_LOGS, null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return lastId;
    }

    /**
     * Retrieves all exercises that were logged for a specific workout split session on a specific date.
     */
    public List<Exercise> getExercisesForSession(String splitName, String date) {
        List<Exercise> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT DISTINCT e." + COL_EX_ID + ", e." + COL_EX_TITLE + ", e." + COL_EX_BODY_PART + ", " +
                "e." + COL_EX_EQUIPMENT + ", e." + COL_EX_DIFFICULTY + ", e." + COL_EX_INSTRUCTIONS + ", " +
                "e." + COL_EX_YOUTUBE_URL + ", e." + COL_EX_TYPE + ", e." + COL_EX_RATING + ", " +
                "e." + COL_EX_VIDEO_TITLE + ", e." + COL_EX_YOUTUBE_CHANNEL + ", e." + COL_EX_VIDEO_MATCH + " " +
                "FROM " + TABLE_EXERCISES + " e " +
                "INNER JOIN " + TABLE_WORKOUT_LOGS + " wl ON e." + COL_EX_ID + " = wl." + COL_LOG_EXERCISE_ID + " " +
                "WHERE wl." + COL_LOG_SPLIT_NAME + " = ? AND wl." + COL_LOG_DATE + " = ? " +
                "ORDER BY wl." + COL_LOG_ID + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{splitName, date});
        if (cursor != null && cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndex(COL_EX_ID);
            int titleIdx = cursor.getColumnIndex(COL_EX_TITLE);
            int bpIdx = cursor.getColumnIndex(COL_EX_BODY_PART);
            int eqIdx = cursor.getColumnIndex(COL_EX_EQUIPMENT);
            int diffIdx = cursor.getColumnIndex(COL_EX_DIFFICULTY);
            int instIdx = cursor.getColumnIndex(COL_EX_INSTRUCTIONS);
            int ytIdx = cursor.getColumnIndex(COL_EX_YOUTUBE_URL);
            int typeIdx = cursor.getColumnIndex(COL_EX_TYPE);
            int ratingIdx = cursor.getColumnIndex(COL_EX_RATING);
            int vtIdx = cursor.getColumnIndex(COL_EX_VIDEO_TITLE);
            int ycIdx = cursor.getColumnIndex(COL_EX_YOUTUBE_CHANNEL);
            int vmIdx = cursor.getColumnIndex(COL_EX_VIDEO_MATCH);

            do {
                long id = idIdx != -1 ? cursor.getLong(idIdx) : 0;
                String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                String bp = bpIdx != -1 ? cursor.getString(bpIdx) : "";
                String eq = eqIdx != -1 ? cursor.getString(eqIdx) : "";
                String diff = diffIdx != -1 ? cursor.getString(diffIdx) : "";
                String inst = instIdx != -1 ? cursor.getString(instIdx) : "";
                String yt = ytIdx != -1 ? cursor.getString(ytIdx) : "";
                String type = typeIdx != -1 ? cursor.getString(typeIdx) : "Strength";
                double rating = ratingIdx != -1 ? cursor.getDouble(ratingIdx) : 4.8;
                String vt = vtIdx != -1 ? cursor.getString(vtIdx) : "";
                String yc = ycIdx != -1 ? cursor.getString(ycIdx) : "";
                String vm = vmIdx != -1 ? cursor.getString(vmIdx) : "Related";

                list.add(new Exercise(id, title, inst, type, bp, eq, diff, rating, vt, yc, yt, vm));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    /**
     * Updates an existing workout split session across the entire database.
     * When user modifies anything:
     * 1. If split name changed, updates all occurrences of that split name throughout TABLE_WORKOUT_LOGS in the whole DB.
     * 2. Synchronizes this session's date, exercises, and notes in TABLE_WORKOUT_LOGS.
     */
    public boolean updateWorkoutSession(String oldSplitName, String oldDate, String newSplitName, String newDate, List<Exercise> updatedExercises, String notes) {
        if (updatedExercises == null || updatedExercises.isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            String finalSplit = (newSplitName != null && !newSplitName.trim().isEmpty()) ? newSplitName.trim() : "Workout Split";
            String finalDate = (newDate != null && !newDate.trim().isEmpty()) ? newDate.trim() : oldDate;

            // 1. If split name changed, update the split name everywhere in the DB!
            if (oldSplitName != null && !oldSplitName.equals(finalSplit)) {
                ContentValues renameCv = new ContentValues();
                renameCv.put(COL_LOG_SPLIT_NAME, finalSplit);
                db.update(TABLE_WORKOUT_LOGS, renameCv, COL_LOG_SPLIT_NAME + " = ?", new String[]{oldSplitName});
            }

            // 2. Fetch existing logs for this session to preserve existing sets/reps/weights
            Map<Long, WorkoutLog> existingLogsByExercise = new HashMap<>();
            String fetchLogsSql = "SELECT * FROM " + TABLE_WORKOUT_LOGS + " WHERE " + COL_LOG_SPLIT_NAME + " = ? AND " + COL_LOG_DATE + " = ?";
            Cursor cursor = db.rawQuery(fetchLogsSql, new String[]{finalSplit, oldDate});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long exId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_EXERCISE_ID));
                    long logId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_ID));
                    int sets = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_SETS));
                    int reps = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_REPS));
                    double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_WEIGHT));
                    existingLogsByExercise.put(exId, new WorkoutLog(logId, 1, exId, "", oldDate, sets, reps, weight, notes, finalSplit));
                } while (cursor.moveToNext());
                cursor.close();
            }

            // 3. Remove old session logs for (finalSplit, oldDate)
            db.delete(TABLE_WORKOUT_LOGS, COL_LOG_SPLIT_NAME + " = ? AND " + COL_LOG_DATE + " = ?", new String[]{finalSplit, oldDate});

            // If date changed, also clean up any collision for (finalSplit, finalDate)
            if (!oldDate.equals(finalDate)) {
                db.delete(TABLE_WORKOUT_LOGS, COL_LOG_SPLIT_NAME + " = ? AND " + COL_LOG_DATE + " = ?", new String[]{finalSplit, finalDate});
            }

            // 4. Re-insert updated exercises with updated date, notes, and preserved/default metrics
            for (Exercise ex : updatedExercises) {
                ContentValues cv = new ContentValues();
                cv.put(COL_LOG_USER_ID, 1);
                cv.put(COL_LOG_EXERCISE_ID, ex.getId());
                cv.put(COL_LOG_DATE, finalDate);
                cv.put(COL_LOG_SPLIT_NAME, finalSplit);
                cv.put(COL_LOG_NOTES, notes);

                WorkoutLog existing = existingLogsByExercise.get(ex.getId());
                if (existing != null) {
                    cv.put(COL_LOG_SETS, existing.getSets());
                    cv.put(COL_LOG_REPS, existing.getReps());
                    cv.put(COL_LOG_WEIGHT, existing.getWeight());
                } else {
                    cv.put(COL_LOG_SETS, 3);
                    cv.put(COL_LOG_REPS, 10);
                    cv.put(COL_LOG_WEIGHT, 0.0);
                }
                db.insert(TABLE_WORKOUT_LOGS, null, cv);
            }

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating workout session: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Deletes a specific workout split session by split name and date.
     */
    public int deleteWorkoutSession(String splitName, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WORKOUT_LOGS,
                COL_LOG_SPLIT_NAME + " = ? AND " + COL_LOG_DATE + " = ?",
                new String[]{splitName, date});
    }

    /**
     * Deletes all logs for a split name across the entire database.
     */
    public int deleteSplitAcrossDb(String splitName) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WORKOUT_LOGS, COL_LOG_SPLIT_NAME + " = ?", new String[]{splitName});
    }

    /**
     * Returns a distinct list of all workout split names previously logged by the user.
     */
    public List<String> getDistinctWorkoutSplitNames() {
        List<String> splits = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT DISTINCT " + COL_LOG_SPLIT_NAME + " FROM " + TABLE_WORKOUT_LOGS +
                " WHERE " + COL_LOG_SPLIT_NAME + " IS NOT NULL AND TRIM(" + COL_LOG_SPLIT_NAME + ") != '' " +
                " ORDER BY " + COL_LOG_DATE + " DESC, " + COL_LOG_ID + " DESC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty() && !splits.contains(name.trim())) {
                    splits.add(name.trim());
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return splits;
    }

    /**
     * Adds an exercise to an existing workout session on a given date, or creates a new split session if it doesn't exist yet.
     * Prevents duplicate exercise entries in the same session.
     */
    public boolean addExerciseToSplitSession(String splitName, String date, Exercise exercise, int sets, int reps, double weight, String notes) {
        if (exercise == null) return false;
        String finalSplit = (splitName != null && !splitName.trim().isEmpty()) ? splitName.trim() : "Workout Split";
        String finalDate = (date != null && !date.trim().isEmpty()) ? date.trim() : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

        SQLiteDatabase db = this.getWritableDatabase();

        // Check if exercise already exists in this split and date
        String checkQuery = "SELECT " + COL_LOG_ID + " FROM " + TABLE_WORKOUT_LOGS +
                " WHERE " + COL_LOG_SPLIT_NAME + " = ? AND " + COL_LOG_DATE + " = ? AND " + COL_LOG_EXERCISE_ID + " = ?";
        Cursor cursor = db.rawQuery(checkQuery, new String[]{finalSplit, finalDate, String.valueOf(exercise.getId())});
        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            // Already present, update the set/rep info
            ContentValues updateCv = new ContentValues();
            updateCv.put(COL_LOG_SETS, sets > 0 ? sets : 3);
            updateCv.put(COL_LOG_REPS, reps > 0 ? reps : 10);
            updateCv.put(COL_LOG_WEIGHT, weight);
            if (notes != null && !notes.trim().isEmpty()) {
                updateCv.put(COL_LOG_NOTES, notes.trim());
            }
            db.update(TABLE_WORKOUT_LOGS, updateCv,
                    COL_LOG_SPLIT_NAME + " = ? AND " + COL_LOG_DATE + " = ? AND " + COL_LOG_EXERCISE_ID + " = ?",
                    new String[]{finalSplit, finalDate, String.valueOf(exercise.getId())});
            return true;
        }
        if (cursor != null) cursor.close();

        // Insert new entry into this session
        ContentValues cv = new ContentValues();
        cv.put(COL_LOG_USER_ID, 1);
        cv.put(COL_LOG_EXERCISE_ID, exercise.getId());
        cv.put(COL_LOG_DATE, finalDate);
        cv.put(COL_LOG_SETS, sets > 0 ? sets : 3);
        cv.put(COL_LOG_REPS, reps > 0 ? reps : 10);
        cv.put(COL_LOG_WEIGHT, weight);
        cv.put(COL_LOG_NOTES, notes != null ? notes.trim() : "");
        cv.put(COL_LOG_SPLIT_NAME, finalSplit);
        long id = db.insert(TABLE_WORKOUT_LOGS, null, cv);
        return id > 0;
    }

    /**
     * Renames a split across all records in the database.
     */
    public int renameSplitAcrossDb(String oldSplitName, String newSplitName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LOG_SPLIT_NAME, newSplitName);
        return db.update(TABLE_WORKOUT_LOGS, cv, COL_LOG_SPLIT_NAME + " = ?", new String[]{oldSplitName});
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
        UserProfile profile = getPrimaryUserProfile();
        long userId = (profile != null && profile.getId() > 0) ? profile.getId() : 1;
        ContentValues cv = new ContentValues();
        cv.put(COL_WH_USER_ID, userId);
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

    public boolean hasCorruptExerciseData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EXERCISES +
                    " WHERE " + COL_EX_YOUTUBE_URL + " NOT LIKE 'http%' OR " +
                    COL_EX_BODY_PART + " LIKE '%exercise targeting%'", null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0) > 0;
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return false;
    }

    public void repopulateExercises(List<Exercise> exercises) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_EXERCISES, null, null);
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
                cv.put(COL_EX_TYPE, ex.getExerciseType());
                cv.put(COL_EX_RATING, ex.getRating());
                cv.put(COL_EX_VIDEO_TITLE, ex.getVideoTitle());
                cv.put(COL_EX_YOUTUBE_CHANNEL, ex.getYoutubeChannel());
                cv.put(COL_EX_VIDEO_MATCH, ex.getVideoMatch());
                cv.put(COL_EX_IS_FAVORITE, ex.isFavorite() ? 1 : 0);
                db.insertWithOnConflict(TABLE_EXERCISES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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
        cv.put(COL_EX_TYPE, ex.getExerciseType());
        cv.put(COL_EX_RATING, ex.getRating());
        cv.put(COL_EX_VIDEO_TITLE, ex.getVideoTitle());
        cv.put(COL_EX_YOUTUBE_CHANNEL, ex.getYoutubeChannel());
        cv.put(COL_EX_VIDEO_MATCH, ex.getVideoMatch());
        cv.put(COL_EX_IS_FAVORITE, ex.isFavorite() ? 1 : 0);
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
                cv.put(COL_EX_TYPE, ex.getExerciseType());
                cv.put(COL_EX_RATING, ex.getRating());
                cv.put(COL_EX_VIDEO_TITLE, ex.getVideoTitle());
                cv.put(COL_EX_YOUTUBE_CHANNEL, ex.getYoutubeChannel());
                cv.put(COL_EX_VIDEO_MATCH, ex.getVideoMatch());
                cv.put(COL_EX_IS_FAVORITE, ex.isFavorite() ? 1 : 0);
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
        String safeQuery = query != null ? query.trim() : "";
        if ("Exact".equalsIgnoreCase(bodyPartFilter) || "★ Exact".equalsIgnoreCase(bodyPartFilter) || "Exact Videos".equalsIgnoreCase(bodyPartFilter)) {
            return queryExercises(COL_EX_VIDEO_MATCH + " = 'Exact' AND " + COL_EX_TITLE + " LIKE ?",
                    new String[]{"%" + safeQuery + "%"});
        } else if ("Favorites".equalsIgnoreCase(bodyPartFilter) || "★ Favorites".equalsIgnoreCase(bodyPartFilter) || "Liked".equalsIgnoreCase(bodyPartFilter)) {
            return queryExercises(COL_EX_IS_FAVORITE + " = 1 AND " + COL_EX_TITLE + " LIKE ?",
                    new String[]{"%" + safeQuery + "%"});
        } else if (bodyPartFilter != null && !bodyPartFilter.equalsIgnoreCase("All")) {
            return queryExercises(COL_EX_BODY_PART + " = ? AND " + COL_EX_TITLE + " LIKE ?",
                    new String[]{bodyPartFilter, "%" + safeQuery + "%"});
        } else {
            return queryExercises(COL_EX_TITLE + " LIKE ?", new String[]{"%" + safeQuery + "%"});
        }
    }

    public boolean setExerciseFavorite(long exerciseId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EX_IS_FAVORITE, isFavorite ? 1 : 0);
        int rows = db.update(TABLE_EXERCISES, cv, COL_EX_ID + " = ?", new String[]{String.valueOf(exerciseId)});
        return rows > 0;
    }

    public Exercise getExerciseById(long id) {
        List<Exercise> list = queryExercises(COL_EX_ID + " = ?", new String[]{String.valueOf(id)});
        return list.isEmpty() ? null : list.get(0);
    }

    public List<String> getDistinctBodyParts() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COL_EX_BODY_PART + " FROM " + TABLE_EXERCISES +
                " WHERE " + COL_EX_BODY_PART + " NOT LIKE '%exercise targeting%' AND length(" + COL_EX_BODY_PART + ") < 30" +
                " ORDER BY " + COL_EX_BODY_PART + " ASC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String bp = cursor.getString(0);
                if (bp != null && !bp.trim().isEmpty()) {
                    list.add(bp.trim());
                }
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
        // Priority sort: EXACT video matches appear at the very TOP, followed by high ratings, then alphabetical
        String orderBy = "CASE WHEN " + COL_EX_VIDEO_MATCH + " = 'Exact' THEN 0 ELSE 1 END, " +
                COL_EX_RATING + " DESC, " + COL_EX_TITLE + " ASC";
        Cursor cursor = db.query(TABLE_EXERCISES, null, selection, selectionArgs, null, null, orderBy);
        if (cursor != null && cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndex(COL_EX_ID);
            int titleIdx = cursor.getColumnIndex(COL_EX_TITLE);
            int bpIdx = cursor.getColumnIndex(COL_EX_BODY_PART);
            int eqIdx = cursor.getColumnIndex(COL_EX_EQUIPMENT);
            int diffIdx = cursor.getColumnIndex(COL_EX_DIFFICULTY);
            int instIdx = cursor.getColumnIndex(COL_EX_INSTRUCTIONS);
            int ytIdx = cursor.getColumnIndex(COL_EX_YOUTUBE_URL);
            int typeIdx = cursor.getColumnIndex(COL_EX_TYPE);
            int ratingIdx = cursor.getColumnIndex(COL_EX_RATING);
            int vtIdx = cursor.getColumnIndex(COL_EX_VIDEO_TITLE);
            int ycIdx = cursor.getColumnIndex(COL_EX_YOUTUBE_CHANNEL);
            int vmIdx = cursor.getColumnIndex(COL_EX_VIDEO_MATCH);
            int favIdx = cursor.getColumnIndex(COL_EX_IS_FAVORITE);

            do {
                long id = idIdx != -1 ? cursor.getLong(idIdx) : 0;
                String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                String bp = bpIdx != -1 ? cursor.getString(bpIdx) : "";
                String eq = eqIdx != -1 ? cursor.getString(eqIdx) : "";
                String diff = diffIdx != -1 ? cursor.getString(diffIdx) : "";
                String inst = instIdx != -1 ? cursor.getString(instIdx) : "";
                String yt = ytIdx != -1 ? cursor.getString(ytIdx) : "";
                String type = typeIdx != -1 ? cursor.getString(typeIdx) : "Strength";
                double rating = ratingIdx != -1 ? cursor.getDouble(ratingIdx) : 4.8;
                String vt = vtIdx != -1 ? cursor.getString(vtIdx) : "";
                String yc = ycIdx != -1 ? cursor.getString(ycIdx) : "";
                String vm = vmIdx != -1 ? cursor.getString(vmIdx) : "Related";
                boolean isFav = favIdx != -1 && cursor.getInt(favIdx) == 1;

                Exercise ex = new Exercise(id, title, inst, type, bp, eq, diff, rating, vt, yc, yt, vm);
                ex.setFavorite(isFav);
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
        String query = "SELECT " +
                "wl." + COL_LOG_DATE + " AS workout_date, " +
                "COALESCE(wl." + COL_LOG_SPLIT_NAME + ", 'Workout Split') AS split_name, " +
                "COUNT(DISTINCT wl." + COL_LOG_EXERCISE_ID + ") AS exercises_count, " +
                "SUM(wl." + COL_LOG_SETS + ") AS total_sets, " +
                "SUM(wl." + COL_LOG_SETS + " * wl." + COL_LOG_REPS + ") AS total_reps, " +
                "SUM(wl." + COL_LOG_SETS + " * wl." + COL_LOG_REPS + " * wl." + COL_LOG_WEIGHT + ") AS total_volume, " +
                "GROUP_CONCAT(DISTINCT e." + COL_EX_TITLE + ") AS exercise_names, " +
                "MAX(wl." + COL_LOG_NOTES + ") AS notes " +
                "FROM " + TABLE_WORKOUT_LOGS + " wl " +
                "INNER JOIN " + TABLE_EXERCISES + " e ON wl." + COL_LOG_EXERCISE_ID + " = e." + COL_EX_ID + " " +
                "GROUP BY wl." + COL_LOG_DATE + ", COALESCE(wl." + COL_LOG_SPLIT_NAME + ", 'Workout Split') " +
                "ORDER BY wl." + COL_LOG_DATE + " DESC, MAX(wl." + COL_LOG_ID + ") DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                WorkoutSummary ws = new WorkoutSummary(
                        cursor.getString(cursor.getColumnIndexOrThrow("workout_date")),
                        cursor.getString(cursor.getColumnIndexOrThrow("split_name")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("exercises_count")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("total_sets")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("total_reps")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("total_volume")),
                        cursor.getString(cursor.getColumnIndexOrThrow("exercise_names")),
                        cursor.getString(cursor.getColumnIndexOrThrow("notes"))
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
                insertWorkoutLog(new WorkoutLog(1, 1, 1, "Barbell Bench Press", today, 4, 10, 80.0, "Smooth pause reps", "Push Day Split"));
                insertWorkoutLog(new WorkoutLog(2, 1, 33, "Overhead Press (OHP)", today, 3, 8, 55.0, "Strict form", "Push Day Split"));
                insertWorkoutLog(new WorkoutLog(3, 1, 21, "Barbell Back Squat", "2026-09-03", 5, 5, 110.0, "Heavy sets", "Leg Day Split"));
                insertWorkoutLog(new WorkoutLog(4, 1, 24, "Romanian Deadlift", "2026-09-03", 4, 8, 100.0, "Hamstring tension", "Leg Day Split"));
            }
        }
    }

    public int getWorkoutLogCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORKOUT_LOGS, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    /**
     * Database diagnostic status summary for validating DB connection and operational readiness.
     */
    public static class DatabaseStatus {
        public final boolean isConnected;
        public final boolean isWritable;
        public final int exerciseCount;
        public final int workoutLogCount;
        public final int weightEntryCount;
        public final boolean hasUserProfile;
        public final String databaseName;
        public final int version;
        public final long latencyMs;
        public final String message;

        public DatabaseStatus(boolean isConnected, boolean isWritable, int exerciseCount,
                              int workoutLogCount, int weightEntryCount, boolean hasUserProfile,
                              String databaseName, int version, long latencyMs, String message) {
            this.isConnected = isConnected;
            this.isWritable = isWritable;
            this.exerciseCount = exerciseCount;
            this.workoutLogCount = workoutLogCount;
            this.weightEntryCount = weightEntryCount;
            this.hasUserProfile = hasUserProfile;
            this.databaseName = databaseName;
            this.version = version;
            this.latencyMs = latencyMs;
            this.message = message;
        }
    }

    /**
     * Validates database connection, read/write state, and returns comprehensive table metrics.
     */
    public DatabaseStatus checkDatabaseStatus() {
        long startTime = System.currentTimeMillis();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            boolean connected = db != null && db.isOpen();
            boolean writable = db != null && !db.isReadOnly();

            int exercises = getExerciseCount();
            int workouts = getWorkoutLogCount();
            int weightEntries = getAllWeightHistory() != null ? getAllWeightHistory().size() : 0;
            boolean hasProfile = getUserProfile() != null;

            long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
            String msg = "SQLite connection verified in " + elapsed + "ms • " + exercises + " exercises loaded";
            return new DatabaseStatus(connected, writable, exercises, workouts, weightEntries, hasProfile, DATABASE_NAME, DATABASE_VERSION, elapsed, msg);
        } catch (Exception e) {
            long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
            return new DatabaseStatus(false, false, 0, 0, 0, false, DATABASE_NAME, DATABASE_VERSION, elapsed, "DB connection error: " + e.getMessage());
        }
    }

    // ==========================================
    // ACCOUNT AUTHENTICATION & PROFILE METHODS
    // ==========================================

    public enum AuthStatus {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_PASSWORD,
        EMPTY_CREDENTIALS,
        ERROR
    }

    public static class AuthResult {
        private final AuthStatus status;
        private final String message;
        private final UserAccount account;

        public AuthResult(AuthStatus status, String message, UserAccount account) {
            this.status = status;
            this.message = message;
            this.account = account;
        }

        public AuthStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public UserAccount getAccount() {
            return account;
        }

        public boolean isSuccess() {
            return status == AuthStatus.SUCCESS;
        }
    }

    /**
     * Checks if an account already exists with the given Gmail, email, or mobile number.
     */
    public boolean isAccountRegistered(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT " + COL_ACC_ID + " FROM " + TABLE_USER_ACCOUNTS +
                            " WHERE LOWER(" + COL_ACC_IDENTIFIER + ") = ? LIMIT 1",
                    new String[]{identifier.trim().toLowerCase(Locale.ROOT)}
            );
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "Error checking account registration: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Registers a new account and immediately initializes their UserProfile baselines.
     */
    public boolean registerAccount(String name, String identifier, String password, double height, double currentWeight, double targetWeight) {
        if (name == null || name.trim().isEmpty() || identifier == null || identifier.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }
        String cleanIdentifier = identifier.trim().toLowerCase(Locale.ROOT);
        String cleanName = name.trim();

        if (isAccountRegistered(cleanIdentifier)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            ContentValues cv = new ContentValues();
            cv.put(COL_ACC_NAME, cleanName);
            cv.put(COL_ACC_IDENTIFIER, cleanIdentifier);
            cv.put(COL_ACC_PASSWORD, password);
            cv.put(COL_ACC_CREATED_AT, now);
            long accId = db.insert(TABLE_USER_ACCOUNTS, null, cv);
            if (accId == -1) {
                return false;
            }

            // Also create or update primary UserProfile
            UserProfile profile = getPrimaryUserProfile();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            if (profile == null) {
                profile = new UserProfile(1, cleanName, height > 0 ? height : 175.0, currentWeight > 0 ? currentWeight : 70.0, targetWeight > 0 ? targetWeight : 68.0, today, today, null);
                insertUserProfile(profile);
            } else {
                profile.setName(cleanName);
                if (height > 0) profile.setHeightCm(height);
                if (currentWeight > 0) profile.setCurrentWeightKg(currentWeight);
                if (targetWeight > 0) profile.setTargetWeightKg(targetWeight);
                saveOrUpdateUserProfile(profile, false);
            }

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error registering account: " + e.getMessage(), e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Authenticates a user by matching their registered Gmail, email, or mobile number and password.
     */
    public AuthResult authenticateUser(String identifier, String password) {
        if (identifier == null || identifier.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return new AuthResult(AuthStatus.EMPTY_CREDENTIALS, "Please enter both your Gmail / mobile number and password.", null);
        }

        String cleanIdentifier = identifier.trim().toLowerCase(Locale.ROOT);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT " + COL_ACC_ID + ", " + COL_ACC_NAME + ", " + COL_ACC_IDENTIFIER + ", " + COL_ACC_PASSWORD + ", " + COL_ACC_CREATED_AT +
                            " FROM " + TABLE_USER_ACCOUNTS + " WHERE LOWER(" + COL_ACC_IDENTIFIER + ") = ? LIMIT 1",
                    new String[]{cleanIdentifier}
            );

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                String name = cursor.getString(1);
                String iden = cursor.getString(2);
                String dbPass = cursor.getString(3);
                String createdAt = cursor.getString(4);

                if (password.equals(dbPass)) {
                    UserAccount account = new UserAccount(id, name, iden, dbPass, createdAt);
                    return new AuthResult(AuthStatus.SUCCESS, "Sign in successful!", account);
                } else {
                    return new AuthResult(AuthStatus.WRONG_PASSWORD, "Incorrect password. Please try again.", null);
                }
            } else {
                return new AuthResult(AuthStatus.USER_NOT_FOUND, "No account found with this Gmail or mobile number. Please create an account first.", null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Authentication error: " + e.getMessage(), e);
            return new AuthResult(AuthStatus.ERROR, "Authentication error: " + e.getMessage(), null);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Deletes user account and permanently removes all personal fitness data.
     */
    public void deleteUserAccountAndData(String identifier) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            if (identifier != null && !identifier.trim().isEmpty()) {
                db.delete(TABLE_USER_ACCOUNTS, "LOWER(" + COL_ACC_IDENTIFIER + ") = ?", new String[]{identifier.trim().toLowerCase(Locale.ROOT)});
            }
            db.delete(TABLE_WORKOUT_LOGS, null, null);
            db.delete(TABLE_WEIGHT_HISTORY, null, null);
            db.delete(TABLE_PROGRESS_PHOTOS, null, null);
            db.delete(TABLE_USER_PROFILE, null, null);
            db.setTransactionSuccessful();
            Log.i(TAG, "User account and all personal data deleted successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error deleting user account and data: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }
}
