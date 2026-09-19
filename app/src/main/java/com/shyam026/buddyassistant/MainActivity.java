package com.shyam026.buddyassistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String PREF = "buddy_prefs";
    private static final String KEY_WAKE = "wake_mode";
    private static final String KEY_LANG = "language";

    private static final int REQ_MIC = 700;
    private static final int REQ_PHONE_PERMISSIONS = 701;
    private static final int REQ_ASSISTANT_ROLE = 702;

    private SharedPreferences prefs;
    private TextView stateText;
    private TextView userText;
    private TextView buddyText;
    private TextView accessText;
    private TextView wakeText;
    private TextView aiStateText;
    private TextView aiModelText;
    private Button talkButton;
    private Switch wakeSwitch;
    private Button languageButton;
    private ImageView orb;
    private String language = "en-IN";

    private ObjectAnimator pulseX;
    private ObjectAnimator pulseY;

    private final Map<String, String> apps = new LinkedHashMap<>();

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!BuddyVoiceService.ACTION_STATUS.equals(intent.getAction())) return;

            String state = intent.getStringExtra(BuddyVoiceService.EXTRA_STATE);
            String transcript = intent.getStringExtra(BuddyVoiceService.EXTRA_TRANSCRIPT);
            String reply = intent.getStringExtra(BuddyVoiceService.EXTRA_REPLY);

            if (transcript != null && !transcript.trim().isEmpty()) {
                userText.setText("You  •  " + transcript);
            }
            if (reply != null && !reply.trim().isEmpty()) {
                buddyText.setText("Buddy  •  " + reply);
            }
            renderState(state == null ? BuddyVoiceService.STATE_IDLE : state);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        language = prefs.getString(KEY_LANG, "en-IN");

        String[][] supportedApps = {
                {"youtube", "com.google.android.youtube"},
                {"chrome", "com.android.chrome"},
                {"whatsapp", "com.whatsapp"},
                {"instagram", "com.instagram.android"},
                {"spotify", "com.spotify.music"},
                {"telegram", "org.telegram.messenger"},
                {"maps", "com.google.android.apps.maps"},
                {"gmail", "com.google.android.gm"},
                {"calculator", "com.google.android.calculator"}
        };
        for (String[] app : supportedApps) apps.put(app[0], app[1]);

        getWindow().setStatusBarColor(Color.rgb(7, 10, 18));
        getWindow().setNavigationBarColor(Color.rgb(7, 10, 18));

        buildUi();

        IntentFilter filter = new IntentFilter(BuddyVoiceService.ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
    }

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

    private Button button(String label, int height, int background, float size) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(size);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(background, 0, 16));
        return b;
    }

    private void space(LinearLayout parent, int height) {
        Space s = new Space(this);
        parent.addView(s, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void sectionTitle(LinearLayout parent, String title, String subtitle) {
        TextView t = text(title.toUpperCase(), 11, Color.rgb(89, 207, 255), true);
        t.setLetterSpacing(0.08f);
        parent.addView(t);
        TextView s = text(subtitle, 13, Color.rgb(154, 166, 195), false);
        s.setPadding(0, dp(3), 0, dp(10));
        parent.addView(s);
    }

    private LinearLayout card(int fill, int stroke, int radius, int pad) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(pad), dp(pad), dp(pad), dp(pad));
        l.setBackground(rounded(fill, stroke, radius));
        return l;
    }

    private void buildUi() {
        final int bg = Color.rgb(7, 10, 18);
        final int surface = Color.rgb(15, 20, 33);
        final int surface2 = Color.rgb(22, 29, 47);
        final int line = Color.rgb(46, 58, 84);
        final int white = Color.WHITE;
        final int muted = Color.rgb(154, 166, 195);
        final int accent = Color.rgb(117, 95, 255);
        final int cyan = Color.rgb(89, 207, 255);
        final int green = Color.rgb(82, 220, 151);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.buddy_logo);
        logo.setBackground(rounded(Color.rgb(31, 28, 75), line, 18));
        logo.setPadding(dp(7), dp(7), dp(7), dp(7));
        top.addView(logo, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(11), 0, dp(5), 0);
        brand.addView(text("BUDDY", 22, white, true));
        brand.addView(text("your phone's little genius", 12, muted, false));
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        languageButton = button(
                language.equals("hi-IN") ? "HI" : "EN",
                42, surface2, 11);
        languageButton.setOnClickListener(v -> {
            language = language.equals("en-IN") ? "hi-IN" : "en-IN";
            prefs.edit().putString(KEY_LANG, language).apply();
            languageButton.setText(language.equals("hi-IN") ? "HI" : "EN");
        });
        top.addView(languageButton, new LinearLayout.LayoutParams(dp(52), dp(42)));

        Button settings = button("⚙", 42, surface2, 20);
        settings.setContentDescription("Open Buddy Settings");
        settings.setOnClickListener(v ->
                startActivity(new Intent(this, BuddySettingsActivity.class)));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(50), dp(42));
        sp.setMargins(dp(7), 0, 0, 0);
        top.addView(settings, sp);

        content.addView(top);
        space(content, 16);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(18), dp(19), dp(18), dp(18));
        hero.setBackground(gradient(
                Color.rgb(28, 24, 70),
                Color.rgb(15, 36, 54), 26));

        TextView badge = text("✦  BUDDY IS READY", 11, cyan, true);
        badge.setLetterSpacing(0.08f);
        hero.addView(badge);

        orb = new ImageView(this);
        orb.setImageResource(R.drawable.buddy_logo);
        orb.setPadding(dp(13), dp(13), dp(13), dp(13));
        orb.setBackground(gradient(
                Color.rgb(91, 72, 230),
                Color.rgb(36, 181, 225), 100));
        LinearLayout.LayoutParams orbParams = new LinearLayout.LayoutParams(dp(94), dp(94));
        orbParams.setMargins(0, dp(12), 0, dp(10));
        hero.addView(orb, orbParams);

        stateText = text("●  Ready", 21, white, true);
        hero.addView(stateText);

        wakeText = text(
                "Tap the big button and tell me what to do.",
                13, muted, false);
        wakeText.setGravity(Gravity.CENTER);
        wakeText.setPadding(0, dp(4), 0, 0);
        hero.addView(wakeText);

        talkButton = button("🎙  TAP TO TALK", 56, accent, 15);
        talkButton.setOnClickListener(v -> startCommandListening());
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(56));
        tp.setMargins(dp(0), dp(15), dp(0), 0);
        hero.addView(talkButton, tp);

        content.addView(hero);
        space(content, 12);

        LinearLayout transcript = card(surface, line, 22, 15);
        sectionTitle(transcript, "Conversation", "Latest voice command and Buddy reply.");

        userText = text("You  •  —", 14, muted, false);
        transcript.addView(userText);
        buddyText = text("Buddy  •  Bolo. Main sun rahi hoon.", 15, white, true);
        buddyText.setPadding(0, dp(8), 0, 0);
        transcript.addView(buddyText);

        content.addView(transcript);
        space(content, 12);

        LinearLayout wake = new LinearLayout(this);
        wake.setGravity(Gravity.CENTER_VERTICAL);
        wake.setPadding(dp(15), dp(12), dp(12), dp(12));
        wake.setBackground(rounded(surface, line, 20));

        LinearLayout wakeInfo = new LinearLayout(this);
        wakeInfo.setOrientation(LinearLayout.VERTICAL);
        wakeInfo.addView(text("Hey Buddy mode", 15, white, true));
        wakeInfo.addView(text(
                "Mic stays off until an explicit assistant interaction.",
                12, muted, false));
        wake.addView(wakeInfo, new LinearLayout.LayoutParams(0, -2, 1));

        wakeSwitch = new Switch(this);
        wakeSwitch.setChecked(prefs.getBoolean(KEY_WAKE, false));
        wakeSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_WAKE, checked).apply();
            stopVoiceService();
            renderState(checked
                    ? BuddyVoiceService.STATE_WAITING_WAKE
                    : BuddyVoiceService.STATE_IDLE);
        });
        wake.addView(wakeSwitch);

        content.addView(wake);
        space(content, 15);

        content.addView(text("QUICK ACTIONS", 11, muted, true));
        space(content, 7);

        LinearLayout row1 = new LinearLayout(this);
        addQuick(row1, "▶  YouTube", "YouTube kholo", surface2);
        addQuick(row1, "⌕  Search", "search JEE physics", surface2);
        content.addView(row1);
        space(content, 7);

        LinearLayout row2 = new LinearLayout(this);
        addQuick(row2, "🔊  Volume +", "volume badhao", surface2);
        addQuick(row2, "☀  Brightness", "brightness 60", surface2);
        content.addView(row2);
        space(content, 7);

        LinearLayout row3 = new LinearLayout(this);
        addQuick(row3, "‹  Back", "back jao", surface2);
        addQuick(row3, "⌂  Home", "home jao", surface2);
        content.addView(row3);
        space(content, 7);

        LinearLayout row4 = new LinearLayout(this);
        addQuick(row4, "▣  Screenshot", "screenshot", surface2);
        addQuick(row4, "◉  Notifications", "notifications kholo", surface2);
        content.addView(row4);
        space(content, 15);

        LinearLayout aiCard = card(surface, line, 22, 15);
        sectionTitle(aiCard, "Gemini brain", "Bring your own Gemini key. The key is encrypted on-device.");

        LinearLayout aiRow = new LinearLayout(this);
        aiRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout aiCopy = new LinearLayout(this);
        aiCopy.setOrientation(LinearLayout.VERTICAL);
        aiModelText = text("Active: " + BuddySecrets.getModel(this), 14, white, true);
        aiCopy.addView(aiModelText);
        aiStateText = text("", 12, muted, false);
        aiStateText.setPadding(0, dp(3), 0, 0);
        aiCopy.addView(aiStateText);
        aiRow.addView(aiCopy, new LinearLayout.LayoutParams(0, -2, 1));

        Button aiSettings = button("Open Settings", 44, accent, 12);
        aiSettings.setOnClickListener(v ->
                startActivity(new Intent(this, BuddySettingsActivity.class)));
        aiRow.addView(aiSettings, new LinearLayout.LayoutParams(dp(122), dp(44)));
        aiCard.addView(aiRow);
        content.addView(aiCard);
        space(content, 15);

        LinearLayout setup = card(surface, line, 22, 15);
        sectionTitle(setup, "Phone control", "One-time permissions unlock deeper actions.");

        accessText = text(accessStatus(), 13, white, false);
        accessText.setPadding(0, 0, 0, dp(11));
        setup.addView(accessText);

        LinearLayout setupRow1 = new LinearLayout(this);
        Button permissions = button("Permissions", 44, surface2, 12);
        permissions.setOnClickListener(v -> requestPhonePermissions());
        setupRow1.addView(permissions, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button accessibility = button("Accessibility", 44, surface2, 12);
        accessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams accP = new LinearLayout.LayoutParams(0, dp(44), 1);
        accP.setMargins(dp(7), 0, 0, 0);
        setupRow1.addView(accessibility, accP);
        setup.addView(setupRow1);

        LinearLayout setupRow2 = new LinearLayout(this);
        Button assistant = button("Default assistant", 44, surface2, 12);
        assistant.setOnClickListener(v -> requestAssistantRole());
        setupRow2.addView(assistant, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button settings2 = button("⚙ Settings", 44, surface2, 12);
        settings2.setOnClickListener(v ->
                startActivity(new Intent(this, BuddySettingsActivity.class)));
        LinearLayout.LayoutParams st2 = new LinearLayout.LayoutParams(0, dp(44), 1);
        st2.setMargins(dp(7), dp(7), 0, 0);
        setupRow2.addView(settings2, st2);
        setup.addView(setupRow2);

        content.addView(setup);
        space(content, 15);

        LinearLayout tips = card(
                Color.rgb(13, 20, 31),
                Color.rgb(33, 72, 93), 22, 15);
        sectionTitle(tips, "Try saying", "Natural Hinglish commands work best.");

        TextView examples = text(
                "“Hey Buddy, YouTube kholo”\n" +
                "“Search JEE physics”\n" +
                "“Volume 60”\n" +
                "“Brightness 40”\n" +
                "“Back jao”",
                13, white, false);
        tips.addView(examples);

        content.addView(tips);
        space(content, 15);

        TextView footer = text(
                "Local-first • honest status • one voice engine",
                11, muted, false);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer);

        setContentView(root);
        renderState(BuddyVoiceService.STATE_IDLE);
        refreshAiSummary();
        startOrbAnimation();
    }

    private void startOrbAnimation() {
        if (orb == null) return;

        stopOrbAnimation();

        pulseX = ObjectAnimator.ofFloat(orb, View.SCALE_X, 1.0f, 1.045f, 1.0f);
        pulseY = ObjectAnimator.ofFloat(orb, View.SCALE_Y, 1.0f, 1.045f, 1.0f);
        for (ObjectAnimator animator : new ObjectAnimator[]{pulseX, pulseY}) {
            animator.setDuration(1900L);
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        }
    }

    private void stopOrbAnimation() {
        if (pulseX != null) pulseX.cancel();
        if (pulseY != null) pulseY.cancel();
        pulseX = null;
        pulseY = null;
    }

    private void refreshAiSummary() {
        if (aiStateText != null) {
            if (BuddySecrets.hasGeminiApiKey(this)) {
                aiStateText.setText("● Key saved • ready to test");
                aiStateText.setTextColor(Color.rgb(82, 220, 151));
            } else {
                aiStateText.setText("○ Add a key in Settings");
                aiStateText.setTextColor(Color.rgb(154, 166, 195));
            }
        }
        if (aiModelText != null) {
            aiModelText.setText("Active: " + BuddySecrets.getModel(this));
        }
    }

    private void addQuick(LinearLayout row, String label, String command, int background) {
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button b = button(label, 46, background, 12);
        b.setOnClickListener(v -> sendCommand(command));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(46), 1);
        row.addView(b, p);
    }

    private String accessStatus() {
        return BuddyAccessibilityService.isEnabled()
                ? "✓ Phone control enabled"
                : "○ Enable Accessibility for Back, Home, Recents, scrolling and typing";
    }

    private void renderState(String state) {
        if (state == null) state = BuddyVoiceService.STATE_IDLE;

        final int white = Color.WHITE;
        final int muted = Color.rgb(154, 166, 195);
        final int cyan = Color.rgb(89, 207, 255);
        final int green = Color.rgb(82, 220, 151);
        final int danger = Color.rgb(255, 119, 133);

        if (BuddyVoiceService.STATE_WAITING_WAKE.equals(state)) {
            stateText.setText("●  Assistant ready");
            stateText.setTextColor(cyan);
            wakeText.setText("Mic is OFF. Use the system assistant to invoke Buddy.");
            talkButton.setText("🎙  TAP TO TALK");
        } else if (BuddyVoiceService.STATE_LISTENING_COMMAND.equals(state)) {
            stateText.setText("●  Listening…");
            stateText.setTextColor(cyan);
            wakeText.setText("Command mode is active — speak now.");
            talkButton.setText("●  LISTENING");
        } else if (BuddyVoiceService.STATE_PROCESSING.equals(state)) {
            stateText.setText("●  Thinking…");
            stateText.setTextColor(cyan);
            wakeText.setText("Running your command.");
            talkButton.setText("◌  WORKING");
        } else if (BuddyVoiceService.STATE_SPEAKING.equals(state)) {
            stateText.setText("●  Buddy is speaking");
            stateText.setTextColor(green);
            wakeText.setText("Reply is ready. Ask for the next thing when you're done.");
            talkButton.setText("🎙  TAP TO TALK");
        } else if (BuddyVoiceService.STATE_ERROR.equals(state)) {
            stateText.setText("●  Needs attention");
            stateText.setTextColor(danger);
            wakeText.setText("Check the microphone permission and speech recognition.");
            talkButton.setText("🎙  TRY AGAIN");
        } else {
            stateText.setText("●  Ready");
            stateText.setTextColor(white);
            wakeText.setText(wakeSwitch != null && wakeSwitch.isChecked()
                    ? "Assistant mode enabled. Mic stays off until explicit invocation."
                    : "Tap the big button and tell me what to do.");
            talkButton.setText("🎙  TAP TO TALK");
        }
    }

    private void startCommandListening() {
        ensureMicrophoneThenStart(false);
    }

    private void ensureMicrophoneThenStart(boolean wake) {
        if (wake) {
            prefs.edit().putBoolean(KEY_WAKE, true).apply();
            renderState(BuddyVoiceService.STATE_WAITING_WAKE);
            return;
        }

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }

        startVoiceService(BuddyVoiceService.ACTION_TAP_COMMAND, null);
    }

    private void startVoiceService(String action, String command) {
        try {
            Intent i = new Intent(this, BuddyVoiceService.class);
            i.setAction(action);
            i.setPackage(getPackageName());
            if (command != null) i.putExtra(BuddyVoiceService.EXTRA_COMMAND, command);

            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Throwable t) {
            Toast.makeText(this, "Buddy voice service start nahi hua.", Toast.LENGTH_SHORT).show();
            renderState(BuddyVoiceService.STATE_ERROR);
        }
    }

    private void stopVoiceService() {
        try {
            stopService(new Intent(this, BuddyVoiceService.class));
        } catch (Throwable ignored) {}
    }

    private void sendCommand(String command) {
        if (command == null || command.trim().isEmpty()) return;
        startVoiceService(BuddyVoiceService.ACTION_TAP_COMMAND, command.trim());
    }

    private void requestPhonePermissions() {
        ArrayList<String> missing = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.READ_CONTACTS);
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (missing.isEmpty()) {
            Toast.makeText(this, "Required permissions already granted.", Toast.LENGTH_SHORT).show();
            return;
        }

        requestPermissions(missing.toArray(new String[0]), REQ_PHONE_PERMISSIONS);
    }

    private void requestAssistantRole() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager != null
                        && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)
                        && !roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                    startActivityForResult(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),
                            REQ_ASSISTANT_ROLE);
                    return;
                }
            }
            openVoiceSettings();
        } catch (Throwable t) {
            openVoiceSettings();
        }
    }

    private void openVoiceSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
        } catch (Throwable t) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable ignored) {}
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (accessText != null) accessText.setText(accessStatus());
        refreshAiSummary();

        boolean wake = prefs.getBoolean(KEY_WAKE, false);
        if (wakeSwitch != null && wakeSwitch.isChecked() != wake) {
            wakeSwitch.setOnCheckedChangeListener(null);
            wakeSwitch.setChecked(wake);
            wakeSwitch.setOnCheckedChangeListener((button, checked) -> {
                prefs.edit().putBoolean(KEY_WAKE, checked).apply();
                stopVoiceService();
                renderState(checked
                        ? BuddyVoiceService.STATE_WAITING_WAKE
                        : BuddyVoiceService.STATE_IDLE);
            });
        }
    }

    @Override public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_MIC) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (!granted) {
                if (wakeSwitch != null) {
                    wakeSwitch.setOnCheckedChangeListener(null);
                    wakeSwitch.setChecked(false);
                    wakeSwitch.setOnCheckedChangeListener((button, checked) -> {
                        prefs.edit().putBoolean(KEY_WAKE, checked).apply();
                        stopVoiceService();
                        renderState(checked
                                ? BuddyVoiceService.STATE_WAITING_WAKE
                                : BuddyVoiceService.STATE_IDLE);
                    });
                }
                prefs.edit().putBoolean(KEY_WAKE, false).apply();
                renderState(BuddyVoiceService.STATE_ERROR);
                Toast.makeText(this, "Microphone permission allow karo.", Toast.LENGTH_SHORT).show();
                return;
            }

            startVoiceService(BuddyVoiceService.ACTION_TAP_COMMAND, null);
            return;
        }

        if (requestCode == REQ_PHONE_PERMISSIONS) {
            Toast.makeText(this, "Permission setup updated.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onDestroy() {
        stopOrbAnimation();
        try {
            unregisterReceiver(statusReceiver);
        } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
