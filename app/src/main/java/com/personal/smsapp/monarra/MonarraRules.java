package com.personal.smsapp.monarra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Which SMS go to Monarra. Monarra pushes these rules (senders plus include and deny patterns);
 * the patterns are compiled once, when the rules arrive, not for every message.
 *
 * A message is financial when, in this order:
 *   1. it doesn't look like a one-time code (built in, can't be switched off),
 *   2. its sender is one of the allowed senders (trimmed, upper-cased, exact match),
 *   3. its body matches the include pattern,
 *   4. and doesn't match the deny pattern.
 * With no rules pushed yet ({@link #NONE}), nothing is financial.
 */
public final class MonarraRules {

    public static final int MAX_PATTERN_LENGTH = 1000;
    public static final int MAX_SENDERS        = 200;
    public static final int MAX_SENDER_LENGTH  = 64;

    /**
     * A one-time code: an OTP keyword right next to a 4–8 digit code, or "… is 123456" a little
     * further on. Amounts (12345.00) and phone numbers in groups (0112 303 050) aren't codes, so
     * alerts ending "Never share your OTP. Call: 0112 303 050" still get through. Kept in step with
     * bridge-fixtures/protocol-1/otp_deny.json.
     */
    public static final String BUILT_IN_OTP_REGEX =
            "(?i)"
            // "OTP 123456", "one-time password: 123456"
            + "(\\b(otp|one[- ]?time\\s+(password|passcode|pin|code)|verification\\s+code|security\\s+code)\\b"
            + "[^0-9]{0,12}\\d{4,8}\\b(?![.,]\\d|[ -]\\d))"
            // "123456 is your OTP"
            + "|(\\b\\d{4,8}\\b(?![.,]\\d|[ -]\\d)[^0-9]{0,12}\\b(is|as)\\s+(your|the)\\s+"
            + "(otp|one[- ]?time|verification\\s+code|security\\s+code)\\b)"
            // "OTP for your txn of LKR 5,000.00 is 123456"
            + "|(\\b(otp|one[- ]?time\\s+(password|passcode|pin|code)|verification\\s+code|security\\s+code)\\b"
            + "[^\\n]{0,60}?(\\bis|:)\\s*\\d{4,8}\\b(?![.,]\\d|[ -]\\d))";

    private static final Pattern BUILT_IN_OTP = Pattern.compile(BUILT_IN_OTP_REGEX);

    /** No rules yet: nothing is captured. */
    public static final MonarraRules NONE =
            new MonarraRules(0, Collections.emptyList(), "", null, null, null);

    public final long version;
    /** Normalized (trimmed, upper-cased), in the order Monarra sent them. */
    public final List<String> senders;
    public final String includeFilter;
    public final String denyFilter;

    private final Set<String> senderSet;
    private final Pattern     include;
    private final Pattern     deny;

    private MonarraRules(long version, List<String> senders, String includeFilter, String denyFilter,
                         Pattern include, Pattern deny) {
        this.version       = version;
        this.senders       = Collections.unmodifiableList(senders);
        this.senderSet     = new LinkedHashSet<>(senders);
        this.includeFilter = includeFilter;
        this.denyFilter    = denyFilter;
        this.include       = include;
        this.deny          = deny;
    }

    /**
     * Checks and compiles a rules push. Throws with a message naming the bad field; the caller keeps
     * the previous rules in that case.
     */
    public static MonarraRules create(long version, String[] allowedSenders, String includeFilter,
                                      String denyFilter) throws InvalidRulesException {
        if (version <= 0) throw new InvalidRulesException("rules_version must be positive");
        if (allowedSenders == null) throw new InvalidRulesException("allowed_senders is missing");
        if (allowedSenders.length > MAX_SENDERS) {
            throw new InvalidRulesException("allowed_senders has more than " + MAX_SENDERS + " entries");
        }
        List<String> senders = new ArrayList<>();
        for (String sender : allowedSenders) {
            String normalized = sender == null ? "" : normalizeSender(sender);
            if (normalized.isEmpty() || normalized.length() > MAX_SENDER_LENGTH) {
                throw new InvalidRulesException("allowed_senders has an empty or too long entry");
            }
            if (!senders.contains(normalized)) senders.add(normalized);
        }
        if (includeFilter == null || includeFilter.trim().isEmpty()) {
            throw new InvalidRulesException("include_filter is missing");
        }
        Pattern include = compile("include_filter", includeFilter);
        String deny = denyFilter == null || denyFilter.trim().isEmpty() ? null : denyFilter;
        Pattern denyPattern = deny == null ? null : compile("deny_filter", deny);
        return new MonarraRules(version, senders, includeFilter, deny, include, denyPattern);
    }

    public boolean isFinancial(String sender, String body) {
        if (include == null || sender == null || body == null) return false;
        if (isOneTimeCode(body)) return false;
        if (!senderSet.contains(normalizeSender(sender))) return false;
        if (!include.matcher(body).find()) return false;
        return deny == null || !deny.matcher(body).find();
    }

    public static boolean isOneTimeCode(String body) {
        return body != null && BUILT_IN_OTP.matcher(body).find();
    }

    public static String normalizeSender(String sender) {
        return sender.trim().toUpperCase(Locale.ROOT);
    }

    private static Pattern compile(String field, String pattern) throws InvalidRulesException {
        if (pattern.length() > MAX_PATTERN_LENGTH) {
            throw new InvalidRulesException(field + " is longer than " + MAX_PATTERN_LENGTH + " characters");
        }
        try {
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new InvalidRulesException(field + " doesn't compile: " + e.getDescription());
        }
    }

    /** A rules push that can't be used. The message names the field. */
    public static final class InvalidRulesException extends Exception {
        public InvalidRulesException(String message) {
            super(message);
        }
    }
}
