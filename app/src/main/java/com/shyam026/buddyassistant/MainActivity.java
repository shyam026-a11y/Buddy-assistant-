package com.shyam026.buddyassistant;

import android.Manifest;
import android.app.*;
import android.app.SearchManager;
import android.content.*;
import android.database.Cursor;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.*;
import android.provider.Settings;
import android.provider.ContactsContract;
import android.speech.*;
import android.speech.tts.*;
import android.telephony.SmsManager;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private static final String PREF="buddy_prefs", KEY="openrouter_key";
 private SharedPreferences prefs; private SpeechRecognizer sr; private TextToSpeech tts;
 private boolean ttsReady,listening,handsFree,speaking,suppressRecognizerCallbacks; private String lang="en-IN";
 private enum ListenMode { OFF, COMMAND, WAKE }
 private ListenMode listenMode=ListenMode.OFF;
 private String pendingWaContact="", pendingWaMessage="", pendingCallTarget="", pendingSmsNumber="", pendingSmsText="";
 private final ExecutorService net=Executors.newSingleThreadExecutor();
 private TextView state,user,buddy,accessLabel; private Button mic,langBtn;
 private final int BG=Color.rgb(11,16,32),CARD=Color.rgb(23,28,51),CARD2=Color.rgb(31,37,65),WHITE=Color.WHITE,MUTED=Color.rgb(165,172,201),ACCENT=Color.rgb(124,92,255),CYAN=Color.rgb(93,208,255);
 private final Map<String,String> apps=new LinkedHashMap<>();
 private final Handler mainHandler=new Handler(Looper.getMainLooper());
 private final Runnable wakeRetry=()->{if(handsFree&&listenMode==ListenMode.WAKE&&!listening&&!isFinishing())listenForWakeWord();};

 @Override public void onCreate(Bundle b){
  super.onCreate(b); prefs=getSharedPreferences(PREF,0);
  String[][] a={{"youtube","com.google.android.youtube"},{"chrome","com.android.chrome"},{"whatsapp","com.whatsapp"},{"instagram","com.instagram.android"},{"spotify","com.spotify.music"},{"telegram","org.telegram.messenger"},{"maps","com.google.android.apps.maps"},{"gmail","com.google.android.gm"},{"calculator","com.google.android.calculator"}};
  for(String[] x:a)apps.put(x[0],x[1]);
  initTts(); buildUi();
 }

 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
 private TextView txt(String s,float z,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(null,bold?1:0);return v;}
 private Button btn(String s,int h,int c,float z){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(z);b.setAllCaps(false);b.setMinHeight(0);b.setMinWidth(0);b.setGravity(Gravity.CENTER);b.setBackground(bg(c,18));return b;}
 private void gap(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}

 private void buildUi(){
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(BG);

  ScrollView sc=new ScrollView(this);
  sc.setFillViewport(true);
  LinearLayout c=new LinearLayout(this);
  c.setOrientation(LinearLayout.VERTICAL);
  c.setPadding(dp(18),dp(18),dp(18),dp(28));
  sc.addView(c);
  root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));

  LinearLayout head=new LinearLayout(this);
  head.setGravity(Gravity.CENTER_VERTICAL);
  ImageView logo=new ImageView(this);
  logo.setImageResource(R.drawable.buddy_logo);
  head.addView(logo,new LinearLayout.LayoutParams(dp(62),dp(62)));

  LinearLayout nm=new LinearLayout(this);
  nm.setOrientation(LinearLayout.VERTICAL);
  nm.setPadding(dp(12),0,0,0);
  nm.addView(txt("BUDDY",24,WHITE,true));
  nm.addView(txt("Voice-first phone assistant",13,MUTED,false));
  head.addView(nm,new LinearLayout.LayoutParams(0,-2,1));

  langBtn=btn("EN",44,CARD2,13);
  langBtn.setPadding(dp(14),0,dp(14),0);
  langBtn.setOnClickListener(v->{
   lang=lang.equals("en-IN")?"hi-IN":"en-IN";
   langBtn.setText(lang.equals("en-IN")?"EN":"HI");
   if(ttsReady)try{tts.setLanguage(Locale.forLanguageTag(lang));}catch(Exception ignored){}
  });
  head.addView(langBtn);
  c.addView(head);
  gap(c,14);

  LinearLayout hero=new LinearLayout(this);
  hero.setOrientation(LinearLayout.VERTICAL);
  hero.setPadding(dp(16),dp(16),dp(16),dp(16));
  hero.setBackground(bg(CARD,22));
  TextView title=txt("ASSISTANT STATUS",11,MUTED,true);
  hero.addView(title);
  state=txt("● Ready",14,CYAN,true);
  state.setPadding(0,dp(7),0,dp(10));
  hero.addView(state);
  user=txt("Say something…",17,WHITE,false);
  hero.addView(user);
  buddy=txt("Ready hoon. Bolo.",17,WHITE,true);
  buddy.setPadding(0,dp(7),0,0);
  hero.addView(buddy);
  c.addView(hero);
  gap(c,14);

  mic=btn("🎙  TAP TO TALK",62,ACCENT,16);
  LinearLayout mr=new LinearLayout(this);
  mr.setGravity(Gravity.CENTER);
  mr.addView(mic,new LinearLayout.LayoutParams(-1,dp(62)));
  c.addView(mr);
  mic.setOnClickListener(v->listen());
  gap(c,16);

  c.addView(txt("QUICK ACTIONS",11,MUTED,true));
  gap(c,6);
  LinearLayout row1=new LinearLayout(this);
  row1.setOrientation(LinearLayout.HORIZONTAL);
  chip(row1,"YouTube","YouTube kholo");
  chip(row1,"Volume +","volume badhao");
  c.addView(row1);
  gap(c,6);
  LinearLayout row2=new LinearLayout(this);
  row2.setOrientation(LinearLayout.HORIZONTAL);
  chip(row2,"Brightness","brightness 60");
  chip(row2,"Back","back jao");
  c.addView(row2);
  gap(c,14);

  LinearLayout commands=new LinearLayout(this);
  commands.setOrientation(LinearLayout.VERTICAL);
  commands.setPadding(dp(16),dp(14),dp(16),dp(14));
  commands.setBackground(bg(CARD,18));
  commands.addView(txt("TRY SAYING",11,MUTED,true));
  commands.addView(txt("YouTube kholo\nvolume 60\nbrightness 40\nsearch JEE physics",14,WHITE,false));
  c.addView(commands);
  gap(c,14);

  LinearLayout access=new LinearLayout(this);
  access.setOrientation(LinearLayout.VERTICAL);
  access.setPadding(dp(16),dp(14),dp(16),dp(14));
  access.setBackground(bg(CARD,18));
  access.addView(txt("PHONE CONTROL",11,MUTED,true));
  accessLabel=txt(accessText(),14,WHITE,false);
  accessLabel.setPadding(0,dp(6),0,dp(10));
  access.addView(accessLabel);
  Button ae=btn("Enable phone control",44,CARD2,13);
  ae.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  access.addView(ae);
  c.addView(access);
  gap(c,12);

  LinearLayout opts=new LinearLayout(this);
  opts.setGravity(Gravity.CENTER_VERTICAL);
  opts.setPadding(dp(14),dp(4),0,dp(4));
  Switch sw=new Switch(this);
  sw.setText("Hey Buddy wake mode");
  sw.setTextColor(WHITE);
  sw.setTextSize(14);
  sw.setOnCheckedChangeListener((v,x)->{
   handsFree=x;
   mainHandler.removeCallbacks(wakeRetry);
   if(x){
    stopTts();
    listenForWakeWord();
   }else{
    listenMode=ListenMode.OFF;
    stopListening();
    setState("● Ready",CYAN);
   }
  });
  opts.addView(sw,new LinearLayout.LayoutParams(0,-2,1));

  Button set=btn("Settings",44,CARD2,13);
  set.setOnClickListener(v->settings());
  opts.addView(set);
  c.addView(opts);
  gap(c,10);

  TextView foot=txt("Local phone control • Offline-first voice • AI fallback",12,MUTED,false);
  foot.setGravity(Gravity.CENTER);
  c.addView(foot);

  setContentView(root);
  initSpeech();
 }
 private String accessText(){return BuddyAccessibilityService.isEnabled()?"✓ Full navigation control enabled":"○ Optional: enable Accessibility for Back/Home/Recents/scroll/tap/type";}
 private void chip(LinearLayout r,String label,String cmd){Button b=btn(label,42,CARD2,12);b.setOnClickListener(v->command(cmd));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,42,1);p.setMargins(3,0,3,0);r.addView(b,p);}

 private void initTts(){
  tts=new TextToSpeech(this,x->{
   if(x==TextToSpeech.SUCCESS){
    ttsReady=true;
    BuddyVoiceProfile.apply(tts,Locale.forLanguageTag(lang));
    tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
     @Override public void onStart(String id){}
     @Override public void onDone(String id){runOnUiThread(MainActivity.this::resumeWakeAfterSpeech);}
     @Override public void onError(String id){runOnUiThread(MainActivity.this::resumeWakeAfterSpeech);}
    });
   }
  });
 }

 private void initSpeech(){
  if(!SpeechRecognizer.isRecognitionAvailable(this)){
   setState("● Speech recognition unavailable",Color.RED);
   sr=null;
   return;
  }
  try{
   if(sr!=null){try{sr.destroy();}catch(Exception ignored){}}
   sr=null;
   if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this)){
    try{sr=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);}catch(RuntimeException ignored){}
   }
   if(sr==null)sr=SpeechRecognizer.createSpeechRecognizer(this);
   sr.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle b){
     listening=true;
     if(mic!=null)mic.setText(listenMode==ListenMode.WAKE?"● WAITING FOR BUDDY":"● LISTENING");
     setState(listenMode==ListenMode.WAKE?"● Waiting for “Hey Buddy”…":"● Listening…",CYAN);
    }
    public void onBeginningOfSpeech(){}
    public void onRmsChanged(float r){}
    public void onBufferReceived(byte[] b){}
    public void onEndOfSpeech(){}
    public void onError(int e){
     listening=false;
     if(suppressRecognizerCallbacks)return;
     if(mic!=null)mic.setText("🎙  TAP TO TALK");
     if(handsFree&&listenMode==ListenMode.WAKE){
      if(e==SpeechRecognizer.ERROR_RECOGNIZER_BUSY||e==SpeechRecognizer.ERROR_CLIENT){
       try{initSpeech();}catch(Exception ignored){}
      }
      scheduleWake(650);
     }else{
      listenMode=ListenMode.OFF;
      setState(e==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS?"● Mic permission required":"● Ready",e==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS?Color.RED:CYAN);
     }
    }
    public void onResults(Bundle b){
     listening=false;
     if(suppressRecognizerCallbacks)return;
     ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
     if(listenMode==ListenMode.WAKE){
      boolean woke=false;
      String wakeCommand=null;
      if(a!=null)for(String r:a){
       if(CommandRouter.isWakePhrase(r)){
        woke=true;
        wakeCommand=CommandRouter.removeWakePhrase(r);
        break;
       }
      }
      if(woke){
       mainHandler.removeCallbacks(wakeRetry);
       listenMode=ListenMode.COMMAND;
       if(mic!=null)mic.setText("● LISTENING");
       if(wakeCommand!=null&&!wakeCommand.trim().isEmpty())command(wakeCommand.trim());
       else listen();
      }else if(handsFree){
       scheduleWake(250);
      }
      return;
     }
     if(mic!=null)mic.setText("🎙  TAP TO TALK");
     if(a!=null&&!a.isEmpty())command(a.get(0));
     else{
      listenMode=handsFree?ListenMode.WAKE:ListenMode.OFF;
      setState(handsFree?"● Waiting for “Hey Buddy”…":"● Ready",CYAN);
      if(handsFree)scheduleWake(250);
     }
    }
    public void onPartialResults(Bundle b){}
    public void onEvent(int e,Bundle b){}
   });
  }catch(Throwable t){
   sr=null;
   setState("● Voice input unavailable",Color.RED);
  }
 }

 private void scheduleWake(long delay){
  mainHandler.removeCallbacks(wakeRetry);
  if(handsFree&&!isFinishing())mainHandler.postDelayed(wakeRetry,delay);
 }

 private void listen(){
  mainHandler.removeCallbacks(wakeRetry);
  startRecognition(false);
 }

 private void listenForWakeWord(){
  if(!handsFree||speaking||isFinishing())return;
  if(listening)return;
  startRecognition(true);
 }

 private void startRecognition(boolean wake){
  suppressRecognizerCallbacks=false;
  stopTts();
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},7);
   return;
  }
  if(sr==null)initSpeech();
  if(sr==null){
   setState("● Voice input unavailable",Color.RED);
   return;
  }
  listenMode=wake?ListenMode.WAKE:ListenMode.COMMAND;
  Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,wake?"en-IN":lang);
  i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
  i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
  i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
  i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,900);
  i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,700);
  try{
   sr.startListening(i);
  }catch(RuntimeException e){
   listening=false;
   try{sr.destroy();}catch(Exception ignored){}
   sr=null;
   if(wake){scheduleWake(700);}
   else{
    setState("● Mic busy — tap again",Color.RED);
    if(mic!=null)mic.setText("🎙  TAP TO TALK");
   }
  }
 }

 private void stopListening(){
  suppressRecognizerCallbacks=true;
  mainHandler.removeCallbacks(wakeRetry);
  if(sr!=null&&listening){
   try{sr.cancel();}catch(Exception ignored){}
  }
  listening=false;
  if(mic!=null)mic.setText("🎙  TAP TO TALK");
 }
 private void stopTts(){
  speaking=false;
  if(tts!=null)try{tts.stop();}catch(Exception ignored){}
 }

 private void resumeWakeAfterSpeech(){
  speaking=false;
  if(handsFree && listenMode!=ListenMode.COMMAND && !isFinishing()){
   listenMode=ListenMode.WAKE;
   scheduleWake(180);
  }
 }
 private String norm(String s){return CommandRouter.normalize(s);}
 private boolean has(String s,String...x){for(String a:x)if(s.contains(a))return true;return false;}
 private Integer num(String s){return CommandRouter.extractNumber(s);}

 private void command(String raw){
  if(raw==null||raw.trim().isEmpty())return;
  user.setText(raw);
  setState("● Thinking…",CYAN);
  CommandEngine.execute(this,raw,msg->reply(msg));
 }
 private String extractAfter(String s,String...p){for(String x:p)if(s.startsWith(x))return s.substring(x.length()).trim();return null;}
 private void volume(String s){AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);Integer p=num(s);if(has(s,"mute","silent"))exec("Volume mute kar diya.",()->a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_MUTE,AudioManager.FLAG_SHOW_UI));else if(p!=null){int v=Math.round(a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*p/100f);exec("Volume "+p+" percent.",()->a.setStreamVolume(AudioManager.STREAM_MUSIC,Math.max(0,Math.min(a.getStreamMaxVolume(AudioManager.STREAM_MUSIC),v)),AudioManager.FLAG_SHOW_UI));}else if(has(s,"kam","down","decrease"))exec("Volume kam kar diya.",()->a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI));else exec("Volume badha diya.",()->a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI));}
 private void brightness(String s){Integer p=num(s);if(p!=null){final int q=p;exec("Brightness "+q+" percent.",()->setBright(q));}else if(has(s,"kam","down"))exec("Brightness kam kar raha hoon.",()->changeBright(-20));else exec("Brightness badha raha hoon.",()->changeBright(20));}
 private void openSettings(String action,String name){exec(name+" khol raha hoon.",()->startActivity(new Intent(action)));}
 private void startHome(){Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_HOME);startActivity(i);reply("Home.");}
 private void callFlow(String target){
  String raw=target==null?"":target.trim();
  if(raw.isEmpty()){reply("Number ya contact naam nahi mila.");return;}
  String number=raw.replaceAll("[^0-9+]","");
  if(number.isEmpty()){
   if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){
    pendingCallTarget=raw;
    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},11);
    reply("Contacts permission chahiye. Ek baar allow kar do.");
    return;
   }
   number=findContactNumber(raw);
  }
  if(number==null||number.isEmpty()){reply("Contact nahi mila.");return;}
  final String finalNumber=number;
  new AlertDialog.Builder(this)
   .setTitle("Call")
   .setMessage("Call "+raw+"?")
   .setNegativeButton("Cancel",null)
   .setPositiveButton("Call",(d,w)->placeCall(finalNumber))
   .show();
 }

 private void placeCall(String raw){
  String n=raw.replaceAll("[^0-9+]","");
  if(n.isEmpty()){reply("Valid number nahi mila.");return;}
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){
   pendingWaMessage="CALL:"+n;
   requestPermissions(new String[]{Manifest.permission.CALL_PHONE},10);
   reply("Phone call permission allow kar do.");
   return;
  }
  try{
   Intent i=new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+Uri.encode(n)));
   startActivity(i);
   reply("Calling…");
  }catch(Exception e){
   try{
    Intent i=new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(n)));
    startActivity(i);
    reply("Direct call blocked. Dialer open kar diya.");
   }catch(Exception ignored){reply("Call start nahi hua.");}
  }
 }
 private void sendSmsFlow(String s){
  final EditText e=new EditText(this);
  e.setText(s);
  e.setHint("message text");
  new AlertDialog.Builder(this).setTitle("Send SMS")
   .setMessage("Number manually enter karo. Buddy bina confirmation ke SMS nahi bhejega.")
   .setView(e).setNegativeButton("Cancel",null)
   .setPositiveButton("Continue",(d,w)->{
    final EditText n=new EditText(this);
    n.setHint("phone number");
    new AlertDialog.Builder(this).setTitle("Recipient").setView(n)
     .setNegativeButton("Cancel",null)
     .setPositiveButton("Send",(d2,w2)->sendSms(n.getText().toString(),e.getText().toString())).show();
   }).show();
 }

 private void sendSms(String n,String msg){
  String number=n==null?"":n.replaceAll("[^0-9+]","");
  String text=msg==null?"":msg.trim();
  if(number.isEmpty()){reply("Valid phone number nahi mila.");return;}
  if(text.isEmpty()){reply("Message empty hai.");return;}
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED){
   pendingSmsNumber=number; pendingSmsText=text;
   requestPermissions(new String[]{Manifest.permission.SEND_SMS},8);
   reply("SMS permission allow kar do.");
   return;
  }
  try{
   SmsManager.getDefault().sendTextMessage(number,null,text,null,null);
   reply("SMS request send kar di.");
  }catch(Exception e){reply("SMS send nahi hua.");}
 }
 private void toggleFlash(){
  try{
   android.hardware.camera2.CameraManager cm=(android.hardware.camera2.CameraManager)getSystemService(CAMERA_SERVICE);
   String id=null;
   for(String cameraId:cm.getCameraIdList()){
    android.hardware.camera2.CameraCharacteristics ch=cm.getCameraCharacteristics(cameraId);
    Boolean back=ch.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)==android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK;
    Boolean flash=ch.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
    if(Boolean.TRUE.equals(back)&&Boolean.TRUE.equals(flash)){id=cameraId;break;}
   }
   if(id==null)throw new IllegalStateException();
   boolean now=getPreferences(0).getBoolean("flash",false);
   cm.setTorchMode(id,!now);
   getPreferences(0).edit().putBoolean("flash",!now).apply();
   reply(!now?"Torch on.":"Torch off.");
  }catch(Exception e){reply("Torch control available nahi hai.");}
 }
 private String search(String s){return CommandRouter.googleSearchQuery(s);}
 private void exec(String msg,Runnable r){try{r.run();reply(msg);}catch(Exception e){reply("Phone ne ye action allow nahi kiya.");}}
 private void ok(String msg,Runnable r){try{r.run();reply(msg+" done.");}catch(Exception e){reply(msg+" nahi hua.");}}
 private void reply(String s){
  final String msg=(s==null||s.trim().isEmpty())?"Done.":s.trim();
  runOnUiThread(()->{
   if(isFinishing()||isDestroyed())return;
   if(buddy!=null)buddy.setText(msg);
   stopListening();
   if(handsFree)listenMode=ListenMode.WAKE; else listenMode=ListenMode.OFF;
   setState(handsFree?"● Waiting for “Hey Buddy”…":"● Ready",CYAN);
   if(ttsReady&&tts!=null){
    speaking=true;
    try{
     int r=tts.speak(msg,TextToSpeech.QUEUE_FLUSH,null,"buddy-"+System.nanoTime());
     if(r==TextToSpeech.ERROR){speaking=false;resumeWakeAfterSpeech();}
    }catch(Throwable ignored){
     speaking=false;
     resumeWakeAfterSpeech();
    }
   }else resumeWakeAfterSpeech();
  });
 }
 private void youtubeSearch(String q){
  String query=q==null?"":q.trim();
  if(query.isEmpty()){openApp("youtube");return;}
  String url="https://www.youtube.com/results?search_query="+Uri.encode(query);
  try{
   Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
   i.setPackage("com.google.android.youtube");
   startActivity(i);
  }catch(Exception e){
   try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
   catch(Exception ignored){reply("YouTube search open nahi hua.");}
  }
 }
 private void sendWhatsAppFlow(String contact,String message){
  pendingWaContact=contact;
  pendingWaMessage=message;
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},9);
   reply("Contacts permission chahiye. Ek baar allow kar do.");
   return;
  }
  openWhatsAppForContact(contact,message);
 }

 private void openWhatsAppForContact(String contact,String message){
  String number=findContactNumber(contact);
  if(number==null||number.isEmpty()){
   new AlertDialog.Builder(this)
    .setTitle("WhatsApp message")
    .setMessage("Contact “"+contact+"” nahi mila.")
    .setNegativeButton("Cancel",null)
    .setPositiveButton("Open WhatsApp",(d,w)->openApp("whatsapp"))
    .show();
   return;
  }
  String phone=formatWhatsAppNumber(number);
  String text=message==null?"":message.trim();
  String url="whatsapp://send?phone="+phone+"&text="+Uri.encode(text);
  try{
   Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
   i.setPackage("com.whatsapp");
   startActivity(i);
   reply("WhatsApp chat ready hai. Send tap kar dena.");
  }catch(Exception e){
   try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+phone+"?text="+Uri.encode(text))));reply("WhatsApp chat ready hai. Send tap kar dena.");}
   catch(Exception ignored){reply("WhatsApp open nahi hua.");}
  }
 }
 private String findContactNumber(String wanted){
  Cursor c=null;
  try{
   String q=wanted.replace("'","''");
   c=getContentResolver().query(
    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ?",
    new String[]{"%"+q+"%"},
    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY+" DESC"
   );
   if(c!=null&&c.moveToFirst()) return c.getString(0);
  }catch(Exception ignored){} finally {if(c!=null)c.close();}
  return null;
 }

 private String formatWhatsAppNumber(String raw){
  String n=raw.replaceAll("[^0-9]","");
  if(n.length()==10)n="91"+n;
  return n;
 }

 private void shareWhatsApp(String message){
  try{
   Intent i=new Intent(Intent.ACTION_SEND);
   i.setType("text/plain");
   i.setPackage("com.whatsapp");
   i.putExtra(Intent.EXTRA_TEXT,message);
   startActivity(i);
   reply("WhatsApp open hai, chat select karke Send tap karo.");
  }catch(Exception e){reply("WhatsApp available nahi hai.");}
 }

 private void openApp(String a){Intent i=getPackageManager().getLaunchIntentForPackage(apps.get(a));if(i==null)throw new ActivityNotFoundException();startActivity(i);}
 private void setBright(int p){if(!Settings.System.canWrite(this)){allowWrite();throw new IllegalStateException();}Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,Math.min(100,p))*255/100);}
 private void changeBright(int d){if(!Settings.System.canWrite(this)){allowWrite();throw new IllegalStateException();}int c=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128);Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,Math.min(255,c+(255*d/100))));}
 private void allowWrite(){startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())));}
 private void web(String q){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+URLEncoder.encode(q,"UTF-8"))));}catch(Exception e){reply("Search open nahi hua.");}}
 private String battery(){Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(i==null)return"Battery status nahi mila.";int l=i.getIntExtra("level",-1),sc=i.getIntExtra("scale",100);return l>=0?"Battery "+Math.round(l*100f/sc)+" percent hai.":"Battery status nahi mila.";}
 private boolean networkAvailable(){
  ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
  Network n=cm==null?null:cm.getActiveNetwork();
  NetworkCapabilities c=cm==null?null:cm.getNetworkCapabilities(n);
  return c!=null&&(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)||c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
 }

 private void cloud(String raw){
  String k=prefs.getString(KEY,"").trim();
  if(k.isEmpty()){reply("AI key add nahi hai.");return;}
  if(!networkAvailable()){reply("Offline ho — local commands use karo.");return;}
  net.execute(()->{
   HttpURLConnection c=null;
   try{
    JSONObject body=new JSONObject();
    body.put("model","openrouter/free");
    body.put("temperature",0.1);
    body.put("max_tokens",80);
    JSONArray m=new JSONArray();
    m.put(new JSONObject().put("role","system").put("content","You are Buddy. Reply in ONE short Hinglish/English sentence, maximum 12 words. Never claim device actions were completed."));
    m.put(new JSONObject().put("role","user").put("content",raw));
    body.put("messages",m);

    c=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();
    c.setRequestMethod("POST");
    c.setConnectTimeout(1200);
    c.setReadTimeout(3000);
    c.setUseCaches(true);
    c.setRequestProperty("Authorization","Bearer "+k);
    c.setRequestProperty("Content-Type","application/json");
    c.setRequestProperty("Accept","application/json");
    c.setDoOutput(true);
    try(OutputStream o=c.getOutputStream()){
      o.write(body.toString().getBytes(StandardCharsets.UTF_8));
    }
    int code=c.getResponseCode();
    if(code<200||code>=300){reply("AI response nahi aa raha.");return;}
    InputStream in=c.getInputStream();
    BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
    StringBuilder out=new StringBuilder(); String line;
    while((line=br.readLine())!=null)out.append(line);
    String ans=new JSONObject(out.toString()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","");
    reply(ans.replace("\n"," ").trim());
   }catch(Exception e){
    reply("AI slow hai. Local command bolo.");
   }finally{if(c!=null)c.disconnect();}
  });
 }
 private void settings(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(24),0,dp(24),0);EditText e=new EditText(this);e.setHint("OpenRouter API key");e.setSingleLine(true);e.setInputType(0x81);e.setText(prefs.getString(KEY,""));p.addView(e);Button a=btn("Accessibility settings",46,CARD2,13);a.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));p.addView(a);Button n=btn("Notification access",46,CARD2,13);n.setOnClickListener(v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));p.addView(n);new AlertDialog.Builder(this).setTitle("Buddy Settings").setView(p).setNegativeButton("Close",null).setPositiveButton("Save",(d,w)->prefs.edit().putString(KEY,e.getText().toString().trim()).apply()).show();}
 private void setState(String s,int c){runOnUiThread(()->{if(state!=null){state.setText(s);state.setTextColor(c);}});}
 private String pretty(String s){return s.substring(0,1).toUpperCase(Locale.ROOT)+s.substring(1);}
 @Override protected void onResume(){
  super.onResume();
  if(accessLabel!=null)accessLabel.setText(accessText());
  if(handsFree&&!listening&&listenMode==ListenMode.WAKE)scheduleWake(250);
 }

 @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
  super.onRequestPermissionsResult(requestCode,permissions,grantResults);
  boolean granted=grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED;
  if(requestCode==7){
   if(granted){if(handsFree)listenForWakeWord();else listen();}
   else reply("Microphone permission deny hua.");
  }else if(requestCode==8){
   if(granted&&!pendingSmsNumber.isEmpty()&&!pendingSmsText.isEmpty()){
    String n=pendingSmsNumber,m=pendingSmsText; pendingSmsNumber="";pendingSmsText="";
    sendSms(n,m);
   }else if(!granted)reply("SMS permission deny hua.");
  }else if(requestCode==9){
   if(granted) openWhatsAppForContact(pendingWaContact,pendingWaMessage);
   else reply("Contacts permission deny hua.");
  }else if(requestCode==10){
   String callNumber=pendingWaMessage!=null&&pendingWaMessage.startsWith("CALL:")?pendingWaMessage.substring(5):"";
   pendingWaMessage="";
   if(granted&&!callNumber.isEmpty())placeCall(callNumber);
   else if(!granted)reply("Phone call permission deny hua.");
  }else if(requestCode==11){
   if(granted){
    String n=findContactNumber(pendingCallTarget);
    if(n!=null&&!n.isEmpty()){
     final String number=n;
     new AlertDialog.Builder(this).setTitle("Call")
      .setMessage("Call "+pendingCallTarget+"?")
      .setNegativeButton("Cancel",null)
      .setPositiveButton("Call",(d,w)->placeCall(number)).show();
    }else reply("Contact nahi mila.");
   }else reply("Contacts permission deny hua.");
   pendingCallTarget="";
  }
 }

 @Override protected void onDestroy(){
  mainHandler.removeCallbacksAndMessages(null);
  suppressRecognizerCallbacks=true;
  if(sr!=null){try{sr.cancel();}catch(Exception ignored){}try{sr.destroy();}catch(Exception ignored){}sr=null;}
  if(tts!=null){try{tts.stop();}catch(Exception ignored){}tts.shutdown();}
  net.shutdownNow();
  super.onDestroy();
 }
}