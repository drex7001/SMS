package com.personal.smsapp.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * Required to be registered for the RESPOND_VIA_MESSAGE intent to qualify
 * as a default SMS app (Android checks for this service before granting the role).
 *
 * This handles "quick reply" from system call screens — when someone calls you
 * and you tap "Reply with message" instead of answering, the system fires
 * RESPOND_VIA_MESSAGE here with the selected canned response.
 *
 * android:permission="SEND_RESPOND_VIA_MESSAGE" on the manifest entry ensures
 * only the system can invoke this service.
 */
public class RespondViaMessageService extends Service {

    private static final String TAG = "RespondViaMessageSvc";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        Uri recipientUri = intent.getData();
        String message   = intent.getStringExtra(Intent.EXTRA_TEXT);

        if (recipientUri != null && message != null && !message.isEmpty()) {
            String number = recipientUri.getSchemeSpecificPart();
            if (number != null && !number.isEmpty()) {
                sendSms(number, message);
            }
        } else {
            Log.w(TAG, "RESPOND_VIA_MESSAGE received with null recipient or message");
        }

        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void sendSms(String number, String message) {
        try {
            SmsManager smsManager = getSystemService(SmsManager.class);
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(number, null, parts, null, null);
            Log.d(TAG, "Quick reply sent to " + number);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send quick reply", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
