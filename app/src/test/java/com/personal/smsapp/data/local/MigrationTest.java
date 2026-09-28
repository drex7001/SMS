package com.personal.smsapp.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * v2 → v3 adds the Monarra feed. The v2 database is made by taking a v3 one back to v2 (dropping the
 * feed), so the other tables are exactly what Room creates; Room then runs MIGRATION_2_3 and checks
 * the result against the entities, failing if the migration's SQL doesn't match FeedEntry.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class MigrationTest {

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @After
    public void tearDown() {
        AppDatabase.closeInstance();
        io.shutdown();
    }

    @Test
    public void migrate2To3KeepsMessagesAndAddsTheFeed() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        long messageId = onIo(() -> {
            Message message = new Message();
            message.address = "COMBANK";
            message.body = "LKR 100.00 debited";
            message.date = 1_790_000_000_000L;
            return AppDatabase.getInstance(app).messageDao().insert(message);
        });
        AppDatabase.closeInstance();

        File file = app.getDatabasePath(AppDatabase.FILE_NAME);
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
            db.execSQL("DROP TABLE financial_feed");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'financial_feed'");
            db.setVersion(2);
        }

        Message kept = onIo(() -> AppDatabase.getInstance(app).messageDao().getById(messageId));
        assertNotNull(kept);
        assertEquals("LKR 100.00 debited", kept.body);

        long seq = onIo(() -> {
            FeedEntry entry = new FeedEntry();
            entry.messageHash = "hash";
            entry.sender = "COMBANK";
            entry.body = "LKR 100.00 debited";
            return AppDatabase.getInstance(app).feedDao().insert(entry);
        });
        assertTrue(seq > 0);
        assertEquals(seq, (long) onIo(() -> AppDatabase.getInstance(app).feedDao().headSeq()));
    }

    private <T> T onIo(java.util.concurrent.Callable<T> work) throws Exception {
        Future<T> result = io.submit(work);
        return result.get();
    }
}
