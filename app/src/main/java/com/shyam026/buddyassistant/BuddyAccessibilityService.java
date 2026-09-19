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
    private static volatile BuddyAccessibilityService instance;

    public static boolean isEnabled() { return instance != null; }
    public static BuddyAccessibilityService get() { return instance; }

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}

    public boolean global(String action) {
        if ("back".equals(action)) return performGlobalAction(GLOBAL_ACTION_BACK);
        if ("home".equals(action)) return performGlobalAction(GLOBAL_ACTION_HOME);
        if ("recents".equals(action)) return performGlobalAction(GLOBAL_ACTION_RECENTS);
        if ("notifications".equals(action)) return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        if ("quick_settings".equals(action)) return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
        if ("screenshot".equals(action) && Build.VERSION.SDK_INT >= 30) return performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        if ("lock".equals(action) && Build.VERSION.SDK_INT >= 28) return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        if ("power".equals(action) && Build.VERSION.SDK_INT >= 21) return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
        if ("all_apps".equals(action) && Build.VERSION.SDK_INT >= 31) return performGlobalAction(GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS);
        return false;
    }

    public boolean scroll(boolean down) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        return root != null && scrollNode(root, down);
    }

    private boolean scrollNode(AccessibilityNodeInfo node, boolean down) {
        if (node == null) return false;
        if (node.isScrollable()) {
            return node.performAction(down
                    ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = node.getChild(i);
            if (scrollNode(found, down)) return true;
        }
        return false;
    }

    public boolean clickText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        String wanted = normalize(text);
        if (wanted.isEmpty()) return false;
        AccessibilityNodeInfo exact = findClickable(root, wanted, true);
        if (exact != null) return exact.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        AccessibilityNodeInfo contains = findClickable(root, wanted, false);
        return contains != null && contains.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private AccessibilityNodeInfo findClickable(AccessibilityNodeInfo node, String wanted, boolean exact) {
        if (node == null) return null;
        String text = normalize(node.getText());
        String desc = normalize(node.getContentDescription());
        boolean match = exact
                ? wanted.equals(text) || wanted.equals(desc)
                : text.contains(wanted) || desc.contains(wanted);
        if (match && node.isClickable() && node.isVisibleToUser()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findClickable(node.getChild(i), wanted, exact);
            if (found != null) return found;
        }
        return null;
    }

    public boolean typeText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null && focused.isEditable()) return setText(focused, text);
        AccessibilityNodeInfo editable = findEditable(root);
        return editable != null && setText(editable, text);
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable() && node.isVisibleToUser()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findEditable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private boolean setText(AccessibilityNodeInfo node, String text) {
        Bundle args = new Bundle();
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text == null ? "" : text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    public boolean tap(float x, float y) {
        if (Build.VERSION.SDK_INT < 24) return false;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 70))
                .build();
        return dispatchGesture(gesture, null, null);
    }

    private static String normalize(CharSequence value) {
        return value == null ? "" : value.toString()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
