package com.akash.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MyAccessibilityService extends AccessibilityService {

    private long lastActionTime = 0;
    private static final long COOLDOWN = 15000; // 15 seconds cooldown

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Cooldown check: Taki lagatar clicks na hon
        if (System.currentTimeMillis() - lastActionTime < COOLDOWN) return;

        android.content.SharedPreferences sp = getSharedPreferences("MyPref", MODE_PRIVATE);
        long lastCmdTime = sp.getLong("LastCommandTime", 0);
        
        // Command window: Increase to 30 seconds to give settings time to load
        if (System.currentTimeMillis() - lastCmdTime > 30000) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // Ab ye screen par kisi bhi "OFF" switch ko dhundega aur "ON" kar dega
        findAndActivateToggles(rootNode, sp);

        rootNode.recycle();
    }

    private boolean findAndActivateToggles(AccessibilityNodeInfo node, android.content.SharedPreferences sp) {
        if (node == null) return false;

        // Check if this node is a switch or checkable AND is currently OFF
        if (node.isVisibleToUser() && (node.isCheckable() || "android.widget.Switch".equals(node.getClassName()))) {
            if (!node.isChecked()) {
                // If it's a switch/checkbox and it's not checked, click it
                return attemptClick(node, sp);
            }
        }

        // Check if any text on screen indicates an "Off" state
        CharSequence text = node.getText();
        if (text != null) {
            String t = text.toString().toLowerCase();
            if (t.equals("off") || t.equals("disabled") || t.contains("turn on")) {
                // If we find "Off" text, try to click a switch nearby or the text itself
                AccessibilityNodeInfo switchNode = findSwitchInRow(node);
                if (switchNode != null && !switchNode.isChecked()) {
                    return attemptClick(switchNode, sp);
                } else {
                    return attemptClick(node, sp);
                }
            }
        }

        // Recursively check all children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (findAndActivateToggles(child, sp)) {
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
                }, 4000);
            }
        }, 500);
        
        return true;
    }

    private AccessibilityNodeInfo findSwitchInRow(AccessibilityNodeInfo node) {
        // Same logic as before to find switch in parent or grandparent container
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                AccessibilityNodeInfo child = parent.getChild(i);
                if (child != null) {
                    if (child.isCheckable() || "android.widget.Switch".equals(child.getClassName())) {
                        return child;
                    }
                    child.recycle();
                }
            }
            AccessibilityNodeInfo grandParent = parent.getParent();
            if (grandParent != null) {
                for (int i = 0; i < grandParent.getChildCount(); i++) {
                    AccessibilityNodeInfo child = grandParent.getChild(i);
                    if (child != null) {
                        if (child.isCheckable() || "android.widget.Switch".equals(child.getClassName())) {
                            parent.recycle();
                            return child;
                        }
                        child.recycle();
                    }
                }
                grandParent.recycle();
            }
            parent.recycle();
        }
        return null;
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
