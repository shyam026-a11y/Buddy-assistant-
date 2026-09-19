package com.shyam026.buddyassistant;

import android.service.voice.VoiceInteractionService;

public class BuddyVoiceInteractionService extends VoiceInteractionService {
    @Override public void onReady() {
        super.onReady();
        // Intentionally lightweight: Android keeps the selected VoiceInteractionService running.
        // Do not start a microphone foreground service here. Audio begins only when an interaction is invoked.
    }

    @Override public void onShutdown() {
        super.onShutdown();
    }
}
