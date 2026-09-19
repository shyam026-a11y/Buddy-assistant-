package com.shyam026.buddyassistant;

import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;

public final class BuddyActionVerifier {
    public interface Callback {
        void onResult(boolean success);
    }

    private BuddyActionVerifier() {}

    public static void verifyActivePackage(String expectedPackage, long timeoutMs, Callback callback) {
        if (callback == null) return;
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMs);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable poll = new Runnable() {
            @Override public void run() {
                BuddyAccessibilityService service = BuddyAccessibilityService.get();
                String active = null;
                if (service != null) {
                    AccessibilityNodeInfo root = service.getRootInActiveWindow();
                    if (root != null && root.getPackageName() != null) {
                        active = root.getPackageName().toString();
                    }
                    if (root != null) root.recycle();
                }

                if (expectedPackage != null && expectedPackage.equals(active)) {
                    callback.onResult(true);
                    return;
                }

                if (System.currentTimeMillis() >= deadline) {
                    callback.onResult(false);
                    return;
                }
                handler.postDelayed(this, 120);
            }
        };
        handler.post(poll);
    }
}
