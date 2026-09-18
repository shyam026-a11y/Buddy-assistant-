package com.shyam026.buddyassistant;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.*;

public final class BuddyVoiceProfile {
    private BuddyVoiceProfile(){}

    public static void apply(TextToSpeech tts, Locale preferred){
        if(tts==null)return;
        Locale p=preferred==null?Locale.forLanguageTag("en-IN"):preferred;
        Voice best=choose(tts.getVoices(),p);
        try{
            if(best!=null)tts.setVoice(best);
            else tts.setLanguage(p);
        }catch(Throwable ignored){
            try{tts.setLanguage(p);}catch(Throwable ignored2){}
        }
        tts.setSpeechRate(0.98f);
        tts.setPitch(1.10f);
    }

    public static Voice choose(Set<Voice> voices,Locale preferred){
        if(voices==null||voices.isEmpty())return null;
        Voice best=null;
        int bestScore=Integer.MIN_VALUE;
        for(Voice v:voices){
            if(v==null||v.getLocale()==null)continue;
            Locale l=v.getLocale();
            String name=(v.getName()==null?"":v.getName()).toLowerCase(Locale.ROOT);
            int score=0;

            boolean sameLanguage=l.getLanguage().equalsIgnoreCase(preferred.getLanguage());
            boolean sameCountry=!preferred.getCountry().isEmpty()&&l.getCountry().equalsIgnoreCase(preferred.getCountry());
            if(sameLanguage)score+=80;
            if(sameCountry)score+=60;
            if(l.toLanguageTag().equalsIgnoreCase(preferred.toLanguageTag()))score+=120;

            score+=Math.min(120,v.getQuality()/4);
            score-=Math.min(40,v.getLatency()/10);

            // Voice names are not standardized for gender; use only a weak preference
            // when the installed engine exposes a clearly female-labelled voice.
            if(name.contains("female")||name.contains("woman"))score+=35;

            if(best==null||score>bestScore){
                best=v;
                bestScore=score;
            }
        }
        return best;
    }

    public static String naturalize(String text){
        if(text==null)return "";
        String s=text.trim().replaceAll("\s+"," ");
        s=s.replace("...","…");
        return s;
    }
}