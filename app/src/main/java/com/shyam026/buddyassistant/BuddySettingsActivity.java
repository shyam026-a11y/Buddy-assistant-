package com.shyam026.buddyassistant;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuddySettingsActivity extends Activity {
    private EditText apiKey;
    private Spinner modelSpinner;
    private ArrayAdapter<String> modelAdapter;
    private final List<String> availableModels = new ArrayList<>();

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

    private void setModels(List<String> models, String preferred) {
        availableModels.clear();
        if (models != null) {
            for (String value : models) {
                if (value == null) continue;
                String clean = value.trim();
                if (!clean.isEmpty() && !availableModels.contains(clean)) {
                    availableModels.add(clean);
                }
            }
        }

        if (availableModels.isEmpty()) {
            availableModels.addAll(GeminiModelCatalog.defaults());
        }

        String current = preferred == null || preferred.trim().isEmpty()
                ? BuddySecrets.getModel(this)
                : preferred.trim();

        int index = availableModels.indexOf(current);
        if (index < 0 && !current.isEmpty()) {
            availableModels.add(0, current);
            index = 0;
        }
        if (index < 0) index = 0;

        modelAdapter.notifyDataSetChanged();
        modelSpinner.setSelection(index, false);
    }

    private String selectedModel() {
        int position = modelSpinner == null ? -1 : modelSpinner.getSelectedItemPosition();
        if (position < 0 || position >= availableModels.size()) return "";
        return availableModels.get(position);
    }

    private void refreshModels() {
        GeminiModelCatalog.fetchAvailable(this, (models, error) -> {
            setModels(models, BuddySecrets.getModel(this));
            if (status != null) {
                status.setText(error == null
                        ? "Models refreshed. Active: " + BuddySecrets.getModel(this)
                        : error);
            }
        });
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

        content.addView(label("Gemini AI", 12, true));
        content.addView(info(
                "Gemini API key is encrypted with Android Keystore. Select any compatible Gemini model and switch it anytime without reinstalling Buddy."));

        apiKey = field(BuddySecrets.getGeminiApiKey(this), "Gemini API key", true);
        content.addView(apiKey, new LinearLayout.LayoutParams(-1, dp(52)));

        content.addView(label("Active Gemini model", 15, true));
        modelSpinner = new Spinner(this);
        modelAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                availableModels);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setBackground(background(
                Color.rgb(23, 30, 48),
                Color.rgb(55, 66, 94),
                16));
        content.addView(modelSpinner, new LinearLayout.LayoutParams(-1, dp(52)));

        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= availableModels.size()) return;
                String selected = availableModels.get(position);
                if (!selected.isEmpty()) {
                    BuddySecrets.saveModel(BuddySettingsActivity.this, selected);
                    if (status != null) status.setText("Active model: " + selected);
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        LinearLayout aiButtons = new LinearLayout(this);
        aiButtons.setPadding(0, dp(8), 0, 0);

        Button save = button("Save Gemini Key");
        save.setOnClickListener(v -> {
            BuddySecrets.saveGeminiApiKey(this, apiKey.getText().toString());
            String selected = selectedModel();
            if (!selected.isEmpty()) BuddySecrets.saveModel(this, selected);
            status.setText("Gemini key saved securely.");
            refreshModels();
        });
        aiButtons.addView(save, new LinearLayout.LayoutParams(0, dp(46), 1));

        Button refresh = button("Refresh Models");
        refresh.setOnClickListener(v -> {
            BuddySecrets.saveGeminiApiKey(this, apiKey.getText().toString());
            status.setText("Refreshing Gemini models…");
            refreshModels();
        });
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(dp(132), dp(46));
        refreshParams.setMargins(dp(8), 0, 0, 0);
        aiButtons.addView(refresh, refreshParams);
        content.addView(aiButtons);

        Button clear = button("Clear Gemini Key");
        clear.setOnClickListener(v -> {
            BuddySecrets.clearGeminiApiKey(this);
            apiKey.setText("");
            setModels(GeminiModelCatalog.defaults(), BuddySecrets.getModel(this));
            status.setText("Gemini API key removed.");
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-1, dp(46));
        clearParams.setMargins(0, dp(8), 0, 0);
        content.addView(clear, clearParams);

        Button test = button("Test Selected Model");
        test.setOnClickListener(v -> {
            BuddySecrets.saveGeminiApiKey(this, apiKey.getText().toString());
            String selected = selectedModel();
            if (!selected.isEmpty()) BuddySecrets.saveModel(this, selected);
            status.setText("Testing " + (selected.isEmpty() ? "Gemini" : selected) + "…");
            CommandEngine.execute(this, "hello", reply ->
                    status.setText(reply == null || reply.trim().isEmpty()
                            ? "No Gemini response."
                            : "Gemini responded: " + reply));
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
                "Local actions do not require Gemini. Buddy only reports an action as done when its local operation returned success."));
        status = new TextView(this);
        status.setText("Ready.");
        status.setTextColor(Color.rgb(82, 220, 151));
        status.setTextSize(14);
        status.setPadding(0, dp(12), 0, 0);
        content.addView(status);

        setModels(GeminiModelCatalog.defaults(), BuddySecrets.getModel(this));
        setContentView(root);
        refreshModels();
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
