package com.shyam026.buddyassistant;

import android.content.Context;
import android.content.SharedPreferences;

public final class BuddyMemory {
    private static final String PREF = "buddy_memory";
    private static final int MAX_VALUE = 240;

    private BuddyMemory() {}

    public static void remember(Context context, String value) {
        if (context == null || value == null) return;
        String clean = value.trim();
        if (clean.isEmpty() || clean.length() > MAX_VALUE) return;
        get(context).edit().putString("last_note", clean).apply();
    }

    public static String getLastMemory(Context context) {
        return get(context).getString("last_note", "");
    }

    public static void clear(Context context) {
        get(context).edit().clear().apply();
    }

    private static SharedPreferences get(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
