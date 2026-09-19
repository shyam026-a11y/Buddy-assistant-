package com.shyam026.buddyassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuddySettingsActivity extends Activity {
    private static final String PREF = "buddy_prefs";
    private static final String KEY_RATE = "speech_rate";
    private static final String KEY_PITCH = "speech_pitch";
    private static final String KEY_WAKE = "wake_mode";
    private static final int REQ_WAKE_MIC = 8301;

    private EditText apiKey;
    private Spinner modelSpinner;
    private ArrayAdapter<String> modelAdapter;
    private final List<String> availableModels = new ArrayList<>();

    private SeekBar rate;
    private SeekBar pitch;
    private TextView status;
    private TextView keyState;
    private TextView selectedModelText;
    private TextView wakeState;
    private Switch wakeSwitch;
    private TextToSpeech previewTts;

    private final int bg = Color.rgb(7, 9, 16);
    private final int card = Color.rgb(16, 21, 34);
    private final int card2 = Color.rgb(22, 29, 47);
    private final int line = Color.rgb(49, 61, 88);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(151, 162, 188);
    private final int accent = Color.rgb(117, 95, 255);
    private final int cyan = Color.rgb(92, 208, 255);
    private final int green = Color.rgb(82, 220, 151);
    private final int danger = Color.rgb(255, 119, 133);

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(
            int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) {
            d.setStroke(dp(1), stroke);
        }
        return d;
    }

    private GradientDrawable gradient(
            int start, int end, int radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, end});
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(
            String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(
                null,
                bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private void gap(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(
                v,
                new LinearLayout.LayoutParams(
                        1, dp(height)));
    }

    private void section(
            LinearLayout parent,
            String title,
            String subtitle) {
        TextView over =
                text(
                        title.toUpperCase(Locale.ROOT),
                        11,
                        cyan,
                        true);
        over.setLetterSpacing(0.08f);
        parent.addView(over);

        TextView info =
                text(subtitle, 13, muted, false);
        info.setPadding(
                0, dp(3), 0, dp(11));
        parent.addView(info);
    }

    private Button button(
            String value, int height, int fill, float size) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(white);
        b.setTextSize(size);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(dp(11), 0, dp(11), 0);
        b.setGravity(Gravity.CENTER);
        b.setBackground(
                rounded(fill, 0, 16));
        return b;
    }

    private LinearLayout card() {
        LinearLayout l =
                new LinearLayout(this);
        l.setOrientation(
                LinearLayout.VERTICAL);
        l.setPadding(
                dp(16), dp(15),
                dp(16), dp(16));
        l.setBackground(
                rounded(card, line, 22));
        return l;
    }

    private EditText keyField() {
        EditText e =
                new EditText(this);
        e.setSingleLine(true);
        e.setText("");
        e.setTextSize(14);
        e.setTextColor(white);
        e.setHintTextColor(
                Color.rgb(120, 132, 160));
        e.setHint(
                "Enter a new Gemini API key to replace");
        e.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setTransformationMethod(
                PasswordTransformationMethod.getInstance());
        e.setPadding(
                dp(14), 0, dp(12), 0);
        e.setBackground(
                rounded(card2, line, 16));
        return e;
    }

    private void setStatus(
            String value, int color) {
        if (status == null) return;
        status.setText(value);
        status.setTextColor(color);
    }

    private void updateKeyState() {
        if (keyState == null) return;

        String key =
                BuddySecrets.getGeminiApiKey(this);

        if (key.isEmpty()) {
            keyState.setText("○  NO KEY SAVED");
            keyState.setTextColor(muted);
            return;
        }

        String tail =
                key.length() >= 4
                        ? key.substring(key.length() - 4)
                        : "••••";

        keyState.setText(
                "●  KEY SAVED  ••••" + tail
                        + "  •  "
                        + BuddySecrets.getStorageStatus(this));
        keyState.setTextColor(green);
    }

    private void updateWakeState() {
        boolean enabled =
                getSharedPreferences(
                        PREF,
                        MODE_PRIVATE)
                        .getBoolean(KEY_WAKE, false);

        if (wakeSwitch != null
                && wakeSwitch.isChecked()
                != enabled) {
            wakeSwitch.setOnCheckedChangeListener(null);
            wakeSwitch.setChecked(enabled);
            installWakeListener();
        }

        if (wakeState != null) {
            wakeState.setText(
                    enabled
                            ? "●  Wake word ON — say “Hey Buddy”"
                            : "○  Wake word OFF");
            wakeState.setTextColor(
                    enabled ? green : muted);
        }
    }

    private void installWakeListener() {
        if (wakeSwitch == null) return;

        wakeSwitch.setOnCheckedChangeListener(
                (buttonView, checked) -> {
                    if (checked
                            && Build.VERSION.SDK_INT >= 23
                            && checkSelfPermission(
                                    Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {

                        wakeSwitch.setOnCheckedChangeListener(null);
                        wakeSwitch.setChecked(false);
                        installWakeListener();

                        requestPermissions(
                                new String[]{
                                        Manifest.permission.RECORD_AUDIO},
                                REQ_WAKE_MIC);
                        setStatus(
                                "Microphone permission required for wake mode.",
                                danger);
                        return;
                    }

                    getSharedPreferences(
                            PREF, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_WAKE, checked)
                            .apply();

                    if (checked) {
                        startVoiceService(
                                BuddyVoiceService.ACTION_ENABLE_WAKE);
                        setStatus(
                                "✓ Hey Buddy wake listener enabled.",
                                green);
                    } else {
                        startVoiceService(
                                BuddyVoiceService.ACTION_DISABLE_WAKE);
                        setStatus(
                                "Wake listener stopped.",
                                muted);
                    }

                    updateWakeState();
                });
    }

    private void startVoiceService(
            String action) {
        try {
            Intent i =
                    new Intent(
                            this,
                            BuddyVoiceService.class);
            i.setAction(action);
            i.setPackage(
                    getPackageName());

            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Throwable t) {
            setStatus(
                    "Buddy voice service start failed.",
                    danger);
        }
    }

    private void setModels(
            List<String> models,
            String preferred) {
        availableModels.clear();

        if (models != null) {
            for (String value : models) {
                if (value == null) continue;
                String clean = value.trim();
                if (!clean.isEmpty()
                        && !availableModels.contains(
                                clean)) {
                    availableModels.add(clean);
                }
            }
        }

        if (availableModels.isEmpty()) {
            availableModels.addAll(
                    GeminiModelCatalog.defaults());
        }

        String current =
                preferred == null
                        || preferred.trim().isEmpty()
                        ? BuddySecrets.getModel(this)
                        : preferred.trim();

        int index =
                availableModels.indexOf(
                        current);

        if (index < 0 && !current.isEmpty()) {
            availableModels.add(
                    0, current);
            index = 0;
        }

        if (index < 0) index = 0;

        modelAdapter.notifyDataSetChanged();
        modelSpinner.setSelection(
                index, false);
        updateModelText();
    }

    private String selectedModel() {
        int position =
                modelSpinner == null
                        ? -1
                        : modelSpinner
                                .getSelectedItemPosition();

        if (position < 0
                || position >= availableModels.size()) {
            return "";
        }

        return availableModels.get(position);
    }

    private void updateModelText() {
        if (selectedModelText == null) return;

        String model = selectedModel();

        selectedModelText.setText(
                model.isEmpty()
                        ? "No model selected"
                        : "Active: " + model);
    }

    private boolean saveKeyAndModel() {
        String raw =
                apiKey == null
                        ? ""
                        : apiKey.getText()
                                .toString()
                                .trim();

        String existing =
                BuddySecrets.getGeminiApiKey(this);

        if (raw.isEmpty()) {
            if (existing.isEmpty()) {
                setStatus(
                        "Enter a Gemini API key first.",
                        danger);
                return false;
            }

            if (!BuddySecrets.hasGeminiApiKey(this)) {
                setStatus(
                        "Saved key could not be read.",
                        danger);
                return false;
            }
        } else {
            boolean saved =
                    BuddySecrets.saveGeminiApiKey(
                            this, raw);

            if (!saved) {
                setStatus(
                        "Key could not be saved on this device.",
                        danger);
                return false;
            }

            String readBack =
                    BuddySecrets.getGeminiApiKey(
                            this);

            if (!raw.equals(readBack)) {
                setStatus(
                        "Key save verification failed.",
                        danger);
                return false;
            }

            apiKey.setText("");
        }

        String selected = selectedModel();

        if (!selected.isEmpty()
                && !BuddySecrets.saveModel(
                        this, selected)) {
            setStatus(
                    "Key saved, but model setting failed.",
                    danger);
            return false;
        }

        updateKeyState();
        updateModelText();

        setStatus(
                "✓ Gemini key saved and verified.",
                green);

        return true;
    }

    private void refreshModels() {
        if (!BuddySecrets.hasGeminiApiKey(this)) {
            setModels(
                    GeminiModelCatalog.defaults(),
                    BuddySecrets.getModel(this));
            setStatus(
                    "Add a Gemini key before refreshing the live model list.",
                    muted);
            return;
        }

        setStatus(
                "Refreshing Gemini models…",
                cyan);

        GeminiModelCatalog.fetchAvailable(
                this,
                (models, error) -> {
                    setModels(
                            models,
                            BuddySecrets.getModel(
                                    this));

                    if (error == null) {
                        setStatus(
                                "✓ Live Gemini model list updated.",
                                green);
                    } else {
                        setStatus(
                                error,
                                muted);
                    }
                });
    }

    private void testSelectedModel() {
        String typed =
                apiKey == null
                        ? ""
                        : apiKey.getText()
                                .toString()
                                .trim();

        if (!typed.isEmpty()
                && !saveKeyAndModel()) {
            return;
        }

        if (!BuddySecrets.hasGeminiApiKey(this)) {
            setStatus(
                    "Enter and save your Gemini API key first.",
                    danger);
            return;
        }

        String key =
                BuddySecrets.getGeminiApiKey(
                        this);
        String model =
                selectedModel();

        setStatus(
                "Testing "
                        + (model.isEmpty()
                        ? "Gemini"
                        : model)
                        + "…",
                cyan);

        GeminiApiClient.test(
                this,
                key,
                model,
                (success, message) -> {
                    setStatus(
                            message,
                            success
                                    ? green
                                    : danger);
                    updateKeyState();
                });
    }

    private void testVoice() {
        if (previewTts != null) {
            try {
                previewTts.stop();
                previewTts.shutdown();
            } catch (Throwable ignored) {}
        }

        previewTts =
                new TextToSpeech(
                        this,
                        result -> {
                            if (result
                                    != TextToSpeech.SUCCESS
                                    || previewTts == null) {
                                setStatus(
                                        "TTS engine could not start.",
                                        danger);
                                return;
                            }

                            BuddyVoiceProfile.apply(
                                    previewTts,
                                    Locale.forLanguageTag(
                                            getSharedPreferences(
                                                    PREF,
                                                    MODE_PRIVATE)
                                                    .getString(
                                                            "language",
                                                            "en-IN")));

                            previewTts.setSpeechRate(
                                    0.75f
                                            + (rate.getProgress()
                                            / 60f)
                                            * 0.60f);

                            previewTts.setPitch(
                                    0.85f
                                            + (pitch.getProgress()
                                            / 60f)
                                            * 0.50f);

                            previewTts.speak(
                                    "Hi, I am Buddy. Bolo, main sun rahi hoon.",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "buddy-settings-test");

                            setStatus(
                                    "✓ Voice preview started.",
                                    green);
                        });
    }

    private void openAccessibility() {
        try {
            startActivity(
                    new Intent(
                            Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Throwable t) {
            startActivity(
                    new Intent(
                            Settings.ACTION_SETTINGS));
        }
    }

    private void openNotifications() {
        try {
            startActivity(
                    new Intent(
                            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Throwable t) {
            Toast.makeText(
                    this,
                    "Notification access settings unavailable.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void requestAssistant() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                android.app.role.RoleManager rm =
                        getSystemService(
                                android.app.role.RoleManager.class);

                if (rm != null
                        && rm.isRoleAvailable(
                                android.app.role.RoleManager.ROLE_ASSISTANT)) {

                    if (rm.isRoleHeld(
                            android.app.role.RoleManager.ROLE_ASSISTANT)) {
                        setStatus(
                                "✓ Buddy is already the default assistant.",
                                green);
                    } else {
                        startActivityForResult(
                                rm.createRequestRoleIntent(
                                        android.app.role.RoleManager.ROLE_ASSISTANT),
                                2001);
                    }
                    return;
                }
            }

            Toast.makeText(
                    this,
                    "Opening voice assistant settings…",
                    Toast.LENGTH_SHORT).show();

            startActivity(
                    new Intent(
                            Settings.ACTION_VOICE_INPUT_SETTINGS));
        } catch (Throwable t) {
            openAccessibility();
        }
    }

    private void build() {
        LinearLayout root =
                new LinearLayout(this);
        root.setOrientation(
                LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        ScrollView scroll =
                new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content =
                new LinearLayout(this);
        content.setOrientation(
                LinearLayout.VERTICAL);
        content.setPadding(
                dp(18), dp(13),
                dp(18), dp(34));

        scroll.addView(content);
        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1, 0, 1));

        LinearLayout top =
                new LinearLayout(this);
        top.setGravity(
                Gravity.CENTER_VERTICAL);

        Button back =
                button("‹", 46, card2, 28);
        back.setOnClickListener(
                v -> finish());

        top.addView(
                back,
                new LinearLayout.LayoutParams(
                        dp(48), dp(46)));

        LinearLayout titles =
                new LinearLayout(this);
        titles.setOrientation(
                LinearLayout.VERTICAL);
        titles.setPadding(
                dp(12), 0, dp(8), 0);

        titles.addView(
                text("Buddy Settings",
                        23, white, true));
        titles.addView(
                text("Everything Buddy can control",
                        12, muted, false));

        top.addView(
                titles,
                new LinearLayout.LayoutParams(
                        0, -2, 1));

        Button done =
                button(
                        "Done",
                        44,
                        card2,
                        13);
        done.setOnClickListener(
                v -> finish());

        top.addView(
                done,
                new LinearLayout.LayoutParams(
                        dp(74), dp(44)));

        content.addView(top);
        gap(content, 15);

        LinearLayout hero =
                new LinearLayout(this);
        hero.setOrientation(
                LinearLayout.VERTICAL);
        hero.setPadding(
                dp(18), dp(18),
                dp(18), dp(18));
        hero.setBackground(
                gradient(
                        Color.rgb(26, 24, 69),
                        Color.rgb(14, 35, 53),
                        24));

        hero.addView(
                text(
                        "✦  BUDDY CONTROL CENTER",
                        11, cyan, true));

        TextView ht =
                text(
                        "Configure once. Use everywhere.",
                        22, white, true);
        ht.setPadding(
                0, dp(6), 0, dp(4));
        hero.addView(ht);

        hero.addView(
                text(
                        "AI • voice • wake word • phone control",
                        13, muted, false));

        content.addView(hero);
        gap(content, 16);

        LinearLayout ai = card();
        content.addView(ai);

        section(
                ai,
                "Gemini AI",
                "Key storage is verified after every save. A blank field keeps the current key.");

        apiKey = keyField();
        ai.addView(
                apiKey,
                new LinearLayout.LayoutParams(
                        -1, dp(54)));

        keyState =
                text("", 12, green, true);
        keyState.setPadding(
                dp(3), dp(8), 0, 0);
        ai.addView(keyState);
        updateKeyState();

        Button save =
                button(
                        "SAVE & VERIFY KEY",
                        50,
                        accent,
                        14);

        save.setOnClickListener(
                v -> saveKeyAndModel());

        LinearLayout.LayoutParams saveP =
                new LinearLayout.LayoutParams(
                        -1, dp(50));
        saveP.setMargins(
                0, dp(10), 0, 0);

        ai.addView(save, saveP);

        LinearLayout modelRow =
                new LinearLayout(this);
        modelRow.setGravity(
                Gravity.CENTER_VERTICAL);

        modelRow.addView(
                text(
                        "AI model",
                        14,
                        white,
                        true),
                new LinearLayout.LayoutParams(
                        0, dp(42), 1));

        selectedModelText =
                text("", 12, cyan, false);
        selectedModelText.setGravity(
                Gravity.CENTER_VERTICAL);

        modelRow.addView(
                selectedModelText,
                new LinearLayout.LayoutParams(
                        -2, dp(42)));

        ai.addView(modelRow);

        modelSpinner =
                new Spinner(this);
        modelAdapter =
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_item,
                        availableModels);

        modelAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        modelSpinner.setAdapter(
                modelAdapter);

        modelSpinner.setBackground(
                rounded(card2, line, 16));

        ai.addView(
                modelSpinner,
                new LinearLayout.LayoutParams(
                        -1, dp(52)));

        modelSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        if (position < 0
                                || position >= availableModels.size()) {
                            return;
                        }

                        String selected =
                                availableModels.get(
                                        position);

                        if (!selected.isEmpty()) {
                            boolean ok =
                                    BuddySecrets.saveModel(
                                            BuddySettingsActivity.this,
                                            selected);

                            if (!ok) {
                                setStatus(
                                        "Model could not be saved.",
                                        danger);
                            }
                        }

                        updateModelText();
                    }

                    @Override public void onNothingSelected(
                            AdapterView<?> parent) {}
                });

        LinearLayout modelActions =
                new LinearLayout(this);

        Button refresh =
                button(
                        "Refresh models",
                        44,
                        card2,
                        12);
        refresh.setOnClickListener(
                v -> refreshModels());

        modelActions.addView(
                refresh,
                new LinearLayout.LayoutParams(
                        0, dp(44), 1));

        Button test =
                button(
                        "Test connection",
                        44,
                        card2,
                        12);
        test.setOnClickListener(
                v -> testSelectedModel());

        LinearLayout.LayoutParams testP =
                new LinearLayout.LayoutParams(
                        0, dp(44), 1);
        testP.setMargins(
                dp(8), 0, 0, 0);

        modelActions.addView(
                test, testP);

        LinearLayout.LayoutParams modelActionP =
                new LinearLayout.LayoutParams(
                        -1, dp(44));
        modelActionP.setMargins(
                0, dp(9), 0, 0);

        ai.addView(
                modelActions,
                modelActionP);

        Button clear =
                button(
                        "Clear saved Gemini key",
                        44,
                        card2,
                        12);

        clear.setOnClickListener(
                v -> {
                    boolean ok =
                            BuddySecrets.clearGeminiApiKey(
                                    this);
                    apiKey.setText("");
                    updateKeyState();

                    setStatus(
                            ok
                                    ? "Saved Gemini key removed."
                                    : "Key removal could not be verified.",
                            ok ? green : danger);
                });

        LinearLayout.LayoutParams clearP =
                new LinearLayout.LayoutParams(
                        -1, dp(44));
        clearP.setMargins(
                0, dp(8), 0, 0);

        ai.addView(clear, clearP);
        gap(content, 14);

        LinearLayout wake =
                card();
        content.addView(wake);

        section(
                wake,
                "Hey Buddy wake word",
                "When enabled, Buddy keeps a controlled speech-recognition listener active until you turn it off.");

        LinearLayout wakeRow =
                new LinearLayout(this);
        wakeRow.setGravity(
                Gravity.CENTER_VERTICAL);

        LinearLayout wakeCopy =
                new LinearLayout(this);
        wakeCopy.setOrientation(
                LinearLayout.VERTICAL);

        wakeCopy.addView(
                text(
                        "Say “Hey Buddy”",
                        15, white, true));
        wakeState =
                text("", 12, muted, false);
        wakeState.setPadding(
                0, dp(4), 0, 0);

        wakeCopy.addView(wakeState);

        wakeRow.addView(
                wakeCopy,
                new LinearLayout.LayoutParams(
                        0, -2, 1));

        wakeSwitch =
                new Switch(this);

        wakeSwitch.setChecked(
                getSharedPreferences(
                        PREF,
                        MODE_PRIVATE)
                        .getBoolean(
                                KEY_WAKE,
                                false));

        wakeRow.addView(wakeSwitch);
        wake.addView(wakeRow);

        installWakeListener();
        updateWakeState();

        gap(content, 14);

        LinearLayout voice =
                card();
        content.addView(voice);

        section(
                voice,
                "Buddy Voice",
                "These values are used by the real voice service and by the preview.");

        int savedRate =
                getSharedPreferences(
                        PREF,
                        MODE_PRIVATE)
                        .getInt(KEY_RATE, 30);

        int savedPitch =
                getSharedPreferences(
                        PREF,
                        MODE_PRIVATE)
                        .getInt(KEY_PITCH, 36);

        voice.addView(
                text(
                        "Speech speed",
                        14, white, true));

        rate =
                new SeekBar(this);
        rate.setMax(60);
        rate.setProgress(
                savedRate);
        voice.addView(
                rate,
                new LinearLayout.LayoutParams(
                        -1, dp(46)));

        voice.addView(
                text(
                        "Voice pitch",
                        14, white, true));

        pitch =
                new SeekBar(this);
        pitch.setMax(60);
        pitch.setProgress(
                savedPitch);
        voice.addView(
                pitch,
                new LinearLayout.LayoutParams(
                        -1, dp(46)));

        rate.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(
                            SeekBar bar,
                            int progress,
                            boolean fromUser) {
                        getSharedPreferences(
                                PREF,
                                MODE_PRIVATE)
                                .edit()
                                .putInt(
                                        KEY_RATE,
                                        progress)
                                .apply();
                    }

                    @Override public void onStartTrackingTouch(
                            SeekBar bar) {}

                    @Override public void onStopTrackingTouch(
                            SeekBar bar) {}
                });

        pitch.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(
                            SeekBar bar,
                            int progress,
                            boolean fromUser) {
                        getSharedPreferences(
                                PREF,
                                MODE_PRIVATE)
                                .edit()
                                .putInt(
                                        KEY_PITCH,
                                        progress)
                                .apply();
                    }

                    @Override public void onStartTrackingTouch(
                            SeekBar bar) {}

                    @Override public void onStopTrackingTouch(
                            SeekBar bar) {}
                });

        Button voiceTest =
                button(
                        "▶  Test Buddy voice",
                        48,
                        card2,
                        13);
        voiceTest.setOnClickListener(
                v -> testVoice());

        LinearLayout.LayoutParams voiceTestP =
                new LinearLayout.LayoutParams(
                        -1, dp(48));
        voiceTestP.setMargins(
                0, dp(8), 0, 0);

        voice.addView(
                voiceTest,
                voiceTestP);

        gap(content, 14);

        LinearLayout controls =
                card();
        content.addView(controls);

        section(
                controls,
                "Phone control",
                "Open the required Android control panels from Buddy.");

        Button accessibility =
                button(
                        "Open Accessibility settings",
                        48,
                        card2,
                        13);
        accessibility.setOnClickListener(
                v -> openAccessibility());
        controls.addView(accessibility);

        Button notifications =
                button(
                        "Open Notification access",
                        48,
                        card2,
                        13);
        notifications.setOnClickListener(
                v -> openNotifications());

        LinearLayout.LayoutParams np =
                new LinearLayout.LayoutParams(
                        -1, dp(48));
        np.setMargins(
                0, dp(8), 0, 0);

        controls.addView(
                notifications, np);

        Button assistant =
                button(
                        "Set Buddy as default assistant",
                        48,
                        card2,
                        13);
        assistant.setOnClickListener(
                v -> requestAssistant());

        LinearLayout.LayoutParams ap =
                new LinearLayout.LayoutParams(
                        -1, dp(48));
        ap.setMargins(
                0, dp(8), 0, 0);

        controls.addView(
                assistant, ap);

        gap(content, 14);

        LinearLayout diag =
                card();
        content.addView(diag);

        section(
                diag,
                "Diagnostics",
                "Everything below updates from live operations.");

        status =
                text(
                        "Ready.",
                        14,
                        green,
                        true);
        status.setPadding(
                0, dp(5), 0, 0);

        diag.addView(status);

        setModels(
                GeminiModelCatalog.defaults(),
                BuddySecrets.getModel(this));

        setContentView(root);
        updateKeyState();
        updateModelText();

        if (BuddySecrets.hasGeminiApiKey(this)) {
            refreshModels();
        } else {
            setStatus(
                    "Add your Gemini key above, then tap SAVE & VERIFY KEY.",
                    muted);
        }
    }

    @Override protected void onCreate(
            Bundle state) {
        super.onCreate(state);
        build();
    }

    @Override public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == REQ_WAKE_MIC) {
            boolean granted =
                    grantResults.length > 0
                            && grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                wakeSwitch.setOnCheckedChangeListener(null);
                wakeSwitch.setChecked(true);
                installWakeListener();

                getSharedPreferences(
                        PREF,
                        MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_WAKE, true)
                        .apply();

                startVoiceService(
                        BuddyVoiceService.ACTION_ENABLE_WAKE);

                updateWakeState();

                setStatus(
                        "✓ Hey Buddy wake listener enabled.",
                        green);
            } else {
                wakeSwitch.setOnCheckedChangeListener(null);
                wakeSwitch.setChecked(false);
                installWakeListener();

                setStatus(
                        "Microphone permission was denied.",
                        danger);
            }
        }
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
