package com.personal.smsapp.data.remote;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
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
}
