package com.example;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Manages user authentication state, credentials, and session persistence.
 */
public final class AuthManager {

    private static final String PREF_NAME = "gym_auth_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_IDENTIFIER = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_LAST_REGISTERED_IDENTIFIER = "last_registered_identifier";

    private AuthManager() {}

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isLoggedIn(@NonNull Context context) {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static void setLoggedIn(@NonNull Context context, boolean loggedIn, @Nullable String identifier, @Nullable String name) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, loggedIn);
        if (identifier != null) {
            editor.putString(KEY_USER_IDENTIFIER, identifier);
        }
        if (name != null) {
            editor.putString(KEY_USER_NAME, name);
        }
        editor.apply();
    }

    @NonNull
    public static String getUserEmail(@NonNull Context context) {
        return getPrefs(context).getString(KEY_USER_IDENTIFIER, "");
    }

    @NonNull
    public static String getUserName(@NonNull Context context) {
        return getPrefs(context).getString(KEY_USER_NAME, "Athlete");
    }

    public static void setLastRegisteredIdentifier(@NonNull Context context, @NonNull String identifier) {
        getPrefs(context).edit().putString(KEY_LAST_REGISTERED_IDENTIFIER, identifier).apply();
    }

    @NonNull
    public static String getLastRegisteredIdentifier(@NonNull Context context) {
        return getPrefs(context).getString(KEY_LAST_REGISTERED_IDENTIFIER, "");
    }

    public static void logout(@NonNull Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    public static void clearAll(@NonNull Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.clear();
        editor.apply();
    }
}
