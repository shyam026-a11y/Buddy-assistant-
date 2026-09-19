package com.shyam026.buddyassistant;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.Locale;

public class BuddyVoiceService extends Service {
    public static final String ACTION_STATUS = "com.shyam026.buddyassistant.VOICE_STATUS";
    public static final String ACTION_ENABLE_WAKE = "com.shyam026.buddyassistant.ENABLE_WAKE";
    public static final String ACTION_TAP_COMMAND = "com.shyam026.buddyassistant.TAP_COMMAND";
    public static final String ACTION_STOP = "com.shyam026.buddyassistant.STOP";

    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_TRANSCRIPT = "transcript";
    public static final String EXTRA_REPLY = "reply";
    public static final String EXTRA_COMMAND = "command";

    public static final String STATE_IDLE = "idle";
    public static final String STATE_WAITING_WAKE = "waiting_wake";
    public static final String STATE_LISTENING_COMMAND = "listening_command";
    public static final String STATE_PROCESSING = "processing";
    public static final String STATE_SPEAKING = "speaking";
    public static final String STATE_ERROR = "error";

    private static final String PREF = "buddy_prefs";
    private static final String KEY_WAKE = "wake_mode";
    private static final String KEY_LANG = "language";
    private static final String KEY_RATE = "speech_rate";
    private static final String KEY_PITCH = "speech_pitch";
    private static final int NOTIFICATION_ID = 4242;

    private enum Mode { IDLE, WAKE, COMMAND }

    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Mode mode = Mode.IDLE;
    private boolean listening;
    private boolean speaking;
    private boolean stopping;
    private boolean recognizerStarting;
    private long lastWakeTriggerAt;

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initTts();
        initRecognizer();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        stopping = false;
        startAsForeground();

        String action = intent == null ? null : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopVoiceService();
            return START_NOT_STICKY;
        }

        if (ACTION_ENABLE_WAKE.equals(action)) {
            getSharedPreferences(PREF, MODE_PRIVATE)
                    .edit().putBoolean(KEY_WAKE, true).apply();
            stopSpeaking();
            cancelRecognition();
            mode = Mode.WAKE;
            announce(STATE_WAITING_WAKE, null,
                    "Hey Buddy mode on hai. Bolo.");
            startWakeRecognition();
            return START_STICKY;
        }

        if (ACTION_TAP_COMMAND.equals(action)) {
            String explicit = intent.getStringExtra(EXTRA_COMMAND);
            mode = Mode.COMMAND;
            stopSpeaking();
            cancelRecognition();
            if (explicit != null && !explicit.trim().isEmpty()) {
                executeCommand(explicit.trim());
            } else {
                startRecognition();
            }
            return START_NOT_STICKY;
        }

        mode = Mode.IDLE;
        announce(STATE_IDLE, null, null);
        return START_NOT_STICKY;
    }

    private void startAsForeground() {
        try {
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Throwable t) {
            announce(STATE_ERROR, null, "Buddy voice service could not start.");
        }
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) return;
            applyLanguage();
            applyVoiceTuning();
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {
                    speaking = true;
                    announce(STATE_SPEAKING, null, null);
                }

                @Override public void onDone(String id) {
                    handler.post(() -> resumeAfterSpeech());
                }

                @Override public void onError(String id) {
                    handler.post(() -> resumeAfterSpeech());
                }
            });
        });
    }

    private void applyLanguage() {
        try {
            String tag = getSharedPreferences(PREF, MODE_PRIVATE)
                    .getString(KEY_LANG, "en-IN");
            BuddyVoiceProfile.apply(tts, Locale.forLanguageTag(tag));
        } catch (Throwable ignored) {}
    }

    private void applyVoiceTuning() {
        if (tts == null) return;
        try {
            android.content.SharedPreferences p =
                    getSharedPreferences(PREF, MODE_PRIVATE);
            int rate = Math.max(0, Math.min(60, p.getInt(KEY_RATE, 30)));
            int pitch = Math.max(0, Math.min(60, p.getInt(KEY_PITCH, 36)));
            tts.setSpeechRate(0.75f + (rate / 60f) * 0.60f);
            tts.setPitch(0.85f + (pitch / 60f) * 0.50f);
        } catch (Throwable ignored) {}
    }

    private void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = null;
            announce(STATE_ERROR, null, "Speech recognition is unavailable on this device.");
            return;
        }

        try {
            if (recognizer != null) recognizer.destroy();

            recognizer = null;
            if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                try {
                    recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
                } catch (Throwable ignored) {}
            }
            if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(this);

            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    listening = true;
                    if (mode == Mode.COMMAND) announce(STATE_LISTENING_COMMAND, null, null);
                    else if (mode == Mode.WAKE) announce(STATE_WAITING_WAKE, null, null);
                }

                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}

                @Override public void onError(int error) {
                    listening = false;
                    recognizerStarting = false;
                    if (stopping || speaking) return;

                    if (mode == Mode.WAKE
                            && getSharedPreferences(PREF, MODE_PRIVATE)
                            .getBoolean(KEY_WAKE, false)) {
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            mode = Mode.IDLE;
                            announce(STATE_ERROR, null,
                                    "Microphone permission required.");
                            return;
                        }
                        announce(STATE_WAITING_WAKE, null, null);
                        handler.postDelayed(() -> startWakeRecognition(), 350L);
                        return;
                    }

                    String msg = error == SpeechRecognizer.ERROR_NO_MATCH
                            ? "Voice samajh nahi aayi. Dobara bolo."
                            : error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                            ? "Microphone permission required."
                            : "Voice input failed. Dobara try karo.";
                    mode = Mode.IDLE;
                    announce(error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                            ? STATE_ERROR : STATE_IDLE, null, msg);
                }

                @Override public void onResults(Bundle results) {
                    listening = false;
                    recognizerStarting = false;
                    if (stopping || speaking) return;

                    ArrayList<String> phrases = results == null
                            ? null
                            : results.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION);

                    String command = firstUsable(phrases);

                    if (mode == Mode.WAKE) {
                        handleWakeTranscript(command);
                        return;
                    }

                    if (command == null) {
                        finishCommand();
                    } else {
                        executeCommand(command);
                    }
                }

                @Override public void onPartialResults(Bundle partialResults) {
                    if (stopping || speaking || mode != Mode.WAKE) return;
                    ArrayList<String> phrases = partialResults == null
                            ? null
                            : partialResults.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION);
                    String partial = firstUsable(phrases);
                    if (partial != null) handleWakeTranscript(partial);
                }
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        } catch (Throwable t) {
            recognizer = null;
            announce(STATE_ERROR, null, "Voice input could not be initialized.");
        }
    }

    private String firstUsable(ArrayList<String> phrases) {
        if (phrases == null) return null;
        for (String p : phrases) {
            if (p != null && !p.trim().isEmpty()) return p.trim();
        }
        return null;
    }

    private void executeCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            finishCommand();
            return;
        }
        cancelRecognition();
        announce(STATE_PROCESSING, command, null);
        CommandEngine.execute(this, command, message -> speak(message));
    }

    private void handleWakeTranscript(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) return;

        String normalized = CommandRouter.normalize(transcript);
        String remainder = CommandRouter.removeWakePhrase(normalized);
        if (remainder == null) return;

        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastWakeTriggerAt < 1200L) return;
        lastWakeTriggerAt = now;

        cancelRecognition();

        if (remainder.trim().isEmpty()) {
            speak("Haan, bolo.");
        } else {
            executeCommand(remainder);
        }
    }

    private void startWakeRecognition() {
        if (stopping || speaking || listening || recognizerStarting) return;

        if (!getSharedPreferences(PREF, MODE_PRIVATE)
                .getBoolean(KEY_WAKE, false)) {
            mode = Mode.IDLE;
            announce(STATE_IDLE, null, null);
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            mode = Mode.IDLE;
            announce(STATE_ERROR, null, "Microphone permission required.");
            return;
        }

        if (recognizer == null) {
            initRecognizer();
            if (recognizer == null) return;
        }

        mode = Mode.WAKE;
        recognizerStarting = true;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE,
                        getSharedPreferences(PREF, MODE_PRIVATE)
                                .getString(KEY_LANG, "en-IN"))
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                .putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                        1200L)
                .putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                        900L)
                .putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                        250L);

        try {
            recognizer.startListening(intent);
        } catch (Throwable t) {
            recognizerStarting = false;
            listening = false;
            handler.postDelayed(this::startWakeRecognition, 500L);
        }
    }

    private void startRecognition() {
        if (stopping || speaking || listening || recognizerStarting) return;

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mode = Mode.IDLE;
            announce(STATE_ERROR, null, "Microphone permission required.");
            return;
        }

        if (recognizer == null) {
            initRecognizer();
            if (recognizer == null) return;
        }

        mode = Mode.COMMAND;
        recognizerStarting = true;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                        getSharedPreferences(PREF, MODE_PRIVATE).getString(KEY_LANG, "en-IN"))
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 6)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 950L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 750L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 180L);

        try {
            recognizer.startListening(intent);
        } catch (Throwable t) {
            recognizerStarting = false;
            listening = false;
            announce(STATE_ERROR, null, "Mic is busy. Try again.");
        }
    }

    private void cancelRecognition() {
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (Throwable ignored) {}
        }
        listening = false;
        recognizerStarting = false;
    }

    private void stopSpeaking() {
        speaking = false;
        if (tts != null) {
            try { tts.stop(); } catch (Throwable ignored) {}
        }
    }

    private void speak(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "Command samajh nahi aayi.";
        }

        cancelRecognition();
        applyLanguage();
        applyVoiceTuning();
        speaking = true;
        announce(STATE_SPEAKING, null, message);

        if (tts == null) {
            resumeAfterSpeech();
            return;
        }

        try {
            int result = tts.speak(
                    BuddyVoiceProfile.naturalize(message),
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "buddy-" + System.nanoTime());
            if (result == TextToSpeech.ERROR) resumeAfterSpeech();
        } catch (Throwable t) {
            resumeAfterSpeech();
        }
    }

    private void resumeAfterSpeech() {
        if (stopping) return;
        speaking = false;

        boolean wake = getSharedPreferences(PREF, MODE_PRIVATE)
                .getBoolean(KEY_WAKE, false);
        if (wake) {
            mode = Mode.WAKE;
            announce(STATE_WAITING_WAKE, null, null);
            handler.postDelayed(() -> startWakeRecognition(), 300L);
        } else {
            mode = Mode.IDLE;
            announce(STATE_IDLE, null, null);
        }
    }

    private void finishCommand() {
        mode = Mode.IDLE;
        announce(STATE_IDLE, null, null);
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "buddy_voice")
                : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Buddy")
                .setContentText("Tap to talk or use system assistant")
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(
                    "buddy_voice",
                    "Buddy voice",
                    NotificationManager.IMPORTANCE_LOW));
        }
    }

    private void announce(String state, String transcript, String reply) {
        Intent i = new Intent(ACTION_STATUS).setPackage(getPackageName());
        i.putExtra(EXTRA_STATE, state);
        if (transcript != null) i.putExtra(EXTRA_TRANSCRIPT, transcript);
        if (reply != null) i.putExtra(EXTRA_REPLY, reply);
        sendBroadcast(i);
    }

    private void stopVoiceService() {
        stopping = true;
        handler.removeCallbacksAndMessages(null);
        cancelRecognition();
        stopSpeaking();
        mode = Mode.IDLE;
        stopSelf();
    }

    @Override public void onDestroy() {
        stopping = true;
        handler.removeCallbacksAndMessages(null);
        cancelRecognition();
        stopSpeaking();
        if (recognizer != null) {
            try { recognizer.destroy(); } catch (Throwable ignored) {}
            recognizer = null;
        }
        if (tts != null) {
            try { tts.stop(); tts.shutdown(); } catch (Throwable ignored) {}
            tts = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
