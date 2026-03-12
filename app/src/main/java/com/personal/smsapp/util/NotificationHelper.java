package com.personal.smsapp.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.personal.smsapp.R;
import com.personal.smsapp.ui.MessageThreadActivity;

public class NotificationHelper {

    private static final String CHANNEL_MESSAGES = "messages";
    private static final int    BASE_ID           = 1000;

    public static void createChannels(Context ctx) {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_MESSAGES,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        );
        ch.setDescription("Incoming SMS notifications");
        ch.enableVibration(true);
        ch.setShowBadge(true);

        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    public static void showIncoming(Context ctx, String sender, String body) {
        if (!Prefs.areNotificationsEnabled(ctx)) return;

        Intent tapIntent = new Intent(ctx, MessageThreadActivity.class);
        tapIntent.putExtra(MessageThreadActivity.EXTRA_ADDRESS, sender);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(ctx,
            sender.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(ctx, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_sms)
            .setContentTitle(sender)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build();

        try {
            NotificationManagerCompat.from(ctx)
                .notify(BASE_ID + Math.abs(sender.hashCode() % 1000), n);
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS not granted
        }
    }

    public static void cancelForSender(Context ctx, String sender) {
        NotificationManagerCompat.from(ctx)
            .cancel(BASE_ID + Math.abs(sender.hashCode() % 1000));
    }
}
