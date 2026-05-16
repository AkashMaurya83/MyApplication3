package com.akash.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class ToggleAccessibilityService extends AccessibilityService {

    public static String ACTION = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            if (ACTION.equals("data")) {
                if (event.getClassName().toString().contains("Settings")) {
                    ACTION = "";
                    new Handler().postDelayed(() -> {
                        processTextToggle(new String[]{"mobile data", "मोबाइल डेटा", "मोबाइल डाटा"});
                    }, 1000);
                }
            }

            else if (ACTION.equals("location")) {
                if (event.getClassName().toString().contains("Settings")) {
                    ACTION = "";
                    new Handler().postDelayed(() -> {
                        processFirstSwitchToggle();
                    }, 1200); // Thoda extra time diya location page ke liye
                }
            }
        }
    }

    // --- LOCATION WALA LOGIC ---
    private void processFirstSwitchToggle() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            AccessibilityNodeInfo toggleNode = findFirstSwitch(rootNode);

            if (toggleNode != null) {
                if (toggleNode.isChecked()) {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                } else {
                    // Aggressive Click call karo
                    forceClickToggle(toggleNode);
                    new Handler().postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 1500);
                }
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }
    }

    // Ye function switch, uske parent aur grandparent teeno par ek saath click karega
    private void forceClickToggle(AccessibilityNodeInfo node) {
        if (node == null) return;

        // 1. Switch par click
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        // 2. Parent par click (Kai MIUI/Realme phones mein yahi kaam karta hai)
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);

            // 3. GrandParent par click
            AccessibilityNodeInfo grandParent = parent.getParent();
            if (grandParent != null) {
                grandParent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private AccessibilityNodeInfo findFirstSwitch(AccessibilityNodeInfo node) {
        if (node == null) return null;

        if (isToggle(node)) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo foundNode = findFirstSwitch(node.getChild(i));
            if (foundNode != null) return foundNode;
        }
        return null;
    }


    // --- MOBILE DATA WALA LOGIC ---
    private void processTextToggle(String[] textsToFind) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            AccessibilityNodeInfo toggleNode = null;
            for (String text : textsToFind) {
                toggleNode = findToggleByText(rootNode, text);
                if (toggleNode != null) break;
            }

            if (toggleNode != null) {
                if (toggleNode.isChecked()) {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                } else {
                    forceClickToggle(toggleNode); // Data ke liye bhi aggressive click
                    new Handler().postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 1500);
                }
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        }
    }

    private AccessibilityNodeInfo findToggleByText(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;

        if (node.getText() != null && node.getText().toString().toLowerCase().contains(text.toLowerCase())) {
            AccessibilityNodeInfo toggleNode = findToggleNearby(node);
            if (toggleNode != null) return toggleNode;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo foundNode = findToggleByText(node.getChild(i), text);
            if (foundNode != null) return foundNode;
        }
        return null;
    }

    private AccessibilityNodeInfo findToggleNearby(AccessibilityNodeInfo textNode) {
        if (isToggle(textNode)) return textNode;

        AccessibilityNodeInfo parent = textNode.getParent();
        if (parent != null) {
            if (isToggle(parent)) return parent;
            for (int i = 0; i < parent.getChildCount(); i++) {
                AccessibilityNodeInfo child = parent.getChild(i);
                if (child != null && isToggle(child)) return child;
            }

            AccessibilityNodeInfo grandParent = parent.getParent();
            if (grandParent != null) {
                if (isToggle(grandParent)) return grandParent;
                for (int i = 0; i < grandParent.getChildCount(); i++) {
                    AccessibilityNodeInfo child = grandParent.getChild(i);
                    if (child != null && isToggle(child)) return child;
                }
            }
        }
        return null;
    }

    // Toggle dhundhne ka function (Switch, Checkbox aur ToggleButton sab ko catch karega)
    private boolean isToggle(AccessibilityNodeInfo node) {
        if (node == null) return false;
        CharSequence className = node.getClassName();
        if (className != null) {
            String cn = className.toString().toLowerCase();
            // "switch", "checkbox" aur "togglebutton" teeno check kiye gaye hain
            return cn.contains("switch") || cn.contains("checkbox") || cn.contains("togglebutton");
        }
        return false;
    }

    @Override
    public void onInterrupt() {}
}