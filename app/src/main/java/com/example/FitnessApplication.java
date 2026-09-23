package com.example;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.util.Log;

/**
 * Main Application class for Fitness Assistant.
 * Implements modern memory management and trim callbacks (ComponentCallbacks2)
 * as recommended since Android Q instead of deprecated ashmem pinning.
 */
public class FitnessApplication extends Application {

    private static final String TAG = "FitnessApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Fitness Assistant Application initialized with modern memory management.");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            Log.w(TAG, "High memory pressure detected (level: " + level + "), trimming caches.");
        }

        // Actively trim in-memory image and bitmap caches in response to system memory pressure
        ImageRotationHelper.trimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            // Under moderate or critical memory pressure, request garbage collection of unreferenced buffers
            System.gc();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "onLowMemory received - releasing all non-essential cached memory.");
        ImageRotationHelper.clearMemory();
        System.gc();
    }
}
