package com.personal.smsapp.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One financial SMS waiting for (or already handed to) Monarra. Rows stay until Monarra
 * acknowledges them; then the body is wiped after 7 days and the row deleted after 30.
 */
@Entity(
    tableName = "financial_feed",
    indices = {
        @Index(value = "message_hash", unique = true),
        @Index("acked_at")
    }
)
public class FeedEntry {

    /** Strictly increasing and never reused (AUTOINCREMENT): Monarra's cursor. */
    @PrimaryKey(autoGenerate = true)
    public long seq;

    @NonNull
    @ColumnInfo(name = "message_hash")
    public String messageHash = "";

    @NonNull
    @ColumnInfo(name = "sender")
    public String sender = "";

    /** Null once the retention wipe has run. */
    @Nullable
    @ColumnInfo(name = "body")
    public String body;

    /** Service-centre timestamp (SmsMessage.getTimestampMillis()). */
    @ColumnInfo(name = "sent_at")
    public long sentAt;

    /** Device clock at capture; for backfill, the stored message date. */
    @ColumnInfo(name = "received_at")
    public long receivedAt;

    @ColumnInfo(name = "sim_slot")
    public int simSlot = -1;

    /** LIVE or BACKFILL. */
    @NonNull
    @ColumnInfo(name = "origin")
    public String origin = "LIVE";

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @Nullable
    @ColumnInfo(name = "acked_at")
    public Long ackedAt;
}
