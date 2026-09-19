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
    private static final String MODEL = "gemini_model";

    private BuddySecrets() {}

    public static boolean saveGeminiApiKey(Context context, String key) {
        String clean = key == null ? "" : key.trim();
        if (clean.isEmpty()) {
            clearGeminiApiKey(context);
            return false;
        }

        SharedPreferences prefs = getPrefs(context);

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                SecretKey secretKey = getOrCreateKey();
                String packed = encrypt(clean, secretKey);

                // Verify the actual crypto round-trip before touching preferences.
                if (!clean.equals(decrypt(packed, secretKey))) {
                    deleteKey();
                    continue;
                }

                // commit() gives the UI a definitive persistence result.
                if (!prefs.edit().putString(API_KEY, packed).commit()) {
                    return false;
                }

                // Verify what was persisted. Prefer the same key first so a
                // transient keystore reload issue cannot create a false failure.
                if (clean.equals(decrypt(prefs.getString(API_KEY, ""), secretKey))) {
                    return true;
                }
                if (clean.equals(get(context, API_KEY))) {
                    return true;
                }
            } catch (Throwable ignored) {
                // Android Keystore providers can fail transiently or after key
                // invalidation. Recreate the local AES key once and retry.
            }

            deleteKey();
        }

        return false;
    }

    public static String getGeminiApiKey(Context context) {
        return get(context, API_KEY);
    }

    public static boolean hasGeminiApiKey(Context context) {
        return !getGeminiApiKey(context).trim().isEmpty();
    }

    public static boolean saveModel(Context context, String model) {
        String clean = model == null || model.trim().isEmpty()
                ? "gemini-3.8-flash"
                : model.trim();
        return putAndVerify(context, MODEL, clean);
    }

    public static String getModel(Context context) {
        String model = get(context, MODEL);
        return model.isEmpty() ? "gemini-3.8-flash" : model;
    }

    public static boolean clearGeminiApiKey(Context context) {
        return getPrefs(context).edit().remove(API_KEY).commit()
                && !hasGeminiApiKey(context);
    }

    private static boolean putAndVerify(
            Context context, String key, String value) {
        try {
            String packed = encrypt(value, getOrCreateKey());
            return getPrefs(context).edit().putString(key, packed).commit()
                    && value.equals(get(context, key));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String encrypt(String value, SecretKey secretKey)
            throws Exception {
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                secretKey,
                new GCMParameterSpec(128, iv));

        byte[] encrypted =
                cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

        return Base64.encodeToString(iv, Base64.NO_WRAP)
                + "."
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private static String decrypt(String packed, SecretKey secretKey) {
        if (packed == null || packed.isEmpty()) return "";

        try {
            String[] parts = packed.split("\\.", 2);
            if (parts.length != 2) return "";

            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(128, iv));

            return new String(
                    cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String get(Context context, String key) {
        String packed = getPrefs(context).getString(key, "");
        if (packed == null || packed.isEmpty()) return "";

        try {
            return decrypt(packed, getOrCreateKey());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
            if (entry instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            }
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

    private static void deleteKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
            }
        } catch (Throwable ignored) {}
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
