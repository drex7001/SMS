package com.personal.smsapp.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * User-defined message classification rule.
 *
 * When an incoming message matches the signal:
 *  - tag is applied locally
 *  - if send_to_server = false, the message skips the API call entirely
 */
@Entity(tableName = "local_filters")
public class LocalFilter {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Human-readable category name (e.g. "OTP", "Bank", "Personal") */
    @ColumnInfo(name = "name")
    public String name = "";

    /** Keyword or regex pattern to match against message body */
    @ColumnInfo(name = "signal")
    public String signal = "";

    /** If true, signal is treated as a Java regex; otherwise plain substring match */
    @ColumnInfo(name = "is_regex")
    public boolean isRegex = false;

    /** Tag to apply to matched messages (shown as a chip in the UI) */
    @ColumnInfo(name = "tag")
    public String tag = "";

    /** If false, matched messages are processed locally and never sent to the API */
    @ColumnInfo(name = "send_to_server")
    public boolean sendToServer = false;

    /** Whether this rule is active */
    @ColumnInfo(name = "enabled")
    public boolean enabled = true;
}
