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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
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
    private TextView hintText;
    private TextView userText;
    private TextView buddyText;
    private TextView wakeText;
    private ImageView orb;

    private ObjectAnimator pulse;
    private String language = "en-IN";

    private final BroadcastReceiver statusReceiver =
            new BroadcastReceiver() {
                @Override public void onReceive(
                        Context context, Intent intent) {
                    if (!BuddyVoiceService.ACTION_STATUS.equals(
                            intent.getAction())) return;

                    String state = intent.getStringExtra(
                            BuddyVoiceService.EXTRA_STATE);
                    String transcript = intent.getStringExtra(
                            BuddyVoiceService.EXTRA_TRANSCRIPT);
                    String reply = intent.getStringExtra(
                            BuddyVoiceService.EXTRA_REPLY);

                    if (transcript != null
                            && !transcript.trim().isEmpty()) {
                        userText.setText(transcript);
                    }

                    if (reply != null
                            && !reply.trim().isEmpty()) {
                        buddyText.setText(reply);
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
        language = prefs.getString(KEY_LANG, "en-IN");

        getWindow().setStatusBarColor(Color.rgb(7, 9, 16));
        getWindow().setNavigationBarColor(Color.rgb(7, 9, 16));

        buildUi();

        IntentFilter filter = new IntentFilter(
                BuddyVoiceService.ACTION_STATUS);

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
                value * getResources()
                        .getDisplayMetrics().density);
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
        t.setGravity(Gravity.CENTER);
        t.setTypeface(
                null,
                bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private void buildUi() {
        final int bg = Color.rgb(7, 9, 16);
        final int white = Color.WHITE;
        final int muted = Color.rgb(151, 162, 188);
        final int cyan = Color.rgb(92, 208, 255);
        final int green = Color.rgb(82, 220, 151);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(18), dp(12), dp(16), 0);

        TextView brand = text("BUDDY", 20, white, true);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(
                brand,
                new LinearLayout.LayoutParams(
                        0, dp(46), 1));

        ImageView settings = new ImageView(this);
        settings.setImageResource(
                android.R.drawable.ic_menu_preferences);
        settings.setPadding(dp(11), dp(11), dp(11), dp(11));
        settings.setBackground(
                gradient(
                        Color.rgb(21, 28, 44),
                        Color.rgb(29, 35, 55),
                        18));
        settings.setContentDescription(
                "Open Buddy Settings");
        settings.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                this,
                                BuddySettingsActivity.class)));

        top.addView(
                settings,
                new LinearLayout.LayoutParams(
                        dp(48), dp(48)));

        root.addView(top);

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        center.setPadding(
                dp(20), dp(10), dp(20), dp(10));

        center.addView(
                new Space(this),
                new LinearLayout.LayoutParams(
                        1, 0, 1));

        TextView title = text(
                "How can I help?",
                27, white, true);
        center.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1, dp(46)));

        hintText = text(
                "Tap the circle and speak",
                13, muted, false);
        center.addView(
                hintText,
                new LinearLayout.LayoutParams(
                        -1, dp(34)));

        orb = new ImageView(this);
        orb.setImageResource(R.drawable.buddy_logo);
        orb.setPadding(dp(25), dp(25), dp(25), dp(25));
        orb.setBackground(
                gradient(
                        Color.rgb(82, 59, 224),
                        Color.rgb(43, 188, 230),
                        200));
        orb.setElevation(dp(14));
        orb.setContentDescription(
                "Talk to Buddy");

        orb.setOnClickListener(v -> {
            if (BuddyVoiceService.STATE_LISTENING_COMMAND
                    .equals(currentState)) {
                stopVoiceService();
                renderState(
                        prefs.getBoolean(
                                KEY_WAKE, false)
                                ? BuddyVoiceService.STATE_WAITING_WAKE
                                : BuddyVoiceService.STATE_IDLE);
            } else {
                startCommandListening();
            }
        });

        LinearLayout.LayoutParams orbParams =
                new LinearLayout.LayoutParams(
                        dp(190), dp(190));
        orbParams.setMargins(0, dp(15), 0, dp(16));
        center.addView(orb, orbParams);

        stateText = text(
                "Ready",
                19, white, true);
        center.addView(
                stateText,
                new LinearLayout.LayoutParams(
                        -1, dp(36)));

        wakeText = text(
                "Hey Buddy is off",
                12, muted, false);
        center.addView(
                wakeText,
                new LinearLayout.LayoutParams(
                        -1, dp(32)));

        center.addView(
                new Space(this),
                new LinearLayout.LayoutParams(
                        1, 0, 1));

        root.addView(
                center,
                new LinearLayout.LayoutParams(
                        -1, 0, 1));

        LinearLayout transcript = new LinearLayout(this);
        transcript.setOrientation(LinearLayout.VERTICAL);
        transcript.setPadding(
                dp(20), dp(8), dp(20), dp(14));

        userText = text(
                "",
                13, muted, false);
        userText.setGravity(Gravity.START);
        transcript.addView(userText);

        buddyText = text(
                "",
                14, white, false);
        buddyText.setGravity(Gravity.START);
        buddyText.setPadding(
                0, dp(5), 0, 0);
        transcript.addView(buddyText);

        root.addView(
                transcript,
                new LinearLayout.LayoutParams(
                        -1, dp(64)));

        setContentView(root);

        currentState = BuddyVoiceService.STATE_IDLE;
        renderState(currentState);

        if (prefs.getBoolean(KEY_WAKE, false)) {
            startVoiceService(
                    BuddyVoiceService.ACTION_ENABLE_WAKE,
                    null);
        }

        pulse = ObjectAnimator.ofFloat(
                orb, View.SCALE_X, 1.0f, 1.035f, 1.0f);
        pulse.setDuration(1750L);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setInterpolator(
                new AccelerateDecelerateInterpolator());
        pulse.start();
    }

    private String currentState =
            BuddyVoiceService.STATE_IDLE;

    private void renderState(String state) {
        currentState = state == null
                ? BuddyVoiceService.STATE_IDLE
                : state;

        if (stateText == null || hintText == null
                || wakeText == null) return;

        if (BuddyVoiceService.STATE_WAITING_WAKE.equals(
                currentState)) {
            stateText.setText("Listening for “Hey Buddy”");
            stateText.setTextColor(Color.rgb(92, 208, 255));
            hintText.setText(
                    "Say “Hey Buddy” anytime");
            wakeText.setText(
                    "Wake word ON • microphone listening");
            wakeText.setTextColor(
                    Color.rgb(82, 220, 151));
            return;
        }

        if (BuddyVoiceService.STATE_LISTENING_COMMAND.equals(
                currentState)) {
            stateText.setText("I'm listening…");
            stateText.setTextColor(
                    Color.rgb(92, 208, 255));
            hintText.setText(
                    "Speak your command");
            wakeText.setText(
                    "Tap the circle again to stop");
            return;
        }

        if (BuddyVoiceService.STATE_PROCESSING.equals(
                currentState)) {
            stateText.setText("On it…");
            stateText.setTextColor(
                    Color.rgb(92, 208, 255));
            hintText.setText(
                    "Processing your request");
            wakeText.setText(
                    "Local command or Gemini AI");
            return;
        }

        if (BuddyVoiceService.STATE_SPEAKING.equals(
                currentState)) {
            stateText.setText("Buddy is speaking");
            stateText.setTextColor(
                    Color.rgb(82, 220, 151));
            hintText.setText(
                    "Ready for the next request");
            wakeText.setText(
                    prefs.getBoolean(KEY_WAKE, false)
                            ? "Hey Buddy is ON"
                            : "Hey Buddy is OFF");
            return;
        }

        if (BuddyVoiceService.STATE_ERROR.equals(
                currentState)) {
            stateText.setText("Something needs attention");
            stateText.setTextColor(
                    Color.rgb(255, 119, 133));
            hintText.setText(
                    "Open Settings to fix permissions");
            wakeText.setText(
                    "Tap the circle to retry");
            return;
        }

        stateText.setText("Ready");
        stateText.setTextColor(Color.WHITE);
        hintText.setText("Tap the circle and speak");
        wakeText.setText(
                prefs.getBoolean(KEY_WAKE, false)
                        ? "Hey Buddy is ON"
                        : "Hey Buddy is OFF");
        wakeText.setTextColor(
                prefs.getBoolean(KEY_WAKE, false)
                        ? Color.rgb(82, 220, 151)
                        : Color.rgb(151, 162, 188));
    }

    private void startCommandListening() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO},
                    REQ_MIC);
            return;
        }

        startVoiceService(
                BuddyVoiceService.ACTION_TAP_COMMAND,
                null);
    }

    private void startVoiceService(
            String action, String command) {
        try {
            Intent i = new Intent(
                    this,
                    BuddyVoiceService.class);
            i.setAction(action);
            i.setPackage(getPackageName());

            if (command != null) {
                i.putExtra(
                        BuddyVoiceService.EXTRA_COMMAND,
                        command);
            }

            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        } catch (Throwable t) {
            renderState(
                    BuddyVoiceService.STATE_ERROR);
            Toast.makeText(
                    this,
                    "Buddy voice service start nahi hua.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVoiceService() {
        try {
            startVoiceService(
                    BuddyVoiceService.ACTION_STOP,
                    null);
        } catch (Throwable ignored) {
            try {
                stopService(
                        new Intent(
                                this,
                                BuddyVoiceService.class));
            } catch (Throwable ignoredAgain) {}
        }
    }

    private void requestPhonePermissions() {
        java.util.ArrayList<String> missing =
                new java.util.ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(
                    Manifest.permission.RECORD_AUDIO);
        }

        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(
                        Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(
                    Manifest.permission.READ_CONTACTS);
        }

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(
                    Manifest.permission.POST_NOTIFICATIONS);
        }

        if (missing.isEmpty()) {
            Toast.makeText(
                    this,
                    "All required permissions are already granted.",
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
                RoleManager rm =
                        getSystemService(RoleManager.class);

                if (rm != null
                        && rm.isRoleAvailable(
                                RoleManager.ROLE_ASSISTANT)) {
                    if (rm.isRoleHeld(
                            RoleManager.ROLE_ASSISTANT)) {
                        Toast.makeText(
                                this,
                                "Buddy is already the default assistant.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        startActivityForResult(
                                rm.createRequestRoleIntent(
                                        RoleManager.ROLE_ASSISTANT),
                                REQ_ASSISTANT_ROLE);
                    }
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
            startActivity(
                    new Intent(
                            Settings.ACTION_VOICE_INPUT_SETTINGS));
        } catch (Throwable ignored) {
            startActivity(
                    new Intent(
                            Settings.ACTION_SETTINGS));
        }
    }

    @Override protected void onResume() {
        super.onResume();

        if (prefs == null) return;

        boolean wake = prefs.getBoolean(
                KEY_WAKE, false);

        if (wake
                && !BuddyVoiceService.STATE_WAITING_WAKE.equals(
                        currentState)
                && !BuddyVoiceService.STATE_LISTENING_COMMAND.equals(
                        currentState)
                && !BuddyVoiceService.STATE_PROCESSING.equals(
                        currentState)
                && !BuddyVoiceService.STATE_SPEAKING.equals(
                        currentState)) {
            startVoiceService(
                    BuddyVoiceService.ACTION_ENABLE_WAKE,
                    null);
        }

        renderState(currentState);
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

            if (granted) {
                startCommandListening();
            } else {
                renderState(
                        BuddyVoiceService.STATE_ERROR);
                Toast.makeText(
                        this,
                        "Microphone permission allow karo.",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (requestCode == REQ_PHONE_PERMISSIONS) {
            Toast.makeText(
                    this,
                    "Permission setup updated.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onDestroy() {
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }

        try {
            unregisterReceiver(statusReceiver);
        } catch (Throwable ignored) {}

        super.onDestroy();
    }
}
