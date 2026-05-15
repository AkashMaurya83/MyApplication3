package com.akash.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

@SuppressLint("AccessibilityPolicy")
public class MyAccessibilityService extends AccessibilityService {

    private long lastActionTime = 0;
    private static final long COOLDOWN = 1500; // 15 seconds cooldown

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Cooldown check: Taki lagatar clicks na hon
        if (System.currentTimeMillis() - lastActionTime < COOLDOWN) return;

        android.content.SharedPreferences sp = getSharedPreferences("MyPref", MODE_PRIVATE);
        long lastCmdTime = sp.getLong("LastCommandTime", 0);
        
        // Command window: Increase to 30 seconds to give settings time to load
        if (System.currentTimeMillis() - lastCmdTime > 10000) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // Ab ye screen par kisi bhi "OFF" switch ko dhundega aur "ON" kar dega
        findAndActivateToggles(rootNode, sp);

        rootNode.recycle();
    }

    private boolean findAndActivateToggles(AccessibilityNodeInfo node, android.content.SharedPreferences sp) {
        if (node == null) return false;

        // 1. Agar ye koi toggle/switch hai aur OFF hai, to click karo
        if (node.isCheckable() && !node.isChecked()) {
            if (node.isVisibleToUser()) {
                return attemptClick(node, sp);
            }
        }

        // 2. Kuch phones mein pura row clickable hota hai switch ki jagah
        // Isliye "OFF" ya "Location" text wale node ke parent ko bhi check karein
        CharSequence text = node.getText();
        if (text != null) {
            String t = text.toString().toLowerCase();
            if (t.contains("off") || t.contains("turn on") || t.contains("disabled")) {
                return attemptClick(node, sp);
            }
        }
        // MyAccessibilityService.java mein ye try karein
        if (node.getContentDescription() != null &&
                node.getContentDescription().toString().toLowerCase().contains("location")) {
            // Ye tab kaam karega jab node ka naam "Location" ho
            attemptClick(node, sp);
        }

        // 3. Sabhi bacho (children) ko scan karein
        for (int i = 0; i < node.getChildCount(); i++) {
            if (findAndActivateToggles(node.getChild(i), sp)) {
                return true;
            }
        }
        return false;
    }

    private boolean attemptClick(AccessibilityNodeInfo node, android.content.SharedPreferences sp) {
        lastActionTime = System.currentTimeMillis();
        
        // Faster response: 500ms delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (performClick(node)) {
                // Click successful, go home after 4 seconds
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    sp.edit().putLong("LastCommandTime", 0).apply();
                }, 400);
            }
        }, 500);
        
        return true;
    }

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                boolean res = performClick(parent);
                parent.recycle();
                return res;
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {}
}
