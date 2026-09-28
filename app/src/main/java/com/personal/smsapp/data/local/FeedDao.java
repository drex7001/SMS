package com.personal.smsapp.data.local;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/** The Monarra feed. Column names in rowsAfter are the bridge contract's, not the table's. */
@Dao
public interface FeedDao {

    /** Returns the new seq, or -1 if a row with the same message_hash is already there. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(FeedEntry entry);

    @Query("SELECT seq, message_hash, sender, body, sent_at AS sent_at_ms, received_at AS received_at_ms, "
            + "sim_slot, origin FROM financial_feed WHERE seq > :after ORDER BY seq ASC LIMIT :limit")
    Cursor rowsAfter(long after, int limit);

    @Query("SELECT COALESCE(MAX(seq), 0) FROM financial_feed")
    long headSeq();

    @Query("SELECT COUNT(*) FROM financial_feed WHERE acked_at IS NULL")
    long pendingCount();

    /** Acks are monotonic: rows already acknowledged keep their first ack time. */
    @Query("UPDATE financial_feed SET acked_at = :now WHERE seq <= :upToSeq AND acked_at IS NULL")
    int ack(long upToSeq, long now);

    @Query("UPDATE financial_feed SET body = NULL WHERE acked_at < :before AND body IS NOT NULL")
    int wipeBodiesAckedBefore(long before);

    @Query("DELETE FROM financial_feed WHERE acked_at < :before")
    int deleteAckedBefore(long before);
}
