package com.personal.smsapp.monarra;

/**
 * The on-device bridge to Monarra, protocol 1.
 *
 * The full contract lives in Monarra's repo (docs/SMS_BRIDGE.md); both apps keep a copy of these
 * constants and test against the same fixtures in bridge-fixtures/protocol-1/. IDs that differ
 * between debug and release builds (Monarra's package, the permission) are BuildConfig fields.
 */
public final class BridgeContract {

    public static final int PROTOCOL = 1;

    public static final String ACTION_FEED_UPDATED = "com.personal.monarra.action.SMS_FEED_UPDATED";

    // ── Feed query: content://<authority>/feed?after=<seq>&limit=<n> ───────
    public static final String PATH_FEED     = "feed";
    public static final String QUERY_AFTER   = "after";
    public static final String QUERY_LIMIT   = "limit";
    public static final int    DEFAULT_LIMIT = 200;
    public static final int    MAX_LIMIT     = 500;

    public static final String ORIGIN_LIVE     = "LIVE";
    public static final String ORIGIN_BACKFILL = "BACKFILL";

    public static final String BACKFILL_IDLE    = "IDLE";
    public static final String BACKFILL_RUNNING = "RUNNING";
    public static final String BACKFILL_DONE    = "DONE";
    public static final String BACKFILL_FAILED  = "FAILED";

    public static final class Methods {
        public static final String HELLO        = "hello";
        public static final String SET_RULES    = "setRules";
        public static final String ACK          = "ack";
        public static final String BACKFILL     = "backfill";
        public static final String STATUS       = "status";
        /** Added in protocol 1 (additive): senders seen on this phone, to pick bank senders from. */
        public static final String LIST_SENDERS = "listSenders";

        private Methods() {}
    }

    public static final class Columns {
        public static final String SEQ            = "seq";
        public static final String MESSAGE_HASH   = "message_hash";
        public static final String SENDER         = "sender";
        public static final String BODY           = "body";
        public static final String SENT_AT_MS     = "sent_at_ms";
        public static final String RECEIVED_AT_MS = "received_at_ms";
        public static final String SIM_SLOT       = "sim_slot";
        public static final String ORIGIN         = "origin";

        private Columns() {}
    }

    public static final class Extras {
        public static final String CLIENT_PROTOCOL    = "client_protocol";
        public static final String CLIENT_VERSION     = "client_version";
        public static final String PROTOCOL           = "protocol";
        public static final String APP_VERSION        = "app_version";
        public static final String IS_DEFAULT_SMS_APP = "is_default_sms_app";
        public static final String FEED_HEAD_SEQ      = "feed_head_seq";
        public static final String RULES_VERSION      = "rules_version";
        public static final String ALLOWED_SENDERS    = "allowed_senders";
        public static final String INCLUDE_FILTER     = "include_filter";
        public static final String DENY_FILTER        = "deny_filter";
        public static final String OK                 = "ok";
        public static final String ERROR              = "error";
        public static final String UP_TO_SEQ          = "up_to_seq";
        public static final String DAYS               = "days";
        public static final String STARTED            = "started";
        public static final String PENDING_COUNT      = "pending_count";
        public static final String LAST_LIVE_AT_MS    = "last_live_at_ms";
        public static final String BACKFILL_STATE     = "backfill_state";
        public static final String BACKFILL_SCANNED   = "backfill_scanned";
        public static final String BACKFILL_MATCHED   = "backfill_matched";
        public static final String SENDERS            = "senders";
        public static final String SENDER_COUNTS      = "sender_counts";

        private Extras() {}
    }

    private BridgeContract() {}
}
