package com.personal.smsapp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.personal.smsapp.data.local.LocalFilter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LocalFilterHelperTest {

    private static final List<LocalFilter> DEFAULTS = DefaultFilters.get();

    @Test
    public void otpIsTaggedAndKeptLocal() {
        LocalFilterHelper.FilterResult result =
                LocalFilterHelper.classify("Your OTP is 482913. Do not share it.", DEFAULTS);

        assertTrue(result.matched);
        assertEquals("otp", result.tag);
        assertFalse(result.sendToServer);
    }

    @Test
    public void bankTransactionIsTaggedAndKeptLocal() {
        LocalFilterHelper.FilterResult result =
                LocalFilterHelper.classify("Your A/C 1234 debited Rs. 1,250.00 at SUPERMARKET", DEFAULTS);

        assertTrue(result.matched);
        assertEquals("bank", result.tag);
        assertFalse(result.sendToServer);
    }

    @Test
    public void promoIsSentToServer() {
        LocalFilterHelper.FilterResult result =
                LocalFilterHelper.classify("Weekend sale: 50% off everything!", DEFAULTS);

        assertEquals("promo", result.tag);
        assertTrue(result.sendToServer);
    }

    @Test
    public void unmatchedMessageFallsThroughToServer() {
        LocalFilterHelper.FilterResult result =
                LocalFilterHelper.classify("Are we still meeting tomorrow?", DEFAULTS);

        assertFalse(result.matched);
        assertTrue(result.sendToServer);
    }

    @Test
    public void invalidRegexRuleIsSkippedInsteadOfCrashing() {
        LocalFilter broken = new LocalFilter();
        broken.signal = "(unclosed";
        broken.isRegex = true;
        broken.tag = "broken";

        LocalFilter keyword = new LocalFilter();
        keyword.signal = "parcel";
        keyword.tag = "delivery";
        keyword.sendToServer = true;

        LocalFilterHelper.FilterResult result =
                LocalFilterHelper.classify("Your PARCEL is on the way", Arrays.asList(broken, keyword));

        assertEquals("delivery", result.tag);
    }

    @Test
    public void emptyRulesMatchNothing() {
        assertFalse(LocalFilterHelper.classify("anything", Collections.emptyList()).matched);
    }
}
