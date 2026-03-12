package com.personal.smsapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class Prefs {

    public static final String KEY_API_URL           = "api_url";
    public static final String KEY_API_KEY           = "api_key";
    public static final String KEY_API_ENABLED       = "api_enabled";
    public static final String KEY_NOTIFICATION      = "notifications_enabled";
    public static final String KEY_FIRST_LAUNCH      = "first_launch";
    public static final String KEY_FONT_SIZE         = "font_size";
    /** Set to true once we have done the one-time system-SMS import */
    public static final String KEY_INITIAL_SYNC_DONE = "initial_sync_done";

    private static SharedPreferences get(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static String getApiUrl(Context ctx) {
        return get(ctx).getString(KEY_API_URL, "");
    }

    public static void setApiUrl(Context ctx, String url) {
        get(ctx).edit().putString(KEY_API_URL, url).apply();
    }

    public static String getApiKey(Context ctx) {
        return get(ctx).getString(KEY_API_KEY, "");
    }

    public static void setApiKey(Context ctx, String key) {
        get(ctx).edit().putString(KEY_API_KEY, key).apply();
    }

    public static boolean isApiEnabled(Context ctx) {
        return get(ctx).getBoolean(KEY_API_ENABLED, false);
    }

    public static boolean isFirstLaunch(Context ctx) {
        return get(ctx).getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public static void setFirstLaunchDone(Context ctx) {
        get(ctx).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }

    public static boolean areNotificationsEnabled(Context ctx) {
        return get(ctx).getBoolean(KEY_NOTIFICATION, true);
    }

    public static int getFontSize(Context ctx) {
        return get(ctx).getInt(KEY_FONT_SIZE, 16); // sp
    }

    public static boolean isInitialSyncDone(Context ctx) {
        return get(ctx).getBoolean(KEY_INITIAL_SYNC_DONE, false);
    }

    public static void setInitialSyncDone(Context ctx) {
        get(ctx).edit().putBoolean(KEY_INITIAL_SYNC_DONE, true).apply();
    }
}
