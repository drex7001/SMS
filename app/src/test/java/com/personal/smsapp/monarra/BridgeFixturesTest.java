package com.personal.smsapp.monarra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The shared contract fixtures (bridge-fixtures/protocol-1/). Monarra runs the same files, so a
 * change here that isn't mirrored there fails one of the two builds.
 */
public class BridgeFixturesTest {

    @Test
    public void messageHashes() throws Exception {
        JSONArray cases = fixture("message_hash.json").getJSONArray("cases");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            assertEquals(c.toString(), c.getString("hash"),
                    MessageHash.of(c.getString("sender"), c.getLong("sent_at_ms"), c.getString("body")));
        }
    }

    @Test
    public void builtInOtpDeny() throws Exception {
        JSONArray cases = fixture("otp_deny.json").getJSONArray("cases");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            assertEquals(c.getString("note"), c.getBoolean("deny"), MonarraRules.isOneTimeCode(c.getString("body")));
        }
    }

    @Test
    public void validRulesAreAccepted() throws Exception {
        JSONArray cases = fixture("rules_valid.json").getJSONArray("cases");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            MonarraRules rules = rules(c);
            assertEquals(c.getString("name"), strings(c.getJSONArray("senders")), rules.senders);
            assertEquals(c.getLong("rules_version"), rules.version);
        }
    }

    @Test
    public void invalidRulesAreRejectedNamingTheField() throws Exception {
        JSONArray cases = fixture("rules_invalid.json").getJSONArray("cases");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            try {
                rules(c);
                fail(c.getString("name") + " was accepted");
            } catch (MonarraRules.InvalidRulesException e) {
                assertTrue(c.getString("name") + ": " + e.getMessage(),
                        e.getMessage().contains(c.getString("error_field")));
            }
        }
    }

    @Test
    public void captureCases() throws Exception {
        JSONObject fixture = fixture("capture_cases.json");
        MonarraRules rules = rules(fixture.getJSONObject("rules"));
        JSONArray cases = fixture.getJSONArray("cases");
        for (int i = 0; i < cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            assertEquals(c.getString("note"), c.getBoolean("captured"),
                    rules.isFinancial(c.getString("sender"), c.getString("body")));
        }
    }

    @Test
    public void noRulesCaptureNothing() {
        assertEquals(false, MonarraRules.NONE.isFinancial("COMBANK", "LKR 100.00 debited"));
    }

    private static MonarraRules rules(JSONObject c) throws Exception {
        String[] senders = c.isNull("allowed_senders")
                ? null
                : strings(c.getJSONArray("allowed_senders")).toArray(new String[0]);
        return MonarraRules.create(
                c.getLong("rules_version"),
                senders,
                c.isNull("include_filter") ? null : c.getString("include_filter"),
                c.isNull("deny_filter") ? null : c.getString("deny_filter"));
    }

    private static List<String> strings(JSONArray array) throws Exception {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) out.add(array.getString(i));
        return out;
    }

    /** Unit tests run in app/, and the fixtures sit at the repo root. */
    static JSONObject fixture(String name) throws Exception {
        File file = new File("../bridge-fixtures/protocol-1/" + name);
        if (!file.exists()) file = new File("bridge-fixtures/protocol-1/" + name);
        return new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }
}
