package com.personal.smsapp.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences wrapper for app settings.
 *
 * Uses Context.getSharedPreferences directly — no androidx.preference dependency needed.
 * The androidx.preference library is only required if you use PreferenceFragmentCompat
 * (XML-driven settings screens). Since we have a manual SettingsActivity with regular
 * views, the platform SharedPreferences API is all we need.
 */
public class Prefs {

    private static final String PREFS_NAME = "smsapp_prefs";

    public static final String KEY_API_URL      = "api_url";
    public static final String KEY_API_KEY      = "api_key";
    public static final String KEY_API_ENABLED  = "api_enabled";
    public static final String KEY_NOTIFICATION = "notifications_enabled";
    public static final String KEY_FIRST_LAUNCH = "first_launch";
    public static final String KEY_FONT_SIZE    = "font_size";

    private static SharedPreferences get(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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

    public static void setApiEnabled(Context ctx, boolean enabled) {
        get(ctx).edit().putBoolean(KEY_API_ENABLED, enabled).apply();
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

    public static void setNotificationsEnabled(Context ctx, boolean enabled) {
        get(ctx).edit().putBoolean(KEY_NOTIFICATION, enabled).apply();
    }

    public static int getFontSize(Context ctx) {
        return get(ctx).getInt(KEY_FONT_SIZE, 16);
    }

    /** Save all settings at once — called by SettingsActivity on Save button */
    public static void saveAll(Context ctx, String apiUrl, String apiKey,
                               boolean apiEnabled, boolean notificationsEnabled) {
        get(ctx).edit()
            .putString(KEY_API_URL,      apiUrl)
            .putString(KEY_API_KEY,      apiKey)
            .putBoolean(KEY_API_ENABLED,  apiEnabled)
            .putBoolean(KEY_NOTIFICATION, notificationsEnabled)
            .apply();
    }
}
