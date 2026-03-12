package com.personal.smsapp.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ConversationDao_Impl implements ConversationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Conversation> __insertionAdapterOfConversation;

  private final EntityDeletionOrUpdateAdapter<Conversation> __updateAdapterOfConversation;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByThreadId;

  private final SharedSQLiteStatement __preparedStmtOfUpdateUnreadCount;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTagInfo;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSnippet;

  public ConversationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfConversation = new EntityInsertionAdapter<Conversation>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `conversations` (`thread_id`,`display_name`,`address`,`snippet`,`date`,`unread_count`,`message_count`,`last_tag`,`has_important`,`is_archived`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final Conversation entity) {
        statement.bindLong(1, entity.threadId);
        if (entity.displayName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.displayName);
        }
        if (entity.address == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.address);
        }
        if (entity.snippet == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.snippet);
        }
        statement.bindLong(5, entity.date);
        statement.bindLong(6, entity.unreadCount);
        statement.bindLong(7, entity.messageCount);
        if (entity.lastTag == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.lastTag);
        }
        final int _tmp = entity.hasImportant ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isArchived ? 1 : 0;
        statement.bindLong(10, _tmp_1);
      }
    };
    this.__updateAdapterOfConversation = new EntityDeletionOrUpdateAdapter<Conversation>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `conversations` SET `thread_id` = ?,`display_name` = ?,`address` = ?,`snippet` = ?,`date` = ?,`unread_count` = ?,`message_count` = ?,`last_tag` = ?,`has_important` = ?,`is_archived` = ? WHERE `thread_id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final Conversation entity) {
        statement.bindLong(1, entity.threadId);
        if (entity.displayName == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.displayName);
        }
        if (entity.address == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.address);
        }
        if (entity.snippet == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.snippet);
        }
        statement.bindLong(5, entity.date);
        statement.bindLong(6, entity.unreadCount);
        statement.bindLong(7, entity.messageCount);
        if (entity.lastTag == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.lastTag);
        }
        final int _tmp = entity.hasImportant ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.isArchived ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        statement.bindLong(11, entity.threadId);
      }
    };
    this.__preparedStmtOfDeleteByThreadId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM conversations WHERE thread_id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateUnreadCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE conversations SET unread_count = ? WHERE thread_id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateTagInfo = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE conversations SET last_tag = ?, has_important = ? WHERE thread_id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateSnippet = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE conversations SET snippet = ?, date = ? WHERE thread_id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertOrReplace(final Conversation conversation) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfConversation.insert(conversation);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final Conversation conversation) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfConversation.handle(conversation);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteByThreadId(final long threadId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByThreadId.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, threadId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteByThreadId.release(_stmt);
    }
  }

  @Override
  public void updateUnreadCount(final long threadId, final int count) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateUnreadCount.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, count);
    _argIndex = 2;
    _stmt.bindLong(_argIndex, threadId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdateUnreadCount.release(_stmt);
    }
  }

  @Override
  public void updateTagInfo(final long threadId, final String tag, final boolean hasImportant) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTagInfo.acquire();
    int _argIndex = 1;
    if (tag == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, tag);
    }
    _argIndex = 2;
    final int _tmp = hasImportant ? 1 : 0;
    _stmt.bindLong(_argIndex, _tmp);
    _argIndex = 3;
    _stmt.bindLong(_argIndex, threadId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdateTagInfo.release(_stmt);
    }
  }

  @Override
  public void updateSnippet(final long threadId, final String snippet, final long date) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSnippet.acquire();
    int _argIndex = 1;
    if (snippet == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, snippet);
    }
    _argIndex = 2;
    _stmt.bindLong(_argIndex, date);
    _argIndex = 3;
    _stmt.bindLong(_argIndex, threadId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdateSnippet.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Conversation>> getAllActive() {
    final String _sql = "SELECT * FROM conversations WHERE is_archived = 0 ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"conversations"}, false, new Callable<List<Conversation>>() {
      @Override
      @Nullable
      public List<Conversation> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "thread_id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "snippet");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unread_count");
          final int _cursorIndexOfMessageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "message_count");
          final int _cursorIndexOfLastTag = CursorUtil.getColumnIndexOrThrow(_cursor, "last_tag");
          final int _cursorIndexOfHasImportant = CursorUtil.getColumnIndexOrThrow(_cursor, "has_important");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "is_archived");
          final List<Conversation> _result = new ArrayList<Conversation>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Conversation _item;
            _item = new Conversation();
            _item.threadId = _cursor.getLong(_cursorIndexOfThreadId);
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _item.displayName = null;
            } else {
              _item.displayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _item.address = null;
            } else {
              _item.address = _cursor.getString(_cursorIndexOfAddress);
            }
            if (_cursor.isNull(_cursorIndexOfSnippet)) {
              _item.snippet = null;
            } else {
              _item.snippet = _cursor.getString(_cursorIndexOfSnippet);
            }
            _item.date = _cursor.getLong(_cursorIndexOfDate);
            _item.unreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            _item.messageCount = _cursor.getInt(_cursorIndexOfMessageCount);
            if (_cursor.isNull(_cursorIndexOfLastTag)) {
              _item.lastTag = null;
            } else {
              _item.lastTag = _cursor.getString(_cursorIndexOfLastTag);
            }
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasImportant);
            _item.hasImportant = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsArchived);
            _item.isArchived = _tmp_1 != 0;
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LiveData<List<Conversation>> getArchived() {
    final String _sql = "SELECT * FROM conversations WHERE is_archived = 1 ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"conversations"}, false, new Callable<List<Conversation>>() {
      @Override
      @Nullable
      public List<Conversation> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "thread_id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "snippet");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unread_count");
          final int _cursorIndexOfMessageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "message_count");
          final int _cursorIndexOfLastTag = CursorUtil.getColumnIndexOrThrow(_cursor, "last_tag");
          final int _cursorIndexOfHasImportant = CursorUtil.getColumnIndexOrThrow(_cursor, "has_important");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "is_archived");
          final List<Conversation> _result = new ArrayList<Conversation>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Conversation _item;
            _item = new Conversation();
            _item.threadId = _cursor.getLong(_cursorIndexOfThreadId);
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _item.displayName = null;
            } else {
              _item.displayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _item.address = null;
            } else {
              _item.address = _cursor.getString(_cursorIndexOfAddress);
            }
            if (_cursor.isNull(_cursorIndexOfSnippet)) {
              _item.snippet = null;
            } else {
              _item.snippet = _cursor.getString(_cursorIndexOfSnippet);
            }
            _item.date = _cursor.getLong(_cursorIndexOfDate);
            _item.unreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            _item.messageCount = _cursor.getInt(_cursorIndexOfMessageCount);
            if (_cursor.isNull(_cursorIndexOfLastTag)) {
              _item.lastTag = null;
            } else {
              _item.lastTag = _cursor.getString(_cursorIndexOfLastTag);
            }
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasImportant);
            _item.hasImportant = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsArchived);
            _item.isArchived = _tmp_1 != 0;
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Conversation getByThreadId(final long threadId) {
    final String _sql = "SELECT * FROM conversations WHERE thread_id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, threadId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "thread_id");
      final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
      final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
      final int _cursorIndexOfSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "snippet");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unread_count");
      final int _cursorIndexOfMessageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "message_count");
      final int _cursorIndexOfLastTag = CursorUtil.getColumnIndexOrThrow(_cursor, "last_tag");
      final int _cursorIndexOfHasImportant = CursorUtil.getColumnIndexOrThrow(_cursor, "has_important");
      final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "is_archived");
      final Conversation _result;
      if (_cursor.moveToFirst()) {
        _result = new Conversation();
        _result.threadId = _cursor.getLong(_cursorIndexOfThreadId);
        if (_cursor.isNull(_cursorIndexOfDisplayName)) {
          _result.displayName = null;
        } else {
          _result.displayName = _cursor.getString(_cursorIndexOfDisplayName);
        }
        if (_cursor.isNull(_cursorIndexOfAddress)) {
          _result.address = null;
        } else {
          _result.address = _cursor.getString(_cursorIndexOfAddress);
        }
        if (_cursor.isNull(_cursorIndexOfSnippet)) {
          _result.snippet = null;
        } else {
          _result.snippet = _cursor.getString(_cursorIndexOfSnippet);
        }
        _result.date = _cursor.getLong(_cursorIndexOfDate);
        _result.unreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
        _result.messageCount = _cursor.getInt(_cursorIndexOfMessageCount);
        if (_cursor.isNull(_cursorIndexOfLastTag)) {
          _result.lastTag = null;
        } else {
          _result.lastTag = _cursor.getString(_cursorIndexOfLastTag);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfHasImportant);
        _result.hasImportant = _tmp != 0;
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfIsArchived);
        _result.isArchived = _tmp_1 != 0;
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<Conversation>> search(final String query) {
    final String _sql = "SELECT * FROM conversations WHERE address LIKE '%' || ? || '%' OR display_name LIKE '%' || ? || '%' ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (query == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, query);
    }
    _argIndex = 2;
    if (query == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, query);
    }
    return __db.getInvalidationTracker().createLiveData(new String[] {"conversations"}, false, new Callable<List<Conversation>>() {
      @Override
      @Nullable
      public List<Conversation> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "thread_id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "snippet");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unread_count");
          final int _cursorIndexOfMessageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "message_count");
          final int _cursorIndexOfLastTag = CursorUtil.getColumnIndexOrThrow(_cursor, "last_tag");
          final int _cursorIndexOfHasImportant = CursorUtil.getColumnIndexOrThrow(_cursor, "has_important");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "is_archived");
          final List<Conversation> _result = new ArrayList<Conversation>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Conversation _item;
            _item = new Conversation();
            _item.threadId = _cursor.getLong(_cursorIndexOfThreadId);
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _item.displayName = null;
            } else {
              _item.displayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _item.address = null;
            } else {
              _item.address = _cursor.getString(_cursorIndexOfAddress);
            }
            if (_cursor.isNull(_cursorIndexOfSnippet)) {
              _item.snippet = null;
            } else {
              _item.snippet = _cursor.getString(_cursorIndexOfSnippet);
            }
            _item.date = _cursor.getLong(_cursorIndexOfDate);
            _item.unreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            _item.messageCount = _cursor.getInt(_cursorIndexOfMessageCount);
            if (_cursor.isNull(_cursorIndexOfLastTag)) {
              _item.lastTag = null;
            } else {
              _item.lastTag = _cursor.getString(_cursorIndexOfLastTag);
            }
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasImportant);
            _item.hasImportant = _tmp != 0;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsArchived);
            _item.isArchived = _tmp_1 != 0;
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
