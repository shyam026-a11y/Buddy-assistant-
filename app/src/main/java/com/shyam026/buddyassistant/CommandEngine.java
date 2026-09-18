package com.shyam026.buddyassistant;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.*;
import android.view.KeyEvent;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.database.Cursor;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import org.json.*;

public final class CommandEngine {
    public interface Callback { void onResult(String message); }
    private static final String PREF="buddy_prefs", KEY="openrouter_key";
    private CommandEngine(){}

    public static void execute(Context context,String raw,Callback callback){
        Context c=context.getApplicationContext();
        String s=CommandRouter.normalize(raw);
        if(s.isEmpty()){done(c,callback,"Bolo, command clear nahi mila.");return;}

        try{
            if(has(s,"hey buddy","ok buddy","hello buddy","hi buddy","namaste")){
                done(c,callback,"Haan, main yahin hoon.");
                return;
            }
            if(has(s,"time batao","what time","kitne baje","time kya")){
                done(c,callback,new SimpleDateFormat("hh:mm a",Locale.getDefault()).format(new Date()));
                return;
            }
            if(has(s,"date batao","aaj ki date","today date")){
                done(c,callback,new SimpleDateFormat("dd MMMM yyyy",Locale.getDefault()).format(new Date()));
                return;
            }
            if(has(s,"battery","charge kitna")){
                done(c,callback,battery(c)); return;
            }

            String yt=CommandRouter.youtubeQuery(s);
            if(yt!=null&&!yt.isEmpty()){
                if(launch(c,new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(yt))),"YouTube search"))
                    done(c,callback,"YouTube par "+yt+" search kar raha hoon.");
                else done(c,callback,"YouTube search open nahi hua.");
                return;
            }

            CommandRouter.WhatsAppRequest wa=CommandRouter.parseWhatsApp(s);
            if(wa!=null){
                String number=findContactNumber(c,wa.contact);
                if(number==null){done(c,callback,"Contact nahi mila: "+wa.contact);return;}
                String phone=formatWhatsAppNumber(number);
                Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("whatsapp://send?phone="+phone+"&text="+Uri.encode(wa.message)));
                i.setPackage("com.whatsapp");
                if(!launch(c,i,"WhatsApp")) {
                    i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+phone+"?text="+Uri.encode(wa.message)));
                    if(!launch(c,i,"WhatsApp")){done(c,callback,"WhatsApp open nahi hua.");return;}
                }
                done(c,callback,"WhatsApp chat ready hai. Send tap kar dena.");
                return;
            }

            if(has(s,"back jao","go back","back")){global(c,"back",callback);return;}
            if(has(s,"home screen","go home","home jao")){global(c,"home",callback);return;}
            if(has(s,"recent apps","recents")){global(c,"recents",callback);return;}
            if(has(s,"notifications kholo","notification panel","notifications")){global(c,"notifications",callback);return;}
            if(has(s,"quick settings")){global(c,"quick_settings",callback);return;}
            if(has(s,"screenshot","screen shot","screen capture")){global(c,"screenshot",callback);return;}
            if(has(s,"lock phone","lock screen","phone lock")){global(c,"lock",callback);return;}
            if(has(s,"power menu","power button menu")){global(c,"power",callback);return;}
            if(has(s,"all apps","app drawer")){global(c,"all_apps",callback);return;}

            if(has(s,"scroll down","neeche scroll","scroll neeche")){boolean r=accessScroll(c,true);done(c,callback,r?"Neeche scroll kar diya.":"Scroll nahi ho paya.");return;}
            if(has(s,"scroll up","upar scroll","scroll upar")){bool r=accessScroll(c,false);done(c,callback,r?"Upar scroll kar diya.":"Scroll nahi ho paya.");return;}

            String tap=after(s,"tap ","click ");
            if(tap!=null&&!tap.isEmpty()){
                boolean r=accessClick(c,tap);done(c,callback,r?"“"+tap+"” click kar diya.":"“"+tap+"” nahi mila.");return;
            }
            String type=after(s,"type ","likho ");
            if(type!=null&&!type.isEmpty()){
                boolean r=accessType(c,type);done(c,callback,r?"Text enter kar diya.":"Editable field nahi mila.");return;
            }

            if(has(s,"mute","silent","volume")){doVolume(c,s,callback);return;}
            if(has(s,"brightness","roshni","screen bright")){doBrightness(c,s,callback);return;}
            if(has(s,"flashlight","torch")){toggleFlash(c,callback);return;}

            if(has(s,"turn on wifi","wifi on","wifi chalu","wifi band","turn off wifi","wi fi")){
                if(has(s,"on","chalu")){boolean r=quickToggle(c,"Wi-Fi");done(c,callback,r?"Wi-Fi toggle kar diya.":"Wi-Fi controls open nahi hue.");}
                else if(has(s,"off","band")){boolean r=quickToggle(c,"Wi-Fi");done(c,callback,r?"Wi-Fi toggle kar diya.":"Wi-Fi controls open nahi hue.");}
                else settings(c,Settings.ACTION_WIFI_SETTINGS,"Wi-Fi settings",callback);
                return;
            }
            if(has(s,"bluetooth on","bluetooth chalu","bluetooth off","bluetooth band","bluetooth")){
                if(has(s,"on","chalu")||has(s,"off","band")){boolean r=quickToggle(c,"Bluetooth");done(c,callback,r?"Bluetooth toggle kar diya.":"Bluetooth controls open nahi hue.");}
                else settings(c,Settings.ACTION_BLUETOOTH_SETTINGS,"Bluetooth settings",callback);
                return;
            }
            if(has(s,"settings khol","open settings")){settings(c,Settings.ACTION_SETTINGS,"Settings",callback);return;}

            CommandRouter.CallRequest call=CommandRouter.parseCall(s);
            if(call!=null){doCall(c,call.target,callback);return;}

            CommandRouter.SmsRequest sms=CommandRouter.parseSms(s);
            if(sms!=null){doSms(c,sms.target,sms.message,callback);return;}

            boolean open=has(s,"khol","open","launch","start","chala","run");
            if(open){
                String app=null;
                String[][] apps={{"youtube","com.google.android.youtube"},{"chrome","com.android.chrome"},{"whatsapp","com.whatsapp"},{"instagram","com.instagram.android"},{"spotify","com.spotify.music"},{"telegram","org.telegram.messenger"},{"maps","com.google.android.apps.maps"},{"gmail","com.google.android.gm"},{"calculator","com.google.android.calculator"},{"camera","com.android.camera"}};
                for(String[] a:apps)if(s.contains(a[0])){app=a[0];break;}
                if(app!=null){
                    String pretty=app.substring(0,1).toUpperCase(Locale.ROOT)+app.substring(1);
                    Intent i=c.getPackageManager().getLaunchIntentForPackage(packageFor(app,apps));
                    if(i!=null&&launch(c,i,pretty)){done(c,callback,pretty+" khol raha hoon.");}
                    else done(c,callback,pretty+" app nahi mila.");
                    return;
                }
            }

            if(has(s,"play music","music chala","pause music","media")){
                AudioManager am=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                done(c,callback,"Media control.");
                return;
            }

            String q=CommandRouter.googleSearchQuery(s);
            if(q!=null&&!q.isEmpty()){
                Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q)));
                if(launch(c,i,"Google search"))done(c,callback,"Google par "+q+" search kar raha hoon.");
                else done(c,callback,"Search open nahi hua.");
                return;
            }

            cloud(c,raw,callback);
        }catch(Throwable t){
            done(c,callback,"Command fail hua. Dobara bolo.");
        }
    }

    private static boolean has(String s,String...xs){for(String x:xs)if(s.contains(x))return true;return false;}
    private static String after(String s,String...prefixes){for(String p:prefixes)if(s.startsWith(p))return s.substring(p.length()).trim();return null;}
    private static void done(Context c,Callback cb,String msg){if(cb==null)return;new Handler(Looper.getMainLooper()).post(()->cb.onResult(msg));}

    private static void settings(Context c,String action,String name,Callback cb){
        boolean ok=launch(c,new Intent(action),name);
        done(c,cb,ok?name+" khol raha hoon.":name+" open nahi hua.");
    }

    private static void global(Context c,String action,Callback cb){
        if(BuddyAccessibilityService.isEnabled()){
            boolean ok=BuddyAccessibilityService.get().global(action);
            done(c,cb,ok?actionText(action)+" done.":actionText(action)+" nahi hua.");
        }else{
            if("home".equals(action)){
                boolean ok=launch(c,new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),"Home");
                done(c,cb,ok?"Home.":"Home open nahi hua.");
            }else done(c,cb,"Phone control ke liye Accessibility enable karo.");
        }
    }

    private static String actionText(String a){
        if("quick_settings".equals(a))return "Quick settings";
        if("all_apps".equals(a))return "App drawer";
        if("screenshot".equals(a))return "Screenshot";
        if("lock".equals(a))return "Lock";
        if("power".equals(a))return "Power menu";
        if("notifications".equals(a))return "Notifications";
        if("recents".equals(a))return "Recent apps";
        return "Back";
    }

    private static boolean accessScroll(Context c,boolean down){return BuddyAccessibilityService.isEnabled()&&BuddyAccessibilityService.get().scroll(down);}
    private static boolean accessClick(Context c,String t){return BuddyAccessibilityService.isEnabled()&&BuddyAccessibilityService.get().clickText(t);}
    private static boolean accessType(Context c,String t){return BuddyAccessibilityService.isEnabled()&&BuddyAccessibilityService.get().typeText(t);}

    private static void doVolume(Context c,String s,Callback cb){
        AudioManager a=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        Integer p=CommandRouter.extractNumber(s);
        if(has(s,"mute","silent")){a.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.FLAG_SHOW_UI);done(c,cb,"Volume mute.");}
        else if(p!=null){int v=Math.round(a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*p/100f);a.setStreamVolume(AudioManager.STREAM_MUSIC,Math.max(0,Math.min(a.getStreamMaxVolume(AudioManager.STREAM_MUSIC),v)),AudioManager.FLAG_SHOW_UI);done(c,cb,"Volume "+p+" percent.");}
        else if(has(s,"kam","down","decrease")){a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);done(c,cb,"Volume kam kar diya.");}
        else {a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);done(c,cb,"Volume badha diya.");}
    }

    private static void doBrightness(Context c,String s,Callback cb){
        Integer p=CommandRouter.extractNumber(s);
        try{
            if(!Settings.System.canWrite(c)){launch(c,new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+c.getPackageName())),"Brightness permission");done(c,cb,"Brightness permission allow karo.");return;}
            if(p!=null){Settings.System.putInt(c.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,Math.min(100,p))*255/100);done(c,cb,"Brightness "+p+" percent.");}
            else{
                int d=has(s,"kam","down")?-20:20;
                int cur=Settings.System.getInt(c.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128);
                Settings.System.putInt(c.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,Math.min(255,cur+(255*d/100))));
                done(c,cb,d<0?"Brightness kam kar diya.":"Brightness badha diya.");
            }
        }catch(Throwable t){done(c,cb,"Brightness control nahi hua.");}
    }

    private static void toggleFlash(Context c,Callback cb){
        try{
            CameraManager cm=(CameraManager)c.getSystemService(Context.CAMERA_SERVICE);
            for(String id:cm.getCameraIdList()){
                CameraCharacteristics ch=cm.getCameraCharacteristics(id);
                Boolean back=ch.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK;
                Boolean flash=ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if(Boolean.TRUE.equals(back)&&Boolean.TRUE.equals(flash)){
                    boolean now=c.getSharedPreferences(PREF,0).getBoolean("flash",false);
                    cm.setTorchMode(id,!now);
                    c.getSharedPreferences(PREF,0).edit().putBoolean("flash",!now).apply();
                    done(c,cb,!now?"Torch on.":"Torch off.");return;
                }
            }
        }catch(Throwable ignored){}
        done(c,cb,"Torch control available nahi hai.");
    }

    private static boolean quickToggle(Context c,String wanted){
        if(!BuddyAccessibilityService.isEnabled())return launch(c,wanted.equals("Wi-Fi")?new Intent(Settings.ACTION_WIFI_SETTINGS):new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),wanted);
        BuddyAccessibilityService.get().global("quick_settings");
        new Handler(Looper.getMainLooper()).postDelayed(()->BuddyAccessibilityService.get().clickText(wanted),700);
        return true;
    }

    private static void doCall(Context c,String target,Callback cb){
        String n=target==null?"":target.replaceAll("[^0-9+]","");
        if(n.isEmpty())n=findContactNumber(c,target);
        if(n==null||n.isEmpty()){done(c,cb,"Contact/number nahi mila.");return;}
        if(Build.VERSION.SDK_INT>=23&&c.checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){done(c,cb,"Phone call permission allow karo.");return;}
        try{
            Intent i=new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+Uri.encode(n)));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            done(c,cb,"Calling…");
        }catch(Throwable t){done(c,cb,"Call start nahi hua.");}
    }

    private static void doSms(Context c,String target,String message,Callback cb){
        String n=target==null?"":target.replaceAll("[^0-9+]","");
        if(n.isEmpty())n=findContactNumber(c,target);
        if(n==null||n.isEmpty()){done(c,cb,"SMS recipient nahi mila.");return;}
        if(Build.VERSION.SDK_INT>=23&&c.checkSelfPermission(Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED){done(c,cb,"SMS permission allow karo.");return;}
        try{SmsManager.getDefault().sendTextMessage(n,null,message,null,null);done(c,cb,"SMS send kar diya.");}
        catch(Throwable t){done(c,cb,"SMS send nahi hua.");}
    }

    private static String findContactNumber(Context c,String wanted){
        Cursor cur=null;
        try{
            String q=wanted==null?"":wanted.replace("%","\\%");
            cur=c.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ? ESCAPE '\\'",
                    new String[]{"%"+q+"%"},
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY+" DESC");
            if(cur!=null&&cur.moveToFirst())return cur.getString(0);
        }catch(Throwable ignored){}finally{if(cur!=null)cur.close();}
        return null;
    }

    private static String formatWhatsAppNumber(String raw){String n=raw.replaceAll("[^0-9]","");if(n.length()==10)n="91"+n;return n;}

    private static String packageFor(String app,String[][] apps){for(String[] a:apps)if(a[0].equals(app))return a[1];return "";}

    private static boolean launch(Context c,Intent i,String label){
        try{
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i); return true;
        }catch(Throwable ignored){ return false; }
    }

    private static String battery(Context c){
        Intent i=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(i==null)return "Battery status nahi mila.";
        int l=i.getIntExtra("level",-1),sc=i.getIntExtra("scale",100);
        return l>=0?"Battery "+Math.round(l*100f/sc)+" percent hai.":"Battery status nahi mila.";
    }

    private static void cloud(Context c,String raw,Callback cb){
        String key=c.getSharedPreferences(PREF,0).getString(KEY,"").trim();
        if(key.isEmpty()){done(c,cb,"AI key add nahi hai.");return;}
        new Thread(()->{
            HttpURLConnection h=null;
            try{
                JSONObject body=new JSONObject().put("model","openrouter/free").put("temperature",0.1).put("max_tokens",80);
                JSONArray m=new JSONArray();
                m.put(new JSONObject().put("role","system").put("content","You are Buddy. Reply in one short Hinglish/English sentence, maximum 12 words. Never claim device actions were completed."));
                m.put(new JSONObject().put("role","user").put("content",raw));
                body.put("messages",m);
                h=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();
                h.setRequestMethod("POST");h.setConnectTimeout(1500);h.setReadTimeout(3500);h.setDoOutput(true);
                h.setRequestProperty("Authorization","Bearer "+key);
                h.setRequestProperty("Content-Type","application/json");
                try(OutputStream o=h.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}
                int code=h.getResponseCode();
                if(code<200||code>=300){done(c,cb,"AI response nahi aa raha.");return;}
                BufferedReader r=new BufferedReader(new InputStreamReader(h.getInputStream(),StandardCharsets.UTF_8));
                StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);
                String ans=new JSONObject(b.toString()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","");
                done(c,cb,ans.replace("\n"," ").trim());
            }catch(Throwable t){done(c,cb,"AI slow hai. Local command bolo.");}
            finally{if(h!=null)h.disconnect();}
        }).start();
    }
}