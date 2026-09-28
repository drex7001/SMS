package com.personal.smsapp.monarra;

import static com.personal.smsapp.monarra.BridgeTestSupport.onBinderThread;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.work.Configuration;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.personal.smsapp.BuildConfig;
import com.personal.smsapp.data.local.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The provider side of the bridge contract. Plain Application: the real one opens encrypted
 * preferences through the Android Keystore, which Robolectric doesn't have.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class BridgeProviderTest {

    private static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".monarrabridge";
    private static final Uri    FEED      = Uri.parse("content://" + AUTHORITY + "/feed");
    private static final long   SENT_AT   = 1_790_000_000_000L;

    private Application    app;
    private BridgeProvider provider;

    @Before
    public void setUp() {
        app = RuntimeEnvironment.getApplication();
        shadowOf(app).grantPermissions(BuildConfig.BRIDGE_PERMISSION);
        WorkManagerTestInitHelper.initializeTestWorkManager(app,
                new Configuration.Builder()
                        .setExecutor(new SynchronousExecutor())
                        .setTaskExecutor(new SynchronousExecutor())
                        .build());
        provider = Robolectric.setupContentProvider(BridgeProvider.class, AUTHORITY);
        BridgeTestSupport.FakeSmsProvider.ROWS.clear();
    }

    @After
    public void tearDown() {
        AppDatabase.closeInstance();
    }

    @Test
    public void everyEntryPointNeedsTheBridgePermission() throws Exception {
        shadowOf(app).denyPermissions(BuildConfig.BRIDGE_PERMISSION);
        try {
            onBinderThread(() -> provider.call(BridgeContract.Methods.HELLO, null, new Bundle()));
            fail("call() without the permission");
        } catch (SecurityException expected) {
            // ok
        }
        try {
            onBinderThread(() -> provider.query(FEED, null, null, null, null));
            fail("query() without the permission");
        } catch (SecurityException expected) {
            // ok
        }
    }

    @Test
    public void helloReportsProtocolRulesAndHead() throws Exception {
        Bundle hello = call(BridgeContract.Methods.HELLO, new Bundle());
        assertEquals(BridgeContract.PROTOCOL, hello.getInt(BridgeContract.Extras.PROTOCOL));
        assertEquals(0, hello.getLong(BridgeContract.Extras.RULES_VERSION));
        assertEquals(0, hello.getLong(BridgeContract.Extras.FEED_HEAD_SEQ));
        assertFalse(hello.getBoolean(BridgeContract.Extras.IS_DEFAULT_SMS_APP));
        assertNotNull(hello.getString(BridgeContract.Extras.APP_VERSION));

        pushRules(5);
        capture("COMBANK", "LKR 100.00 debited from A/C XX1234", SENT_AT);
        hello = call(BridgeContract.Methods.HELLO, new Bundle());
        assertEquals(5, hello.getLong(BridgeContract.Extras.RULES_VERSION));
        assertEquals(1, hello.getLong(BridgeContract.Extras.FEED_HEAD_SEQ));
    }

    @Test
    public void aBadRulesPushIsRejectedAndTheOldRulesStay() throws Exception {
        assertTrue(pushRules(3).getBoolean(BridgeContract.Extras.OK));

        Bundle bad = rulesBundle(4);
        bad.putString(BridgeContract.Extras.INCLUDE_FILTER, "(?i)(debited");
        Bundle result = call(BridgeContract.Methods.SET_RULES, bad);
        assertFalse(result.getBoolean(BridgeContract.Extras.OK));
        assertTrue(result.getString(BridgeContract.Extras.ERROR).contains("include_filter"));

        assertEquals(3, call(BridgeContract.Methods.HELLO, new Bundle()).getLong(BridgeContract.Extras.RULES_VERSION));
        assertEquals(1, capture("COMBANK", "LKR 100.00 debited", SENT_AT));
    }

    @Test
    public void onlyFinancialSmsFromAllowedSendersEnterTheFeed() throws Exception {
        pushRules(1);
        assertEquals(1, capture("COMBANK", "LKR 4,500.00 debited from A/C XX1234", SENT_AT));
        assertEquals(-1, capture("COMBANK", "LKR 4,500.00 debited from A/C XX1234", SENT_AT)); // duplicate
        assertEquals(-1, capture("COMBANK", "Your OTP is 482913 for a payment debited to your card", SENT_AT + 1));
        assertEquals(-1, capture("SAMPATH", "LKR 1,000.00 debited", SENT_AT + 2));
        assertEquals(-1, capture("HNB", "Your statement is ready", SENT_AT + 3));
        assertEquals(1, feed(0, 200).size());
    }

    @Test
    public void feedPagesByCursorInSeqOrderWithContractColumns() throws Exception {
        pushRules(1);
        long first = capture("COMBANK", "LKR 1.00 debited", SENT_AT);
        long second = capture("HNB", "LKR 2.00 credited", SENT_AT + 1);
        long third = capture("combank ", "LKR 3.00 debited", SENT_AT + 2);

        try (Cursor c = onBinderThread(() -> provider.query(FEED, null, null, null, null))) {
            assertArrayEquals(new String[]{
                BridgeContract.Columns.SEQ, BridgeContract.Columns.MESSAGE_HASH, BridgeContract.Columns.SENDER,
                BridgeContract.Columns.BODY, BridgeContract.Columns.SENT_AT_MS, BridgeContract.Columns.RECEIVED_AT_MS,
                BridgeContract.Columns.SIM_SLOT, BridgeContract.Columns.ORIGIN,
            }, c.getColumnNames());
            assertTrue(c.moveToFirst());
            assertEquals(first, c.getLong(c.getColumnIndexOrThrow(BridgeContract.Columns.SEQ)));
            assertEquals(MessageHash.of("COMBANK", SENT_AT, "LKR 1.00 debited"),
                    c.getString(c.getColumnIndexOrThrow(BridgeContract.Columns.MESSAGE_HASH)));
            assertEquals(SENT_AT, c.getLong(c.getColumnIndexOrThrow(BridgeContract.Columns.SENT_AT_MS)));
            assertEquals(BridgeContract.ORIGIN_LIVE, c.getString(c.getColumnIndexOrThrow(BridgeContract.Columns.ORIGIN)));
        }
        assertEquals(Arrays.asList(second, third), feed(first, 200));
        assertEquals(Arrays.asList(first), feed(0, 1));
        try {
            feed(0, BridgeContract.MAX_LIMIT + 1);
            fail("limit above the maximum");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void acksAreMonotonicAndCountPending() throws Exception {
        pushRules(1);
        long first = capture("COMBANK", "LKR 1.00 debited", SENT_AT);
        long second = capture("COMBANK", "LKR 2.00 debited", SENT_AT + 1);
        capture("COMBANK", "LKR 3.00 debited", SENT_AT + 2);
        assertEquals(3, status().getLong(BridgeContract.Extras.PENDING_COUNT));

        ack(second);
        assertEquals(1, status().getLong(BridgeContract.Extras.PENDING_COUNT));
        ack(first); // older ack: ignored
        assertEquals(1, status().getLong(BridgeContract.Extras.PENDING_COUNT));
        assertEquals(3, feed(0, 200).size()); // acknowledged rows stay until retention removes them
    }

    @Test
    public void liveCaptureNudgesMonarra() throws Exception {
        pushRules(1);
        onBinderThread(() -> {
            MonarraFeed.captureLive(app, "COMBANK", "LKR 9.00 debited", SENT_AT, 0);
            return null;
        });
        List<Intent> nudges = new ArrayList<>();
        for (Intent intent : shadowOf(app).getBroadcastIntents()) {
            if (BridgeContract.ACTION_FEED_UPDATED.equals(intent.getAction())) nudges.add(intent);
        }
        assertEquals(1, nudges.size());
        assertEquals(BuildConfig.MONARRA_PACKAGE, nudges.get(0).getPackage());
        assertEquals(1, nudges.get(0).getLongExtra(BridgeContract.Extras.FEED_HEAD_SEQ, 0));
        assertTrue(status().getLong(BridgeContract.Extras.LAST_LIVE_AT_MS) > 0);
    }

    @Test
    public void backfillNeedsRulesAndAddsHistoryOnce() throws Exception {
        Bundle days = new Bundle();
        days.putInt(BridgeContract.Extras.DAYS, 180);
        Bundle noRules = call(BridgeContract.Methods.BACKFILL, days);
        assertFalse(noRules.getBoolean(BridgeContract.Extras.STARTED));
        assertNotNull(noRules.getString(BridgeContract.Extras.ERROR));

        pushRules(1);
        capture("COMBANK", "LKR 1.00 debited", SENT_AT); // already in the feed from live capture
        Robolectric.setupContentProvider(BridgeTestSupport.FakeSmsProvider.class, "sms");
        BridgeTestSupport.FakeSmsProvider.add("COMBANK", "LKR 1.00 debited", SENT_AT + 5_000, SENT_AT);
        BridgeTestSupport.FakeSmsProvider.add("HNB", "LKR 2.00 credited", SENT_AT + 9_000, 0); // no DATE_SENT
        BridgeTestSupport.FakeSmsProvider.add("HNB", "Your OTP is 551122", SENT_AT + 10_000, SENT_AT + 10_000);
        BridgeTestSupport.FakeSmsProvider.add("MOM", "Call me", SENT_AT + 11_000, SENT_AT + 11_000);

        assertTrue(call(BridgeContract.Methods.BACKFILL, days).getBoolean(BridgeContract.Extras.STARTED));

        Bundle status = status();
        for (int i = 0; i < 50 && BridgeContract.BACKFILL_RUNNING.equals(
                status.getString(BridgeContract.Extras.BACKFILL_STATE)); i++) {
            Thread.sleep(100);
            status = status();
        }
        assertEquals(BridgeContract.BACKFILL_DONE, status.getString(BridgeContract.Extras.BACKFILL_STATE));
        assertEquals(4, status.getLong(BridgeContract.Extras.BACKFILL_SCANNED));
        assertEquals(2, status.getLong(BridgeContract.Extras.BACKFILL_MATCHED));
        assertEquals(2, feed(0, 200).size());
        try (Cursor c = onBinderThread(() -> provider.query(FEED.buildUpon()
                .appendQueryParameter(BridgeContract.QUERY_AFTER, "1").build(), null, null, null, null))) {
            assertTrue(c.moveToFirst());
            assertEquals(BridgeContract.ORIGIN_BACKFILL, c.getString(c.getColumnIndexOrThrow(BridgeContract.Columns.ORIGIN)));
            assertEquals(SENT_AT + 9_000, c.getLong(c.getColumnIndexOrThrow(BridgeContract.Columns.SENT_AT_MS)));
        }
    }

    @Test
    public void badArgumentsAndUnknownMethodsAreRefused() throws Exception {
        Bundle days = new Bundle();
        days.putInt(BridgeContract.Extras.DAYS, 0);
        expectIllegalArgument(BridgeContract.Methods.BACKFILL, days);
        expectIllegalArgument(BridgeContract.Methods.ACK, new Bundle());
        expectIllegalArgument("dropTables", new Bundle());
        try {
            provider.insert(FEED, new ContentValues());
            fail("insert() is not allowed");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void listSendersSkipsPersonalNumbers() {
        assertTrue(BridgeProvider.isServiceSender("COMBANK"));
        assertTrue(BridgeProvider.isServiceSender("HNB-ALERTS"));
        assertTrue(BridgeProvider.isServiceSender("8888"));
        assertFalse(BridgeProvider.isServiceSender("+94771234567"));
        assertFalse(BridgeProvider.isServiceSender("0771234567"));
        assertFalse(BridgeProvider.isServiceSender(" "));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Bundle call(String method, Bundle extras) throws Exception {
        return onBinderThread(() -> provider.call(method, null, extras));
    }

    private Bundle rulesBundle(long version) {
        Bundle rules = new Bundle();
        rules.putLong(BridgeContract.Extras.RULES_VERSION, version);
        rules.putStringArray(BridgeContract.Extras.ALLOWED_SENDERS, new String[]{"COMBANK", "HNB"});
        rules.putString(BridgeContract.Extras.INCLUDE_FILTER, "(?i)\\b(debited|credited)\\b");
        return rules;
    }

    private Bundle pushRules(long version) throws Exception {
        return call(BridgeContract.Methods.SET_RULES, rulesBundle(version));
    }

    private long capture(String sender, String body, long sentAt) throws Exception {
        return onBinderThread(() -> MonarraFeed.capture(app, BridgeStore.get(app).rules(), sender, body,
                sentAt, sentAt + 1_000, -1, BridgeContract.ORIGIN_LIVE));
    }

    private void ack(long upTo) throws Exception {
        Bundle extras = new Bundle();
        extras.putLong(BridgeContract.Extras.UP_TO_SEQ, upTo);
        assertTrue(call(BridgeContract.Methods.ACK, extras).getBoolean(BridgeContract.Extras.OK));
    }

    private Bundle status() throws Exception {
        return call(BridgeContract.Methods.STATUS, new Bundle());
    }

    private List<Long> feed(long after, int limit) throws Exception {
        Uri uri = FEED.buildUpon()
                .appendQueryParameter(BridgeContract.QUERY_AFTER, String.valueOf(after))
                .appendQueryParameter(BridgeContract.QUERY_LIMIT, String.valueOf(limit))
                .build();
        List<Long> seqs = new ArrayList<>();
        try (Cursor c = onBinderThread(() -> provider.query(uri, null, null, null, null))) {
            while (c.moveToNext()) seqs.add(c.getLong(0));
        }
        return seqs;
    }

    private void expectIllegalArgument(String method, Bundle extras) throws Exception {
        try {
            call(method, extras);
            fail(method + " should have been refused");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
