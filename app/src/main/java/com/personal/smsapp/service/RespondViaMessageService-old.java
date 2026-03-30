package com.personal.smsapp.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;

/**
 * REQUIRED by Android for default SMS app eligibility.
 * Android will not grant the default SMS role to any app that does not
 * expose a Service handling android.intent.action.RESPOND_VIA_MESSAGE.
 *
 * Also handles quick-reply from lock-screen / call-screen notifications.
 */
@SuppressWarnings("deprecation") // IntentService is sufficient; no UI involved
public class RespondViaMessageService extends IntentService {

    public RespondViaMessageService() {
        super("RespondViaMessageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        Uri    uri  = intent.getData();
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (uri == null || text == null || text.isEmpty()) return;

        // Extract number from sms:+123456789 or smsto:+123456789
        String number = uri.getSchemeSpecificPart();
        if (number == null || number.isEmpty()) return;

        try {
            SmsManager.getDefault().sendTextMessage(number, null, text, null, null);
        } catch (Exception ignored) {
            // Best-effort quick reply; failures are silent
        }
    }
}
