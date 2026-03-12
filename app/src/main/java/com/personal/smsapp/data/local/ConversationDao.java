package com.personal.smsapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(Conversation conversation);

    @Update
    void update(Conversation conversation);

    @Query("DELETE FROM conversations WHERE thread_id = :threadId")
    void deleteByThreadId(long threadId);

    @Query("SELECT * FROM conversations WHERE is_archived = 0 ORDER BY date DESC")
    LiveData<List<Conversation>> getAllActive();

    @Query("SELECT * FROM conversations WHERE is_archived = 1 ORDER BY date DESC")
    LiveData<List<Conversation>> getArchived();

    @Query("SELECT * FROM conversations WHERE thread_id = :threadId LIMIT 1")
    Conversation getByThreadId(long threadId);

    @Query("UPDATE conversations SET unread_count = :count WHERE thread_id = :threadId")
    void updateUnreadCount(long threadId, int count);

    @Query("UPDATE conversations SET last_tag = :tag, has_important = :hasImportant WHERE thread_id = :threadId")
    void updateTagInfo(long threadId, String tag, boolean hasImportant);

    @Query("UPDATE conversations SET snippet = :snippet, date = :date WHERE thread_id = :threadId")
    void updateSnippet(long threadId, String snippet, long date);

    @Query("SELECT * FROM conversations WHERE address LIKE '%' || :query || '%' OR display_name LIKE '%' || :query || '%' ORDER BY date DESC")
    LiveData<List<Conversation>> search(String query);
}
