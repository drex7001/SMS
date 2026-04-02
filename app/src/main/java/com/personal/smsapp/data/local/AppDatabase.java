package com.personal.smsapp.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
    entities = {Message.class, Conversation.class, LocalFilter.class},
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract MessageDao messageDao();
    public abstract ConversationDao conversationDao();
    public abstract LocalFilterDao localFilterDao();

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

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "sms_app.db"
                        )
                        .addMigrations(MIGRATION_1_2)
                        .build();
                }
            }
        }
        return INSTANCE;
    }
}
