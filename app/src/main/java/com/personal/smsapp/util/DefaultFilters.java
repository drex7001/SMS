package com.personal.smsapp.util;

import com.personal.smsapp.data.local.LocalFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Built-in starter rules. Evaluated in order — first match wins.
 *
 * Strategy:
 *  - Sensitive categories (OTP, bank, security) → send_to_server = false (handled locally)
 *  - Informational / actionable categories (delivery, appointment) → send_to_server = true
 *  - Spam / promo → send_to_server = true so the API can auto-delete if desired
 */
public class DefaultFilters {

    public static List<LocalFilter> get() {
        return Arrays.asList(
            rule("OTP",
                "(?i)\\b(otp|one[- ]time password|one[- ]time passcode)\\b",
                true, "otp", false),

            rule("Verification Code",
                "(?i)(verif(y|ication)|confirm(ation)?|authenticat).{0,50}\\b\\d{4,8}\\b",
                true, "otp", false),

            rule("Login Code",
                "(?i)\\b\\d{4,8}\\b.{0,50}(login|sign[- ]in|access code|auth)",
                true, "otp", false),

            rule("Bank Transaction",
                "(?i)(debited|credited|a\\/c|acct|account).{0,80}(rs\\.?|inr|usd|eur|\\$|£|€)?\\s*[\\d,]+",
                true, "bank", false),

            rule("Balance Alert",
                "(?i)(available balance|closing balance|current balance|bal(ance)?\\s*:)",
                true, "bank", false),

            rule("Password Reset",
                "(?i)(reset.{0,30}password|password.{0,30}reset|reset your|login link|sign-in link)",
                true, "security", false),

            rule("Security Alert",
                "(?i)(unusual (activity|sign.in|login)|new (device|login)|security alert|suspicious)",
                true, "security", false),

            rule("Delivery",
                "(?i)(out for delivery|your (order|shipment|package)|tracking (id|no\\.?|number)|dispatched|delivered)",
                true, "delivery", true),

            rule("Appointment Reminder",
                "(?i)(appointment|your booking|scheduled for|reminder:|slot confirmed)",
                true, "reminder", true),

            rule("Promotional",
                "(?i)(\\d+%\\s*off|special offer|limited.time|exclusive deal|reply (stop|end|quit)|unsubscribe)",
                true, "promo", true),

            rule("Spam",
                "(?i)(click here to claim|you('ve| have) won|free prize|congratulations.{0,20}won|winner selected)",
                true, "spam", true)
        );
    }

    private static LocalFilter rule(String name, String signal,
                                    boolean isRegex, String tag, boolean sendToServer) {
        LocalFilter f = new LocalFilter();
        f.name         = name;
        f.signal       = signal;
        f.isRegex      = isRegex;
        f.tag          = tag;
        f.sendToServer = sendToServer;
        f.enabled      = true;
        return f;
    }
}
