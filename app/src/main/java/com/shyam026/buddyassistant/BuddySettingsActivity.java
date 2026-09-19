package com.shyam026.buddyassistant;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class BuddySettingsActivity extends Activity {
    private EditText apiKey;
    private EditText model;
    private SeekBar rate;
    private SeekBar pitch;
    private TextView status;
    private TextToSpeech previewTts;

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable background(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView label(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setPadding(0, dp(10), 0, dp(6));
        return t;
    }

    private EditText field(String value, String hint, boolean password) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setText(value);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(145, 155, 178));
        e.setTextColor(Color.WHITE);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(background(Color.rgb(23, 30, 48), Color.rgb(55, 66, 94), 16));
        if (password) {
            e.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        return e;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setBackground(background(Color.rgb(77, 62, 200), Color.TRANSPARENT, 16));
        return b;
    }

    private TextView info(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.rgb(155, 165, 188));
        t.setTextSize(13);
        t.setPadding(0, dp(4), 0, dp(5));
        return t;
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 10, 18));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(10));

        TextView title = label("Buddy Settings", 23, true);
        title.setPadding(0, 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button done = button("Done");
        done.setOnClickListener(v -> finish());
        header.addView(done, new LinearLayout.LayoutParams(dp(80), dp(44)));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), 0, dp(18), dp(30));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        content.addView(label("AI", 12, true));
        content.addView(info("API keys are encrypted with Android Keystore and are not written to Git."));
        apiKey = field(BuddySecrets.getApiKey(this), "OpenRouter API key", true);
        content.addView(apiKey, new LinearLayout.LayoutParams(-1, dp(52)));

        model = field(BuddySecrets.getModel(this), "openrouter/free", false);
        LinearLayout.LayoutParams modelParams = new LinearLayout.LayoutParams(-1, dp(52));
        modelParams.setMargins(0, dp(8), 0, 0);
        content.addView(model, modelParams);

        LinearLayout aiButtons = new LinearLayout(this);
        aiButtons.setPadding(0, dp(8), 0, 0);
        Button save = button("Save");
        save.setOnClickListener(v -> {
            BuddySecrets.saveApiKey(this, apiKey.getText().toString());
            BuddySecrets.saveModel(this, model.getText().toString());
            status.setText("Saved securely.");
        });
        aiButtons.addView(save, new LinearLayout.LayoutParams(0, dp(46), 1));

        Button clear = button("Clear");
        clear.setOnClickListener(v -> {
            BuddySecrets.clearApiKey(this);
            apiKey.setText("");
            status.setText("API key removed.");
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(90), dp(46));
        clearParams.setMargins(dp(8), 0, 0, 0);
        aiButtons.addView(clear, clearParams);
        content.addView(aiButtons);

        Button test = button("Test AI");
        test.setOnClickListener(v -> {
            BuddySecrets.saveApiKey(this, apiKey.getText().toString());
            BuddySecrets.saveModel(this, model.getText().toString());
            status.setText("Testing AI…");
            CommandEngine.execute(this, "hello", reply ->
                    status.setText(reply == null || reply.trim().isEmpty()
                            ? "No AI response."
                            : "AI responded: " + reply));
        });
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(-1, dp(46));
        testParams.setMargins(0, dp(8), 0, 0);
        content.addView(test, testParams);

        content.addView(label("Buddy Voice", 12, true));
        content.addView(info("Uses the voice engine installed on the phone. Buddy prefers an Indian English/Hindi voice when available."));

        rate = new SeekBar(this);
        rate.setMax(60);
        rate.setProgress(30);
        content.addView(label("Speech speed", 15, true));
        content.addView(rate, new LinearLayout.LayoutParams(-1, dp(48)));

        pitch = new SeekBar(this);
        pitch.setMax(60);
        pitch.setProgress(36);
        content.addView(label("Voice pitch", 15, true));
        content.addView(pitch, new LinearLayout.LayoutParams(-1, dp(48)));

        Button voiceTest = button("Test Buddy Voice");
        voiceTest.setOnClickListener(v -> {
            if (previewTts != null) {
                try {
                    previewTts.stop();
                    previewTts.shutdown();
                } catch (Throwable ignored) {}
            }
            previewTts = new TextToSpeech(this, result -> {
                if (result != TextToSpeech.SUCCESS || previewTts == null) return;
                BuddyVoiceProfile.apply(previewTts, Locale.forLanguageTag("en-IN"));
                previewTts.setSpeechRate(0.75f + (rate.getProgress() / 60f) * 0.60f);
                previewTts.setPitch(0.85f + (pitch.getProgress() / 60f) * 0.50f);
                previewTts.speak("Hi, I am Buddy. Bolo, main sun rahi hoon.",
                        TextToSpeech.QUEUE_FLUSH, null, "buddy-settings-test");
            });
        });
        content.addView(voiceTest, new LinearLayout.LayoutParams(-1, dp(46)));

        content.addView(label("Hands-free / System Assistant", 12, true));
        content.addView(info(
                "Buddy does not start SpeechRecognizer automatically. That prevents repeated mic on/off cycles. " +
                "Select Buddy as the default assistant to use Android's assistant invocation. " +
                "A true custom always-on “Hey Buddy” hotword still requires a dedicated local hotword engine."));
        Button assistant = button("Set Buddy as default assistant");
        assistant.setOnClickListener(v -> {
            try {
                if (android.os.Build.VERSION.SDK_INT < 29) return;
                android.app.role.RoleManager rm = getSystemService(android.app.role.RoleManager.class);
                if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)) {
                    startActivityForResult(
                            rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_ASSISTANT), 2001);
                }
            } catch (Throwable ignored) {}
        });
        content.addView(assistant, new LinearLayout.LayoutParams(-1, dp(46)));

        content.addView(label("Permissions", 12, true));
        content.addView(info(
                "Microphone is requested only for an active command. Accessibility is optional and enables navigation, tapping, scrolling and text entry."));
        Button accessibility = button("Accessibility Settings");
        accessibility.setOnClickListener(v ->
                startActivity(new android.content.Intent(
                        android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        content.addView(accessibility, new LinearLayout.LayoutParams(-1, dp(46)));

        Button notifications = button("Notification Access");
        notifications.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(
                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            } catch (Throwable ignored) {}
        });
        LinearLayout.LayoutParams notificationParams = new LinearLayout.LayoutParams(-1, dp(46));
        notificationParams.setMargins(0, dp(8), 0, 0);
        content.addView(notifications, notificationParams);

        content.addView(label("Diagnostics", 12, true));
        content.addView(info(
                "Local actions do not require the AI key. Buddy only reports an action as done when its local operation returned success."));
        status = new TextView(this);
        status.setText("Ready.");
        status.setTextColor(Color.rgb(82, 220, 151));
        status.setTextSize(14);
        status.setPadding(0, dp(12), 0, 0);
        content.addView(status);

        setContentView(root);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
    }

    @Override protected void onDestroy() {
        if (previewTts != null) {
            try {
                previewTts.stop();
                previewTts.shutdown();
            } catch (Throwable ignored) {}
            previewTts = null;
        }
        super.onDestroy();
    }
}
