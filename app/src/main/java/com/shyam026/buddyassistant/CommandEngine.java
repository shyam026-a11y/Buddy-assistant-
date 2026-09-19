package com.shyam026.buddyassistant;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.KeyEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CommandEngine {
    public interface Callback { void onResult(String message); }

    private static final String PREF = "buddy_prefs";
    private static final String AI_KEY = "openrouter_key";

    private CommandEngine() {}

    public static void execute(Context context, String raw, Callback callback) {
        Context c = context.getApplicationContext();
        String s = CommandRouter.normalize(raw);
        String wakeRemainder = CommandRouter.removeWakePhrase(s);
        if (wakeRemainder != null) s = CommandRouter.normalize(wakeRemainder);
        if (s.isEmpty()) {
            done(c, callback, "Bolo, command clear nahi mila.");
            return;
        }

        try {
            if (has(s, "hey buddy", "hello buddy", "hi buddy", "namaste buddy")) {
                done(c, callback, "Haan, main yahin hoon.");
                return;
            }

            if (has(s, "time batao", "what time", "kitne baje", "time kya", "current time")) {
                done(c, callback, new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
                return;
            }

            if (has(s, "date batao", "aaj ki date", "today date", "what date", "today")) {
                done(c, callback, new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date()));
                return;
            }

            if (has(s, "battery", "charge kitna", "battery kitni")) {
                done(c, callback, battery(c));
                return;
            }

            String remember = CommandRouter.memoryText(raw);
            if (remember != null) {
                BuddyMemory.remember(c, remember);
                done(c, callback, "Yaad rakh liya.");
                return;
            }

            if (has(s, "what did you remember", "last memory", "maine kya yaad karaya", "yaad kya hai")) {
                String memory = BuddyMemory.getLastMemory(c);
                done(c, callback, memory.isEmpty() ? "Abhi kuch saved nahi hai." : memory);
                return;
            }

            if (has(s, "forget that", "forget memory", "sab bhool jao", "memory clear")) {
                BuddyMemory.clear(c);
                done(c, callback, "Saved memory clear kar di.");
                return;
            }

            if (has(s, "last notification", "latest notification", "notification padh", "notification batao")) {
                String notification = BuddyNotificationService.getLastNotification();
                done(c, callback, notification.isEmpty()
                        ? "Koi recent notification available nahi hai."
                        : notification);
                BuddyNotificationService.clearLastNotification();
                return;
            }

            String yt = CommandRouter.youtubeQuery(s);
            if (yt != null && !yt.isEmpty()) {
                Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(yt)));
                i.setPackage("com.google.android.youtube");
                boolean opened = launch(c, i);
                if (!opened) {
                    opened = launch(c, new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(yt))));
                }
                done(c, callback, opened
                        ? "YouTube par " + yt + " search kar raha hoon."
                        : "YouTube search open nahi hua.");
                return;
            }

            CommandRouter.WhatsAppRequest wa = CommandRouter.parseWhatsApp(s);
            if (wa != null) {
                String number = findContactNumber(c, wa.contact);
                if (number == null) {
                    done(c, callback, "Contact nahi mila: " + wa.contact);
                    return;
                }
                String phone = formatWhatsAppNumber(number);
                Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("whatsapp://send?phone=" + phone + "&text=" + Uri.encode(wa.message)));
                i.setPackage("com.whatsapp");
                boolean opened = launch(c, i);
                if (!opened) {
                    opened = launch(c, new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/" + phone + "?text=" + Uri.encode(wa.message))));
                }
                done(c, callback, opened
                        ? "WhatsApp message ready hai. Send tum confirm karna."
                        : "WhatsApp open nahi hua.");
                return;
            }

            if (has(s, "back jao", "go back", "back")) {
                global(c, "back", callback);
                return;
            }
            if (has(s, "home screen", "go home", "home jao", "home")) {
                global(c, "home", callback);
                return;
            }
            if (has(s, "recent apps", "recents")) {
                global(c, "recents", callback);
                return;
            }
            if (has(s, "notifications kholo", "notification panel", "notifications")) {
                global(c, "notifications", callback);
                return;
            }
            if (has(s, "quick settings")) {
                global(c, "quick_settings", callback);
                return;
            }
            if (has(s, "screenshot", "screen shot", "screen capture")) {
                global(c, "screenshot", callback);
                return;
            }
            if (has(s, "lock phone", "lock screen", "phone lock")) {
                global(c, "lock", callback);
                return;
            }

            if (has(s, "scroll down", "neeche scroll", "scroll neeche")) {
                done(c, callback, accessScroll(true) ? "Neeche scroll kar diya." : "Scroll nahi ho paya.");
                return;
            }
            if (has(s, "scroll up", "upar scroll", "scroll upar")) {
                done(c, callback, accessScroll(false) ? "Upar scroll kar diya." : "Scroll nahi ho paya.");
                return;
            }

            String tap = after(s, "tap ", "click ");
            if (tap != null && !tap.isEmpty()) {
                boolean r = BuddyAccessibilityService.isEnabled()
                        && BuddyAccessibilityService.get().clickText(tap);
                done(c, callback, r ? "“" + tap + "” click kar diya." : "“" + tap + "” nahi mila.");
                return;
            }

            String type = after(s, "type ", "likho ");
            if (type != null && !type.isEmpty()) {
                boolean r = BuddyAccessibilityService.isEnabled()
                        && BuddyAccessibilityService.get().typeText(type);
                done(c, callback, r ? "Text enter kar diya." : "Editable field nahi mila.");
                return;
            }

            if (has(s, "volume", "mute", "silent")) {
                doVolume(c, s, callback);
                return;
            }

            if (has(s, "brightness", "roshni", "screen bright")) {
                doBrightness(c, s, callback);
                return;
            }

            if (has(s, "flashlight", "torch")) {
                toggleFlash(c, callback);
                return;
            }

            if (has(s, "wifi on", "wifi chalu", "wifi off", "wifi band", "wi fi")) {
                openWirelessSettings(c, Settings.ACTION_WIFI_SETTINGS, "Wi-Fi", callback);
                return;
            }

            if (has(s, "bluetooth on", "bluetooth chalu", "bluetooth off", "bluetooth band", "bluetooth")) {
                openWirelessSettings(c, Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth", callback);
                return;
            }

            if (has(s, "settings khol", "open settings", "settings")) {
                openSettings(c, callback);
                return;
            }

            if (has(s, "camera kholo", "open camera", "camera open")) {
                boolean ok = launch(c, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
                done(c, callback, ok ? "Camera khol diya." : "Camera open nahi hua.");
                return;
            }

            if (has(s, "set timer", "timer lagao", "timer")) {
                Integer seconds = CommandRouter.extractNumber(s);
                if (seconds == null) {
                    done(c, callback, "Timer duration batao, jaise 10 minutes.");
                } else {
                    Intent i = new Intent(AlarmClock.ACTION_SET_TIMER)
                            .putExtra(AlarmClock.EXTRA_LENGTH, Math.min(86400, seconds * 60))
                            .putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                    done(c, callback, launch(c, i) ? "Timer screen ready hai." : "Timer open nahi hua.");
                }
                return;
            }

            if (has(s, "set alarm", "alarm lagao", "alarm")) {
                Integer hour = CommandRouter.extractNumber(s);
                if (hour == null || hour > 23) {
                    done(c, callback, "Alarm ka time batao, jaise 7.");
                } else {
                    Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
                            .putExtra(AlarmClock.EXTRA_HOUR, hour)
                            .putExtra(AlarmClock.EXTRA_MINUTES, 0)
                            .putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                    done(c, callback, launch(c, i) ? "Alarm screen ready hai." : "Alarm open nahi hua.");
                }
                return;
            }

            if (has(s, "next song", "next track", "agla gana")) {
                media(c, KeyEvent.KEYCODE_MEDIA_NEXT);
                done(c, callback, "Next track.");
                return;
            }
            if (has(s, "previous song", "previous track", "pichla gana")) {
                media(c, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                done(c, callback, "Previous track.");
                return;
            }
            if (has(s, "pause music", "music pause")) {
                media(c, KeyEvent.KEYCODE_MEDIA_PAUSE);
                done(c, callback, "Music pause.");
                return;
            }
            if (has(s, "play music", "music chala", "music play", "resume music")) {
                media(c, KeyEvent.KEYCODE_MEDIA_PLAY);
                done(c, callback, "Music play.");
                return;
            }

            CommandRouter.CallRequest call = CommandRouter.parseCall(s);
            if (call != null) {
                doCall(c, call.target, callback);
                return;
            }

            CommandRouter.SmsRequest sms = CommandRouter.parseSms(s);
            if (sms != null) {
                doSms(c, sms.target, sms.message, callback);
                return;
            }

            if (has(s, "calendar event", "calendar mein", "calendar me", "create event")) {
                Intent i = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.TITLE, "Buddy event");
                done(c, callback, launch(c, i) ? "Calendar event screen ready hai." : "Calendar open nahi hua.");
                return;
            }

            if (has(s, "open", "khol", "launch", "start", "chala")) {
                String[][] apps = {
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
                for (String[] a : apps) {
                    if (s.contains(a[0])) {
                        Intent i = c.getPackageManager().getLaunchIntentForPackage(a[1]);
                        boolean ok = i != null && launch(c, i);
                        done(c, callback, ok ? cap(a[0]) + " khol diya." : cap(a[0]) + " app nahi mila.");
                        return;
                    }
                }
            }

            String q = CommandRouter.googleSearchQuery(s);
            if (q != null && !q.isEmpty()) {
                boolean ok = launch(c, new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))));
                done(c, callback, ok ? "Google par search kar raha hoon." : "Search open nahi hua.");
                return;
            }

            cloud(c, raw, callback);
        } catch (Throwable t) {
            done(c, callback, "Command fail hua. Dobara bolo.");
        }
    }

    private static boolean has(String s, String... values) {
        for (String value : values) if (s.contains(value)) return true;
        return false;
    }

    private static String after(String s, String... prefixes) {
        for (String prefix : prefixes) if (s.startsWith(prefix)) return s.substring(prefix.length()).trim();
        return null;
    }

    private static String cap(String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private static void done(Context c, Callback cb, String msg) {
        if (cb == null) return;
        new Handler(Looper.getMainLooper()).post(() -> cb.onResult(msg));
    }

    private static void global(Context c, String action, Callback cb) {
        BuddyAccessibilityService service = BuddyAccessibilityService.get();
        if (service == null) {
            done(c, cb, "Phone control ke liye Accessibility enable karo.");
            return;
        }
        boolean ok = service.global(action);
        done(c, cb, ok ? actionText(action) + " done." : actionText(action) + " nahi hua.");
    }

    private static String actionText(String action) {
        switch (action) {
            case "quick_settings": return "Quick settings";
            case "notifications": return "Notifications";
            case "recents": return "Recent apps";
            case "screenshot": return "Screenshot";
            case "lock": return "Lock";
            default: return "Back";
        }
    }

    private static boolean accessScroll(boolean down) {
        return BuddyAccessibilityService.isEnabled() && BuddyAccessibilityService.get().scroll(down);
    }

    private static void doVolume(Context c, String s, Callback cb) {
        AudioManager a = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (a == null) {
            done(c, cb, "Volume service unavailable.");
            return;
        }
        Integer p = CommandRouter.extractNumber(s);
        if (has(s, "mute", "silent")) {
            a.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
            done(c, cb, "Volume mute.");
            return;
        }
        if (p != null) {
            int max = a.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int v = Math.round(max * Math.max(0, Math.min(100, p)) / 100f);
            a.setStreamVolume(AudioManager.STREAM_MUSIC, v, AudioManager.FLAG_SHOW_UI);
            done(c, cb, "Volume " + p + " percent.");
            return;
        }
        boolean down = has(s, "kam", "down", "decrease", "lower");
        a.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                down ? AudioManager.ADJUST_LOWER : AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI);
        done(c, cb, down ? "Volume kam kar diya." : "Volume badha diya.");
    }

    private static void doBrightness(Context c, String s, Callback cb) {
        Integer p = CommandRouter.extractNumber(s);
        try {
            if (!Settings.System.canWrite(c)) {
                launch(c, new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + c.getPackageName())));
                done(c, cb, "Brightness permission allow karo.");
                return;
            }
            if (p != null) {
                int value = Math.max(1, Math.min(100, p)) * 255 / 100;
                Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
                done(c, cb, "Brightness " + p + " percent.");
                return;
            }
            int cur = Settings.System.getInt(c.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 128);
            int delta = has(s, "kam", "down", "decrease") ? -32 : 32;
            int value = Math.max(1, Math.min(255, cur + delta));
            Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
            done(c, cb, delta < 0 ? "Brightness kam kar diya." : "Brightness badha diya.");
        } catch (Throwable t) {
            done(c, cb, "Brightness control nahi hua.");
        }
    }

    private static void toggleFlash(Context c, Callback cb) {
        try {
            android.hardware.camera2.CameraManager cm =
                    (android.hardware.camera2.CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
            if (cm == null) {
                done(c, cb, "Torch unavailable.");
                return;
            }
            for (String id : cm.getCameraIdList()) {
                android.hardware.camera2.CameraCharacteristics ch = cm.getCameraCharacteristics(id);
                Integer facing = ch.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                Boolean flash = ch.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Integer.valueOf(android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK).equals(facing)
                        && Boolean.TRUE.equals(flash)) {
                    boolean now = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("flash", false);
                    cm.setTorchMode(id, !now);
                    c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("flash", !now).apply();
                    done(c, cb, !now ? "Torch on." : "Torch off.");
                    return;
                }
            }
        } catch (Throwable ignored) {}
        done(c, cb, "Torch control available nahi hai.");
    }

    private static void openWirelessSettings(Context c, String action, String name, Callback cb) {
        boolean toggling = false;
        // Modern Android versions intentionally limit silent Wi-Fi/Bluetooth toggles for ordinary apps.
        if (BuddyAccessibilityService.isEnabled()) {
            BuddyAccessibilityService service = BuddyAccessibilityService.get();
            service.global("quick_settings");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                boolean clicked = service.clickText(name);
                done(c, cb, clicked ? name + " setting change kar di." : name + " quick setting nahi mili.");
            }, 450L);
            toggling = true;
        }
        if (!toggling) {
            boolean opened = launch(c, new Intent(action));
            done(c, cb, opened ? name + " settings open kar di." : name + " settings open nahi hui.");
        }
    }

    private static void openSettings(Context c, Callback cb) {
        done(c, cb, launch(c, new Intent(Settings.ACTION_SETTINGS))
                ? "Settings khol diya." : "Settings open nahi hua.");
    }

    private static void media(Context c, int keyCode) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }

    private static void doCall(Context c, String target, Callback cb) {
        String number = target == null ? "" : target.replaceAll("[^0-9+]", "");
        if (number.isEmpty()) number = findContactNumber(c, target);
        if (number == null || number.isEmpty()) {
            done(c, cb, "Contact/number nahi mila.");
            return;
        }
        // Safer default: open the dialer, leaving final call initiation to the user.
        Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)));
        done(c, cb, launch(c, i) ? "Dialer ready hai. Call button tum confirm karna." : "Dialer open nahi hua.");
    }

    private static void doSms(Context c, String target, String message, Callback cb) {
        String number = target == null ? "" : target.replaceAll("[^0-9+]", "");
        if (number.isEmpty()) number = findContactNumber(c, target);
        if (number == null || number.isEmpty()) {
            done(c, cb, "SMS recipient nahi mila.");
            return;
        }
        Intent i = new Intent(Intent.ACTION_SENDTO,
                Uri.parse("smsto:" + Uri.encode(number)));
        i.putExtra("sms_body", message);
        done(c, cb, launch(c, i) ? "SMS composer ready hai. Send tum confirm karna." : "SMS composer open nahi hua.");
    }

    private static String findContactNumber(Context c, String wanted) {
        Cursor cur = null;
        try {
            String q = wanted == null ? "" : wanted.replace("%", "\\%");
            cur = c.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? ESCAPE '\\'",
                    new String[]{"%" + q + "%"},
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY + " DESC");
            if (cur != null && cur.moveToFirst()) return cur.getString(0);
        } catch (Throwable ignored) {
        } finally {
            if (cur != null) cur.close();
        }
        return null;
    }

    private static String formatWhatsAppNumber(String raw) {
        String n = raw.replaceAll("[^0-9]", "");
        if (n.length() == 10) n = "91" + n;
        return n;
    }

    private static boolean launch(Context c, Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String battery(Context c) {
        Intent i = c.registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return "Battery status nahi mila.";
        int level = i.getIntExtra("level", -1);
        int scale = i.getIntExtra("scale", 100);
        return level >= 0 ? "Battery " + Math.round(level * 100f / scale) + " percent hai." : "Battery status nahi mila.";
    }

    private static void cloud(Context c, String raw, Callback cb) {
        String key = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(AI_KEY, "").trim();
        if (key.isEmpty()) {
            done(c, cb, "AI key add nahi hai.");
            return;
        }
        new Thread(() -> {
            HttpURLConnection h = null;
            try {
                JSONObject body = new JSONObject()
                        .put("model", "openrouter/free")
                        .put("temperature", 0.2)
                        .put("max_tokens", 80);
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content",
                        "You are Buddy, a friendly Indian voice assistant. " +
                        "Reply naturally in concise Hinglish or English. Maximum 18 words. " +
                        "Never claim a device action happened unless a local tool reported success."));
                messages.put(new JSONObject().put("role", "user").put("content", raw));
                body.put("messages", messages);

                h = (HttpURLConnection) new URL("https://openrouter.ai/api/v1/chat/completions").openConnection();
                h.setRequestMethod("POST");
                h.setConnectTimeout(1200);
                h.setReadTimeout(3000);
                h.setDoOutput(true);
                h.setRequestProperty("Authorization", "Bearer " + key);
                h.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = h.getOutputStream()) {
                    out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = h.getResponseCode();
                if (code < 200 || code >= 300) {
                    done(c, cb, "AI response nahi aa raha.");
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(h.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                String answer = new JSONObject(sb.toString())
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .optString("content", "")
                        .replace("\\n", " ")
                        .trim();

                done(c, cb, answer.isEmpty() ? "Mujhe samajh nahi aaya." : answer);
            } catch (Throwable t) {
                done(c, cb, "AI slow hai. Local command try karo.");
            } finally {
                if (h != null) h.disconnect();
            }
        }, "buddy-ai").start();
    }
}
