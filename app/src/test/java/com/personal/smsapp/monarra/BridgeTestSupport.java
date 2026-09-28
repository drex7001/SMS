package com.personal.smsapp.monarra;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Helpers for the Robolectric bridge tests. */
final class BridgeTestSupport {

    /**
     * Runs {@code work} off the main thread, as the binder threads do on a phone (Room refuses the
     * main thread). Rethrows what it threw.
     */
    static <T> T onBinderThread(Callable<T> work) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            return executor.submit(work).get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw e;
        } finally {
            executor.shutdown();
        }
    }

    /** Stands in for the phone's SMS inbox (content://sms) in backfill tests. */
    public static class FakeSmsProvider extends ContentProvider {
        static final List<Object[]> ROWS = new ArrayList<>();

        static void add(String sender, String body, long date, long dateSent) {
            ROWS.add(new Object[]{sender, body, date, dateSent});
        }

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                            String sortOrder) {
            MatrixCursor cursor = new MatrixCursor(new String[]{
                Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.DATE_SENT
            });
            for (Object[] row : ROWS) cursor.addRow(row);
            return cursor;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            throw new UnsupportedOperationException();
        }
    }

    private BridgeTestSupport() {}
}
