package com.shyam026.buddyassistant;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.*;
import android.speech.tts.*;
import java.util.*;

public class BuddyVoiceService extends Service {
    private static final int NOTIFICATION_ID=4242;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean listening=false, speaking=false, stopping=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable restart=()->startWakeListening();

    @Override public void onCreate(){
        super.onCreate();
        createChannel();
        tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS) BuddyVoiceProfile.apply(tts,Locale.forLanguageTag("en-IN"));});
        initRecognizer();
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        stopping=false;
        startForeground(NOTIFICATION_ID,notification());
        if(Build.VERSION.SDK_INT>=31 && Build.VERSION.SDK_INT<34 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            stopSelf(); return START_NOT_STICKY;
        }
        startWakeListening();
        return START_STICKY;
    }

    private void initRecognizer(){
        if(!SpeechRecognizer.isRecognitionAvailable(this))return;
        try{
            if(Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))
                recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            else recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener(){
                public void onReadyForSpeech(Bundle b){listening=true;}
                public void onBeginningOfSpeech(){}
                public void onRmsChanged(float r){}
                public void onBufferReceived(byte[] b){}
                public void onEndOfSpeech(){}
                public void onError(int e){
                    listening=false;
                    if(!stopping&&!speaking) schedule(500);
                }
                public void onResults(Bundle b){
                    listening=false;
                    if(stopping)return;
                    ArrayList<String> results=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String command=null;
                    if(results!=null)for(String r:results){
                        if(CommandRouter.isWakePhrase(r)){
                            command=CommandRouter.removeWakePhrase(r);
                            break;
                        }
                    }
                    if(command!=null){
                        if(command.trim().isEmpty()){
                            speak("Haan, bolo.");
                        }else{
                            execute(command.trim());
                        }
                    }
                    schedule(command!=null?900:150);
                }
                public void onPartialResults(Bundle b){}
                public void onEvent(int e,Bundle b){}
            });
        }catch(Throwable ignored){recognizer=null;}
    }

    private void startWakeListening(){
        if(stopping||speaking||listening)return;
        if(recognizer==null){initRecognizer();if(recognizer==null)return;}
        if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return;
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
        i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,900);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,700);
        try{recognizer.startListening(i);listening=true;}catch(Throwable t){listening=false;schedule(800);}
    }

    private void execute(String command){
        CommandEngine.execute(this,command,msg->{
            speak(msg);
            sendBroadcast(new Intent("com.shyam026.buddyassistant.COMMAND_RESULT").putExtra("message",msg));
        });
    }

    private void speak(String msg){
        if(tts==null||msg==null)return;
        speaking=true;
        try{
            String natural=BuddyVoiceProfile.naturalize(msg);
            int result=tts.speak(natural,TextToSpeech.QUEUE_FLUSH,null,"buddy-service-"+System.nanoTime());
            if(result==TextToSpeech.ERROR){
                speaking=false;
                schedule(250);
                return;
            }
            handler.postDelayed(()->{
                speaking=false;
                schedule(250);
            },Math.max(1000,natural.length()*48L));
        }catch(Throwable t){
            speaking=false;
            schedule(250);
        }
    }
    private void schedule(long delay){handler.removeCallbacks(restart);if(!stopping)handler.postDelayed(restart,delay);}

    private Notification notification(){
        return new Notification.Builder(this,"buddy_voice")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Buddy is listening")
            .setContentText("Say “Hey Buddy” to activate")
            .setOngoing(true)
            .build();
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager nm=getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("buddy_voice","Buddy voice",NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override public void onDestroy(){
        stopping=true;handler.removeCallbacksAndMessages(null);
        if(recognizer!=null){try{recognizer.cancel();}catch(Exception ignored){}try{recognizer.destroy();}catch(Exception ignored){}}
        if(tts!=null){try{tts.stop();}catch(Exception ignored){}tts.shutdown();}
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}