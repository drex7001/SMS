package com.personal.smsapp.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.personal.smsapp.data.local.Message;
import com.personal.smsapp.data.local.SmsRepository;
import com.personal.smsapp.data.remote.ApiService;
import com.personal.smsapp.util.Prefs;

import java.util.List;

/**
 * Battery-efficient background processor.
 *
 * WorkManager guarantees execution even after app death/reboot.
 * We use NETWORK_CONNECTED constraint so we never run without connectivity.
 * Multiple incoming messages get batched into a single work execution.
 *
 * Power notes:
 *   - One-time work, not periodic — only runs when there's something to process.
 *   - KEEP policy prevents redundant queuing when messages arrive in bursts.
 *   - No wakelocks held manually; WorkManager handles that.
 */
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

        // Load settings
        String apiUrl = Prefs.getApiUrl(ctx);
        String apiKey = Prefs.getApiKey(ctx);

        if (apiUrl == null || apiUrl.isEmpty()) {
            Log.d(TAG, "API URL not configured, skipping sync");
            return Result.success();
        }

        SmsRepository repo = SmsRepository.getInstance(ctx);
        ApiService    api  = new ApiService(apiUrl, apiKey);

        List<Message> pending = repo.getUnprocessedMessages();
        Log.d(TAG, "Processing " + pending.size() + " unprocessed messages");

        for (Message msg : pending) {
            try {
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
                    + " tag=" + response.tag
                    + " important=" + response.important);
            } catch (Exception e) {
                Log.e(TAG, "Failed processing message " + msg.id, e);
                // Continue with next message rather than failing the whole job
            }
        }

        return Result.success();
    }

    /**
     * Enqueue with KEEP policy — if a sync is already queued, don't add another.
     * This batches rapid incoming messages efficiently.
     */
    public static void enqueue(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ApiSyncWorker.class)
            .setConstraints(constraints)
            .build();

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }
}
