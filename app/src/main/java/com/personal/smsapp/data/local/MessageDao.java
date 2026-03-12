package com.personal.smsapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Message message);

    @Update
    void update(Message message);

    @Query("DELETE FROM messages WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM messages WHERE thread_id = :threadId")
    void deleteByThreadId(long threadId);

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY date ASC")
    LiveData<List<Message>> getMessagesForThread(long threadId);

    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY date ASC")
    List<Message> getMessagesForThreadSync(long threadId);

    @Query("SELECT * FROM messages WHERE api_processed = 0 AND type = 1 ORDER BY date ASC")
    List<Message> getUnprocessedIncoming();

    @Query("UPDATE messages SET api_processed = 1, api_tag = :tag, is_important = :important WHERE id = :id")
    void applyApiResult(long id, String tag, boolean important);

    @Query("UPDATE messages SET read = 1 WHERE thread_id = :threadId")
    void markThreadRead(long threadId);

    @Query("SELECT * FROM messages WHERE id = :id")
    Message getById(long id);

    @Query("SELECT * FROM messages WHERE system_sms_id = :systemId LIMIT 1")
    Message getBySystemId(long systemId);

    @Query("SELECT COUNT(*) FROM messages WHERE thread_id = :threadId AND read = 0")
    int getUnreadCount(long threadId);
}
