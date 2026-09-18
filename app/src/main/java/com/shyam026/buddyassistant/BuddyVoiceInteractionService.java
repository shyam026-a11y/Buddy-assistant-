package com.shyam026.buddyassistant;

import android.content.Intent;
import android.os.IBinder;
import android.service.voice.VoiceInteractionService;

public class BuddyVoiceInteractionService extends VoiceInteractionService {
    @Override public void onReady(){
        super.onReady();
        // Android keeps the selected voice-interaction service available for assistant/hotword flows.
    }

    @Override public void onShutdown(){
        super.onShutdown();
    }

    public void openBuddy(){
        try{
            Intent i=new Intent(this,MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }catch(Throwable ignored){}
    }
}