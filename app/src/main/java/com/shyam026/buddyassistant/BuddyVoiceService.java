package com.shyam026.buddyassistant;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
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
    public static final String ACTION_STATUS =
            "com.shyam026.buddyassistant.VOICE_STATUS";
    public static final String ACTION_ENABLE_WAKE =
            "com.shyam026.buddyassistant.ENABLE_WAKE";
    public static final String ACTION_TAP_COMMAND =
            "com.shyam026.buddyassistant.TAP_COMMAND";
    public static final String ACTION_STOP =
            "com.shyam026.buddyassistant.STOP";

    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_TRANSCRIPT = "transcript";
    public static final String EXTRA_REPLY = "reply";
    public static final String EXTRA_COMMAND = "command";

    public static final String STATE_IDLE = "idle";
    public static final String STATE_WAITING_WAKE = "waiting_wake";
    public static final String STATE_LISTENING_COMMAND = "listening_command";
    public static final String STATE_SPEAKING = "speaking";
    public static final String STATE_ERROR = "error";

    private static final String PREF = "buddy_prefs";
    private static final String KEY_WAKE = "wake_mode";
    private static final String KEY_LANG = "language";
    private static final int NOTIFICATION_ID = 4242;

    private enum Mode { IDLE, WAKE, COMMAND }

    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Mode mode = Mode.IDLE;
    private boolean listening;
    private boolean speaking;
    private boolean stopping;
    private boolean followUpAfterWake;
    private long retryDelay = 600L;

    private final Runnable wakeRetry = () -> {
        if (!stopping && mode == Mode.WAKE && !listening && !speaking) {
            startRecognition(true);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initTts();
        initRecognizer();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        stopping = false;
        startForeground(NOTIFICATION_ID, buildNotification());

        String action = intent == null ? null : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopVoiceService();
            return START_NOT_STICKY;
        }

        if (ACTION_ENABLE_WAKE.equals(action)) {
            getSharedPreferences(PREF, MODE_PRIVATE).edit()
                    .putBoolean(KEY_WAKE, true)
                    .apply();
            followUpAfterWake = false;
            stopSpeaking();
            mode = Mode.WAKE;
            stopListeningOnly();
            retryDelay = 600L;
            scheduleWake(100);
            sendState(STATE_WAITING_WAKE, null, null);
            return START_STICKY;
        }

        if (ACTION_TAP_COMMAND.equals(action)) {
            String explicitCommand = intent == null
                    ? null
                    : intent.getStringExtra(EXTRA_COMMAND);

            getSharedPreferences(PREF, MODE_PRIVATE).edit()
                    .putBoolean(KEY_WAKE, getSharedPreferences(PREF, MODE_PRIVATE)
                            .getBoolean(KEY_WAKE, false))
                    .apply();

            mode = Mode.COMMAND;
            followUpAfterWake = false;
            stopSpeaking();
            stopListeningOnly();

            if (explicitCommand != null && !explicitCommand.trim().isEmpty()) {
                executeCommand(explicitCommand.trim());
            } else {
                startRecognition(false);
            }
            return START_STICKY;
        }

        boolean wake = getSharedPreferences(PREF, MODE_PRIVATE)
                .getBoolean(KEY_WAKE, false);
        mode = wake ? Mode.WAKE : Mode.IDLE;
        if (wake) {
            scheduleWake(100);
            sendState(STATE_WAITING_WAKE, null, null);
        } else {
            sendState(STATE_IDLE, null, null);
        }
        return START_STICKY;
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                applyLanguage();
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {
                        speaking = true;
                        sendState(STATE_SPEAKING, null, null);
                    }

                    @Override public void onDone(String utteranceId) {
                        handler.post(this::resumeAfterSpeech);
                    }

                    @Override public void onError(String utteranceId) {
                        handler.post(this::resumeAfterSpeech);
                    }

                    private void resumeAfterSpeech() {
                        speaking = false;
                        if (stopping) return;

                        if (followUpAfterWake) {
                            followUpAfterWake = false;
                            mode = Mode.COMMAND;
                            scheduleCommandRecognition(160);
                        } else if (getSharedPreferences(PREF, MODE_PRIVATE)
                                .getBoolean(KEY_WAKE, false)) {
                            mode = Mode.WAKE;
                            scheduleWake(220);
                            sendState(STATE_WAITING_WAKE, null, null);
                        } else {
                            mode = Mode.IDLE;
                            sendState(STATE_IDLE, null, null);
                        }
                    }
                });
            }
        });
    }

    private void applyLanguage() {
        try {
            String tag = getSharedPreferences(PREF, MODE_PRIVATE)
                    .getString(KEY_LANG, "en-IN");
            BuddyVoiceProfile.apply(tts, Locale.forLanguageTag(tag));
        } catch (Throwable ignored) {
        }
    }

    private void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = null;
            sendState(STATE_ERROR, null, "Speech recognition unavailable.");
            return;
        }

        try {
            if (recognizer != null) {
                recognizer.destroy();
                recognizer = null;
            }

            if (Build.VERSION.SDK_INT >= 31
                    && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                try {
                    recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
                } catch (Throwable ignored) {
                }
            }

            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            }

            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    listening = true;
                    if (mode == Mode.WAKE) {
                        sendState(STATE_WAITING_WAKE, null, null);
                    } else if (mode == Mode.COMMAND) {
                        sendState(STATE_LISTENING_COMMAND, null, null);
                    }
                }

                @Override public void onBeginningOfSpeech() {}

                @Override public void onRmsChanged(float rmsdB) {}

                @Override public void onBufferReceived(byte[] buffer) {}

                @Override public void onEndOfSpeech() {}

                @Override public void onError(int error) {
                    listening = false;
                    if (stopping || speaking) return;

                    if (mode == Mode.WAKE) {
                        scheduleWake(error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                                ? 1400 : 600);
                        return;
                    }

                    if (mode == Mode.COMMAND) {
                        mode = getSharedPreferences(PREF, MODE_PRIVATE)
                                .getBoolean(KEY_WAKE, false) ? Mode.WAKE : Mode.IDLE;
                        if (mode == Mode.WAKE) {
                            sendState(STATE_WAITING_WAKE, null, null);
                            scheduleWake(350);
                        } else {
                            sendState(
                                    error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                                            ? STATE_ERROR
                                            : STATE_IDLE,
                                    null,
                                    error == SpeechRecognizer.ERROR_NO_MATCH
                                            ? "Voice samajh nahi aayi."
                                            : null);
                        }
                    }
                }

                @Override public void onResults(Bundle results) {
                    listening = false;
                    if (stopping || speaking) return;

                    ArrayList<String> phrases =
                            results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                    if (mode == Mode.WAKE) {
                        handleWakeResults(phrases);
                        return;
                    }

                    if (mode == Mode.COMMAND) {
                        String command = firstUsable(phrases);
                        if (command == null) {
                            mode = getSharedPreferences(PREF, MODE_PRIVATE)
                                    .getBoolean(KEY_WAKE, false) ? Mode.WAKE : Mode.IDLE;
                            if (mode == Mode.WAKE) {
                                scheduleWake(250);
                                sendState(STATE_WAITING_WAKE, null, null);
                            } else {
                                sendState(STATE_IDLE, null, null);
                            }
                        } else {
                            executeCommand(command);
                        }
                    }
                }

                @Override public void onPartialResults(Bundle partialResults) {}

                @Override public void onEvent(int eventType, Bundle params) {}
            });
        } catch (Throwable t) {
            recognizer = null;
            sendState(STATE_ERROR, null, "Voice input unavailable.");
        }
    }

    private void handleWakeResults(ArrayList<String> phrases) {
        String remainder = null;

        if (phrases != null) {
            for (String phrase : phrases) {
                if (CommandRouter.isWakePhrase(phrase)) {
                    remainder = CommandRouter.removeWakePhrase(phrase);
                    break;
                }
            }
        }

        if (remainder == null) {
            scheduleWake(250);
            sendState(STATE_WAITING_WAKE, null, null);
            return;
        }

        retryDelay = 600L;
        handler.removeCallbacks(wakeRetry);

        if (remainder.trim().isEmpty()) {
            followUpAfterWake = true;
            mode = Mode.COMMAND;
            speak("Haan, bolo.");
        } else {
            executeCommand(remainder.trim());
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
        if (command == null || command.trim().isEmpty()) return;

        handler.removeCallbacks(wakeRetry);
        stopListeningOnly();
        sendState(STATE_LISTENING_COMMAND, command, null);

        CommandEngine.execute(this, command, message -> speak(message));
    }

    private void startRecognition(boolean wake) {
        if (stopping || speaking) return;
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            mode = Mode.IDLE;
            sendState(STATE_ERROR, null, "Microphone permission required.");
            return;
        }

        if (recognizer == null) {
            initRecognizer();
            if (recognizer == null) {
                mode = Mode.IDLE;
                return;
            }
        }

        mode = wake ? Mode.WAKE : Mode.COMMAND;
        stopListeningOnly();

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                wake
                        ? "en-IN"
                        : getSharedPreferences(PREF, MODE_PRIVATE)
                                .getString(KEY_LANG, "en-IN"));
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);

        // Longer wake-session windows reduce recognizer churn and the visible mic flicker.
        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                wake ? 1900L : 1200L);
        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                wake ? 1600L : 1000L);
        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                wake ? 900L : 250L);

        try {
            recognizer.startListening(recognizerIntent);
            retryDelay = 600L;
        } catch (Throwable t) {
            listening = false;
            scheduleWake(wake ? 900 : 0);
            if (!wake) sendState(STATE_ERROR, null, "Mic busy. Tap again.");
        }
    }

    private void scheduleCommandRecognition(long delay) {
        handler.postDelayed(() -> {
            if (!stopping && !speaking && mode == Mode.COMMAND) {
                startRecognition(false);
            }
        }, Math.max(80L, delay));
    }

    private void scheduleWake(long delay) {
        handler.removeCallbacks(wakeRetry);
        if (stopping || mode != Mode.WAKE || speaking) return;

        long actual = Math.max(delay, retryDelay);
        handler.postDelayed(wakeRetry, actual);
        retryDelay = Math.min(5000L, Math.max(700L, retryDelay * 2L));
    }

    private void stopListeningOnly() {
        handler.removeCallbacks(wakeRetry);
        if (recognizer != null && listening) {
            try {
                recognizer.cancel();
            } catch (Throwable ignored) {
            }
        }
        listening = false;
    }

    private void stopSpeaking() {
        speaking = false;
        if (tts != null) {
            try {
                tts.stop();
            } catch (Throwable ignored) {
            }
        }
    }

    private void speak(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = "Command samajh nahi aayi.";
        }

        stopListeningOnly();
        applyLanguage();
        speaking = true;
        sendState(STATE_SPEAKING, null, message);

        try {
            int result = tts.speak(
                    BuddyVoiceProfile.naturalize(message),
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "buddy-" + System.nanoTime());

            if (result == TextToSpeech.ERROR) {
                speaking = false;
                resumeAfterSpeechFallback();
            }
        } catch (Throwable t) {
            speaking = false;
            resumeAfterSpeechFallback();
        }
    }

    private void resumeAfterSpeechFallback() {
        if (stopping) return;

        if (followUpAfterWake) {
            followUpAfterWake = false;
            mode = Mode.COMMAND;
            scheduleCommandRecognition(180);
        } else if (getSharedPreferences(PREF, MODE_PRIVATE)
                .getBoolean(KEY_WAKE, false)) {
            mode = Mode.WAKE;
            sendState(STATE_WAITING_WAKE, null, null);
            scheduleWake(250);
        } else {
            mode = Mode.IDLE;
            sendState(STATE_IDLE, null, null);
        }
    }

    private Notification buildNotification() {
        String content = getSharedPreferences(PREF, MODE_PRIVATE)
                .getBoolean(KEY_WAKE, false)
                ? "Wake word ready — say “Hey Buddy”"
                : "Tap to talk from Buddy";
        return new Notification.Builder(this, "buddy_voice")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Buddy")
                .setContentText(content)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(
                        new NotificationChannel(
                                "buddy_voice",
                                "Buddy voice",
                                NotificationManager.IMPORTANCE_LOW));
            }
        }
    }

    private void sendState(String state, String transcript, String reply) {
        Intent i = new Intent(ACTION_STATUS);
        i.setPackage(getPackageName());
        i.putExtra(EXTRA_STATE, state);
        if (transcript != null) i.putExtra(EXTRA_TRANSCRIPT, transcript);
        if (reply != null) i.putExtra(EXTRA_REPLY, reply);
        sendBroadcast(i);
    }

    private void stopVoiceService() {
        stopping = true;
        handler.removeCallbacksAndMessages(null);
        stopListeningOnly();
        stopSpeaking();
        mode = Mode.IDLE;
        stopSelf();
    }

    @Override public void onDestroy() {
        stopping = true;
        handler.removeCallbacksAndMessages(null);

        if (recognizer != null) {
            try {
                recognizer.cancel();
            } catch (Throwable ignored) {
            }
            try {
                recognizer.destroy();
            } catch (Throwable ignored) {
            }
            recognizer = null;
        }

        if (tts != null) {
            try {
                tts.stop();
            } catch (Throwable ignored) {
            }
            try {
                tts.shutdown();
            } catch (Throwable ignored) {
            }
            tts = null;
        }

        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
