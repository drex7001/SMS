package com.personal.smsapp.data.remote;

import android.util.Log;

import com.personal.smsapp.data.local.LocalFilter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends incoming messages to your API and parses the response.
 *
 * Expected API response JSON shape:
 * {
 *   "action": "keep" | "delete",
 *   "tag":    "otp" | "promo" | "personal" | "bank" | ... (any string),
 *   "important": true | false
 * }
 *
 * Configure your endpoint in app Settings.
 */
public class ApiService {

    private static final String TAG = "ApiService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String apiUrl;
    private final String apiKey;

    public ApiService(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;

        this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    public ApiResponse forwardMessage(long messageId, String sender, String body, long timestamp) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("message_id", messageId);
            payload.put("sender", sender);
            payload.put("body", body);
            payload.put("timestamp", timestamp);

            RequestBody requestBody = RequestBody.create(payload.toString(), JSON);

            Request.Builder builder = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .header("Content-Type", "application/json");

            if (apiKey != null && !apiKey.isEmpty()) {
                builder.header("X-API-Key", apiKey);
            }

            try (Response response = client.newCall(builder.build()).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "API returned non-success: " + response.code());
                    return ApiResponse.defaultKeep();
                }

                String responseBody = response.body().string();
                return parseResponse(responseBody);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to forward message to API", e);
            return ApiResponse.defaultKeep();
        }
    }

    private ApiResponse parseResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String action    = obj.optString("action", "keep");
            String tag       = obj.optString("tag", "");
            boolean important = obj.optBoolean("important", false);
            return new ApiResponse(action, tag, important);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse API response: " + json, e);
            return ApiResponse.defaultKeep();
        }
    }

    // ── Response POJO ──────────────────────────────────────────────────────

    public static class ApiResponse {
        public final String action;     // "keep" or "delete"
        public final String tag;        // arbitrary tag string
        public final boolean important;

        public ApiResponse(String action, String tag, boolean important) {
            this.action    = action;
            this.tag       = tag;
            this.important = important;
        }

        public boolean shouldDelete() {
            return "delete".equalsIgnoreCase(action);
        }

        public static ApiResponse defaultKeep() {
            return new ApiResponse("keep", "", false);
        }
    }

    // ── Filter backup / restore ────────────────────────────────────────────

    /**
     * Derives the /filters endpoint from the configured API URL.
     * e.g. "http://192.168.1.10:5000/sms-webhook" → "http://192.168.1.10:5000/filters"
     */
    public static String deriveFiltersUrl(String apiUrl) {
        if (apiUrl == null || apiUrl.isEmpty()) return "";
        try {
            java.net.URL url = new java.net.URL(apiUrl);
            String base = url.getProtocol() + "://" + url.getAuthority();
            return base + "/filters";
        } catch (Exception e) {
            // Fallback: strip last path segment
            int lastSlash = apiUrl.lastIndexOf('/');
            String base = lastSlash > 8 ? apiUrl.substring(0, lastSlash) : apiUrl;
            return base + "/filters";
        }
    }

    /**
     * POST all filters as JSON to {base}/filters.
     * Returns true on success.
     */
    public boolean backupFilters(List<LocalFilter> filters) {
        String filtersUrl = deriveFiltersUrl(apiUrl);
        if (filtersUrl.isEmpty()) return false;
        try {
            JSONArray arr = new JSONArray();
            for (LocalFilter f : filters) {
                JSONObject obj = new JSONObject();
                obj.put("name",           f.name);
                obj.put("signal",         f.signal);
                obj.put("is_regex",       f.isRegex);
                obj.put("tag",            f.tag);
                obj.put("send_to_server", f.sendToServer);
                obj.put("enabled",        f.enabled);
                arr.put(obj);
            }
            JSONObject payload = new JSONObject();
            payload.put("filters", arr);

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request.Builder builder = new Request.Builder()
                .url(filtersUrl)
                .post(body)
                .header("Content-Type", "application/json");
            if (apiKey != null && !apiKey.isEmpty()) {
                builder.header("X-API-Key", apiKey);
            }
            try (Response response = client.newCall(builder.build()).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e(TAG, "backupFilters failed", e);
            return false;
        }
    }

    /**
     * GET {base}/filters and parse the response into a list of LocalFilter objects.
     * Returns null on failure.
     */
    public List<LocalFilter> restoreFilters() {
        String filtersUrl = deriveFiltersUrl(apiUrl);
        if (filtersUrl.isEmpty()) return null;
        try {
            Request.Builder builder = new Request.Builder().url(filtersUrl).get();
            if (apiKey != null && !apiKey.isEmpty()) {
                builder.header("X-API-Key", apiKey);
            }
            try (Response response = client.newCall(builder.build()).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                String json = response.body().string();
                JSONObject root = new JSONObject(json);
                JSONArray arr = root.getJSONArray("filters");
                List<LocalFilter> result = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    LocalFilter f = new LocalFilter();
                    f.name         = obj.optString("name", "");
                    f.signal       = obj.optString("signal", "");
                    f.isRegex      = obj.optBoolean("is_regex", false);
                    f.tag          = obj.optString("tag", "");
                    f.sendToServer = obj.optBoolean("send_to_server", false);
                    f.enabled      = obj.optBoolean("enabled", true);
                    result.add(f);
                }
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "restoreFilters failed", e);
            return null;
        }
    }
}
