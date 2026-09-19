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
import javax.crypto.spec.SecretKeySpec;

public final class BuddySecrets {
    private static final String KEY_ALIAS = "BuddySecretsKey";
    private static final String PREF = "buddy_secure";
    private static final String API_KEY = "openrouter_api_key";
    private static final String MODEL = "openrouter_model";

    private BuddySecrets() {}

    public static void saveApiKey(Context context, String key) {
        put(context, API_KEY, key == null ? "" : key.trim());
    }

    public static String getApiKey(Context context) {
        return get(context, API_KEY);
    }

    public static void clearApiKey(Context context) {
        getPrefs(context).edit().remove(API_KEY).apply();
    }

    public static void saveModel(Context context, String model) {
        String clean = model == null || model.trim().isEmpty() ? "openrouter/free" : model.trim();
        put(context, MODEL, clean);
    }

    public static String getModel(Context context) {
        String model = get(context, MODEL);
        return model.isEmpty() ? "openrouter/free" : model;
    }

    private static void put(Context context, String key, String value) {
        try {
            byte[] iv = new byte[12];
            new java.security.SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String packed = Base64.encodeToString(iv, Base64.NO_WRAP)
                    + "."
                    + Base64.encodeToString(encrypted, Base64.NO_WRAP);
            getPrefs(context).edit().putString(key, packed).apply();
        } catch (Exception ignored) {
            // Do not silently write the secret in plaintext.
            getPrefs(context).edit().remove(key).apply();
        }
    }

    private static String get(Context context, String key) {
        String packed = getPrefs(context).getString(key, "");
        if (packed == null || packed.isEmpty()) return "";
        try {
            String[] parts = packed.split("\\.", 2);
            if (parts.length != 2) return "";
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator generator = KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore");
        generator.init(new android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                        | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build());
        return generator.generateKey();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
