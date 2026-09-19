package com.shyam026.buddyassistant;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.view.Gravity;
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
import android.widget.Toast;

import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuddySettingsActivity extends Activity {
    private static final String PREF = "buddy_prefs";
    private static final String KEY_RATE = "speech_rate";
    private static final String KEY_PITCH = "speech_pitch";

    private EditText apiKey;
    private Button keyVisibility;
    private Spinner modelSpinner;
    private ArrayAdapter<String> modelAdapter;
    private final List<String> availableModels = new ArrayList<>();

    private SeekBar rate;
    private SeekBar pitch;
    private TextView status;
    private TextView keyState;
    private TextView selectedModelText;
    private TextToSpeech previewTts;
    private boolean keyVisible;

    private final int bg = Color.rgb(7, 10, 18);
    private final int card = Color.rgb(17, 22, 35);
    private final int card2 = Color.rgb(23, 30, 48);
    private final int line = Color.rgb(49, 61, 88);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(154, 166, 195);
    private final int accent = Color.rgb(117, 95, 255);
    private final int cyan = Color.rgb(89, 207, 255);
    private final int green = Color.rgb(82, 220, 151);
    private final int danger = Color.rgb(255, 119, 133);

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, end});
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private void section(LinearLayout parent, String title, String subtitle) {
        TextView over = text(title.toUpperCase(Locale.ROOT), 11, cyan, true);
        over.setLetterSpacing(0.08f);
        over.setPadding(0, dp(4), 0, dp(4));
        parent.addView(over);

        TextView info = text(subtitle, 13, muted, false);
        info.setPadding(0, 0, 0, dp(11));
        parent.addView(info);
    }

    private Button button(String value, int height, int fill, float size) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(white);
        b.setTextSize(size);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(fill, 0, 16));
        return b;
    }

    private void gap(LinearLayout parent, int h) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
    }

    private EditText keyField(String value) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setText(value);
        e.setTextSize(14);
        e.setTextColor(white);
        e.setHintTextColor(Color.rgb(122, 135, 164));
        e.setHint("Paste your Gemini API key");
        e.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setTransformationMethod(PasswordTransformationMethod.getInstance());
        e.setPadding(dp(14), 0, dp(8), 0);
        e.setBackgroundColor(Color.TRANSPARENT);
        return e;
    }

    private void updateKeyState() {
        if (keyState == null) return;
        boolean saved = BuddySecrets.hasGeminiApiKey(this);
        String stored = BuddySecrets.getGeminiApiKey(this);
        if (saved) {
            String suffix = stored.length() >= 4
                    ? stored.substring(stored.length() - 4)
                    : "••••";
            keyState.setText("●  KEY SAVED  ••••" + suffix);
            keyState.setTextColor(green);
        } else {
            keyState.setText("○  NO KEY SAVED");
            keyState.setTextColor(muted);
        }
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
        updateModelText();
    }

    private void updateModelText() {
        if (selectedModelText != null) {
            String model = selectedModel();
            selectedModelText.setText(model.isEmpty()
                    ? "No model selected"
                    : "Active: " + model);
        }
    }

    private String selectedModel() {
        int position = modelSpinner == null ? -1 : modelSpinner.getSelectedItemPosition();
        if (position < 0 || position >= availableModels.size()) return "";
        return availableModels.get(position);
    }

    private boolean saveKeyAndModel() {
        String raw = apiKey == null ? "" : apiKey.getText().toString();
        if (raw.trim().isEmpty()) {
            status.setText("Enter a Gemini API key first.");
            status.setTextColor(danger);
            return false;
        }

        boolean saved = BuddySecrets.saveGeminiApiKey(this, raw);
        String selected = selectedModel();
        boolean modelSaved = selected.isEmpty()
                || BuddySecrets.saveModel(this, selected);

        if (!saved) {
            status.setText("Key save FAILED — secure storage could not be verified.");
            status.setTextColor(danger);
            return false;
        }
        if (!modelSaved) {
            status.setText("Key saved, but model setting could not be stored.");
            status.setTextColor(danger);
            return false;
        }

        String stored = BuddySecrets.getGeminiApiKey(this);
        boolean verified = raw.trim().equals(stored);
        if (!verified) {
            status.setText("Key write finished but read-back verification failed.");
            status.setTextColor(danger);
            return false;
        }

        updateKeyState();
        status.setText("✓ Key stored securely and verified on this device.");
        status.setTextColor(green);
        return true;
    }

    private void refreshModels() {
        if (!BuddySecrets.hasGeminiApiKey(this)) {
            setModels(GeminiModelCatalog.defaults(), BuddySecrets.getModel(this));
            status.setText("Add and save your Gemini key to load your live model list.");
            status.setTextColor(muted);
            return;
        }

        status.setText("Refreshing Gemini models…");
        status.setTextColor(cyan);

        GeminiModelCatalog.fetchAvailable(this, (models, error) -> {
            setModels(models, BuddySecrets.getModel(this));
            if (error == null) {
                status.setText("✓ Live Gemini model list updated.");
                status.setTextColor(green);
            } else {
                status.setText(error);
                status.setTextColor(muted);
            }
        });
    }

    private void testSelectedModel() {
        if (!saveKeyAndModel()) return;

        String key = BuddySecrets.getGeminiApiKey(this);
        String model = selectedModel();
        status.setText("Testing " + (model.isEmpty() ? "Gemini" : model) + "…");
        status.setTextColor(cyan);

        GeminiApiClient.test(this, key, model, (success, message) -> {
            status.setText(message);
            status.setTextColor(success ? green : danger);
            updateKeyState();
        });
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(14), dp(18), dp(34));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 35, white, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(rounded(card2, line, 18));
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, dp(8), 0);
        titleBox.addView(text("Buddy Settings", 23, white, true));
        titleBox.addView(text("Tune your assistant", 12, muted, false));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        Button done = button("Done", 44, card2, 13);
        done.setOnClickListener(v -> finish());
        top.addView(done, new LinearLayout.LayoutParams(dp(76), dp(44)));
        content.addView(top);
        gap(content, 16);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(gradient(
                Color.rgb(24, 22, 62),
                Color.rgb(17, 31, 48), 24));
        hero.addView(text("✦  BUDDY CONTROL CENTER", 11, cyan, true));
        TextView heroTitle = text("Make Buddy feel like yours.", 22, white, true);
        heroTitle.setPadding(0, dp(6), 0, dp(4));
        hero.addView(heroTitle);
        hero.addView(text(
                "AI brain, voice, phone control and permissions — all from one place.",
                13, muted, false));
        content.addView(hero);
        gap(content, 18);

        LinearLayout aiCard = new LinearLayout(this);
        aiCard.setOrientation(LinearLayout.VERTICAL);
        aiCard.setPadding(dp(16), dp(15), dp(16), dp(16));
        aiCard.setBackground(rounded(card, line, 22));
        content.addView(aiCard);

        section(aiCard, "Gemini AI",
                "Your key is encrypted with Android Keystore. It is never shown in full after saving.");

        LinearLayout keyRow = new LinearLayout(this);
        keyRow.setGravity(Gravity.CENTER_VERTICAL);
        keyRow.setPadding(dp(2), 0, dp(2), 0);
        keyRow.setBackground(rounded(card2, line, 16));
        apiKey = keyField(BuddySecrets.getGeminiApiKey(this));
        keyRow.addView(apiKey, new LinearLayout.LayoutParams(0, dp(54), 1));

        keyVisibility = button("SHOW", 42, card2, 11);
        keyVisibility.setOnClickListener(v -> {
            if (keyVisible) {
                apiKey.setTransformationMethod(PasswordTransformationMethod.getInstance());
                keyVisibility.setText("SHOW");
            } else {
                apiKey.setTransformationMethod(null);
                keyVisibility.setText("HIDE");
            }
            keyVisible = !keyVisible;
            apiKey.setSelection(apiKey.length());
        });
        keyRow.addView(keyVisibility, new LinearLayout.LayoutParams(dp(72), dp(42)));
        aiCard.addView(keyRow);

        keyState = text("", 12, muted, true);
        keyState.setPadding(dp(3), dp(9), 0, dp(2));
        aiCard.addView(keyState);
        updateKeyState();

        Button save = button("SAVE & VERIFY KEY", 50, accent, 14);
        save.setOnClickListener(v -> {
            saveKeyAndModel();
        });
        LinearLayout.LayoutParams saveP = new LinearLayout.LayoutParams(-1, dp(50));
        saveP.setMargins(0, dp(10), 0, 0);
        aiCard.addView(save, saveP);

        LinearLayout modelTitle = new LinearLayout(this);
        modelTitle.setGravity(Gravity.CENTER_VERTICAL);
        TextView modelLabel = text("AI model", 14, white, true);
        modelTitle.addView(modelLabel, new LinearLayout.LayoutParams(0, -2, 1));
        selectedModelText = text("", 12, cyan, false);
        selectedModelText.setGravity(Gravity.CENTER_VERTICAL);
        modelTitle.addView(selectedModelText);
        aiCard.addView(modelTitle, new LinearLayout.LayoutParams(-1, dp(42)));

        modelSpinner = new Spinner(this);
        modelAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, availableModels);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setBackground(rounded(card2, line, 16));
        aiCard.addView(modelSpinner, new LinearLayout.LayoutParams(-1, dp(52)));

        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < availableModels.size()) {
                    String selected = availableModels.get(position);
                    if (!selected.isEmpty()) {
                        BuddySecrets.saveModel(BuddySettingsActivity.this, selected);
                    }
                    updateModelText();
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        LinearLayout modelActions = new LinearLayout(this);
        Button refresh = button("Refresh", 44, card2, 12);
        refresh.setOnClickListener(v -> refreshModels());
        modelActions.addView(refresh, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button test = button("Test connection", 44, card2, 12);
        test.setOnClickListener(v -> testSelectedModel());
        LinearLayout.LayoutParams testP = new LinearLayout.LayoutParams(0, dp(44), 1);
        testP.setMargins(dp(8), 0, 0, 0);
        modelActions.addView(test, testP);
        LinearLayout.LayoutParams maP = new LinearLayout.LayoutParams(-1, dp(44));
        maP.setMargins(0, dp(9), 0, 0);
        aiCard.addView(modelActions, maP);

        Button clear = button("Clear saved key", 44, card2, 12);
        clear.setOnClickListener(v -> {
            boolean ok = BuddySecrets.clearGeminiApiKey(this);
            apiKey.setText("");
            updateKeyState();
            status.setText(ok ? "Saved Gemini key removed." : "Key removal could not be verified.");
            status.setTextColor(ok ? green : danger);
        });
        LinearLayout.LayoutParams clearP = new LinearLayout.LayoutParams(-1, dp(44));
        clearP.setMargins(0, dp(8), 0, 0);
        aiCard.addView(clear, clearP);

        gap(content, 15);

        LinearLayout voiceCard = new LinearLayout(this);
        voiceCard.setOrientation(LinearLayout.VERTICAL);
        voiceCard.setPadding(dp(16), dp(15), dp(16), dp(16));
        voiceCard.setBackground(rounded(card, line, 22));
        content.addView(voiceCard);

        section(voiceCard, "Buddy Voice",
                "These controls are used by Buddy's actual voice service, not only the preview.");

        int savedRate = getSharedPreferences(PREF, MODE_PRIVATE).getInt(KEY_RATE, 30);
        int savedPitch = getSharedPreferences(PREF, MODE_PRIVATE).getInt(KEY_PITCH, 36);

        TextView rateLabel = text("Speech speed", 14, white, true);
        voiceCard.addView(rateLabel);
        rate = new SeekBar(this);
        rate.setMax(60);
        rate.setProgress(savedRate);
        voiceCard.addView(rate, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView pitchLabel = text("Voice pitch", 14, white, true);
        voiceCard.addView(pitchLabel);
        pitch = new SeekBar(this);
        pitch.setMax(60);
        pitch.setProgress(savedPitch);
        voiceCard.addView(pitch, new LinearLayout.LayoutParams(-1, dp(46)));

        rate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                getSharedPreferences(PREF, MODE_PRIVATE).edit().putInt(KEY_RATE, p).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        pitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                getSharedPreferences(PREF, MODE_PRIVATE).edit().putInt(KEY_PITCH, p).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        Button voiceTest = button("▶  Test Buddy voice", 48, card2, 13);
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
                previewTts.speak(
                        "Hi, I am Buddy. Bolo, main sun rahi hoon.",
                        TextToSpeech.QUEUE_FLUSH, null, "buddy-settings-test");
            });
        });
        LinearLayout.LayoutParams voiceP = new LinearLayout.LayoutParams(-1, dp(48));
        voiceP.setMargins(0, dp(8), 0, 0);
        voiceCard.addView(voiceTest, voiceP);

        gap(content, 15);

        LinearLayout controlCard = new LinearLayout(this);
        controlCard.setOrientation(LinearLayout.VERTICAL);
        controlCard.setPadding(dp(16), dp(15), dp(16), dp(16));
        controlCard.setBackground(rounded(card, line, 22));
        content.addView(controlCard);

        section(controlCard, "Phone control",
                "Accessibility unlocks navigation actions such as Home, Back, Recents, scrolling and text entry.");

        Button accessibility = button("Open Accessibility settings", 48, card2, 13);
        accessibility.setOnClickListener(v ->
                startActivity(new android.content.Intent(
                        android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        controlCard.addView(accessibility);

        Button notifications = button("Open Notification access", 48, card2, 13);
        notifications.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(
                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            } catch (Throwable ignored) {
                Toast.makeText(this, "Notification settings unavailable.", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(48));
        np.setMargins(0, dp(8), 0, 0);
        controlCard.addView(notifications, np);

        Button assistant = button("Set Buddy as default assistant", 48, card2, 13);
        assistant.setOnClickListener(v -> {
            try {
                if (android.os.Build.VERSION.SDK_INT < 29) {
                    Toast.makeText(this, "Assistant role needs Android 10+.", Toast.LENGTH_SHORT).show();
                    return;
                }
                android.app.role.RoleManager rm =
                        getSystemService(android.app.role.RoleManager.class);
                if (rm != null
                        && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)) {
                    startActivityForResult(
                            rm.createRequestRoleIntent(
                                    android.app.role.RoleManager.ROLE_ASSISTANT), 2001);
                }
            } catch (Throwable t) {
                Toast.makeText(this, "Assistant settings open nahi hui.", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(48));
        ap.setMargins(0, dp(8), 0, 0);
        controlCard.addView(assistant, ap);

        gap(content, 15);

        LinearLayout infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(dp(16), dp(15), dp(16), dp(16));
        infoCard.setBackground(rounded(card, line, 22));
        content.addView(infoCard);

        section(infoCard, "Hands-free note",
                "Buddy deliberately does not run SpeechRecognizer in a background wake loop.");
        infoCard.addView(text(
                "For a true custom “Hey Buddy” wake word, a dedicated local hotword engine is required. " +
                "The current path uses explicit tap/system-assistant invocation so the mic does not randomly cycle on and off.",
                13, muted, false));

        gap(content, 15);

        LinearLayout diagCard = new LinearLayout(this);
        diagCard.setOrientation(LinearLayout.VERTICAL);
        diagCard.setPadding(dp(16), dp(15), dp(16), dp(16));
        diagCard.setBackground(rounded(card, line, 22));
        content.addView(diagCard);

        section(diagCard, "Diagnostics", "Live status for the settings page.");
        status = text("Ready.", 14, green, true);
        status.setPadding(0, dp(5), 0, 0);
        diagCard.addView(status);

        setModels(GeminiModelCatalog.defaults(), BuddySecrets.getModel(this));
        setContentView(root);
        updateModelText();
        updateKeyState();
        if (BuddySecrets.hasGeminiApiKey(this)) {
            refreshModels();
        } else {
            status.setText("Add your Gemini key above, then tap SAVE & VERIFY KEY.");
            status.setTextColor(muted);
        }
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
