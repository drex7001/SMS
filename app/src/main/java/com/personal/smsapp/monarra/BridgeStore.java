package com.personal.smsapp.monarra;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * The bridge's small state: the rules Monarra pushed and a few timestamps and counters for the
 * status screen. Plain private preferences; excluded from backups with everything else.
 */
public final class BridgeStore {

    private static final String TAG   = "BridgeStore";
    private static final String PREFS = "monarra_bridge";

    private static final String KEY_RULES_VERSION  = "rules_version";
    private static final String KEY_RULES_SENDERS  = "rules_senders";
    private static final String KEY_RULES_INCLUDE  = "rules_include";
    private static final String KEY_RULES_DENY     = "rules_deny";
    private static final String KEY_LAST_LIVE_AT   = "last_live_at_ms";
    private static final String KEY_LAST_ACK_AT    = "last_ack_at_ms";
    private static final String KEY_LAST_HELLO_AT  = "last_hello_at_ms";
    private static final String KEY_BACKFILL_STATE = "backfill_state";
    private static final String KEY_BACKFILL_SCAN  = "backfill_scanned";
    private static final String KEY_BACKFILL_MATCH = "backfill_matched";

    /** Compiled once per rules version, shared by every capture. */
    private static MonarraRules cachedRules;

    private final SharedPreferences prefs;

    private BridgeStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static BridgeStore get(Context context) {
        return new BridgeStore(context);
    }

    // ── Rules ──────────────────────────────────────────────────────────────

    public MonarraRules rules() {
        long version = prefs.getLong(KEY_RULES_VERSION, 0);
        synchronized (BridgeStore.class) {
            if (cachedRules != null && cachedRules.version == version) return cachedRules;
            cachedRules = load(version);
            return cachedRules;
        }
    }

    public void saveRules(MonarraRules rules) {
        synchronized (BridgeStore.class) {
            prefs.edit()
                .putLong(KEY_RULES_VERSION, rules.version)
                .putString(KEY_RULES_SENDERS, new JSONArray(rules.senders).toString())
                .putString(KEY_RULES_INCLUDE, rules.includeFilter)
                .putString(KEY_RULES_DENY, rules.denyFilter)
                .commit();
            cachedRules = rules;
        }
    }

    private MonarraRules load(long version) {
        if (version <= 0) return MonarraRules.NONE;
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_RULES_SENDERS, "[]"));
            String[] senders = new String[array.length()];
            for (int i = 0; i < senders.length; i++) senders[i] = array.getString(i);
            return MonarraRules.create(version, senders,
                    prefs.getString(KEY_RULES_INCLUDE, null), prefs.getString(KEY_RULES_DENY, null));
        } catch (JSONException | MonarraRules.InvalidRulesException e) {
            // Only rules that passed validation are ever saved, so this means the file is damaged.
            Log.e(TAG, "Stored rules are unreadable; capturing nothing until Monarra pushes again", e);
            return MonarraRules.NONE;
        }
    }

    // ── Status ─────────────────────────────────────────────────────────────

    public long lastLiveAt()  { return prefs.getLong(KEY_LAST_LIVE_AT, 0); }
    public long lastAckAt()   { return prefs.getLong(KEY_LAST_ACK_AT, 0); }
    public long lastHelloAt() { return prefs.getLong(KEY_LAST_HELLO_AT, 0); }

    public void setLastLiveAt(long millis)  { prefs.edit().putLong(KEY_LAST_LIVE_AT, millis).apply(); }
    public void setLastAckAt(long millis)   { prefs.edit().putLong(KEY_LAST_ACK_AT, millis).apply(); }
    public void setLastHelloAt(long millis) { prefs.edit().putLong(KEY_LAST_HELLO_AT, millis).apply(); }

    // ── Backfill ───────────────────────────────────────────────────────────

    public String backfillState()  { return prefs.getString(KEY_BACKFILL_STATE, BridgeContract.BACKFILL_IDLE); }
    public long   backfillScanned() { return prefs.getLong(KEY_BACKFILL_SCAN, 0); }
    public long   backfillMatched() { return prefs.getLong(KEY_BACKFILL_MATCH, 0); }

    public void setBackfill(String state, long scanned, long matched) {
        prefs.edit()
            .putString(KEY_BACKFILL_STATE, state)
            .putLong(KEY_BACKFILL_SCAN, scanned)
            .putLong(KEY_BACKFILL_MATCH, matched)
            .commit();
    }
}
