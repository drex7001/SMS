package com.personal.smsapp.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Mirrors the system SMS store but adds our custom fields:
 * - apiTag      : label returned by the API (e.g. "promo", "otp", "personal")
 * - isImportant : API can flag a message as important
 * - apiProcessed: whether this message has been sent to the API already
 */
@Entity(
    tableName = "messages",
    indices = {
        @Index("thread_id"),
        @Index("address"),
        @Index("api_processed")
    }
)
public class Message {

    // ── Types ──────────────────────────────────────────────────────────────
    public static final int TYPE_INBOX  = 1;
    public static final int TYPE_SENT   = 2;
    public static final int TYPE_DRAFT  = 3;

    // ── Status ─────────────────────────────────────────────────────────────
    public static final int STATUS_NONE    = -1;
    public static final int STATUS_PENDING =  0;
    public static final int STATUS_SENT    =  1;
    public static final int STATUS_FAILED  =  2;

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** The system SMS thread id (links to Telephony.Sms.Conversations) */
    @ColumnInfo(name = "thread_id")
    public long threadId;

    /** Sender / recipient phone number */
    @ColumnInfo(name = "address")
    public String address;

    @ColumnInfo(name = "body")
    public String body;

    @ColumnInfo(name = "date")
    public long date;               // milliseconds epoch

    @ColumnInfo(name = "type")
    public int type = TYPE_INBOX;

    @ColumnInfo(name = "read")
    public boolean read = false;

    @ColumnInfo(name = "status")
    public int status = STATUS_NONE;

    // ── API-enriched fields ────────────────────────────────────────────────

    /** Tag label from API response (e.g. "otp", "promo", "important") */
    @ColumnInfo(name = "api_tag")
    public String apiTag = "";

    /** API flagged this as important */
    @ColumnInfo(name = "is_important")
    public boolean isImportant = false;

    /** True once forwarded to API (avoids duplicate calls) */
    @ColumnInfo(name = "api_processed")
    public boolean apiProcessed = false;

    /** System SMS id so we can delete/update the system store if needed */
    @ColumnInfo(name = "system_sms_id")
    public long systemSmsId = -1;

    // ── Helpers ────────────────────────────────────────────────────────────

    public boolean isIncoming() {
        return type == TYPE_INBOX;
    }

    public boolean hasTag() {
        return apiTag != null && !apiTag.isEmpty();
    }
}
