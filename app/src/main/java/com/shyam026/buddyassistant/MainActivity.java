package com.shyam026.buddyassistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import java.util.Locale;
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
    private Button talkButton;
    private Switch wakeSwitch;
    private Button languageButton;
    private String language = "en-IN";

    private final Map<String, String> apps = new LinkedHashMap<>();

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!BuddyVoiceService.ACTION_STATUS.equals(intent.getAction())) return;

            String state = intent.getStringExtra(BuddyVoiceService.EXTRA_STATE);
            String transcript = intent.getStringExtra(BuddyVoiceService.EXTRA_TRANSCRIPT);
            String reply = intent.getStringExtra(BuddyVoiceService.EXTRA_REPLY);

            if (transcript != null && !transcript.trim().isEmpty()) {
                userText.setText(transcript);
            }
            if (reply != null && !reply.trim().isEmpty()) {
                buddyText.setText(reply);
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
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(background, 0, 18));
        return b;
    }

    private void space(LinearLayout parent, int height) {
        Space s = new Space(this);
        parent.addView(s, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void buildUi() {
        final int bg = Color.rgb(7, 10, 18);
        final int card = Color.rgb(17, 22, 35);
        final int card2 = Color.rgb(23, 30, 48);
        final int line = Color.rgb(46, 55, 78);
        final int white = Color.WHITE;
        final int muted = Color.rgb(157, 168, 194);
        final int accent = Color.rgb(112, 92, 255);
        final int cyan = Color.rgb(89, 207, 255);
        final int green = Color.rgb(82, 220, 151);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(30));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.buddy_logo);
        logo.setBackground(rounded(Color.rgb(31, 28, 75), line, 22));
        logo.setPadding(dp(7), dp(7), dp(7), dp(7));
        header.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(12), 0, 0, 0);
        brand.addView(text("BUDDY", 25, white, true));
        brand.addView(text("Your hands-free phone assistant", 13, muted, false));
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        languageButton = button(language.equals("hi-IN") ? "HI" : "EN", 44, card2, 13);
        languageButton.setOnClickListener(v -> {
            language = language.equals("en-IN") ? "hi-IN" : "en-IN";
            prefs.edit().putString(KEY_LANG, language).apply();
            languageButton.setText(language.equals("hi-IN") ? "HI" : "EN");
        });
        header.addView(languageButton);

        content.addView(header);
        space(content, 18);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(rounded(card, line, 24));

        TextView overline = text("VOICE CONTROL", 11, muted, true);
        hero.addView(overline);

        stateText = text("Ready", 20, white, true);
        stateText.setPadding(0, dp(5), 0, dp(7));
        hero.addView(stateText);

        wakeText = text("Wake mode is off. Tap to talk or enable “Hey Buddy”.", 14, muted, false);
        hero.addView(wakeText);

        userText = text("You: —", 16, white, false);
        userText.setPadding(0, dp(16), 0, dp(6));
        hero.addView(userText);

        buddyText = text("Buddy: Ready hoon. Bolo.", 16, white, true);
        hero.addView(buddyText);

        content.addView(hero);
        space(content, 14);

        talkButton = button("🎙  TAP TO TALK", 60, accent, 16);
        talkButton.setOnClickListener(v -> startCommandListening());
        content.addView(talkButton);
        space(content, 16);

        LinearLayout wakeCard = new LinearLayout(this);
        wakeCard.setGravity(Gravity.CENTER_VERTICAL);
        wakeCard.setPadding(dp(16), dp(12), dp(12), dp(12));
        wakeCard.setBackground(rounded(card, line, 20));

        LinearLayout wakeInfo = new LinearLayout(this);
        wakeInfo.setOrientation(LinearLayout.VERTICAL);
        wakeInfo.addView(text("Hey Buddy", 16, white, true));
        wakeInfo.addView(text("No background mic. Use Buddy as the default assistant.", 13, muted, false));
        wakeCard.addView(wakeInfo, new LinearLayout.LayoutParams(0, -2, 1));

        wakeSwitch = new Switch(this);
        wakeSwitch.setText("");
        wakeSwitch.setChecked(prefs.getBoolean(KEY_WAKE, false));
        wakeSwitch.setOnCheckedChangeListener((button, checked) -> {\n            prefs.edit().putBoolean(KEY_WAKE, checked).apply();\n            stopVoiceService();\n            renderState(checked ? BuddyVoiceService.STATE_WAITING_WAKE : BuddyVoiceService.STATE_IDLE);\n        });\n        wakeCard.addView(wakeSwitch);
        content.addView(wakeCard);
        space(content, 16);

        content.addView(text("QUICK ACTIONS", 11, muted, true));
        space(content, 7);

        LinearLayout quickRow1 = new LinearLayout(this);
        quickRow1.setOrientation(LinearLayout.HORIZONTAL);
        addQuick(quickRow1, "YouTube", "YouTube kholo", card2);
        addQuick(quickRow1, "Search", "search JEE physics", card2);
        content.addView(quickRow1);
        space(content, 7);

        LinearLayout quickRow2 = new LinearLayout(this);
        quickRow2.setOrientation(LinearLayout.HORIZONTAL);
        addQuick(quickRow2, "Volume +", "volume badhao", card2);
        addQuick(quickRow2, "Brightness", "brightness 60", card2);
        content.addView(quickRow2);
        space(content, 7);

        LinearLayout quickRow3 = new LinearLayout(this);
        quickRow3.setOrientation(LinearLayout.HORIZONTAL);
        addQuick(quickRow3, "Back", "back jao", card2);
        addQuick(quickRow3, "Home", "home jao", card2);
        content.addView(quickRow3);
        space(content, 16);

        LinearLayout setupCard = new LinearLayout(this);
        setupCard.setOrientation(LinearLayout.VERTICAL);
        setupCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        setupCard.setBackground(rounded(card, line, 20));

        setupCard.addView(text("ONE-TIME SETUP", 11, muted, true));
        accessText = text(accessStatus(), 14, white, false);
        accessText.setPadding(0, dp(7), 0, dp(12));
        setupCard.addView(accessText);

        LinearLayout setupRow = new LinearLayout(this);
        setupRow.setOrientation(LinearLayout.HORIZONTAL);

        Button permissions = button("Permissions", 44, card2, 12);
        permissions.setOnClickListener(v -> requestPhonePermissions());
        setupRow.addView(permissions, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button accessibility = button("Accessibility", 44, card2, 12);
        accessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams accessibilityParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        accessibilityParams.setMargins(dp(7), 0, 0, 0);
        setupRow.addView(accessibility, accessibilityParams);

        setupCard.addView(setupRow);
        space(setupCard, 7);

        LinearLayout assistantRow = new LinearLayout(this);
        assistantRow.setOrientation(LinearLayout.HORIZONTAL);

        Button assistant = button("Default assistant", 44, card2, 12);
        assistant.setOnClickListener(v ->requestAssistantRole());
        assistantRow.addView(assistant, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button settings = button("Buddy Settings", 44, card2, 12);
        settings.setOnClickListener(v -> startActivity(new Intent(this, BuddySettingsActivity.class)));
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        settingsParams.setMargins(dp(7), 0, 0, 0);
        assistantRow.addView(settings, settingsParams);

        setupCard.addView(assistantRow);
        content.addView(setupCard);
        space(content, 15);

        LinearLayout tips = new LinearLayout(this);
        tips.setOrientation(LinearLayout.VERTICAL);
        tips.setPadding(dp(16), dp(14), dp(16), dp(14));
        tips.setBackground(rounded(card, line, 20));
        tips.addView(text("TRY SAYING", 11, muted, true));
        TextView examples = text(
                "“Hey Buddy” → “YouTube kholo”\n" +
                "“Hey Buddy search JEE physics”\n" +
                "“Hey Buddy volume 60”\n" +
                "“Hey Buddy brightness 40”",
                14, white, false);
        examples.setPadding(0, dp(7), 0, 0);
        tips.addView(examples);
        content.addView(tips);
        space(content, 15);

        TextView footer = text("Single voice engine • local-first commands • honest action status", 12, muted, false);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer);

        setContentView(root);
        renderState(BuddyVoiceService.STATE_IDLE);
    }

    private void addQuick(LinearLayout row, String label, String command, int background) {
        Button b = button(label, 46, background, 12);
        b.setOnClickListener(v -> sendCommand(command));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(46), 1);
        p.setMargins(dp(3), 0, dp(3), 0);
        row.addView(b, p);
    }

    private String accessStatus() {
        return BuddyAccessibilityService.isEnabled()
                ? "✓ Phone control enabled"
                : "○ Enable Accessibility for Back, Home, scroll, tap and typing";
    }

    private void renderState(String state) {
        if (state == null) state = BuddyVoiceService.STATE_IDLE;

        final int white = Color.WHITE;
        final int muted = Color.rgb(157, 168, 194);
        final int cyan = Color.rgb(89, 207, 255);
        final int green = Color.rgb(82, 220, 151);

        if (BuddyVoiceService.STATE_WAITING_WAKE.equals(state)) {
            stateText.setText("● Waiting for “Hey Buddy”…");
            stateText.setTextColor(cyan);
            wakeText.setText("Wake mode armed. Normal speech is ignored.");
            wakeText.setTextColor(muted);
            talkButton.setText("🎙  TAP TO TALK");
        } else if (BuddyVoiceService.STATE_LISTENING_COMMAND.equals(state)) {
            stateText.setText("● Listening…");
            stateText.setTextColor(cyan);
            wakeText.setText("Command mode active");
            talkButton.setText("●  LISTENING");
        } else if (BuddyVoiceService.STATE_SPEAKING.equals(state)) {
            stateText.setText("● Buddy is speaking");
            stateText.setTextColor(green);
            wakeText.setText("You can wait for the reply, then say “Hey Buddy” again.");
            talkButton.setText("🎙  TAP TO TALK");
        } else if (BuddyVoiceService.STATE_ERROR.equals(state)) {
            stateText.setText("● Voice needs attention");
            stateText.setTextColor(Color.rgb(255, 120, 120));
            wakeText.setText("Check microphone permission and speech recognition.");
            talkButton.setText("🎙  TAP TO TALK");
        } else {
            stateText.setText("● Ready");
            stateText.setTextColor(cyan);
            wakeText.setText(wakeSwitch != null && wakeSwitch.isChecked()
                    ? "Assistant mode is enabled. Microphone stays off until an assistant interaction starts."
                    : "Tap to talk. Enable assistant mode for system assistant access.");
            talkButton.setText("🎙  TAP TO TALK");
        }
    }

    private void startCommandListening() {
        ensureMicrophoneThenStart(false);
    }

    private void ensureMicrophoneThenStart(boolean wake) {\n        if (wake) {\n            prefs.edit().putBoolean(KEY_WAKE, true).apply();\n            renderState(BuddyVoiceService.STATE_WAITING_WAKE);\n            return;\n        }\n        if (Build.VERSION.SDK_INT >= 23\n                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {\n            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);\n            return;\n        }\n        startVoiceService(BuddyVoiceService.ACTION_TAP_COMMAND, null);\n    }\n\n    private void startVoiceService(String action, String command) {
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
        } catch (Throwable ignored) {
        }
    }

    private void sendCommand(String command) {
        if (command == null || command.trim().isEmpty()) return;
        getPreferences(MODE_PRIVATE).edit().putString("quick_command", command.trim()).apply();
        startVoiceService(BuddyVoiceService.ACTION_TAP_COMMAND, command.trim());
    }

    private void requestPhonePermissions() {
        ArrayList<String> missing = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.READ_CONTACTS);
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
            } catch (Throwable ignored) {
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (accessText != null) accessText.setText(accessStatus());

        boolean wake = prefs.getBoolean(KEY_WAKE, false);
        if (wake && wakeSwitch != null && !wakeSwitch.isChecked()) {
            wakeSwitch.setChecked(true);
        }
    }

    @Override public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_MIC) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            boolean pendingWake = false;\n\n            if (!granted) {
                if (wakeSwitch != null) {
                    wakeSwitch.setOnCheckedChangeListener(null);
                    wakeSwitch.setChecked(false);
                    wakeSwitch.setOnCheckedChangeListener((button, checked) -> {
                        prefs.edit().putBoolean(KEY_WAKE, checked).apply();
                        if (checked) ensureMicrophoneThenStart(true);
                        else {
                            stopVoiceService();
                            renderState(BuddyVoiceService.STATE_IDLE);
                        }
                    });
                }
                prefs.edit().putBoolean(KEY_WAKE, false).apply();
                renderState(BuddyVoiceService.STATE_ERROR);
                Toast.makeText(this, "Microphone permission allow karo.", Toast.LENGTH_SHORT).show();
                return;
            }

            startVoiceService(
                    pendingWake
                            ? BuddyVoiceService.ACTION_ENABLE_WAKE
                            : BuddyVoiceService.ACTION_TAP_COMMAND,
                    null);
            return;
        }

        if (requestCode == REQ_PHONE_PERMISSIONS) {
            Toast.makeText(this, "Permission setup updated.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onDestroy() {
        try {
            unregisterReceiver(statusReceiver);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }
}
