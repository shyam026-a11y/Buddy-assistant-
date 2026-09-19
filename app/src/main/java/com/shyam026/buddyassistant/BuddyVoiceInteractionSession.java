package com.shyam026.buddyassistant;

import android.content.Context;
import android.content.Intent;
import android.service.voice.VoiceInteractionSession;

public class BuddyVoiceInteractionSession extends VoiceInteractionSession {
    public BuddyVoiceInteractionSession(Context context) {
        super(context);
    }

    @Override public void onShow(android.os.Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        try {
            Intent i = new Intent(getContext(), BuddyVoiceService.class)
                    .setAction(BuddyVoiceService.ACTION_TAP_COMMAND)
                    .setPackage(getContext().getPackageName());
            if (android.os.Build.VERSION.SDK_INT >= 26) getContext().startForegroundService(i);
            else getContext().startService(i);
        } catch (Throwable ignored) {
            try {
                Intent i = new Intent(getContext(), BuddyVoiceService.class)
                        .setAction(BuddyVoiceService.ACTION_TAP_COMMAND)
                        .setPackage(getContext().getPackageName());
                getContext().startService(i);
            } catch (Throwable ignoredAgain) {}
        }
    }
}
