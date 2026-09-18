package com.shyam026.buddyassistant;

import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;

public class BuddyVoiceInteractionSessionService extends VoiceInteractionSessionService {
    @Override public VoiceInteractionSession onNewSession(Bundle args){
        return new BuddyVoiceInteractionSession(this);
    }
}