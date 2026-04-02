package com.personal.smsapp.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * SharedPreferences wrapper for app settings.
 *
 * Uses Context.getSharedPreferences directly — no androidx.preference dependency needed.
 * The androidx.preference library is only required if you use PreferenceFragmentCompat
 * (XML-driven settings screens). Since we have a manual SettingsActivity with regular
 * views, the platform SharedPreferences API is all we need.
 */
public class Prefs {

    // New name avoids colliding with any pre-existing unencrypted "smsapp_prefs" file.
    // On first launch after upgrade, settings reset to defaults — this is intentional.
    private static final String PREFS_NAME = "smsapp_prefs_enc";

    public static final String KEY_API_URL      = "api_url";
    public static final String KEY_API_KEY      = "api_key";
    public static final String KEY_API_ENABLED  = "api_enabled";
    public static final String KEY_NOTIFICATION = "notifications_enabled";
    public static final String KEY_FIRST_LAUNCH = "first_launch";
    public static final String KEY_FONT_SIZE    = "font_size";

    private static volatile SharedPreferences instance;

    private static SharedPreferences get(Context ctx) {
        if (instance == null) {
            synchronized (Prefs.class) {
                if (instance == null) {
                    try {
                        MasterKey masterKey = new MasterKey.Builder(ctx.getApplicationContext())
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                .build();
                        instance = EncryptedSharedPreferences.create(
                                ctx.getApplicationContext(),
                                PREFS_NAME,
                                masterKey,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        );
                    } catch (GeneralSecurityException | IOException e) {
                        throw new RuntimeException(
                                "Failed to initialize encrypted preferences", e);
                    }
                }
            }
        }
        return instance;
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

    // KEY_FIRST_LAUNCH doubles as the "initial sync done" flag in SMSApplication,
    // but ConversationListActivity tracks sync completion separately so it can
    // trigger the sync after permissions + default-app role are both confirmed.
    private static final String KEY_INITIAL_SYNC_DONE = "initial_sync_done";

    public static boolean isInitialSyncDone(Context ctx) {
        return get(ctx).getBoolean(KEY_INITIAL_SYNC_DONE, false);
    }

    public static void setInitialSyncDone(Context ctx) {
        get(ctx).edit().putBoolean(KEY_INITIAL_SYNC_DONE, true).apply();
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
