package com.shyam026.buddyassistant;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GeminiApiClient {
    public interface Callback {
        void onResult(boolean success, String message);
    }

    private GeminiApiClient() {}

    public static void test(Context context, String key, String model, Callback callback) {
        final String cleanKey = key == null ? "" : key.trim();
        final String cleanModel = model == null || model.trim().isEmpty()
                ? "gemini-3.8-flash"
                : model.trim();

        if (cleanKey.isEmpty()) {
            post(callback, false, "API key empty hai.");
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject body = new JSONObject()
                        .put("contents", new JSONArray()
                                .put(new JSONObject()
                                        .put("parts", new JSONArray()
                                                .put(new JSONObject().put(
                                                        "text",
                                                        "Reply with exactly: Buddy is connected.")))))
                        .put("generationConfig", new JSONObject()
                                .put("temperature", 0.0)
                                .put("maxOutputTokens", 20));

                URL url = new URL(
                        "https://generativelanguage.googleapis.com/v1beta/models/"
                                + Uri.encode(cleanModel) + ":generateContent");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(8000);
                connection.setDoOutput(true);
                connection.setRequestProperty("x-goog-api-key", cleanKey);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");

                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = connection.getResponseCode();
                String response = readBody(connection, code);

                if (code >= 200 && code < 300) {
                    JSONObject root = new JSONObject(response);
                    JSONArray candidates = root.optJSONArray("candidates");
                    if (candidates != null && candidates.length() > 0) {
                        post(callback, true, "Connected ✓  " + cleanModel);
                    } else {
                        post(callback, true, "Gemini responded ✓  " + cleanModel);
                    }
                    return;
                }

                if (code == 400) {
                    post(callback, false, "Gemini rejected the request (400). Model: " + cleanModel);
                } else if (code == 401 || code == 403) {
                    post(callback, false,
                            "API key rejected (HTTP " + code + "). Use a valid Gemini API key/auth key.");
                } else if (code == 404) {
                    post(callback, false, "Model unavailable (404): " + cleanModel);
                } else if (code == 429) {
                    post(callback, false, "Gemini rate limit reached (429).");
                } else {
                    post(callback, false, "Gemini test failed (HTTP " + code + ").");
                }
            } catch (Throwable t) {
                post(callback, false, "Network test failed. Internet/API access check karo.");
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "buddy-gemini-test").start();
    }

    private static String readBody(HttpURLConnection connection, int code) {
        InputStream stream = null;
        try {
            stream = code >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
            if (stream == null) return "";
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            return body.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void post(Callback callback, boolean success, String message) {
        if (callback == null) return;
        new Handler(Looper.getMainLooper()).post(
                () -> callback.onResult(success, message));
    }
}
