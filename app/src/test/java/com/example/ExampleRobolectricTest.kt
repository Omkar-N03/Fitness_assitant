package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Fitness Assistant", appName)
  }

  @Test
  fun `verify sqlite trigger updates user profile weight`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbHelper = GymDatabaseHelper.getInstance(context)

    // Ensure user profile exists
    val profile = UserProfile(1, "Test Athlete", 180.0, 75.0, 72.0, "2026-09-01", "2026-09-01")
    dbHelper.insertUserProfile(profile)

    // Insert new weight entry into weight_history
    val weightEntry = WeightHistory(80.5, "2026-09-06", "Test Weigh-In")
    dbHelper.insertWeightEntry(weightEntry)

    // Verify SQLite Trigger automatically updated profile current_weight
    val updatedProfile = dbHelper.userProfile
    org.junit.Assert.assertNotNull(updatedProfile)
    assertEquals(80.5, updatedProfile.currentWeightKg, 0.01)
  }

  @Test
  fun `verify sqlite workout summary view aggregates data`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbHelper = GymDatabaseHelper.getInstance(context)

    // Insert exercises and workout logs
    val today = "2026-09-06"
    dbHelper.insertExercise(Exercise(1, "Bench Press", "Chest", "Barbell", "Intermediate", "Press barbell", "https://youtube.com"))
    dbHelper.insertExercise(Exercise(2, "Squat", "Legs", "Barbell", "Advanced", "Squat down", "https://youtube.com"))
    dbHelper.insertWorkoutLog(WorkoutLog(0, 1, 1, "Bench Press", today, 4, 10, 100.0, "Heavy"))
    dbHelper.insertWorkoutLog(WorkoutLog(0, 1, 2, "Squat", today, 3, 10, 120.0, "Leg day"))

    val summaries = dbHelper.getRecentWorkoutSummaries(10)
    org.junit.Assert.assertTrue(summaries.isNotEmpty())
    val todaySummary = summaries.firstOrNull { it.workoutDate == today }
    org.junit.Assert.assertNotNull(todaySummary)
    org.junit.Assert.assertTrue(todaySummary!!.totalVolume > 0)
    assertEquals(7, todaySummary.totalSets)
    assertEquals(70, todaySummary.totalReps)
  }
}
