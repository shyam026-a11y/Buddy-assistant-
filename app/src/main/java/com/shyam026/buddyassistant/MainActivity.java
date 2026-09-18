package com.shyam026.buddyassistant;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
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
 private boolean ttsReady,listening,handsFree; private String lang="en-IN";
 private final ExecutorService net=Executors.newSingleThreadExecutor();
 private TextView state,user,buddy; private Button mic,langBtn;
 private final int BG=Color.rgb(11,16,32),CARD=Color.rgb(23,28,51),CARD2=Color.rgb(31,37,65),WHITE=Color.WHITE,MUTED=Color.rgb(165,172,201),ACCENT=Color.rgb(124,92,255),CYAN=Color.rgb(93,208,255);
 private final Map<String,String> apps=new LinkedHashMap<>();

 @Override public void onCreate(Bundle b){
  super.onCreate(b); prefs=getSharedPreferences(PREF,0);
  String[][] a={{"youtube","com.google.android.youtube"},{"chrome","com.android.chrome"},{"whatsapp","com.whatsapp"},{"instagram","com.instagram.android"},{"spotify","com.spotify.music"},{"telegram","org.telegram.messenger"},{"maps","com.google.android.apps.maps"},{"gmail","com.google.android.gm"}};
  for(String[] x:a)apps.put(x[0],x[1]);
  initTts(); buildUi();
 }

 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
 private TextView txt(String s,float z,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(null,bold?1:0);return v;}
 private Button btn(String s,int h,int c,float z){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(z);b.setAllCaps(false);b.setMinHeight(0);b.setMinWidth(0);b.setGravity(Gravity.CENTER);b.setBackground(bg(c,18));return b;}
 private void gap(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}

 private void buildUi(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
  ScrollView sc=new ScrollView(this);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(20),dp(18),dp(20),dp(24));sc.addView(c);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));

  LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
  ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.buddy_logo);head.addView(logo,new LinearLayout.LayoutParams(dp(58),dp(58)));
  LinearLayout nm=new LinearLayout(this);nm.setOrientation(LinearLayout.VERTICAL);nm.setPadding(dp(12),0,0,0);nm.addView(txt("BUDDY",24,WHITE,true));nm.addView(txt("Fast • Private • Your assistant",13,MUTED,false));head.addView(nm,new LinearLayout.LayoutParams(0,-2,1));
  langBtn=btn("EN",46,CARD2,13);langBtn.setOnClickListener(v->{lang=lang.equals("en-IN")?"hi-IN":"en-IN";langBtn.setText(lang.equals("en-IN")?"EN":"HI");});head.addView(langBtn);c.addView(head);gap(c,14);

  state=txt("● Ready",13,CYAN,true);LinearLayout sb=new LinearLayout(this);sb.setPadding(dp(14),dp(10),dp(14),dp(10));sb.setBackground(bg(CARD,12));sb.addView(state);c.addView(sb);gap(c,12);
  LinearLayout convo=new LinearLayout(this);convo.setOrientation(LinearLayout.VERTICAL);convo.setPadding(dp(16),dp(16),dp(16),dp(16));convo.setBackground(bg(CARD,22));
  convo.addView(txt("YOU",11,MUTED,true));user=txt("Say something…",18,WHITE,false);user.setPadding(0,dp(5),0,dp(14));convo.addView(user);
  convo.addView(txt("BUDDY",11,MUTED,true));buddy=txt("Ready hoon. Bolo.",18,WHITE,false);convo.addView(buddy);c.addView(convo);gap(c,14);

  mic=btn("🎙  TAP TO TALK",58,ACCENT,15);LinearLayout mr=new LinearLayout(this);mr.setGravity(Gravity.CENTER);mr.addView(mic,new LinearLayout.LayoutParams(dp(240),dp(58)));c.addView(mr);mic.setOnClickListener(v->listen());gap(c,14);
  c.addView(txt("QUICK COMMANDS",11,MUTED,true));LinearLayout chips=new LinearLayout(this);
  chip(chips,"YouTube","YouTube kholo");chip(chips,"Volume +","volume badhao");chip(chips,"Brightness","brightness 60");chip(chips,"Back","back jao");c.addView(chips);gap(c,12);

  LinearLayout access=new LinearLayout(this);access.setOrientation(LinearLayout.VERTICAL);access.setPadding(dp(14),dp(12),dp(14),dp(12));access.setBackground(bg(CARD,16));
  access.addView(txt("DEVICE CONTROL",11,MUTED,true));TextView at=txt(accessText(),14,WHITE,false);access.addView(at);Button ae=btn("Enable phone control",44,CARD2,13);ae.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));access.addView(ae);c.addView(access);gap(c,12);

  LinearLayout opts=new LinearLayout(this);Switch sw=new Switch(this);sw.setText("Hey Buddy / hands-free mode");sw.setTextColor(WHITE);sw.setOnCheckedChangeListener((v,x)->{handsFree=x;if(x)listen();});opts.addView(sw,new LinearLayout.LayoutParams(0,-2,1));
  Button set=btn("Settings",46,CARD2,14);set.setOnClickListener(v->settings());opts.addView(set);c.addView(opts);gap(c,12);
  TextView foot=txt("Local actions first • AI fallback • Accessibility is optional",12,MUTED,false);foot.setGravity(Gravity.CENTER);c.addView(foot);
  setContentView(root);initSpeech();
 }

 private String accessText(){return BuddyAccessibilityService.isEnabled()?"✓ Full navigation control enabled":"○ Optional: enable Accessibility for Back/Home/Recents/scroll/tap/type";}
 private void chip(LinearLayout r,String label,String cmd){Button b=btn(label,42,CARD2,12);b.setOnClickListener(v->command(cmd));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,42,1);p.setMargins(3,0,3,0);r.addView(b,p);}

 private void initTts(){tts=new TextToSpeech(this,x->{if(x==TextToSpeech.SUCCESS){ttsReady=true;tts.setSpeechRate(1.08f);tts.setPitch(1.08f);}});}
 private void initSpeech(){
  if(!SpeechRecognizer.isRecognitionAvailable(this)){setState("● Speech recognition unavailable",Color.RED);return;}
  sr=SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(new RecognitionListener(){
   public void onReadyForSpeech(Bundle b){listening=true;mic.setText("● LISTENING");setState("● Listening…",CYAN);}
   public void onBeginningOfSpeech(){} public void onRmsChanged(float r){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){}
   public void onError(int e){listening=false;mic.setText("🎙 TAP TO TALK");setState("● Ready",CYAN);if(handsFree)mic.postDelayed(MainActivity.this::listen,700);}
   public void onResults(Bundle b){listening=false;mic.setText("🎙 TAP TO TALK");ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())command(a.get(0));}
   public void onPartialResults(Bundle b){} public void onEvent(int e,Bundle b){}
  });
 }

 private void listen(){
  if(listening)return;
  if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},7);return;}
  if(sr==null)initSpeech();Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang);i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
  try{sr.startListening(i);}catch(Exception e){setState("● Mic busy",Color.RED);}
 }

 private String norm(String s){return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+% ]"," ").replaceAll("\\s+"," ").trim();}
 private boolean has(String s,String...x){for(String a:x)if(s.contains(a))return true;return false;}
 private Integer num(String s){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\b(\\d{1,3})\\b").matcher(s);return m.find()?Integer.valueOf(m.group(1)):null;}

 private void command(String raw){
  if(raw==null)return;user.setText(raw);setState("● Thinking…",CYAN);String s=norm(raw);
  if(has(s,"hey buddy","ok buddy","hello buddy","hi buddy","namaste")){reply("Hey! Main yahin hoon. Bolo.");return;}
  if(has(s,"time batao","what time","kitne baje","time kya")){reply(new SimpleDateFormat("hh:mm a",Locale.getDefault()).format(new Date()));return;}
  if(has(s,"date batao","aaj ki date","today date")){reply(new SimpleDateFormat("dd MMMM yyyy",Locale.getDefault()).format(new Date()));return;}
  if(has(s,"battery","charge kitna")){reply(battery());return;}

  if(has(s,"back jao","go back","back")){if(BuddyAccessibilityService.isEnabled())ok("Back",()->BuddyAccessibilityService.get().global("back"));else reply("Accessibility enable karo, tab main Back kar sakta hoon.");return;}
  if(has(s,"home screen","go home","home jao")){if(BuddyAccessibilityService.isEnabled())ok("Home",()->BuddyAccessibilityService.get().global("home"));else startHome();return;}
  if(has(s,"recent apps","recents")){if(BuddyAccessibilityService.isEnabled())ok("Recent apps",()->BuddyAccessibilityService.get().global("recents"));else reply("Accessibility enable karo.");return;}
  if(has(s,"notifications kholo","notification panel","notifications")){if(BuddyAccessibilityService.isEnabled())ok("Notifications",()->BuddyAccessibilityService.get().global("notifications"));else reply("Accessibility enable karo.");return;}
  if(has(s,"quick settings")){if(BuddyAccessibilityService.isEnabled())ok("Quick settings",()->BuddyAccessibilityService.get().global("quick_settings"));else reply("Accessibility enable karo.");return;}
  if(has(s,"scroll down","neeche scroll","scroll neeche")){if(BuddyAccessibilityService.isEnabled())ok("Scroll down",()->BuddyAccessibilityService.get().scroll(true));else reply("Accessibility enable karo.");return;}
  if(has(s,"scroll up","upar scroll","scroll upar")){if(BuddyAccessibilityService.isEnabled())ok("Scroll up",()->BuddyAccessibilityService.get().scroll(false));else reply("Accessibility enable karo.");return;}

  if(has(s,"mute","silent","volume")){volume(s);return;}
  if(has(s,"brightness","roshni","screen bright")){brightness(s);return;}
  if(has(s,"wifi","wi fi","wi-fi")){openSettings(Settings.ACTION_WIFI_SETTINGS,"Wi-Fi settings");return;}
  if(has(s,"bluetooth")){openSettings(Settings.ACTION_BLUETOOTH_SETTINGS,"Bluetooth settings");return;}
  if(has(s,"settings khol","open settings")){openSettings(Settings.ACTION_SETTINGS,"Settings");return;}

  String call=extractAfter(s,"call ","phone ");if(call!=null&&!call.isEmpty()){dial(call);return;}
  String sms=extractAfter(s,"sms ","message ","text ");if(sms!=null&&!sms.isEmpty()){sendSmsFlow(sms);return;}

  String tap=extractAfter(s,"tap ","click ");if(tap!=null&&!tap.isEmpty()){if(BuddyAccessibilityService.isEnabled())ok("Tap "+tap,()->BuddyAccessibilityService.get().clickText(tap));else reply("Accessibility enable karo.");return;}
  String type=extractAfter(s,"type ","likho ");if(type!=null&&!type.isEmpty()){if(BuddyAccessibilityService.isEnabled())ok("Text enter kar raha hoon.",()->BuddyAccessibilityService.get().typeText(type));else reply("Accessibility enable karo.");return;}

  boolean open=has(s,"khol","open","launch","start","chala");String app=null;if(open)for(String k:apps.keySet())if(s.contains(k)){app=k;break;}
  if(app!=null){String a=app;exec(pretty(a)+" khol raha hoon.",()->openApp(a));return;}

  if(has(s,"flashlight","torch")){toggleFlash();return;}
  if(has(s,"play music","music chala","pause music","media")){((AudioManager)getSystemService(AUDIO_SERVICE)).dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));reply("Media control.");return;}

  String q=search(s);if(q!=null){exec("Google search khol raha hoon.",()->web(q));return;}
  cloud(raw);
 }

 private String extractAfter(String s,String...p){for(String x:p)if(s.startsWith(x))return s.substring(x.length()).trim();return null;}
 private void volume(String s){AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);Integer p=num(s);if(has(s,"mute","silent"))exec("Volume mute kar diya.",()->a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_MUTE,AudioManager.FLAG_SHOW_UI));else if(p!=null){int v=Math.round(a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*p/100f);exec("Volume "+p+" percent.",()->a.setStreamVolume(AudioManager.STREAM_MUSIC,Math.max(0,Math.min(a.getStreamMaxVolume(AudioManager.STREAM_MUSIC),v)),AudioManager.FLAG_SHOW_UI));}else if(has(s,"kam","down","decrease"))exec("Volume kam kar diya.",()->a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI));else exec("Volume badha diya.",()->a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI));}
 private void brightness(String s){Integer p=num(s);if(p!=null){final int q=p;exec("Brightness "+q+" percent.",()->setBright(q));}else if(has(s,"kam","down"))exec("Brightness kam kar raha hoon.",()->changeBright(-20));else exec("Brightness badha raha hoon.",()->changeBright(20));}
 private void openSettings(String action,String name){exec(name+" khol raha hoon.",()->startActivity(new Intent(action)));}
 private void startHome(){Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_HOME);startActivity(i);reply("Home.");}
 private void dial(String number){String n=number.replaceAll("[^0-9+]","");if(n.isEmpty()){reply("Number nahi mila.");return;}Intent i=new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(n)));startActivity(i);reply("Dialer khol raha hoon.");}
 private void sendSmsFlow(String s){final EditText e=new EditText(this);e.setText(s);e.setHint("message text");new AlertDialog.Builder(this).setTitle("Send SMS").setMessage("Number manually enter karo. Buddy bina confirmation ke SMS nahi bhejega.").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->{final EditText n=new EditText(this);n.setHint("phone number");new AlertDialog.Builder(this).setTitle("Recipient").setView(n).setNegativeButton("Cancel",null).setPositiveButton("Send",(d2,w2)->sendSms(n.getText().toString(),e.getText().toString())).show();}).show();}
 private void sendSms(String n,String msg){try{if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.SEND_SMS},8);return;}SmsManager.getDefault().sendTextMessage(n,null,msg,null,null);reply("SMS send kar diya.");}catch(Exception e){reply("SMS send nahi hua.");}}
 private void toggleFlash(){try{android.hardware.camera2.CameraManager cm=(android.hardware.camera2.CameraManager)getSystemService(CAMERA_SERVICE);String id=cm.getCameraIdList()[0];Boolean now=getPreferences(0).getBoolean("flash",false);cm.setTorchMode(id,!now);getPreferences(0).edit().putBoolean("flash",!now).apply();reply(!now?"Torch on.":"Torch off.");}catch(Exception e){reply("Torch control available nahi hai.");}}
 private String search(String s){String[]p={"google pe search ","google par search ","search ","find "};for(String x:p)if(s.startsWith(x))return s.substring(x.length()).trim();return null;}
 private void exec(String msg,Runnable r){try{r.run();reply(msg);}catch(Exception e){reply("Phone ne ye action allow nahi kiya.");}}
 private void ok(String msg,Runnable r){try{r.run();reply(msg+" done.");}catch(Exception e){reply(msg+" nahi hua.");}}
 private void reply(String s){runOnUiThread(()->{buddy.setText(s);setState("● Ready",CYAN);if(ttsReady)try{tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"buddy");}catch(Exception ignored){}if(handsFree)mic.postDelayed(this::listen,650);});}
 private void openApp(String a){Intent i=getPackageManager().getLaunchIntentForPackage(apps.get(a));if(i==null)throw new ActivityNotFoundException();startActivity(i);}
 private void setBright(int p){if(!Settings.System.canWrite(this)){allowWrite();throw new IllegalStateException();}Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,Math.min(100,p))*255/100);}
 private void changeBright(int d){if(!Settings.System.canWrite(this)){allowWrite();throw new IllegalStateException();}int c=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128);Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,Math.min(255,c+(255*d/100))));}
 private void allowWrite(){startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())));}
 private void web(String q){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+URLEncoder.encode(q,"UTF-8"))));}catch(Exception e){reply("Search open nahi hua.");}}
 private String battery(){Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(i==null)return"Battery status nahi mila.";int l=i.getIntExtra("level",-1),sc=i.getIntExtra("scale",100);return l>=0?"Battery "+Math.round(l*100f/sc)+" percent hai.":"Battery status nahi mila.";}
 private void cloud(String raw){String k=prefs.getString(KEY,"").trim();if(k.isEmpty()){reply("Ye command local mode me samajh nahi aayi. Settings me AI key add kar sakte ho.");return;}net.execute(()->{try{JSONObject body=new JSONObject();body.put("model","openrouter/free");body.put("temperature",0.2);JSONArray m=new JSONArray();m.put(new JSONObject().put("role","system").put("content","You are Buddy. Reply in one short friendly Hinglish or English sentence. Never claim device actions were completed."));m.put(new JSONObject().put("role","user").put("content",raw));body.put("messages",m);HttpURLConnection c=(HttpURLConnection)new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();c.setRequestMethod("POST");c.setConnectTimeout(3500);c.setReadTimeout(7000);c.setRequestProperty("Authorization","Bearer "+k);c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();String line;while((line=br.readLine())!=null)out.append(line);if(code<200||code>=300){reply("Cloud AI unavailable hai.");return;}String ans=new JSONObject(out.toString()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","");reply(ans.replace("\n"," ").trim());}catch(Exception e){reply("Network slow hai ya AI unavailable hai.");}});}
 private void settings(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(24),0,dp(24),0);EditText e=new EditText(this);e.setHint("OpenRouter API key");e.setSingleLine(true);e.setInputType(0x81);e.setText(prefs.getString(KEY,""));p.addView(e);Button a=btn("Accessibility settings",46,CARD2,13);a.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));p.addView(a);Button n=btn("Notification access",46,CARD2,13);n.setOnClickListener(v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));p.addView(n);new AlertDialog.Builder(this).setTitle("Buddy Settings").setView(p).setNegativeButton("Close",null).setPositiveButton("Save",(d,w)->prefs.edit().putString(KEY,e.getText().toString().trim()).apply()).show();}
 private void setState(String s,int c){runOnUiThread(()->{if(state!=null){state.setText(s);state.setTextColor(c);}});}
 private String pretty(String s){return s.substring(0,1).toUpperCase(Locale.ROOT)+s.substring(1);}
 @Override protected void onDestroy(){if(sr!=null)sr.destroy();if(tts!=null)tts.shutdown();net.shutdownNow();super.onDestroy();}
}