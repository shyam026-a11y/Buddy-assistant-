package com.shyam026.buddyassistant;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class BuddyNotificationService extends NotificationListenerService {
    private static volatile BuddyNotificationService instance;
    private static volatile String lastNotification = "";

    public static boolean isEnabled() { return instance != null; }

    public static String getLastNotification() {
        return lastNotification == null ? "" : lastNotification;
    }

    public static void clearLastNotification() {
        lastNotification = "";
    }

    @Override public void onListenerConnected() {
        instance = this;
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return;
        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (title == null && text == null) return;
        String value = (title == null ? "" : title.toString())
                + (text == null ? "" : ": " + text);
        lastNotification = value.trim();
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {}

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
