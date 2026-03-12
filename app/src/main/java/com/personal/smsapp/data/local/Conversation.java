package com.personal.smsapp.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A conversation = one SMS thread.
 * We maintain our own copy so we can add unread counts, tags, and
 * avoid hammering the system ContentProvider on every list scroll.
 */
@Entity(tableName = "conversations")
public class Conversation {

    @PrimaryKey
    @ColumnInfo(name = "thread_id")
    public long threadId;

    /** Contact display name (resolved from Contacts, else raw number) */
    @ColumnInfo(name = "display_name")
    public String displayName;

    @ColumnInfo(name = "address")
    public String address;

    @ColumnInfo(name = "snippet")
    public String snippet;          // last message preview

    @ColumnInfo(name = "date")
    public long date;

    @ColumnInfo(name = "unread_count")
    public int unreadCount = 0;

    @ColumnInfo(name = "message_count")
    public int messageCount = 0;

    /** Aggregated tag from the last API-processed message in this thread */
    @ColumnInfo(name = "last_tag")
    public String lastTag = "";

    @ColumnInfo(name = "has_important")
    public boolean hasImportant = false;

    @ColumnInfo(name = "is_archived")
    public boolean isArchived = false;
}
