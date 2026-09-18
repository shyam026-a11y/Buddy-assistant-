package com.shyam026.buddyassistant;

import android.content.Context;
import android.content.Intent;
import android.service.voice.VoiceInteractionSession;

public class BuddyVoiceInteractionSession extends VoiceInteractionSession {
    public BuddyVoiceInteractionSession(Context context){
        super(context);
    }

    @Override public void onShow(android.os.Bundle args,int showFlags){
        super.onShow(args,showFlags);
        try{
            Intent i=new Intent(getContext(),MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            getContext().startActivity(i);
        }catch(Throwable ignored){}
    }
}