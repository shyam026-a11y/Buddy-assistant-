package com.shyam026.buddyassistant;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

public class BuddyAccessibilityService extends AccessibilityService {
    private static BuddyAccessibilityService instance;

    public static boolean isEnabled(){ return instance != null; }
    public static BuddyAccessibilityService get(){ return instance; }

    @Override public void onServiceConnected(){
        super.onServiceConnected();
        instance=this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){}

    public boolean global(String action){
        if("back".equals(action)) return performGlobalAction(GLOBAL_ACTION_BACK);
        if("home".equals(action)) return performGlobalAction(GLOBAL_ACTION_HOME);
        if("recents".equals(action)) return performGlobalAction(GLOBAL_ACTION_RECENTS);
        if("notifications".equals(action)) return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        if("quick_settings".equals(action)) return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
        if("screenshot".equals(action) && Build.VERSION.SDK_INT>=30) return performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        if("lock".equals(action) && Build.VERSION.SDK_INT>=28) return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        if("power".equals(action) && Build.VERSION.SDK_INT>=21) return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
        if("all_apps".equals(action) && Build.VERSION.SDK_INT>=31) return performGlobalAction(GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS);
        return false;
    }

    public boolean scroll(boolean down){
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null)return false;
        return scrollNode(root,down);
    }

    private boolean scrollNode(AccessibilityNodeInfo n, boolean down){
        if(n==null)return false;
        if(n.isScrollable()){
            return n.performAction(down
                    ?AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    :AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
        for(int i=0;i<n.getChildCount();i++){
            if(scrollNode(n.getChild(i),down)) return true;
        }
        return false;
    }

    public boolean clickText(String text){
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null)return false;
        String wanted=normalizeText(text);
        if(wanted.isEmpty())return false;
        AccessibilityNodeInfo n=findClickable(root,wanted);
        return n!=null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private AccessibilityNodeInfo findClickable(AccessibilityNodeInfo n,String wanted){
        if(n==null)return null;
        String text=normalizeText(n.getText());
        String desc=normalizeText(n.getContentDescription());
        if((text.contains(wanted)||desc.contains(wanted)) && n.isClickable()) return n;
        for(int i=0;i<n.getChildCount();i++){
            AccessibilityNodeInfo found=findClickable(n.getChild(i),wanted);
            if(found!=null)return found;
        }
        return null;
    }

    public boolean typeText(String text){
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null)return false;

        AccessibilityNodeInfo focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if(focused!=null && focused.isEditable()){
            return setText(focused,text);
        }

        AccessibilityNodeInfo editable=findEditable(root);
        return editable!=null && setText(editable,text);
    }

    private boolean setText(AccessibilityNodeInfo node,String text){
        Bundle args=new Bundle();
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text==null?"":text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo n){
        if(n==null)return null;
        if(n.isEditable() && n.isVisibleToUser())return n;
        for(int i=0;i<n.getChildCount();i++){
            AccessibilityNodeInfo x=findEditable(n.getChild(i));
            if(x!=null)return x;
        }
        return null;
    }

    public boolean tap(float x,float y){
        if(Build.VERSION.SDK_INT<24)return false;
        Path p=new Path();
        p.moveTo(x,y);
        GestureDescription g=new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p,0,80))
                .build();
        return dispatchGesture(g,null,null);
    }

    private static String normalizeText(CharSequence value){
        return value==null?"":value.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim();
    }

    @Override public void onDestroy(){
        if(instance==this)instance=null;
        super.onDestroy();
    }
}
