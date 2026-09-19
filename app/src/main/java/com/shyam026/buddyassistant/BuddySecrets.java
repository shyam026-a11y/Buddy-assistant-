package com.shyam026.buddyassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class BuddySecrets {
    private static final String KEY_ALIAS = "BuddySecretsKeyV2";
    private static final String PREF = "buddy_secure";
    private static final String API_KEY = "gemini_api_key";
    private static final String API_KEY_FALLBACK = "gemini_api_key_fallback";
    private static final String MODEL = "gemini_model";

    private BuddySecrets() {}

    public static boolean saveGeminiApiKey(Context context, String key) {
        String clean = key == null ? "" : key.trim();
        if (clean.isEmpty()) {
            return hasGeminiApiKey(context);
        }

        if (saveEncrypted(context, API_KEY, clean)) {
            getPrefs(context).edit().remove(API_KEY_FALLBACK).apply();
            return true;
        }

        // Some vendor/ROM Keystore implementations can reject AES-GCM at runtime.
        // Keep Buddy functional by falling back to app-private storage; backups are disabled.
        boolean fallback = getPrefs(context).edit()
                .putString(API_KEY_FALLBACK, Base64.encodeToString(
                        clean.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP))
                .commit();

        return fallback && clean.equals(getGeminiApiKey(context));
    }

    public static String getGeminiApiKey(Context context) {
        String secure = readEncrypted(context, API_KEY);
        if (!secure.isEmpty()) return secure;

        String fallback = getPrefs(context).getString(API_KEY_FALLBACK, "");
        if (fallback == null || fallback.isEmpty()) return "";

        try {
            return new String(
                    Base64.decode(fallback, Base64.NO_WRAP),
                    StandardCharsets.UTF_8).trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static boolean hasGeminiApiKey(Context context) {
        return !getGeminiApiKey(context).trim().isEmpty();
    }

    public static String getStorageStatus(Context context) {
        if (!hasGeminiApiKey(context)) return "No Gemini key saved.";
        String fallback = getPrefs(context).getString(API_KEY_FALLBACK, "");
        return fallback == null || fallback.isEmpty()
                ? "Stored with Android Keystore."
                : "Stored in app-private fallback storage.";
    }

    public static boolean saveModel(Context context, String model) {
        String clean = model == null || model.trim().isEmpty()
                ? "gemini-3.8-flash"
                : model.trim();

        if (saveEncrypted(context, MODEL, clean)) return true;

        return getPrefs(context).edit()
                .putString(MODEL, Base64.encodeToString(
                        clean.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP))
                .commit();
    }

    public static String getModel(Context context) {
        String model = readEncrypted(context, MODEL);
        if (!model.isEmpty()) return model;

        String encoded = getPrefs(context).getString(MODEL, "");
        if (encoded != null && !encoded.isEmpty()) {
            try {
                String decoded = new String(
                        Base64.decode(encoded, Base64.NO_WRAP),
                        StandardCharsets.UTF_8).trim();
                if (!decoded.isEmpty()) return decoded;
            } catch (Throwable ignored) {}
        }

        return "gemini-3.8-flash";
    }

    public static boolean clearGeminiApiKey(Context context) {
        boolean removed = getPrefs(context).edit()
                .remove(API_KEY)
                .remove(API_KEY_FALLBACK)
                .commit();

        return removed && !hasGeminiApiKey(context);
    }

    private static boolean saveEncrypted(
            Context context, String prefKey, String value) {
        try {
            String packed = encrypt(value);
            boolean written = getPrefs(context).edit()
                    .putString(prefKey, packed)
                    .commit();
            return written && value.equals(readEncrypted(context, prefKey));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String encrypt(String value) throws Exception {
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(),
                new GCMParameterSpec(128, iv));

        byte[] encrypted = cipher.doFinal(
                value.getBytes(StandardCharsets.UTF_8));

        return Base64.encodeToString(iv, Base64.NO_WRAP)
                + "."
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private static String readEncrypted(
            Context context, String prefKey) {
        String packed = getPrefs(context).getString(prefKey, "");
        if (packed == null || packed.isEmpty()) return "";

        try {
            String[] parts = packed.split("\\.", 2);
            if (parts.length != 2) return "";

            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                    new GCMParameterSpec(128, iv));

            return new String(
                    cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8).trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            javax.crypto.SecretKey existing = (javax.crypto.SecretKey)
                    keyStore.getKey(KEY_ALIAS, null);
            if (existing != null) return existing;

            try {
                keyStore.deleteEntry(KEY_ALIAS);
            } catch (Throwable ignored) {}
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore");

        generator.init(new android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                        | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(
                        android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                        android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build());

        return generator.generateKey();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
