package com.example;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility responsible for reading and parsing 'gym_exercises_100_direct_youtube_videos_3.csv'
 * located strictly in res/raw, properly handling CSV quotes, commas, and multi-word fields.
 */
public class CsvLoaderUtil {

    private static final String TAG = "CsvLoaderUtil";

    public interface OnLoadedListener {
        void onLoaded(int count);
    }

    public static void loadExercisesIfEmpty(Context context, GymDatabaseHelper dbHelper, OnLoadedListener listener) {
        new Thread(() -> {
            boolean loaded = loadExercisesIfEmpty(context, dbHelper);
            int count = dbHelper.getExerciseCount();
            if (listener != null) {
                listener.onLoaded(count);
            }
        }).start();
    }

    public static boolean loadExercisesIfEmpty(Context context, GymDatabaseHelper dbHelper) {
        if (dbHelper.getExerciseCount() > 0) {
            Log.d(TAG, "Exercises already loaded in database (" + dbHelper.getExerciseCount() + " records).");
            return true;
        }

        List<Exercise> exercises = parseRawCsv(context);
        if (!exercises.isEmpty()) {
            dbHelper.bulkInsertExercises(exercises);
            Log.d(TAG, "Successfully loaded and saved " + exercises.size() + " exercises to SQLite database.");
            return true;
        } else {
            Log.e(TAG, "Failed to load any exercises from raw CSV.");
            return false;
        }
    }

    public static List<Exercise> parseRawCsv(Context context) {
        List<Exercise> exercises = new ArrayList<>();
        try {
            int resId = context.getResources().getIdentifier(
                    "gym_exercises_100_direct_youtube_videos_3",
                    "raw",
                    context.getPackageName()
            );

            if (resId == 0) {
                Log.e(TAG, "Could not find resource: gym_exercises_100_direct_youtube_videos_3 in res/raw");
                return exercises;
            }

            InputStream is = context.getResources().openRawResource(resId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 7) {
                    try {
                        long id = Long.parseLong(tokens.get(0).trim());
                        String title = tokens.get(1).trim();
                        String bodyPart = tokens.get(2).trim();
                        String equipment = tokens.get(3).trim();
                        String difficulty = tokens.get(4).trim();
                        String instructions = tokens.get(5).trim();
                        String youtubeUrl = tokens.get(6).trim();

                        exercises.add(new Exercise(id, title, bodyPart, equipment, difficulty, instructions, youtubeUrl));
                    } catch (Exception parseEx) {
                        Log.w(TAG, "Error parsing row: " + line, parseEx);
                    }
                }
            }
            reader.close();
            is.close();
        } catch (Exception e) {
            Log.e(TAG, "Error opening or reading CSV file", e);
        }
        return exercises;
    }

    /**
     * Standard CSV row tokenizer handling quoted cells containing commas.
     */
    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder curVal = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                        curVal.append('\"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    curVal.append(ch);
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    result.add(curVal.toString());
                    curVal.setLength(0);
                } else {
                    curVal.append(ch);
                }
            }
        }
        result.add(curVal.toString());
        return result;
    }
}
