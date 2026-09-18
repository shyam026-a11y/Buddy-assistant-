package com.shyam026.buddyassistant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandRouter {
    private CommandRouter(){}

    private static final Pattern NUMBER = Pattern.compile("(?<!\\d)(\\d{1,3})(?!\\d)");
    private static final Pattern CALL_A = Pattern.compile("^(.+?)\\s+(?:ko )?(?:call|phone)(?:\\s+(?:karo|kar do|do))$");
    private static final Pattern CALL_B = Pattern.compile("^(?:call|phone|dial)(?:\\s+karo)?\\s+(.+)$");
    private static final Pattern SMS_A = Pattern.compile("^(?:sms|send sms|text|message)\\s+(.+?)\\s+(?:ko|to)\\s+(.+)$");
    private static final Pattern SMS_B = Pattern.compile("^(.+?)\\s+ko\\s+(?:sms|message|text)\\s+(.+)$");
    private static final Pattern WA_A = Pattern.compile("^(?:message|msg|text|send message|send)\\s+(.+?)\\s+(?:ko|to)\\s+(.+)$");
    private static final Pattern WA_B = Pattern.compile("^(?:message|msg|text|send message|send)\\s+(.+?)\\s+for\\s+(.+)$");
    private static final Pattern WA_C = Pattern.compile("^(.+?)\\s+(?:ko|to)\\s+(?:message|msg|text)\\s+(.+)$");

    public static String normalize(String s){
        if(s==null)return "";
        return s.toLowerCase(Locale.ROOT)
                .replace('’',' ')
                .replace('‘',' ')
                .replace('“',' ')
                .replace('”',' ')
                .replaceAll("[^\\p{L}0-9+% ]"," ")
                .replaceAll("\\s+"," ")
                .trim();
    }

    public static boolean isWakePhrase(String s){
        String n=normalize(s);
        return n.equals("hey buddy") || n.startsWith("hey buddy ");
    }

    public static String removeWakePhrase(String s){
        String n=normalize(s);
        return n.startsWith("hey buddy") ? n.substring(9).trim() : n;
    }

    public static boolean word(String s,String token){
        String n=normalize(s);
        for(String x:n.split(" ")) if(x.equals(token)) return true;
        return false;
    }

    public static final class CallRequest{
        public final String target;
        CallRequest(String t){target=t;}
    }

    public static CallRequest parseCall(String s){
        String n=normalize(s);
        Matcher m=CALL_A.matcher(n);
        if(m.find()) return new CallRequest(m.group(1).trim());
        m=CALL_B.matcher(n);
        if(m.find()) return new CallRequest(m.group(1).trim());
        return null;
    }

    public static Integer extractNumber(String s){
        Matcher m=NUMBER.matcher(s==null?"":s);
        return m.find()?Integer.valueOf(m.group(1)):null;
    }

    public static String youtubeQuery(String s){
        String n=normalize(s);
        String[] prefixes={
                "youtube pe search ","youtube par search ","youtube me search ",
                "youtube search ","youtube pe ","youtube par ",
                "youtube me "
        };
        for(String p:prefixes) if(n.startsWith(p)) return n.substring(p.length()).trim();
        return null;
    }

    public static String googleSearchQuery(String s){
        String n=normalize(s);
        String[] prefixes={
                "google pe search ","google par search ","google me search ",
                "google search ","google pe ","google par ",
                "search ","find ","google me "
        };
        for(String p:prefixes) if(n.startsWith(p)) return n.substring(p.length()).trim();
        return null;
    }

    public static String mapsQuery(String s){
        String n=normalize(s);
        String[] prefixes={
                "maps pe ","maps par ","google maps pe ","navigate to ",
                "directions to ","route to ","map of "
        };
        for(String p:prefixes) if(n.startsWith(p)) return n.substring(p.length()).trim();
        return null;
    }

    public static String openTarget(String s){
        String n=normalize(s);
        String[] prefixes={"open ","khol ","launch ","start ","chalao ","chala "};
        for(String p:prefixes) if(n.startsWith(p)) return n.substring(p.length()).trim();
        return null;
    }

    public static final class SmsRequest{
        public final String target;
        public final String message;
        SmsRequest(String t,String m){target=t;message=m;}
    }

    public static SmsRequest parseSms(String s){
        String n=normalize(s);
        Matcher m=SMS_A.matcher(n);
        if(m.find()) return new SmsRequest(m.group(2).trim(),m.group(1).trim());
        m=SMS_B.matcher(n);
        if(m.find()) return new SmsRequest(m.group(1).trim(),m.group(2).trim());
        return null;
    }

    public static final class WhatsAppRequest{
        public final String contact;
        public final String message;
        WhatsAppRequest(String c,String m){contact=c;message=m;}
    }

    public static WhatsAppRequest parseWhatsApp(String s){
        String n=normalize(s);
        String body=null;
        String[] starts={"whatsapp pe ","whatsapp par ","whatsapp me ","whatsapp "};
        for(String p:starts){
            if(n.startsWith(p)){body=n.substring(p.length()).trim();break;}
        }
        if(body==null)return null;

        Matcher m=WA_A.matcher(body);
        if(m.find())return new WhatsAppRequest(m.group(2).trim(),m.group(1).trim());
        m=WA_B.matcher(body);
        if(m.find())return new WhatsAppRequest(m.group(2).trim(),m.group(1).trim());
        m=WA_C.matcher(body);
        if(m.find())return new WhatsAppRequest(m.group(1).trim(),m.group(2).trim());
        return null;
    }
}
