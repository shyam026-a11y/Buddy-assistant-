package com.shyam026.buddyassistant;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.Set;

public final class BuddyVoiceProfile {
    private BuddyVoiceProfile() {}

    public static void apply(TextToSpeech tts, Locale preferred) {
        if (tts == null) return;
        Locale p = preferred == null ? Locale.forLanguageTag("en-IN") : preferred;
        try {
            Voice best = choose(tts.getVoices(), p);
            if (best != null) {
                tts.setVoice(best);
            } else {
                tts.setLanguage(p);
            }
        } catch (Throwable ignored) {
            try { tts.setLanguage(p); } catch (Throwable ignored2) {}
        }

        // Youthful, friendly voice profile. Exact timbre depends on the installed TTS engine.
        tts.setSpeechRate(1.01f);
        tts.setPitch(1.08f);
    }

    public static Voice choose(Set<Voice> voices, Locale preferred) {
        if (voices == null || voices.isEmpty()) return null;
        Voice best = null;
        int scoreBest = Integer.MIN_VALUE;
        for (Voice v : voices) {
            if (v == null || v.getLocale() == null) continue;
            Locale l = v.getLocale();
            String name = v.getName() == null ? "" : v.getName().toLowerCase(Locale.ROOT);
            int score = 0;
            if (l.getLanguage().equalsIgnoreCase(preferred.getLanguage())) score += 80;
            if (!preferred.getCountry().isEmpty() && l.getCountry().equalsIgnoreCase(preferred.getCountry())) score += 60;
            if (l.toLanguageTag().equalsIgnoreCase(preferred.toLanguageTag())) score += 120;
            score += Math.min(120, v.getQuality() / 4);
            score -= Math.min(40, v.getLatency() / 10);
            if (name.contains("female") || name.contains("woman") || name.contains("girl")) score += 30;
            if (score > scoreBest) {
                best = v;
                scoreBest = score;
            }
        }
        return best;
    }

    public static String naturalize(String text) {
        if (text == null) return "";
        return text.trim()
                .replaceAll("\\s+", " ")
                .replace("...", "…");
    }
}
