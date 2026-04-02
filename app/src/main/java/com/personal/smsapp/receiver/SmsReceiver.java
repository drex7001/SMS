package com.personal.smsapp.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.personal.smsapp.data.local.SmsRepository;
import com.personal.smsapp.util.NotificationHelper;
import com.personal.smsapp.worker.ApiSyncWorker;

/**
 * Receives SMS_DELIVER broadcast (only fires when we are the default SMS app).
 *
 * As the default SMS app we are responsible for:
 *   1. Writing the message to the system SMS content provider.
 *   2. Resolving the correct thread_id via Telephony.Threads.
 *   3. Persisting the message in our own DB.
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) return;

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;

        // Reassemble multi-part messages
        StringBuilder body   = new StringBuilder();
        String        sender = null;
        long          date   = System.currentTimeMillis();

        for (SmsMessage sms : messages) {
            if (sender == null) {
                sender = sms.getOriginatingAddress();
                date   = sms.getTimestampMillis();
            }
            body.append(sms.getMessageBody());
        }

        if (sender == null) return;

        final String finalSender = sender;
        final String finalBody   = body.toString();
        final long   finalDate   = date;

        Log.d(TAG, "Received SMS from " + finalSender);

        // ── 1. Resolve the canonical thread ID for this sender ─────────────
        long threadId;
        try {
            threadId = Telephony.Threads.getOrCreateThreadId(context, finalSender);
        } catch (Exception e) {
            Log.e(TAG, "Could not resolve threadId for " + finalSender, e);
            threadId = 0;
        }
        final long finalThreadId = threadId;

        // ── 2. Write to system SMS inbox (mandatory as default SMS app) ─────
        long sysId = -1;
        try {
            ContentValues cv = new ContentValues();
            cv.put(Telephony.Sms.ADDRESS,   finalSender);
            cv.put(Telephony.Sms.BODY,      finalBody);
            cv.put(Telephony.Sms.DATE,      finalDate);
            cv.put(Telephony.Sms.DATE_SENT, finalDate);
            cv.put(Telephony.Sms.TYPE,      Telephony.Sms.MESSAGE_TYPE_INBOX);
            cv.put(Telephony.Sms.THREAD_ID, finalThreadId);
            cv.put(Telephony.Sms.READ,      0);
            Uri inserted = context.getContentResolver()
                    .insert(Telephony.Sms.Inbox.CONTENT_URI, cv);
            if (inserted != null) {
                String lastSegment = inserted.getLastPathSegment();
                if (lastSegment != null && !lastSegment.isEmpty()) {
                    try {
                        sysId = Long.parseLong(lastSegment);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Unexpected URI path segment from system inbox: " + lastSegment);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to write SMS to system inbox", e);
        }
        final long finalSysId = sysId;

        // ── 3. Persist in our DB and notify ────────────────────────────────
        SmsRepository repo = SmsRepository.getInstance(context);
        repo.insertIncoming(finalSender, finalBody, finalDate, finalSysId, finalThreadId, () -> {
            NotificationHelper.showIncoming(context, finalSender, finalBody);
            ApiSyncWorker.enqueue(context);
        });
    }
}
