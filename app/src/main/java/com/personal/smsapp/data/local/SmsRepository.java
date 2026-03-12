package com.personal.smsapp.data.local;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single source of truth.  All UI and workers go through here.
 *
 * Thread strategy:
 *   - LiveData queries are observed on the main thread (Room handles dispatch)
 *   - Write/delete ops run on the ioExecutor (2 threads is plenty)
 *   - We never touch the system ContentProvider from the main thread
 */
public class SmsRepository {

    private static final String TAG = "SmsRepository";

    private final Context         appContext;
    private final AppDatabase     db;
    private final MessageDao      messageDao;
    private final ConversationDao conversationDao;
    private final ContentResolver resolver;
    private final ExecutorService ioExecutor;

    private static volatile SmsRepository INSTANCE;

    public static SmsRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SmsRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SmsRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private SmsRepository(Context context) {
        appContext      = context;
        db              = AppDatabase.getInstance(context);
        messageDao      = db.messageDao();
        conversationDao = db.conversationDao();
        resolver        = context.getContentResolver();
        ioExecutor      = Executors.newFixedThreadPool(2);
    }

    // ── Conversation queries (LiveData for UI) ─────────────────────────────

    public LiveData<List<Conversation>> getActiveConversations() {
        return conversationDao.getAllActive();
    }

    public LiveData<List<Conversation>> searchConversations(String query) {
        return conversationDao.search(query);
    }

    // ── Message queries ────────────────────────────────────────────────────

    public LiveData<List<Message>> getMessages(long threadId) {
        return messageDao.getMessagesForThread(threadId);
    }

    // ── Write ops (all dispatched to background) ───────────────────────────

    /**
     * Called by SmsReceiver when a new SMS arrives.
     * Inserts into our DB; the API worker will pick it up separately.
     */
    public void insertIncoming(String address, String body, long date,
                               long systemSmsId, long threadId,
                               Runnable onInserted) {
        ioExecutor.execute(() -> {
            Message msg = new Message();
            msg.address     = address;
            msg.body        = body;
            msg.date        = date;
            msg.type        = Message.TYPE_INBOX;
            msg.read        = false;
            msg.systemSmsId = systemSmsId;
            msg.threadId    = threadId;
            msg.apiProcessed = false;

            long newId = messageDao.insert(msg);

            // Update or create conversation row
            upsertConversation(threadId, address, body, date);

            if (onInserted != null) onInserted.run();
        });
    }

    /**
     * Send an outgoing SMS via the system, then persist it locally.
     */
    public void sendMessage(String address, String body, long threadId) {
        ioExecutor.execute(() -> {
            // Resolve real thread ID if caller passed 0
            long resolvedThreadId = threadId;
            if (resolvedThreadId <= 0) {
                try {
                    resolvedThreadId = Telephony.Threads.getOrCreateThreadId(appContext, address);
                } catch (Exception ignored) {}
            }
            final long finalThreadId = resolvedThreadId;

            // Write to system SMS provider (required to appear in other apps & carrier)
            long date = System.currentTimeMillis();
            ContentValues cv = new ContentValues();
            cv.put(Telephony.Sms.ADDRESS,   address);
            cv.put(Telephony.Sms.BODY,      body);
            cv.put(Telephony.Sms.DATE,      date);
            cv.put(Telephony.Sms.TYPE,      Telephony.Sms.MESSAGE_TYPE_SENT);
            cv.put(Telephony.Sms.READ,      1);
            cv.put(Telephony.Sms.THREAD_ID, finalThreadId);
            Uri inserted = resolver.insert(Telephony.Sms.Sent.CONTENT_URI, cv);

            long sysId = inserted != null ? Long.parseLong(inserted.getLastPathSegment()) : -1;

            // Our DB
            Message msg = new Message();
            msg.address      = address;
            msg.body         = body;
            msg.date         = date;
            msg.type         = Message.TYPE_SENT;
            msg.read         = true;
            msg.systemSmsId  = sysId;
            msg.threadId     = finalThreadId;
            msg.apiProcessed = true; // don't forward outgoing

            messageDao.insert(msg);
            upsertConversation(finalThreadId, address, body, date);
        });
    }

    /**
     * Apply the API result: tag + important flag, optionally delete.
     */
    public void applyApiResult(long messageId, String tag, boolean important, boolean delete) {
        ioExecutor.execute(() -> {
            Message msg = messageDao.getById(messageId);
            if (msg == null) return;

            if (delete) {
                // Delete from our DB
                messageDao.deleteById(messageId);
                // Delete from system store
                deleteFromSystem(msg.systemSmsId);
                // Recalculate conversation snippet
                refreshConversationSnippet(msg.threadId);
            } else {
                messageDao.applyApiResult(messageId, tag, important);
                // Propagate tag to conversation header
                conversationDao.updateTagInfo(msg.threadId, tag, important);
            }
        });
    }

    public void markThreadRead(long threadId) {
        ioExecutor.execute(() -> {
            messageDao.markThreadRead(threadId);
            conversationDao.updateUnreadCount(threadId, 0);
            // Also mark read in system provider
            ContentValues cv = new ContentValues();
            cv.put(Telephony.Sms.READ, 1);
            resolver.update(Telephony.Sms.CONTENT_URI, cv,
                Telephony.Sms.THREAD_ID + " = ?",
                new String[]{String.valueOf(threadId)});
        });
    }

    public void deleteConversation(long threadId) {
        ioExecutor.execute(() -> {
            // Delete all messages from system
            List<Message> msgs = messageDao.getMessagesForThreadSync(threadId);
            for (Message m : msgs) deleteFromSystem(m.systemSmsId);

            // Delete our records
            messageDao.deleteByThreadId(threadId);
            conversationDao.deleteByThreadId(threadId);
        });
    }

    public void deleteMessage(Message msg) {
        ioExecutor.execute(() -> {
            // Remove from our DB
            messageDao.deleteById(msg.id);
            // Remove from system SMS store
            deleteFromSystem(msg.systemSmsId);
            // Update conversation snippet / delete conversation if empty
            refreshConversationSnippet(msg.threadId);
        });
    }

    public List<Message> getUnprocessedMessages() {
        return messageDao.getUnprocessedIncoming();
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private void upsertConversation(long threadId, String address, String snippet, long date) {
        Conversation conv = conversationDao.getByThreadId(threadId);
        if (conv == null) {
            conv = new Conversation();
            conv.threadId    = threadId;
            conv.address     = address;
            conv.displayName = resolveContactName(address);
        }
        conv.snippet      = snippet.length() > 80 ? snippet.substring(0, 80) + "…" : snippet;
        conv.date         = date;
        conv.unreadCount  = messageDao.getUnreadCount(threadId);
        conversationDao.insertOrReplace(conv);
    }

    private void refreshConversationSnippet(long threadId) {
        List<Message> msgs = messageDao.getMessagesForThreadSync(threadId);
        if (msgs.isEmpty()) {
            conversationDao.deleteByThreadId(threadId);
        } else {
            Message last = msgs.get(msgs.size() - 1);
            conversationDao.updateSnippet(threadId, last.body, last.date);
        }
    }

    private void deleteFromSystem(long systemSmsId) {
        if (systemSmsId < 0) return;
        try {
            resolver.delete(Telephony.Sms.CONTENT_URI,
                "_id = ?", new String[]{String.valueOf(systemSmsId)});
        } catch (Exception e) {
            Log.w(TAG, "Could not delete from system store: " + e.getMessage());
        }
    }

    private String resolveContactName(String address) {
        try {
            Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(address));
            Cursor c = resolver.query(uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null, null, null);
            if (c != null && c.moveToFirst()) {
                String name = c.getString(0);
                c.close();
                return name;
            }
            if (c != null) c.close();
        } catch (Exception ignored) {}
        return address;
    }

    /**
     * Sync from system SMS provider on first launch or after being set as default.
     * Safe to call multiple times – skips messages already in our DB.
     * Runs on the ioExecutor - safe to call from any thread.
     *
     * @param onComplete optional callback invoked on the ioExecutor after sync finishes
     */
    public void syncFromSystem(Runnable onComplete) {
        ioExecutor.execute(() -> {
            try {
                Cursor c = resolver.query(Telephony.Sms.CONTENT_URI,
                    new String[]{
                        Telephony.Sms._ID,
                        Telephony.Sms.THREAD_ID,
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE,
                        Telephony.Sms.TYPE,
                        Telephony.Sms.READ
                    },
                    null, null, Telephony.Sms.DATE + " ASC");

                if (c == null) return;

                while (c.moveToNext()) {
                    long sysId    = c.getLong(0);
                    long threadId = c.getLong(1);
                    String addr   = c.getString(2);
                    String body   = c.getString(3);
                    long date     = c.getLong(4);
                    int type      = c.getInt(5);
                    boolean read  = c.getInt(6) == 1;

                    // Skip if already imported
                    if (messageDao.getBySystemId(sysId) != null) continue;

                    Message msg = new Message();
                    msg.systemSmsId  = sysId;
                    msg.threadId     = threadId;
                    msg.address      = addr  != null ? addr  : "";
                    msg.body         = body  != null ? body  : "";
                    msg.date         = date;
                    msg.type         = type;
                    msg.read         = read;
                    msg.apiProcessed = (type != Message.TYPE_INBOX);

                    messageDao.insert(msg);
                    upsertConversation(threadId, msg.address, msg.body, date);
                }
                c.close();
            } catch (Exception e) {
                Log.e(TAG, "syncFromSystem failed", e);
            }

            if (onComplete != null) onComplete.run();
        });
    }
}
