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

        if (!getSharedPreferences(PREF, MODE_PRIVATE)
                .getBoolean(KEY_WAKE, false)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            Intent intent = new Intent(this, BuddyVoiceService.class);
            intent.setAction(BuddyVoiceService.ACTION_ENABLE_WAKE);
            intent.setPackage(getPackageName());

            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Throwable ignored) {
            // Some OEMs restrict background service starts; the foreground app can retry.
        }
    }

    @Override public void onShutdown() {
        try {
            stopService(new Intent(this, BuddyVoiceService.class));
        } catch (Throwable ignored) {
        }
        super.onShutdown();
    }

    public void openBuddy() {
        try {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        } catch (Throwable ignored) {
        }
    }
}
