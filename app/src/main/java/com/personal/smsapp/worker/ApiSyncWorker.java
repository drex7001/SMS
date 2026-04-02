package com.personal.smsapp.worker;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.personal.smsapp.data.local.LocalFilter;
import com.personal.smsapp.data.local.Message;
import com.personal.smsapp.data.local.SmsRepository;
import com.personal.smsapp.data.remote.ApiService;
import com.personal.smsapp.util.LocalFilterHelper;
import com.personal.smsapp.util.Prefs;

import org.json.JSONArray;

import java.util.List;

public class ApiSyncWorker extends Worker {

    private static final String TAG       = "ApiSyncWorker";
    private static final String WORK_NAME = "api_sync";

    public ApiSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        String apiUrl = Prefs.getApiUrl(ctx);
        String apiKey = Prefs.getApiKey(ctx);

        if (apiUrl == null || apiUrl.isEmpty()) {
            Log.d(TAG, "API URL not configured, skipping sync");
            return Result.success();
        }

        // ── WiFi SSID gate ─────────────────────────────────────────────────
        if (Prefs.isWifiOnly(ctx) && !isOnAllowedSsid(ctx)) {
            Log.d(TAG, "Not on an allowed WiFi network — retrying later");
            return Result.retry();
        }

        SmsRepository repo    = SmsRepository.getInstance(ctx);
        ApiService    api     = new ApiService(apiUrl, apiKey);
        List<LocalFilter> filters = repo.getEnabledFiltersSync();

        List<Message> pending = repo.getUnprocessedMessages();
        Log.d(TAG, "Processing " + pending.size() + " unprocessed messages");

        for (Message msg : pending) {
            try {
                // ── Local filter classification ────────────────────────────
                LocalFilterHelper.FilterResult local =
                    LocalFilterHelper.classify(msg.body, filters);

                if (local.matched && !local.sendToServer) {
                    // Handle locally — don't call the API
                    repo.applyApiResult(msg.id, local.tag, false, false);
                    Log.d(TAG, "Msg " + msg.id + " handled locally, tag=" + local.tag);
                    continue;
                }

                // ── Forward to API ─────────────────────────────────────────
                ApiService.ApiResponse response = api.forwardMessage(
                    msg.id, msg.address, msg.body, msg.date
                );
                repo.applyApiResult(
                    msg.id,
                    response.tag,
                    response.important,
                    response.shouldDelete()
                );
                Log.d(TAG, "Processed msg " + msg.id
                    + " -> action=" + response.action
                    + " tag=" + response.tag);
            } catch (Exception e) {
                Log.e(TAG, "Failed processing message " + msg.id, e);
            }
        }

        return Result.success();
    }

    /**
     * Returns true if the device is currently connected to one of the user's
     * whitelisted SSIDs. Returns true (permissive) if the SSID list is empty
     * or if the Wi-Fi state cannot be determined (e.g. location permission denied).
     */
    private boolean isOnAllowedSsid(Context ctx) {
        try {
            String ssidsJson = Prefs.getHomeSsidsJson(ctx);
            JSONArray arr = new JSONArray(ssidsJson);
            if (arr.length() == 0) return true; // no restrictions set

            WifiManager wifiManager =
                (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) return true;

            WifiInfo info = wifiManager.getConnectionInfo();
            String current = info.getSSID(); // "<ssid>" with surrounding quotes

            for (int i = 0; i < arr.length(); i++) {
                String allowed = "\"" + arr.getString(i) + "\"";
                if (allowed.equals(current)) return true;
            }
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Could not check SSID: " + e.getMessage());
            return true; // fail-open: don't block forwarding on errors
        }
    }

    /**
     * Enqueue with KEEP policy — if a sync is already queued, don't add another.
     * Uses UNMETERED constraint when wifi_only is enabled, otherwise CONNECTED.
     */
    public static void enqueue(Context context) {
        boolean wifiOnly = Prefs.isWifiOnly(context);
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(wifiOnly ? NetworkType.UNMETERED : NetworkType.CONNECTED)
            .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ApiSyncWorker.class)
            .setConstraints(constraints)
            .build();

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }
}
