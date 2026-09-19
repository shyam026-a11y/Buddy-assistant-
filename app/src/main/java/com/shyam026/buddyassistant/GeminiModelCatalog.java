package com.shyam026.buddyassistant;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GeminiModelCatalog {
    public interface Callback {
        void onResult(List<String> models, String error);
    }

    private GeminiModelCatalog() {}

    public static List<String> defaults() {
        ArrayList<String> models = new ArrayList<>();
        models.add("gemini-2.5-pro");
        models.add("gemini-2.5-flash");
        models.add("gemini-2.5-flash-lite");
        models.add("gemini-3.1-pro-preview");
        models.add("gemini-3-flash-preview");
        models.add("gemini-3.8-flash");
        models.add("gemini-3.7-flash");
        models.add("gemini-3.6-flash");
        models.add("gemini-3.5-flash");
        models.add("gemini-3.5-flash-lite");
        models.add("gemini-3.1-flash-lite");
        return models;
    }

    public static void fetchAvailable(Context context, Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String key = BuddySecrets.getGeminiApiKey(context).trim();
                if (key.isEmpty()) {
                    post(callback, defaults(), "Gemini API key add nahi hai. Showing built-in models.");
                    return;
                }

                connection = (HttpURLConnection) new URL(
                        "https://generativelanguage.googleapis.com/v1beta/openai/models")
                        .openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(2500);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Authorization", "Bearer " + key);
                connection.setRequestProperty("Accept", "application/json");

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    post(callback, defaults(), "Google model list unavailable. Showing built-in models.");
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);

                JSONObject root = new JSONObject(body.toString());
                JSONArray data = root.optJSONArray("data");
                Set<String> result = new LinkedHashSet<>();

                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.optJSONObject(i);
                        if (item == null) continue;

                        String id = item.optString("id", "").trim();
                        String lowerId = id.toLowerCase(Locale.ROOT);
                        if (!lowerId.startsWith("gemini-")) continue;

                        // Buddy currently uses generateContent text requests, so exclude
                        // Live, image, TTS and transcription-only model families.
                        if (lowerId.contains("-live")
                                || lowerId.contains("-tts")
                                || lowerId.contains("-image")
                                || lowerId.contains("-transcribe")
                                || lowerId.contains("-translate")) continue;

                        JSONArray methods = item.optJSONArray("supportedGenerationMethods");
                        boolean generateContent = methods == null;
                        if (methods != null) {
                            for (int j = 0; j < methods.length(); j++) {
                                if ("generateContent".equalsIgnoreCase(methods.optString(j))) {
                                    generateContent = true;
                                    break;
                                }
                            }
                        }

                        if (generateContent) result.add(id);
                    }
                }

                if (result.isEmpty()) {
                    post(callback, defaults(), "No compatible Gemini models returned. Showing built-in models.");
                    return;
                }

                ArrayList<String> models = new ArrayList<>(result);
                Collections.sort(models, String.CASE_INSENSITIVE_ORDER);
                post(callback, models, null);
            } catch (Throwable t) {
                post(callback, defaults(), "Could not refresh Gemini models. Showing built-in models.");
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "buddy-gemini-models");
    }

    private static void post(Callback callback, List<String> models, String error) {
        if (callback == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(
                () -> callback.onResult(models, error));
    }
}
