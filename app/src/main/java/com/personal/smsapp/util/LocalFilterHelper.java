package com.personal.smsapp.util;

import com.personal.smsapp.data.local.LocalFilter;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Classifies an incoming message body against the user's saved LocalFilter rules.
 * Rules are evaluated in insertion order; first match wins.
 */
public class LocalFilterHelper {

    public static final class FilterResult {
        public final boolean matched;
        public final String  tag;
        public final boolean sendToServer;

        private FilterResult(boolean matched, String tag, boolean sendToServer) {
            this.matched      = matched;
            this.tag          = tag;
            this.sendToServer = sendToServer;
        }
    }

    private static final FilterResult NO_MATCH = new FilterResult(false, "", true);

    /**
     * Check body against each enabled filter in order.
     * Returns the first matching rule's result, or NO_MATCH.
     *
     * @param body    message body
     * @param filters enabled filters from DB (call SmsRepository.getEnabledFiltersSync())
     */
    public static FilterResult classify(String body, List<LocalFilter> filters) {
        if (body == null || filters == null || filters.isEmpty()) return NO_MATCH;

        String lowerBody = body.toLowerCase();

        for (LocalFilter f : filters) {
            if (f.signal == null || f.signal.isEmpty()) continue;

            boolean matches;
            if (f.isRegex) {
                try {
                    matches = Pattern.compile(f.signal,
                            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                            .matcher(body)
                            .find();
                } catch (PatternSyntaxException e) {
                    // Invalid regex — skip this rule rather than crash
                    matches = false;
                }
            } else {
                matches = lowerBody.contains(f.signal.toLowerCase());
            }

            if (matches) {
                return new FilterResult(true, f.tag != null ? f.tag : "", f.sendToServer);
            }
        }
        return NO_MATCH;
    }
}
