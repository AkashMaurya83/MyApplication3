package com.akash.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class ToggleAccessibilityService extends AccessibilityService {

    public static String ACTION = "";
    public static boolean allowPowerMenu = false;
    public static ToggleAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            detectPowerMenuAndBlock();
        }

        if (!ACTION.isEmpty()) {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(this::processAction, 600);
            }
        }
    }

    private void detectPowerMenuAndBlock() {
        if (allowPowerMenu) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        String[] powerKeywords = {"Power off", "Restart", "Reboot", "Switch off", "बंद करें", "पुनरारंभ करें"};

        for (String key : powerKeywords) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(key);
            if (nodes != null && !nodes.isEmpty()) {
                performGlobalAction(GLOBAL_ACTION_BACK);

                Intent loginIntent = new Intent(this, LoginActivity.class);
                loginIntent.putExtra("reason", "power_block");
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(loginIntent);
                return;
            }
        }
    }

    public void showPowerMenu() {
        allowPowerMenu = true;
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
        handler.postDelayed(() -> allowPowerMenu = false, 10000);
    }

    private void processAction() {
        if (ACTION.isEmpty()) return;

        String currentAction = ACTION;
        boolean handled = false;

        switch (currentAction) {
            case "data":
                handled = processToggleByText(new String[]{"mobile data", "मोबाइल डेटा", "मोबाइल डाटा", "cell data", "cellular data"}, true);
                break;
            case "location":
                handled = processToggleByText(new String[]{"use location", "location", "लोकेशन", "स्थान"}, true);
                if (!handled) handled = processFirstSwitch(true);
                break;
        }

        if (handled) {
            ACTION = "";
            handler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 1500);
        } else {
            handler.postDelayed(() -> {
                if (!ACTION.isEmpty()) processAction();
            }, 1000);
        }
    }

    private boolean processToggleByText(String[] keywords, boolean targetState) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        String[] commonIds = {"android:id/switch_widget", "com.android.settings:id/switch_widget", "android:id/checkbox"};
        for (String id : commonIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo toggle : nodes) {
                    if (isToggle(toggle)) {
                        if (isSwitchMatchingKeywords(toggle, keywords)) {
                            if (toggle.isChecked() == targetState) return true;
                            forceClick(toggle);
                            return true;
                        }
                    }
                }
            }
        }

        for (String key : keywords) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(key);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    AccessibilityNodeInfo toggle = findToggleNearby(node);
                    if (toggle != null) {
                        if (toggle.isChecked() == targetState) return true;
                        forceClick(toggle);
                        return true;
                    } else if (node.isClickable() && node.getText() != null && node.getText().toString().toLowerCase().contains(key.toLowerCase())) {
                        forceClick(node);
                        return true;
                    }
                }
            }

            AccessibilityNodeInfo descMatch = findByContentDescription(root, key);
            if (descMatch != null) {
                AccessibilityNodeInfo toggle = findToggleNearby(descMatch);
                if (toggle != null) {
                    if (toggle.isChecked() == targetState) return true;
                    forceClick(toggle);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSwitchMatchingKeywords(AccessibilityNodeInfo switchNode, String[] keywords) {
        AccessibilityNodeInfo parent = switchNode.getParent();
        if (parent == null) return false;

        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child != null && child.getText() != null) {
                String text = child.getText().toString().toLowerCase();

                for (String key : keywords) {
                    if (text.contains(key.toLowerCase())) return true;
                }
            }
        }
        return false;
    }

    private AccessibilityNodeInfo findByContentDescription(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        if (node.getContentDescription() != null && node.getContentDescription().toString().toLowerCase().contains(text.toLowerCase())) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findByContentDescription(node.getChild(i), text);
            if (found != null) return found;
        }
        return null;
    }

    private boolean processFirstSwitch(boolean targetState) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        AccessibilityNodeInfo toggle = findFirstToggle(root);
        if (toggle != null) {
            if (toggle.isChecked() == targetState) return true;
            forceClick(toggle);
            return true;
        }
        return false;
    }

    private AccessibilityNodeInfo findFirstToggle(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (isToggle(node)) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findFirstToggle(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private AccessibilityNodeInfo findToggleNearby(AccessibilityNodeInfo node) {
        if (isToggle(node)) return node;
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            AccessibilityNodeInfo found = findFirstToggle(parent);
            if (found != null) return found;

            AccessibilityNodeInfo grandParent = parent.getParent();
            if (grandParent != null) {
                found = findFirstToggle(grandParent);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean isToggle(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isCheckable()) return true;

        CharSequence className = node.getClassName();
        if (className != null) {
            String cn = className.toString().toLowerCase();
            return cn.contains("switch") || cn.contains("checkbox") || cn.contains("toggle") || cn.contains("check");
        }
        return false;
    }

    private void forceClick(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            node.performAction(AccessibilityNodeInfo.ACTION_SELECT);

            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                if (!parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_SELECT);

                    AccessibilityNodeInfo grandParent = parent.getParent();
                    if (grandParent != null) {
                        if (!grandParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            grandParent.performAction(AccessibilityNodeInfo.ACTION_SELECT);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
