package com.shyam026.buddyassistant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandRouter {
    private CommandRouter(){}

    public static String normalize(String s){
        if(s==null)return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}0-9+% ]"," ")
                .replaceAll("\\s+"," ")
                .trim();
    }

    public static boolean isWakePhrase(String s){
        String n=normalize(s);
        return n.equals("hey buddy") || n.equals("hey buddy") ||
               n.startsWith("hey buddy ") || n.startsWith("hey, buddy ");
    }

    public static String removeWakePhrase(String s){
        if(s==null)return "";
        String n=normalize(s);
        if(n.startsWith("hey buddy")) return n.substring("hey buddy".length()).trim();
        return n;
    }

    public static final class CallRequest{
        public final String target;
        CallRequest(String t){target=t;}
    }

    public static CallRequest parseCall(String s){
        String n=normalize(s);
        String[] p={
            "call ","call karo ","phone ","phone karo ","dial ","dial karo ",
            "ko call karo","ko phone karo"
        };
        for(String x:p){
            int idx=n.indexOf(x);
            if(idx>=0){
                String t;
                if(x.startsWith("ko ")) t=n.substring(0,idx).trim();
                else t=n.substring(idx+x.length()).trim();
                if(t.endsWith("ko")) t=t.substring(0,t.length()-2).trim();
                if(!t.isEmpty()) return new CallRequest(t);
            }
        }
        return null;
    }

    public static Integer extractNumber(String s){
        if(s==null)return null;
        Matcher m=Pattern.compile("\\b(\\d{1,3})\\b").matcher(s);
        return m.find()?Integer.valueOf(m.group(1)):null;
    }

    public static String youtubeQuery(String s){
        String n=normalize(s);
        String[] prefixes={
            "youtube pe search ","youtube par search ","youtube me search ",
            "youtube search ","youtube pe ","youtube par "
        };
        for(String p:prefixes) if(n.startsWith(p)) return n.substring(p.length()).trim();
        return null;
    }

    public static String googleSearchQuery(String s){
        String n=normalize(s);
        String[] prefixes={"google pe search ","google par search ","google me search ","search ","find "};
        for(String p:prefixes) if(n.startsWith(p)) return n.substring(p.length()).trim();
        return null;
    }

    public static final class WhatsAppRequest{
        public final String contact;
        public final String message;
        WhatsAppRequest(String c,String m){contact=c;message=m;}
    }

    public static WhatsAppRequest parseWhatsApp(String s){
        String n=normalize(s);
        String[] starts={"whatsapp ","whatsapp pe ","whatsapp par "};
        String body=null;
        for(String p:starts) if(n.startsWith(p)){body=n.substring(p.length()).trim();break;}
        if(body==null)return null;

        Matcher m=Pattern.compile("^(?:message|msg|text|send message)\\s+(.+?)\\s+(?:ko|to)\\s+(.+)$").matcher(body);
        if(m.find()) return new WhatsAppRequest(m.group(2).trim(),m.group(1).trim());

        m=Pattern.compile("^(?:message|msg|text|send message)\\s+(.+?)\\s+for\\s+(.+)$").matcher(body);
        if(m.find()) return new WhatsAppRequest(m.group(2).trim(),m.group(1).trim());

        return null;
    }
}
