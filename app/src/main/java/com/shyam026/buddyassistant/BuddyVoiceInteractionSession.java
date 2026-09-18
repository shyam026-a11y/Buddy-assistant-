package com.shyam026.buddyassistant;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

public class BuddyVoiceInteractionSession extends VoiceInteractionSession {
    public BuddyVoiceInteractionSession(android.content.Context context){ super(context); }

    @Override public void onHandleAssist(Bundle data){
        try{
            Intent i=new Intent(getContext(),MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            getContext().startActivity(i);
        }catch(Throwable ignored){}
    }
}