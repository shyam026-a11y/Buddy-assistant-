package com.shyam026.buddyassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.voice.VoiceInteractionService;

public class BuddyVoiceInteractionService extends VoiceInteractionService {
    private static final String PREF = "buddy_prefs";
    private static final String KEY_WAKE = "wake_mode";

    @Override public void onReady() {
        super.onReady();
        if (!getSharedPreferences(PREF, MODE_PRIVATE).getBoolean(KEY_WAKE, false)) return;
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;

        try {
            Intent intent = new Intent(this, BuddyVoiceService.class)
                    .setAction(BuddyVoiceService.ACTION_ENABLE_WAKE)
                    .setPackage(getPackageName());
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
        } catch (Throwable ignored) {
            // OEM background-start policy can block this; foreground app can start Buddy again.
        }
    }

    @Override public void onShutdown() {
        try { stopService(new Intent(this, BuddyVoiceService.class)); } catch (Throwable ignored) {}
        super.onShutdown();
    }
}
