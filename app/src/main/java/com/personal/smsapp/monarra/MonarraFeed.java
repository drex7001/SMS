package com.personal.smsapp.monarra;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

import com.personal.smsapp.BuildConfig;
import com.personal.smsapp.data.local.AppDatabase;
import com.personal.smsapp.data.local.FeedDao;
import com.personal.smsapp.data.local.FeedEntry;

/**
 * Hands financial SMS to Monarra: filter → feed → nudge. Call it off the main thread.
 */
public final class MonarraFeed {

    private static final String TAG = "MonarraFeed";

    static final long DAY_MS             = 24L * 60 * 60 * 1000;
    static final long WIPE_BODY_AFTER_MS = 7 * DAY_MS;
    static final long DELETE_AFTER_MS    = 30 * DAY_MS;

    /**
     * Adds a live SMS to the feed if the rules say it's financial, and nudges Monarra. Never
     * throws: the SMS itself is already stored, and the bridge must not break receiving.
     */
    public static void captureLive(Context context, String sender, String body, long sentAtMs,
                                   int simSlot) {
        try {
            long receivedAt = System.currentTimeMillis();
            long seq = capture(context, BridgeStore.get(context).rules(), sender, body, sentAtMs,
                    receivedAt, simSlot, BridgeContract.ORIGIN_LIVE);
            if (seq > 0) {
                BridgeStore.get(context).setLastLiveAt(receivedAt);
                nudge(context, seq);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Couldn't hand an SMS to Monarra", e);
        }
    }

    /**
     * Adds one SMS to the feed if {@code rules} say it's financial. Returns the new seq, or -1 if it
     * isn't financial or is already in the feed.
     */
    static long capture(Context context, MonarraRules rules, String sender, String body,
                        long sentAtMs, long receivedAtMs, int simSlot, String origin) {
        if (!rules.isFinancial(sender, body)) return -1;
        FeedEntry entry  = new FeedEntry();
        entry.messageHash = MessageHash.of(sender, sentAtMs, body);
        entry.sender      = sender;
        entry.body        = body;
        entry.sentAt      = sentAtMs;
        entry.receivedAt  = receivedAtMs;
        entry.simSlot     = simSlot;
        entry.origin      = origin;
        entry.createdAt   = System.currentTimeMillis();
        return AppDatabase.getInstance(context).feedDao().insert(entry);
    }

    /**
     * Tells Monarra there's something new. Only a hint: Monarra also syncs when it opens and once a
     * day, and a force-stopped Monarra doesn't get broadcasts at all.
     */
    static void nudge(Context context, long headSeq) {
        Intent intent = new Intent(BridgeContract.ACTION_FEED_UPDATED)
                .setPackage(BuildConfig.MONARRA_PACKAGE)
                .putExtra(BridgeContract.Extras.FEED_HEAD_SEQ, headSeq);
        context.sendBroadcast(intent, BuildConfig.BRIDGE_PERMISSION);
    }

    /** Marks rows up to {@code upToSeq} as handed over and applies the retention policy. */
    static void ack(Context context, long upToSeq) {
        long now = System.currentTimeMillis();
        FeedDao dao = AppDatabase.getInstance(context).feedDao();
        dao.ack(upToSeq, now);
        dao.wipeBodiesAckedBefore(now - WIPE_BODY_AFTER_MS);
        dao.deleteAckedBefore(now - DELETE_AFTER_MS);
        BridgeStore.get(context).setLastAckAt(now);
    }

    public static boolean isDefaultSmsApp(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = context.getSystemService(RoleManager.class);
            return roles != null && roles.isRoleHeld(RoleManager.ROLE_SMS);
        }
        return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
    }

    public static boolean isMonarraInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(BuildConfig.MONARRA_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private MonarraFeed() {}
}
