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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREF = "buddy_prefs";
    private static final String KEY_WAKE = "wake_mode";
    private static final String KEY_LANG = "language";

    private static final int REQ_MIC = 700;
    private static final int REQ_PHONE_PERMISSIONS = 701;
    private static final int REQ_ASSISTANT_ROLE = 702;

    private SharedPreferences prefs;
    private TextView stateText;
    private TextView transcriptText;
    private TextView replyText;
    private TextView hintText;
    private Switch wakeSwitch;
    private ImageView orb;

    private ObjectAnimator pulseX;
    private ObjectAnimator pulseY;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!BuddyVoiceService.ACTION_STATUS.equals(intent.getAction())) return;

            String state = intent.getStringExtra(BuddyVoiceService.EXTRA_STATE);
            String transcript = intent.getStringExtra(BuddyVoiceService.EXTRA_TRANSCRIPT);
            String reply = intent.getStringExtra(BuddyVoiceService.EXTRA_REPLY);

            if (transcript != null && !transcript.trim().isEmpty()) {
                transcriptText.setText(transcript);
            }
            if (reply != null && !reply.trim().isEmpty()) {
                replyText.setText(reply);
            }

            renderState(state == null
                    ? BuddyVoiceService.STATE_IDLE
                    : state);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);

        getWindow().setStatusBarColor(Color.rgb(7, 9, 16));
        getWindow().setNavigationBarColor(Color.rgb(7, 9, 16));

        buildUi();

        IntentFilter filter =
                new IntentFilter(BuddyVoiceService.ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    statusReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
    }

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
        t.setTypeface(null,
                bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private void buildUi() {
        final int bg = Color.rgb(7, 9, 16);
        final int card = Color.rgb(14, 18, 29);
        final int line = Color.rgb(42, 51, 75);
        final int white = Color.WHITE;
        final int muted = Color.rgb(150, 163, 190);
        final int cyan = Color.rgb(90, 209, 255);
        final int green = Color.rgb(82, 220, 151);
        final int accent = Color.rgb(111, 91, 255);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(bg);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16), dp(14), dp(16), dp(8));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(10), 0, 0, 0);
        brand.addView(text("BUDDY", 20, white, true));
        TextView sub = text(
                "voice assistant", 11, muted, false);
        brand.addView(sub);
        top.addView(brand, new LinearLayout.LayoutParams(
                0, -2, 1));

        Button settings = new Button(this);
        settings.setText("⚙");
        settings.setTextSize(19);
        settings.setTextColor(white);
        settings.setAllCaps(false);
        settings.setMinHeight(0);
        settings.setMinWidth(0);
        settings.setPadding(0, 0, 0, 0);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(rounded(
                Color.rgb(22, 28, 45), line, 16));
        settings.setContentDescription(
                "Open Buddy Settings");
        settings.setOnClickListener(v -> startActivity(
                new Intent(this, BuddySettingsActivity.class)));
        top.addView(settings, new LinearLayout.LayoutParams(
                dp(48), dp(48)));

        root.addView(top, new LinearLayout.LayoutParams(
                -1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(20), dp(8), dp(20), dp(28));
        scroll.addView(body);

        TextView status = text(
                "●  READY", 12, cyan, true);
        status.setLetterSpacing(0.1f);
        body.addView(status, new LinearLayout.LayoutParams(
                -2, -2));

        orb = new ImageView(this);
        orb.setImageResource(R.drawable.buddy_logo);
        orb.setPadding(dp(24), dp(24), dp(24), dp(24));
        orb.setBackground(gradient(
                Color.rgb(68, 52, 194),
                Color.rgb(44, 184, 226), 180));

        LinearLayout.LayoutParams orbP =
                new LinearLayout.LayoutParams(
                        dp(210), dp(210));
        orbP.setMargins(0, dp(28), 0, dp(22));
        orb.setContentDescription("Tap to talk to Buddy");
        orb.setOnClickListener(v -> ensureMicrophoneThenStart(false));
        body.addView(orb, orbP);

        stateText = text(
                "Ready", 27, white, true);
        stateText.setGravity(Gravity.CENTER);
        body.addView(stateText);

        hintText = text(
                "Tap the circle and speak.", 14, muted, false);
        hintText.setGravity(Gravity.CENTER);
        hintText.setPadding(0, dp(6), 0, 0);
        body.addView(hintText);

        LinearLayout conversation = new LinearLayout(this);
        conversation.setOrientation(LinearLayout.VERTICAL);
        conversation.setPadding(dp(16), dp(14), dp(16), dp(14));
        conversation.setBackground(rounded(card, line, 22));
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(26), 0, 0);
        body.addView(conversation, cp);

        TextView youLabel = text(
                "YOU", 10, muted, true);
        youLabel.setLetterSpacing(0.08f);
        conversation.addView(youLabel);

        transcriptText = text(
                "—", 14, white, false);
        transcriptText.setPadding(
                0, dp(4), 0, dp(12));
        conversation.addView(transcriptText);

        TextView buddyLabel = text(
                "BUDDY", 10, muted, true);
        buddyLabel.setLetterSpacing(0.08f);
        conversation.addView(buddyLabel);

        replyText = text(
                "Bolo. Main sun rahi hoon.", 15, white, true);
        replyText.setPadding(0, dp(4), 0, 0);
        conversation.addView(replyText);

        LinearLayout wake = new LinearLayout(this);
        wake.setGravity(Gravity.CENTER_VERTICAL);
        wake.setPadding(dp(15), dp(10), dp(10), dp(10));
        wake.setBackground(rounded(card, line, 20));
        LinearLayout.LayoutParams wp =
                new LinearLayout.LayoutParams(-1, -2);
        wp.setMargins(0, dp(12), 0, 0);
        body.addView(wake, wp);

        LinearLayout wakeCopy = new LinearLayout(this);
        wakeCopy.setOrientation(LinearLayout.VERTICAL);
        wakeCopy.addView(text(
                "Hey Buddy wake word", 14, white, true));
        wakeCopy.addView(text(
                "Listen for “Hey Buddy” while this mode is on.",
                12, muted, false));
        wake.addView(wakeCopy,
                new LinearLayout.LayoutParams(0, -2, 1));

        wakeSwitch = new Switch(this);
        wakeSwitch.setChecked(
                prefs.getBoolean(KEY_WAKE, false));
        wakeSwitch.setOnCheckedChangeListener(
                (button, checked) -> {
                    prefs.edit()
                            .putBoolean(KEY_WAKE, checked)
                            .apply();

                    if (checked) {
                        ensureMicrophoneThenStart(true);
                    } else {
                        stopVoiceService();
                        renderState(
                                BuddyVoiceService.STATE_IDLE);
                    }
                });
        wake.addView(wakeSwitch);

        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1));

        setContentView(root);
        renderState(BuddyVoiceService.STATE_IDLE);
        startOrbAnimation();

        // Keep the chosen language without adding visual clutter.
        if (!prefs.contains(KEY_LANG)) {
            prefs.edit().putString(KEY_LANG, "en-IN").apply();
        }
    }

    private void startOrbAnimation() {
        stopOrbAnimation();
        if (orb == null) return;

        pulseX = ObjectAnimator.ofFloat(
                orb, View.SCALE_X, 1.0f, 1.035f, 1.0f);
        pulseY = ObjectAnimator.ofFloat(
                orb, View.SCALE_Y, 1.0f, 1.035f, 1.0f);

        for (ObjectAnimator a :
                new ObjectAnimator[]{pulseX, pulseY}) {
            a.setDuration(1800L);
            a.setRepeatCount(ObjectAnimator.INFINITE);
            a.setInterpolator(
                    new AccelerateDecelerateInterpolator());
            a.start();
        }
    }

    private void stopOrbAnimation() {
        if (pulseX != null) pulseX.cancel();
        if (pulseY != null) pulseY.cancel();
        pulseX = null;
        pulseY = null;
    }

    private void renderState(String state) {
        if (stateText == null) return;

        final int white = Color.WHITE;
        final int muted = Color.rgb(150, 163, 190);
        final int cyan = Color.rgb(90, 209, 255);
        final int green = Color.rgb(82, 220, 151);
        final int danger = Color.rgb(255, 113, 131);

        if (BuddyVoiceService.STATE_WAITING_WAKE.equals(state)) {
            stateText.setText("Waiting for “Hey Buddy”");
            stateText.setTextColor(cyan);
            hintText.setText(
                    "Wake listening is active.");
            if (orb != null) {
                orb.setBackground(gradient(
                        Color.rgb(50, 43, 150),
                        Color.rgb(33, 144, 180), 180));
            }
        } else if (BuddyVoiceService.STATE_LISTENING_COMMAND.equals(state)) {
            stateText.setText("Listening…");
            stateText.setTextColor(cyan);
            hintText.setText("Speak your command now.");
        } else if (BuddyVoiceService.STATE_PROCESSING.equals(state)) {
            stateText.setText("Working…");
            stateText.setTextColor(cyan);
            hintText.setText("Executing your request.");
        } else if (BuddyVoiceService.STATE_SPEAKING.equals(state)) {
            stateText.setText("Buddy is speaking");
            stateText.setTextColor(green);
            hintText.setText("I'm listening again after this reply.");
        } else if (BuddyVoiceService.STATE_ERROR.equals(state)) {
            stateText.setText("Something needs attention");
            stateText.setTextColor(danger);
            hintText.setText(
                    "Check microphone permission and voice recognition.");
        } else {
            stateText.setText("Ready");
            stateText.setTextColor(white);
            hintText.setText(
                    wakeSwitch != null && wakeSwitch.isChecked()
                            ? "Wake word mode is on."
                            : "Tap the circle and speak.");
            if (orb != null) {
                orb.setBackground(gradient(
                        Color.rgb(68, 52, 194),
                        Color.rgb(44, 184, 226), 180));
            }
        }
    }

    private void ensureMicrophoneThenStart(boolean wake) {
        if (wake) {
            if (Build.VERSION.SDK_INT >= 23
                    && checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQ_MIC);
                return;
            }

            startVoiceService(
                    BuddyVoiceService.ACTION_ENABLE_WAKE, null);
            return;
        }

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_MIC);
            return;
        }

        startVoiceService(
                BuddyVoiceService.ACTION_TAP_COMMAND, null);
    }

    private void startVoiceService(
            String action, String command) {
        try {
            Intent i = new Intent(
                    this, BuddyVoiceService.class);
            i.setAction(action);
            i.setPackage(getPackageName());
            if (command != null) {
                i.putExtra(
                        BuddyVoiceService.EXTRA_COMMAND, command);
            }

            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Throwable t) {
            Toast.makeText(
                    this,
                    "Buddy voice service start nahi hua.",
                    Toast.LENGTH_SHORT).show();
            renderState(BuddyVoiceService.STATE_ERROR);
        }
    }

    private void stopVoiceService() {
        try {
            stopService(new Intent(
                    this, BuddyVoiceService.class));
        } catch (Throwable ignored) {}
    }

    private void requestPhonePermissions() {
        java.util.ArrayList<String> missing =
                new java.util.ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.READ_CONTACTS);
        }

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (missing.isEmpty()) {
            Toast.makeText(
                    this,
                    "Required permissions already granted.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        requestPermissions(
                missing.toArray(new String[0]),
                REQ_PHONE_PERMISSIONS);
    }

    private void requestAssistantRole() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager roleManager =
                        getSystemService(RoleManager.class);
                if (roleManager != null
                        && roleManager.isRoleAvailable(
                        RoleManager.ROLE_ASSISTANT)
                        && !roleManager.isRoleHeld(
                        RoleManager.ROLE_ASSISTANT)) {
                    startActivityForResult(
                            roleManager.createRequestRoleIntent(
                                    RoleManager.ROLE_ASSISTANT),
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
            startActivity(new Intent(
                    Settings.ACTION_VOICE_INPUT_SETTINGS));
        } catch (Throwable t) {
            try {
                startActivity(new Intent(
                        Settings.ACTION_SETTINGS));
            } catch (Throwable ignored) {}
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (wakeSwitch != null) {
            boolean wake = prefs.getBoolean(
                    KEY_WAKE, false);
            if (wakeSwitch.isChecked() != wake) {
                wakeSwitch.setOnCheckedChangeListener(null);
                wakeSwitch.setChecked(wake);
                wakeSwitch.setOnCheckedChangeListener(
                        (button, checked) -> {
                            prefs.edit()
                                    .putBoolean(KEY_WAKE, checked)
                                    .apply();
                            if (checked) {
                                ensureMicrophoneThenStart(true);
                            } else {
                                stopVoiceService();
                                renderState(
                                        BuddyVoiceService.STATE_IDLE);
                            }
                        });
            }
        }
    }

    @Override public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == REQ_MIC) {
            boolean granted = grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED;

            if (!granted) {
                prefs.edit()
                        .putBoolean(KEY_WAKE, false)
                        .apply();

                if (wakeSwitch != null) {
                    wakeSwitch.setOnCheckedChangeListener(null);
                    wakeSwitch.setChecked(false);
                    wakeSwitch.setOnCheckedChangeListener(
                            (button, checked) -> {
                                prefs.edit()
                                        .putBoolean(KEY_WAKE, checked)
                                        .apply();
                                if (checked) {
                                    ensureMicrophoneThenStart(true);
                                } else {
                                    stopVoiceService();
                                    renderState(
                                            BuddyVoiceService.STATE_IDLE);
                                }
                            });
                }

                renderState(
                        BuddyVoiceService.STATE_ERROR);
                Toast.makeText(
                        this,
                        "Microphone permission allow karo.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (wakeSwitch != null
                    && wakeSwitch.isChecked()) {
                startVoiceService(
                        BuddyVoiceService.ACTION_ENABLE_WAKE, null);
            } else {
                startVoiceService(
                        BuddyVoiceService.ACTION_TAP_COMMAND, null);
            }
        } else if (requestCode == REQ_PHONE_PERMISSIONS) {
            Toast.makeText(
                    this,
                    "Permission setup updated.",
                    Toast.LENGTH_SHORT).show();
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
