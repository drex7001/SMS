package com.personal.smsapp.monarra;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.personal.smsapp.BuildConfig;
import com.personal.smsapp.data.local.AppDatabase;
import com.personal.smsapp.data.local.SenderCount;

import java.util.ArrayList;
import java.util.List;

/**
 * Monarra's door into this app (docs/SMS_BRIDGE.md in the Monarra repo). Exported, but every entry
 * point requires the bridge's signature permission, so only apps signed with our key get in.
 *
 * Data is read-only: query() returns the feed; all writes go through call().
 */
public class BridgeProvider extends ContentProvider {

    static final int  DEFAULT_BACKFILL_DAYS = 180;
    static final int  MAX_BACKFILL_DAYS     = 3650;
    static final int  MAX_LISTED_SENDERS    = 50;
    private static final String FEED_TYPE   = "vnd.android.cursor.dir/vnd.com.personal.smsapp.feed";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        enforcePermission();
        if (!BridgeContract.PATH_FEED.equals(uri.getLastPathSegment())) {
            throw new IllegalArgumentException("Unknown path: " + uri.getPath());
        }
        long after = parseLong(uri.getQueryParameter(BridgeContract.QUERY_AFTER), 0);
        int limit = (int) parseLong(uri.getQueryParameter(BridgeContract.QUERY_LIMIT), BridgeContract.DEFAULT_LIMIT);
        if (after < 0) throw new IllegalArgumentException("after must not be negative");
        if (limit < 1 || limit > BridgeContract.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be 1–" + BridgeContract.MAX_LIMIT);
        }
        return AppDatabase.getInstance(context()).feedDao().rowsAfter(after, limit);
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        // The framework only checks that the caller may reach this provider at all; it doesn't
        // apply android:permission to call(). Enforce it here.
        enforcePermission();
        Bundle in = extras != null ? extras : Bundle.EMPTY;
        switch (method) {
            case BridgeContract.Methods.HELLO:        return hello();
            case BridgeContract.Methods.SET_RULES:    return setRules(in);
            case BridgeContract.Methods.ACK:          return ack(in);
            case BridgeContract.Methods.BACKFILL:     return backfill(in);
            case BridgeContract.Methods.STATUS:       return status();
            case BridgeContract.Methods.LIST_SENDERS: return listSenders(in);
            default: throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    private Bundle hello() {
        BridgeStore store = BridgeStore.get(context());
        store.setLastHelloAt(System.currentTimeMillis());
        Bundle out = new Bundle();
        out.putInt(BridgeContract.Extras.PROTOCOL, BridgeContract.PROTOCOL);
        out.putString(BridgeContract.Extras.APP_VERSION, BuildConfig.VERSION_NAME);
        out.putBoolean(BridgeContract.Extras.IS_DEFAULT_SMS_APP, MonarraFeed.isDefaultSmsApp(context()));
        out.putLong(BridgeContract.Extras.FEED_HEAD_SEQ, AppDatabase.getInstance(context()).feedDao().headSeq());
        out.putLong(BridgeContract.Extras.RULES_VERSION, store.rules().version);
        return out;
    }

    /** A bad push is rejected as a whole; the previous rules stay in effect. */
    private Bundle setRules(Bundle in) {
        Bundle out = new Bundle();
        try {
            MonarraRules rules = MonarraRules.create(
                    in.getLong(BridgeContract.Extras.RULES_VERSION, 0),
                    in.getStringArray(BridgeContract.Extras.ALLOWED_SENDERS),
                    in.getString(BridgeContract.Extras.INCLUDE_FILTER),
                    in.getString(BridgeContract.Extras.DENY_FILTER));
            BridgeStore.get(context()).saveRules(rules);
            out.putBoolean(BridgeContract.Extras.OK, true);
        } catch (MonarraRules.InvalidRulesException e) {
            out.putBoolean(BridgeContract.Extras.OK, false);
            out.putString(BridgeContract.Extras.ERROR, e.getMessage());
        }
        return out;
    }

    private Bundle ack(Bundle in) {
        long upTo = in.getLong(BridgeContract.Extras.UP_TO_SEQ, -1);
        if (upTo < 0) throw new IllegalArgumentException("up_to_seq is missing or negative");
        MonarraFeed.ack(context(), upTo);
        Bundle out = new Bundle();
        out.putBoolean(BridgeContract.Extras.OK, true);
        return out;
    }

    private Bundle backfill(Bundle in) {
        int days = in.getInt(BridgeContract.Extras.DAYS, DEFAULT_BACKFILL_DAYS);
        if (days < 1 || days > MAX_BACKFILL_DAYS) {
            throw new IllegalArgumentException("days must be 1–" + MAX_BACKFILL_DAYS);
        }
        Bundle out = new Bundle();
        BridgeStore store = BridgeStore.get(context());
        if (store.rules().version == 0) {
            out.putBoolean(BridgeContract.Extras.STARTED, false);
            out.putString(BridgeContract.Extras.ERROR, "No rules yet: push rules before a backfill");
            return out;
        }
        boolean started = !BackfillWorker.isActive(context());
        if (started) {
            store.setBackfill(BridgeContract.BACKFILL_RUNNING, 0, 0);
            BackfillWorker.enqueue(context(), days); // unique work: a racing second call is a no-op
        }
        out.putBoolean(BridgeContract.Extras.STARTED, started);
        return out;
    }

    private Bundle status() {
        BridgeStore store = BridgeStore.get(context());
        Bundle out = new Bundle();
        out.putLong(BridgeContract.Extras.PENDING_COUNT, AppDatabase.getInstance(context()).feedDao().pendingCount());
        out.putLong(BridgeContract.Extras.LAST_LIVE_AT_MS, store.lastLiveAt());
        out.putString(BridgeContract.Extras.BACKFILL_STATE, store.backfillState());
        out.putLong(BridgeContract.Extras.BACKFILL_SCANNED, store.backfillScanned());
        out.putLong(BridgeContract.Extras.BACKFILL_MATCHED, store.backfillMatched());
        return out;
    }

    /**
     * Senders of incoming SMS in the last {@code days} days, busiest first. Only names and short
     * codes: personal phone numbers are none of Monarra's business.
     */
    private Bundle listSenders(Bundle in) {
        int days = in.getInt(BridgeContract.Extras.DAYS, DEFAULT_BACKFILL_DAYS);
        if (days < 1 || days > MAX_BACKFILL_DAYS) {
            throw new IllegalArgumentException("days must be 1–" + MAX_BACKFILL_DAYS);
        }
        long since = System.currentTimeMillis() - days * MonarraFeed.DAY_MS;
        List<SenderCount> rows = AppDatabase.getInstance(context()).messageDao()
                .incomingSenders(since, MAX_LISTED_SENDERS * 4);
        List<SenderCount> kept = new ArrayList<>();
        for (SenderCount row : rows) {
            if (isServiceSender(row.sender) && kept.size() < MAX_LISTED_SENDERS) kept.add(row);
        }
        String[] senders = new String[kept.size()];
        long[] counts = new long[kept.size()];
        for (int i = 0; i < kept.size(); i++) {
            senders[i] = kept.get(i).sender;
            counts[i] = kept.get(i).count;
        }
        Bundle out = new Bundle();
        out.putStringArray(BridgeContract.Extras.SENDERS, senders);
        out.putLongArray(BridgeContract.Extras.SENDER_COUNTS, counts);
        return out;
    }

    /** "COMBANK", "HNB-ALERTS" or a short code like "8888"; not "+94771234567". */
    static boolean isServiceSender(String sender) {
        if (sender == null) return false;
        String s = sender.trim();
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) return true;
        }
        String digits = s.replaceAll("[^0-9]", "");
        return !s.startsWith("+") && digits.length() <= 6;
    }

    /** ContentProvider.requireContext() needs API 30. */
    private Context context() {
        Context context = getContext();
        if (context == null) throw new IllegalStateException("Provider isn't attached");
        return context;
    }

    private void enforcePermission() {
        context().enforceCallingOrSelfPermission(BuildConfig.BRIDGE_PERMISSION, "Monarra bridge");
    }

    private static long parseLong(String value, long fallback) {
        if (value == null) return fallback;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: " + value);
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return FEED_TYPE;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("The feed is read-only; use call()");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("The feed is read-only; use call()");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("The feed is read-only; use call()");
    }
}
