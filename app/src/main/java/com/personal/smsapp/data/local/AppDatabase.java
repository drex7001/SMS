package com.personal.smsapp.data.local;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * v2: local filters. v3: the Monarra feed (financial_feed). Schemas are exported to app/schemas.
 */
@Database(
    entities = {Message.class, Conversation.class, LocalFilter.class, FeedEntry.class},
    version = 3,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract MessageDao messageDao();
    public abstract ConversationDao conversationDao();
    public abstract LocalFilterDao localFilterDao();
    public abstract FeedDao feedDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `local_filters` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`signal` TEXT NOT NULL, " +
                "`is_regex` INTEGER NOT NULL DEFAULT 0, " +
                "`tag` TEXT NOT NULL, " +
                "`send_to_server` INTEGER NOT NULL DEFAULT 0, " +
                "`enabled` INTEGER NOT NULL DEFAULT 1)"
            );
        }
    };

    /** Adds the Monarra feed. The SQL matches what Room generates for {@link FeedEntry}. */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `financial_feed` (" +
                "`seq` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`message_hash` TEXT NOT NULL, " +
                "`sender` TEXT NOT NULL, " +
                "`body` TEXT, " +
                "`sent_at` INTEGER NOT NULL, " +
                "`received_at` INTEGER NOT NULL, " +
                "`sim_slot` INTEGER NOT NULL, " +
                "`origin` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "`acked_at` INTEGER)"
            );
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_feed_message_hash` "
                + "ON `financial_feed` (`message_hash`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_feed_acked_at` "
                + "ON `financial_feed` (`acked_at`)");
        }
    };

    static final String FILE_NAME = "sms_app.db";

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            FILE_NAME
                        )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .build();
                }
            }
        }
        return INSTANCE;
    }

    /** Tests only: closes the shared instance so the next test starts from a fresh file. */
    @VisibleForTesting
    public static void closeInstance() {
        synchronized (AppDatabase.class) {
            if (INSTANCE != null) {
                INSTANCE.close();
                INSTANCE = null;
            }
        }
    }
}
