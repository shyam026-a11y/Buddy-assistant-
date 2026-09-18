package com.shyam026.buddyassistant;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class BuddyNotificationService extends NotificationListenerService {
    private static BuddyNotificationService instance;
    public static boolean isEnabled(){return instance!=null;}
    public static String lastNotification="";
    @Override public void onListenerConnected(){instance=this;}
    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(sbn==null||sbn.getNotification()==null)return;
        CharSequence t=sbn.getNotification().extras.getCharSequence("android.text");
        if(t!=null)lastNotification=t.toString();
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn){}
    @Override public void onDestroy(){instance=null;super.onDestroy();}
}