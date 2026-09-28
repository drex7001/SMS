package com.personal.smsapp.monarra;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Puts SMS history into the feed, so Monarra has something to work with from day one: scans the
 * phone's SMS inbox for the last N days through the current rules, in chunks of 500, nudging
 * Monarra after each chunk. Messages already in the feed are skipped (same message_hash).
 */
public class BackfillWorker extends Worker {

    private static final String TAG       = "BackfillWorker";
    private static final String WORK_NAME = "monarra_backfill";
    private static final String KEY_DAYS  = "days";
    static final int CHUNK = 500;

    public BackfillWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    static void enqueue(Context context, int days) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackfillWorker.class)
                .setInputData(new Data.Builder().putInt(KEY_DAYS, days).build())
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }

    /** Whether a backfill is queued or running. WorkManager, not our own flag, is the truth here. */
    static boolean isActive(Context context) {
        try {
            List<WorkInfo> infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME).get();
            for (WorkInfo info : infos) {
                if (!info.getState().isFinished()) return true;
            }
            return false;
        } catch (ExecutionException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        int days = getInputData().getInt(KEY_DAYS, BridgeProvider.DEFAULT_BACKFILL_DAYS);
        long since = System.currentTimeMillis() - days * MonarraFeed.DAY_MS;
        Scan scan = new Scan(context, BridgeStore.get(context).rules());
        String[] projection = {
            Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.DATE_SENT
        };
        try (Cursor c = context.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI, projection,
                Telephony.Sms.DATE + " >= ?", new String[]{String.valueOf(since)}, Telephony.Sms._ID + " ASC")) {
            if (c == null) throw new IllegalStateException("The SMS inbox can't be read");
            while (c.moveToNext() && !isStopped()) {
                long date = c.getLong(2);
                long dateSent = c.getLong(3);
                scan.add(c.getString(0), c.getString(1), dateSent > 0 ? dateSent : date, date);
            }
        } catch (RuntimeException e) { // including SecurityException without READ_SMS
            Log.e(TAG, "Backfill failed", e);
            scan.finish(BridgeContract.BACKFILL_FAILED);
            return Result.failure();
        }
        if (isStopped()) return Result.retry(); // WorkManager runs it again; duplicates are skipped
        scan.finish(BridgeContract.BACKFILL_DONE);
        return Result.success();
    }

    /** One backfill pass, fed message by message. Separate from the cursor so it can be tested. */
    static final class Scan {
        private final Context      context;
        private final MonarraRules rules;
        private final BridgeStore  store;
        private long scanned;
        private long matched;
        private long newHeadSeq;

        Scan(Context context, MonarraRules rules) {
            this.context = context;
            this.rules   = rules;
            this.store   = BridgeStore.get(context);
            store.setBackfill(BridgeContract.BACKFILL_RUNNING, 0, 0);
        }

        void add(String sender, String body, long sentAtMs, long receivedAtMs) {
            scanned++;
            if (rules.isFinancial(sender, body)) {
                matched++;
                long seq = MonarraFeed.capture(context, rules, sender, body, sentAtMs, receivedAtMs, -1,
                        BridgeContract.ORIGIN_BACKFILL);
                if (seq > 0) newHeadSeq = seq;
            }
            if (scanned % CHUNK == 0) endChunk(BridgeContract.BACKFILL_RUNNING);
        }

        void finish(String state) {
            endChunk(state);
        }

        private void endChunk(String state) {
            store.setBackfill(state, scanned, matched);
            if (newHeadSeq > 0) {
                MonarraFeed.nudge(context, newHeadSeq);
                newHeadSeq = 0;
            }
        }
    }
}
