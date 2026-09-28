package com.personal.smsapp.monarra;

import android.content.Context;
import android.text.format.DateUtils;

import com.personal.smsapp.data.local.AppDatabase;

/** What Settings → Monarra shows. Read it off the main thread. */
public final class BridgeStatus {

    public final boolean monarraInstalled;
    public final boolean defaultSmsApp;
    public final long    rulesVersion;
    public final int     senderCount;
    public final long    lastHandoffAt;
    public final long    pendingCount;
    public final String  backfillState;
    public final long    backfillMatched;

    BridgeStatus(boolean monarraInstalled, boolean defaultSmsApp, long rulesVersion, int senderCount,
                 long lastHandoffAt, long pendingCount, String backfillState, long backfillMatched) {
        this.monarraInstalled = monarraInstalled;
        this.defaultSmsApp    = defaultSmsApp;
        this.rulesVersion     = rulesVersion;
        this.senderCount      = senderCount;
        this.lastHandoffAt    = lastHandoffAt;
        this.pendingCount     = pendingCount;
        this.backfillState    = backfillState;
        this.backfillMatched  = backfillMatched;
    }

    public static BridgeStatus read(Context context) {
        BridgeStore store = BridgeStore.get(context);
        MonarraRules rules = store.rules();
        return new BridgeStatus(
                MonarraFeed.isMonarraInstalled(context),
                MonarraFeed.isDefaultSmsApp(context),
                rules.version,
                rules.senders.size(),
                store.lastAckAt(),
                AppDatabase.getInstance(context).feedDao().pendingCount(),
                store.backfillState(),
                store.backfillMatched());
    }

    /** One line per fact, most important first. */
    public String describe(long now) {
        StringBuilder out = new StringBuilder();
        if (!monarraInstalled) {
            out.append("Monarra isn't installed. Nothing is shared.");
            return out.toString();
        }
        if (rulesVersion == 0) {
            out.append("Connected, but no bank senders yet: add them in Monarra. Nothing is shared until then.");
        } else {
            out.append("Sharing SMS from ").append(senderCount)
               .append(senderCount == 1 ? " bank sender" : " bank senders").append(" with Monarra.");
        }
        out.append("\nLast handoff: ").append(lastHandoffAt > 0
                ? DateUtils.getRelativeTimeSpanString(lastHandoffAt, now, DateUtils.MINUTE_IN_MILLIS)
                : "never");
        if (pendingCount > 0) {
            out.append("\nWaiting for Monarra: ").append(pendingCount)
               .append(pendingCount == 1 ? " message" : " messages");
        }
        if (BridgeContract.BACKFILL_RUNNING.equals(backfillState)) {
            out.append("\nImporting history… ").append(backfillMatched).append(" found so far");
        }
        if (!defaultSmsApp) {
            out.append("\nThis isn't the default SMS app, so new SMS don't arrive here.");
        }
        return out.toString();
    }
}
