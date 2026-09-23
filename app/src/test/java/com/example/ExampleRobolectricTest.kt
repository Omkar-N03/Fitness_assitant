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
    var profile = dbHelper.primaryUserProfile
    if (profile == null) {
      profile = UserProfile(1, "Test Athlete", 180.0, 75.0, 72.0, "2026-09-01", "2026-09-01")
      dbHelper.insertUserProfile(profile)
    } else {
      profile.currentWeightKg = 75.0
      dbHelper.saveOrUpdateUserProfile(profile, false)
    }

    // Insert new weight entry into weight_history
    val weightEntry = WeightHistory(80.5, "2026-09-06", "Test Weigh-In")
    dbHelper.insertWeightEntry(weightEntry)

    // Verify SQLite Trigger automatically updated profile current_weight
    val updatedProfile = dbHelper.primaryUserProfile
    org.junit.Assert.assertNotNull(updatedProfile)
    assertEquals(80.5, updatedProfile!!.currentWeightKg, 0.01)
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

  @Test
  fun `verify auth manager session and logout`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AuthManager.logout(context)
    org.junit.Assert.assertFalse(AuthManager.isLoggedIn(context))

    AuthManager.setLoggedIn(context, true, "athlete@fitness.com", "Marcus Steel")
    org.junit.Assert.assertTrue(AuthManager.isLoggedIn(context))
    assertEquals("athlete@fitness.com", AuthManager.getUserEmail(context))
    assertEquals("Marcus Steel", AuthManager.getUserName(context))

    AuthManager.logout(context)
    org.junit.Assert.assertFalse(AuthManager.isLoggedIn(context))
  }

  @Test
  fun `verify profile image path persistence in sqlite`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbHelper = GymDatabaseHelper.getInstance(context)

    val profile = UserProfile(2, "Sarah Connor", 172.0, 65.0, 63.0, "2026-09-08", "2026-09-08", "/data/user/0/com.example/files/photo.jpg")
    dbHelper.insertUserProfile(profile)

    val retrieved = dbHelper.userProfile
    org.junit.Assert.assertNotNull(retrieved)
    assertEquals("/data/user/0/com.example/files/photo.jpg", profile.profileImagePath)
  }

  @Test
  fun `verify database diagnostic status check`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbHelper = GymDatabaseHelper.getInstance(context)
    val status = dbHelper.checkDatabaseStatus()

    org.junit.Assert.assertNotNull(status)
    org.junit.Assert.assertTrue("Database should be connected", status.isConnected)
    org.junit.Assert.assertTrue("Database should be writable", status.isWritable)
    assertEquals("gym_fitness.db", status.databaseName)
    org.junit.Assert.assertTrue(status.latencyMs >= 0)
    org.junit.Assert.assertTrue(status.message.contains("SQLite"))
  }

  @Test
  fun `verify local user account registration authentication and deletion`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbHelper = GymDatabaseHelper.getInstance(context)

    val testEmail = "athlete@gmail.com"
    val testPassword = "mypassword123"
    val testName = "Alex Rivera"

    // 1. Initial authentication attempt should fail because account is not registered yet
    val initialAuth = dbHelper.authenticateUser(testEmail, testPassword)
    org.junit.Assert.assertFalse(initialAuth.isSuccess)
    assertEquals(GymDatabaseHelper.AuthStatus.USER_NOT_FOUND, initialAuth.status)

    // 2. Register account locally
    val registered = dbHelper.registerAccount(testName, testEmail, testPassword, 180.0, 78.0, 75.0)
    org.junit.Assert.assertTrue("Account should register successfully", registered)
    org.junit.Assert.assertTrue(dbHelper.isAccountRegistered(testEmail))

    // Duplicate registration should be prevented
    val duplicate = dbHelper.registerAccount(testName, testEmail, "other123", 180.0, 78.0, 75.0)
    org.junit.Assert.assertFalse("Duplicate registration must be rejected", duplicate)

    // 3. Test wrong password
    val wrongPassAuth = dbHelper.authenticateUser(testEmail, "wrongpassword")
    org.junit.Assert.assertFalse(wrongPassAuth.isSuccess)
    assertEquals(GymDatabaseHelper.AuthStatus.WRONG_PASSWORD, wrongPassAuth.status)

    // 4. Test successful authentication
    val successAuth = dbHelper.authenticateUser(testEmail, testPassword)
    org.junit.Assert.assertTrue("Valid credentials should authenticate", successAuth.isSuccess)
    org.junit.Assert.assertNotNull(successAuth.account)
    assertEquals(testEmail, successAuth.account.identifier)
    assertEquals(testName, successAuth.account.name)

    // 5. Test session state
    AuthManager.setLoggedIn(context, true, successAuth.account.identifier, successAuth.account.name)
    org.junit.Assert.assertTrue(AuthManager.isLoggedIn(context))
    assertEquals(testEmail, AuthManager.getUserEmail(context))

    // 6. Test delete account permanently
    dbHelper.deleteUserAccountAndData(testEmail)
    AuthManager.clearAll(context)
    org.junit.Assert.assertFalse(AuthManager.isLoggedIn(context))
    org.junit.Assert.assertFalse(dbHelper.isAccountRegistered(testEmail))
  }

  @Test
  fun `verify workout split session add update across DB and delete`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbHelper = GymDatabaseHelper.getInstance(context)

    // Make sure exercises exist
    if (dbHelper.exerciseCount == 0) {
      CsvLoaderUtil.reloadExercises(context, dbHelper)
    }
    val allExercises = dbHelper.allExercises
    org.junit.Assert.assertTrue("Should have exercises in library", allExercises.size >= 2)

    val exercise1 = allExercises[0]
    val exercise2 = allExercises[1]

    val initialSplit = "Chest & Shoulders Split"
    val testDate = "2026-09-20"
    val initialExercises = mutableListOf(exercise1)

    // 1. Add workout split session
    val logId = dbHelper.logWorkoutSession(initialSplit, testDate, initialExercises, "Initial heavy bench sets")
    org.junit.Assert.assertTrue("Session should be logged with valid ID", logId > 0)

    // Verify session exists in DB
    val sessionExercises = dbHelper.getExercisesForSession(initialSplit, testDate)
    assertEquals(1, sessionExercises.size)
    assertEquals(exercise1.title, sessionExercises[0].title)

    val summaries = dbHelper.getRecentWorkoutSummaries()
    val matchSummary = summaries.find { it.splitName == initialSplit && it.workoutDate == testDate }
    org.junit.Assert.assertNotNull("Workout summary should contain logged split", matchSummary)
    assertEquals("Initial heavy bench sets", matchSummary!!.notes)

    // 2. Modify / update split: change split name, add exercise2, update notes and date
    val updatedSplit = "Push Hypertrophy Power"
    val updatedDate = "2026-09-21"
    val updatedExercises = mutableListOf(exercise1, exercise2)
    val updatedNotes = "Pushed to failure on all sets"

    val updatedOk = dbHelper.updateWorkoutSession(
      initialSplit,
      testDate,
      updatedSplit,
      updatedDate,
      updatedExercises,
      updatedNotes
    )
    org.junit.Assert.assertTrue("Session update should succeed", updatedOk)

    // Verify split name changed across whole DB
    val oldLogs = dbHelper.getExercisesForSession(initialSplit, testDate)
    org.junit.Assert.assertTrue("Old split should have no records", oldLogs.isEmpty())

    val updatedSessionExercises = dbHelper.getExercisesForSession(updatedSplit, updatedDate)
    assertEquals(2, updatedSessionExercises.size)

    val updatedSummaries = dbHelper.getRecentWorkoutSummaries()
    val updatedMatch = updatedSummaries.find { it.splitName == updatedSplit && it.workoutDate == updatedDate }
    org.junit.Assert.assertNotNull("Updated summary should reflect new split name across DB", updatedMatch)
    assertEquals(2, updatedMatch!!.exercisesCount)
    assertEquals(updatedNotes, updatedMatch.notes)

    // 3. Delete workout split session
    val deletedCount = dbHelper.deleteWorkoutSession(updatedSplit, updatedDate)
    org.junit.Assert.assertTrue("Session should be deleted", deletedCount > 0)

    val postDeleteExercises = dbHelper.getExercisesForSession(updatedSplit, updatedDate)
    org.junit.Assert.assertTrue("Session should no longer exist after deletion", postDeleteExercises.isEmpty())
  }

  @Test
  fun `verify csv loading and exact video match priority at top`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbHelper = GymDatabaseHelper.getInstance(context)

    // Load exercises from raw CSV
    val loaded = CsvLoaderUtil.reloadExercises(context, dbHelper)
    org.junit.Assert.assertTrue("CSV should load successfully", loaded)
    org.junit.Assert.assertEquals(100, dbHelper.exerciseCount)

    // Query all exercises
    val allExercises = dbHelper.allExercises
    org.junit.Assert.assertEquals(100, allExercises.size)

    // Verify first items have Exact video matches
    org.junit.Assert.assertTrue(
      "First exercise should be an Exact match",
      allExercises[0].isExactMatch
    )
    org.junit.Assert.assertEquals("Exact", allExercises[0].videoMatch)

    // Verify YouTube links are valid URLs
    org.junit.Assert.assertTrue(
      "YouTube link should start with http",
      allExercises[0].youtubeLink.startsWith("http")
    )
    org.junit.Assert.assertNotNull(
      "Video ID should be extractable",
      VideoHelper.extractVideoId(allExercises[0].youtubeLink)
    )

    // Verify filter by Exact videos returns only exact matches
    val exactMatches = dbHelper.searchExercises("", "Exact")
    org.junit.Assert.assertTrue(exactMatches.isNotEmpty())
    exactMatches.forEach {
      org.junit.Assert.assertEquals("Exact", it.videoMatch)
      org.junit.Assert.assertTrue(it.isExactMatch)
    }
  }
}
