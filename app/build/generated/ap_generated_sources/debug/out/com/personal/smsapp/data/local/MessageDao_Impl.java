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
public final class MessageDao_Impl implements MessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Message> __insertionAdapterOfMessage;

  private final EntityDeletionOrUpdateAdapter<Message> __updateAdapterOfMessage;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByThreadId;

  private final SharedSQLiteStatement __preparedStmtOfApplyApiResult;

  private final SharedSQLiteStatement __preparedStmtOfMarkThreadRead;

  public MessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMessage = new EntityInsertionAdapter<Message>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `messages` (`id`,`thread_id`,`address`,`body`,`date`,`type`,`read`,`status`,`api_tag`,`is_important`,`api_processed`,`system_sms_id`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Message entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.threadId);
        if (entity.address == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.address);
        }
        if (entity.body == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.body);
        }
        statement.bindLong(5, entity.date);
        statement.bindLong(6, entity.type);
        final int _tmp = entity.read ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.status);
        if (entity.apiTag == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.apiTag);
        }
        final int _tmp_1 = entity.isImportant ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.apiProcessed ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        statement.bindLong(12, entity.systemSmsId);
      }
    };
    this.__updateAdapterOfMessage = new EntityDeletionOrUpdateAdapter<Message>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `messages` SET `id` = ?,`thread_id` = ?,`address` = ?,`body` = ?,`date` = ?,`type` = ?,`read` = ?,`status` = ?,`api_tag` = ?,`is_important` = ?,`api_processed` = ?,`system_sms_id` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Message entity) {
        statement.bindLong(1, entity.id);
        statement.bindLong(2, entity.threadId);
        if (entity.address == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.address);
        }
        if (entity.body == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.body);
        }
        statement.bindLong(5, entity.date);
        statement.bindLong(6, entity.type);
        final int _tmp = entity.read ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.status);
        if (entity.apiTag == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.apiTag);
        }
        final int _tmp_1 = entity.isImportant ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.apiProcessed ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        statement.bindLong(12, entity.systemSmsId);
        statement.bindLong(13, entity.id);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByThreadId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE thread_id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfApplyApiResult = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET api_processed = 1, api_tag = ?, is_important = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkThreadRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET read = 1 WHERE thread_id = ?";
        return _query;
      }
    };
  }

  @Override
  public long insert(final Message message) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfMessage.insertAndReturnId(message);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final Message message) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfMessage.handle(message);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteById(final long id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, id);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteById.release(_stmt);
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
  public void applyApiResult(final long id, final String tag, final boolean important) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfApplyApiResult.acquire();
    int _argIndex = 1;
    if (tag == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, tag);
    }
    _argIndex = 2;
    final int _tmp = important ? 1 : 0;
    _stmt.bindLong(_argIndex, _tmp);
    _argIndex = 3;
    _stmt.bindLong(_argIndex, id);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfApplyApiResult.release(_stmt);
    }
  }

  @Override
  public void markThreadRead(final long threadId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfMarkThreadRead.acquire();
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
      __preparedStmtOfMarkThreadRead.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Message>> getMessagesForThread(final long threadId) {
    final String _sql = "SELECT * FROM messages WHERE thread_id = ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, threadId);
    return __db.getInvalidationTracker().createLiveData(new String[] {"messages"}, false, new Callable<List<Message>>() {
      @Override
      @Nullable
      public List<Message> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "thread_id");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfRead = CursorUtil.getColumnIndexOrThrow(_cursor, "read");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfApiTag = CursorUtil.getColumnIndexOrThrow(_cursor, "api_tag");
          final int _cursorIndexOfIsImportant = CursorUtil.getColumnIndexOrThrow(_cursor, "is_important");
          final int _cursorIndexOfApiProcessed = CursorUtil.getColumnIndexOrThrow(_cursor, "api_processed");
          final int _cursorIndexOfSystemSmsId = CursorUtil.getColumnIndexOrThrow(_cursor, "system_sms_id");
          final List<Message> _result = new ArrayList<Message>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Message _item;
            _item = new Message();
            _item.id = _cursor.getLong(_cursorIndexOfId);
            _item.threadId = _cursor.getLong(_cursorIndexOfThreadId);
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _item.address = null;
            } else {
              _item.address = _cursor.getString(_cursorIndexOfAddress);
            }
            if (_cursor.isNull(_cursorIndexOfBody)) {
              _item.body = null;
            } else {
              _item.body = _cursor.getString(_cursorIndexOfBody);
            }
            _item.date = _cursor.getLong(_cursorIndexOfDate);
            _item.type = _cursor.getInt(_cursorIndexOfType);
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRead);
            _item.read = _tmp != 0;
            _item.status = _cursor.getInt(_cursorIndexOfStatus);
            if (_cursor.isNull(_cursorIndexOfApiTag)) {
              _item.apiTag = null;
            } else {
              _item.apiTag = _cursor.getString(_cursorIndexOfApiTag);
            }
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsImportant);
            _item.isImportant = _tmp_1 != 0;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfApiProcessed);
            _item.apiProcessed = _tmp_2 != 0;
            _item.systemSmsId = _cursor.getLong(_cursorIndexOfSystemSmsId);
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
  public List<Message> getMessagesForThreadSync(final long threadId) {
    final String _sql = "SELECT * FROM messages WHERE thread_id = ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, threadId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "thread_id");
      final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
      final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
      final int _cursorIndexOfRead = CursorUtil.getColumnIndexOrThrow(_cursor, "read");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfApiTag = CursorUtil.getColumnIndexOrThrow(_cursor, "api_tag");
      final int _cursorIndexOfIsImportant = CursorUtil.getColumnIndexOrThrow(_cursor, "is_important");
      final int _cursorIndexOfApiProcessed = CursorUtil.getColumnIndexOrThrow(_cursor, "api_processed");
      final int _cursorIndexOfSystemSmsId = CursorUtil.getColumnIndexOrThrow(_cursor, "system_sms_id");
      final List<Message> _result = new ArrayList<Message>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Message _item;
        _item = new Message();
        _item.id = _cursor.getLong(_cursorIndexOfId);
        _item.threadId = _cursor.getLong(_cursorIndexOfThreadId);
        if (_cursor.isNull(_cursorIndexOfAddress)) {
          _item.address = null;
        } else {
          _item.address = _cursor.getString(_cursorIndexOfAddress);
        }
        if (_cursor.isNull(_cursorIndexOfBody)) {
          _item.body = null;
        } else {
          _item.body = _cursor.getString(_cursorIndexOfBody);
        }
        _item.date = _cursor.getLong(_cursorIndexOfDate);
        _item.type = _cursor.getInt(_cursorIndexOfType);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfRead);
        _item.read = _tmp != 0;
        _item.status = _cursor.getInt(_cursorIndexOfStatus);
        if (_cursor.isNull(_cursorIndexOfApiTag)) {
          _item.apiTag = null;
        } else {
          _item.apiTag = _cursor.getString(_cursorIndexOfApiTag);
        }
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfIsImportant);
        _item.isImportant = _tmp_1 != 0;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfApiProcessed);
        _item.apiProcessed = _tmp_2 != 0;
        _item.systemSmsId = _cursor.getLong(_cursorIndexOfSystemSmsId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Message> getUnprocessedIncoming() {
    final String _sql = "SELECT * FROM messages WHERE api_processed = 0 AND type = 1 ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "thread_id");
      final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
      final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
      final int _cursorIndexOfRead = CursorUtil.getColumnIndexOrThrow(_cursor, "read");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfApiTag = CursorUtil.getColumnIndexOrThrow(_cursor, "api_tag");
      final int _cursorIndexOfIsImportant = CursorUtil.getColumnIndexOrThrow(_cursor, "is_important");
      final int _cursorIndexOfApiProcessed = CursorUtil.getColumnIndexOrThrow(_cursor, "api_processed");
      final int _cursorIndexOfSystemSmsId = CursorUtil.getColumnIndexOrThrow(_cursor, "system_sms_id");
      final List<Message> _result = new ArrayList<Message>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Message _item;
        _item = new Message();
        _item.id = _cursor.getLong(_cursorIndexOfId);
        _item.threadId = _cursor.getLong(_cursorIndexOfThreadId);
        if (_cursor.isNull(_cursorIndexOfAddress)) {
          _item.address = null;
        } else {
          _item.address = _cursor.getString(_cursorIndexOfAddress);
        }
        if (_cursor.isNull(_cursorIndexOfBody)) {
          _item.body = null;
        } else {
          _item.body = _cursor.getString(_cursorIndexOfBody);
        }
        _item.date = _cursor.getLong(_cursorIndexOfDate);
        _item.type = _cursor.getInt(_cursorIndexOfType);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfRead);
        _item.read = _tmp != 0;
        _item.status = _cursor.getInt(_cursorIndexOfStatus);
        if (_cursor.isNull(_cursorIndexOfApiTag)) {
          _item.apiTag = null;
        } else {
          _item.apiTag = _cursor.getString(_cursorIndexOfApiTag);
        }
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfIsImportant);
        _item.isImportant = _tmp_1 != 0;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfApiProcessed);
        _item.apiProcessed = _tmp_2 != 0;
        _item.systemSmsId = _cursor.getLong(_cursorIndexOfSystemSmsId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Message getById(final long id) {
    final String _sql = "SELECT * FROM messages WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "thread_id");
      final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
      final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
      final int _cursorIndexOfRead = CursorUtil.getColumnIndexOrThrow(_cursor, "read");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfApiTag = CursorUtil.getColumnIndexOrThrow(_cursor, "api_tag");
      final int _cursorIndexOfIsImportant = CursorUtil.getColumnIndexOrThrow(_cursor, "is_important");
      final int _cursorIndexOfApiProcessed = CursorUtil.getColumnIndexOrThrow(_cursor, "api_processed");
      final int _cursorIndexOfSystemSmsId = CursorUtil.getColumnIndexOrThrow(_cursor, "system_sms_id");
      final Message _result;
      if (_cursor.moveToFirst()) {
        _result = new Message();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        _result.threadId = _cursor.getLong(_cursorIndexOfThreadId);
        if (_cursor.isNull(_cursorIndexOfAddress)) {
          _result.address = null;
        } else {
          _result.address = _cursor.getString(_cursorIndexOfAddress);
        }
        if (_cursor.isNull(_cursorIndexOfBody)) {
          _result.body = null;
        } else {
          _result.body = _cursor.getString(_cursorIndexOfBody);
        }
        _result.date = _cursor.getLong(_cursorIndexOfDate);
        _result.type = _cursor.getInt(_cursorIndexOfType);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfRead);
        _result.read = _tmp != 0;
        _result.status = _cursor.getInt(_cursorIndexOfStatus);
        if (_cursor.isNull(_cursorIndexOfApiTag)) {
          _result.apiTag = null;
        } else {
          _result.apiTag = _cursor.getString(_cursorIndexOfApiTag);
        }
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfIsImportant);
        _result.isImportant = _tmp_1 != 0;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfApiProcessed);
        _result.apiProcessed = _tmp_2 != 0;
        _result.systemSmsId = _cursor.getLong(_cursorIndexOfSystemSmsId);
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
  public Message getBySystemId(final long systemId) {
    final String _sql = "SELECT * FROM messages WHERE system_sms_id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, systemId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfThreadId = CursorUtil.getColumnIndexOrThrow(_cursor, "thread_id");
      final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
      final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
      final int _cursorIndexOfRead = CursorUtil.getColumnIndexOrThrow(_cursor, "read");
      final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
      final int _cursorIndexOfApiTag = CursorUtil.getColumnIndexOrThrow(_cursor, "api_tag");
      final int _cursorIndexOfIsImportant = CursorUtil.getColumnIndexOrThrow(_cursor, "is_important");
      final int _cursorIndexOfApiProcessed = CursorUtil.getColumnIndexOrThrow(_cursor, "api_processed");
      final int _cursorIndexOfSystemSmsId = CursorUtil.getColumnIndexOrThrow(_cursor, "system_sms_id");
      final Message _result;
      if (_cursor.moveToFirst()) {
        _result = new Message();
        _result.id = _cursor.getLong(_cursorIndexOfId);
        _result.threadId = _cursor.getLong(_cursorIndexOfThreadId);
        if (_cursor.isNull(_cursorIndexOfAddress)) {
          _result.address = null;
        } else {
          _result.address = _cursor.getString(_cursorIndexOfAddress);
        }
        if (_cursor.isNull(_cursorIndexOfBody)) {
          _result.body = null;
        } else {
          _result.body = _cursor.getString(_cursorIndexOfBody);
        }
        _result.date = _cursor.getLong(_cursorIndexOfDate);
        _result.type = _cursor.getInt(_cursorIndexOfType);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfRead);
        _result.read = _tmp != 0;
        _result.status = _cursor.getInt(_cursorIndexOfStatus);
        if (_cursor.isNull(_cursorIndexOfApiTag)) {
          _result.apiTag = null;
        } else {
          _result.apiTag = _cursor.getString(_cursorIndexOfApiTag);
        }
        final int _tmp_1;
        _tmp_1 = _cursor.getInt(_cursorIndexOfIsImportant);
        _result.isImportant = _tmp_1 != 0;
        final int _tmp_2;
        _tmp_2 = _cursor.getInt(_cursorIndexOfApiProcessed);
        _result.apiProcessed = _tmp_2 != 0;
        _result.systemSmsId = _cursor.getLong(_cursorIndexOfSystemSmsId);
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
  public int getUnreadCount(final long threadId) {
    final String _sql = "SELECT COUNT(*) FROM messages WHERE thread_id = ? AND read = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, threadId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
