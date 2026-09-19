package com.shyam026.buddyassistant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandRouter {
    private CommandRouter() {}

    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}0-9+% ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .replace("you tube", "youtube")
                .replace("wi fi", "wifi")
                .replace("what's app", "whatsapp")
                .replace("what s app", "whatsapp");
    }

    public static String removeWakePhrase(String s) {
        String n = normalize(s);
        if (n.equals("buddy")) return "";
        if (n.startsWith("buddy ")) return n.substring("buddy ".length()).trim();
        if (n.equals("hey buddy")) return "";
        if (n.startsWith("hey buddy ")) return n.substring("hey buddy ".length()).trim();
        return null;
    }

    public static boolean isWakePhrase(String s) {
        return removeWakePhrase(s) != null;
    }

    public static String memoryText(String s) {
        if (s == null) return null;
        String n = normalize(s);
        for (String prefix : new String[]{"remember that ", "remember "}) {
            if (n.startsWith(prefix) && s.length() >= prefix.length()) {
                return s.trim().substring(
                        Math.min(s.trim().length(), prefix.length())).trim();
            }
        }
        return null;
    }

    public static Integer extractNumber(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("\\b(\\d{1,3})\\b").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    public static final class CallRequest {
        public final String target;
        CallRequest(String target) { this.target = target; }
    }

    public static CallRequest parseCall(String s) {
        String n = normalize(s);
        Matcher m = Pattern.compile("^(.+?)\\s+(?:ko )?(?:call|phone)\\s+(?:karo|kar do|do)$").matcher(n);
        if (m.find()) return new CallRequest(m.group(1).trim());
        m = Pattern.compile("^(?:call|phone|dial)(?:\\s+karo)?\\s+(.+)$").matcher(n);
        return m.find() ? new CallRequest(m.group(1).trim()) : null;
    }

    public static final class SmsRequest {
        public final String target;
        public final String message;
        SmsRequest(String target, String message) { this.target = target; this.message = message; }
    }

    public static SmsRequest parseSms(String s) {
        String n = normalize(s);
        Matcher m = Pattern.compile("^(?:sms|send sms|text|message)\\s+(.+?)\\s+(?:ko|to)\\s+(.+)$").matcher(n);
        if (m.find()) return new SmsRequest(m.group(2).trim(), m.group(1).trim());
        m = Pattern.compile("^(.+?)\\s+ko\\s+(?:sms|message|text)\\s+(.+)$").matcher(n);
        return m.find() ? new SmsRequest(m.group(1).trim(), m.group(2).trim()) : null;
    }

    public static final class WhatsAppRequest {
        public final String contact;
        public final String message;
        WhatsAppRequest(String contact, String message) { this.contact = contact; this.message = message; }
    }

    public static WhatsAppRequest parseWhatsApp(String s) {
        String n = normalize(s);
        String body = null;
        for (String prefix : new String[]{"whatsapp pe ", "whatsapp par ", "whatsapp me ", "whatsapp "}) {
            if (n.startsWith(prefix)) { body = n.substring(prefix.length()).trim(); break; }
        }
        if (body == null) return null;
        Matcher m = Pattern.compile("^(?:message|msg|text|send message)\\s+(.+?)\\s+(?:ko|to)\\s+(.+)$").matcher(body);
        if (m.find()) return new WhatsAppRequest(m.group(2).trim(), m.group(1).trim());
        m = Pattern.compile("^(?:message|msg|text|send message)\\s+(.+?)\\s+for\\s+(.+)$").matcher(body);
        return m.find() ? new WhatsAppRequest(m.group(2).trim(), m.group(1).trim()) : null;
    }

    public static String youtubeQuery(String s) {
        String n = normalize(s);
        for (String prefix : new String[]{
                "youtube pe search ", "youtube par search ", "youtube me search ",
                "youtube search ", "youtube pe ", "youtube par ", "youtube me "
        }) {
            if (n.startsWith(prefix)) {
                String q = n.substring(prefix.length()).trim();
                if (q.equals("kholo") || q.equals("open") || q.equals("chalao") || q.equals("play")) return null;
                return q.isEmpty() ? null : q;
            }
        }
        Matcher m = Pattern.compile("^youtube\\s+(.+?)\\s+(?:chalao|bajao|play)$").matcher(n);
        return m.find() ? m.group(1).trim() : null;
    }

    public static String googleSearchQuery(String s) {
        String n = normalize(s);
        for (String prefix : new String[]{"google pe search ", "google par search ", "google me search ", "google search ", "search ", "find "}) {
            if (n.startsWith(prefix)) return n.substring(prefix.length()).trim();
        }
        return null;
    }
}
