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
    private int airplaneRetryCount = 0;
    private String lastActionProcessed = "";

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

        if (!ACTION.equals(lastActionProcessed)) {
            airplaneRetryCount = 0;
            lastActionProcessed = ACTION;
        }

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
            case "airplane_on":
                handled = handleAirplaneMode(true);
                break;
            case "airplane_off":
                handled = handleAirplaneMode(false);
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

    private boolean handleAirplaneMode(boolean targetState) {
        airplaneRetryCount++;
        android.util.Log.d("AirplaneMode", "Attempting " + (targetState ? "ON" : "OFF") + " - Retry: " + airplaneRetryCount);

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        // Strategy 1: Find by Content Description (Highest priority, QS tiles)
        if (findAirplaneByContentDescription(root, targetState)) {
            return true;
        }

        // Strategy 2: Find by Text with strict WiFi exclusion
        if (findAirplaneByTextAndClickTile(root, targetState)) {
            return true;
        }

        // Strategy 3: Open Settings App (After 1st fail)
        if (airplaneRetryCount > 1) {
            return openSettingsAndToggleAirplane(targetState);
        }

        return false;
    }

    private boolean findAirplaneByContentDescription(AccessibilityNodeInfo root, boolean targetState) {
        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        collectAllNodes(root, allNodes);

        for (AccessibilityNodeInfo node : allNodes) {
            String desc = node.getContentDescription() != null ? node.getContentDescription().toString().toLowerCase() : "";
            String text = node.getText() != null ? node.getText().toString().toLowerCase() : "";
            
            // Check if it's Airplane AND NOT WiFi
            if ((desc.contains("airplane") || desc.contains("flight") || desc.contains("विमान")) && 
                !(desc.contains("wifi") || desc.contains("wi-fi") || desc.contains("wlan"))) {
                
                boolean currentState = desc.contains("on") || desc.contains("चालू") || (node.isCheckable() && node.isChecked());
                if (currentState == targetState) return true;

                if (clickNode(node)) return true;
                if (clickNode(findClickableAncestor(node))) return true;
            }
        }
        return false;
    }

    private boolean findAirplaneByTextAndClickTile(AccessibilityNodeInfo root, boolean targetState) {
        String[] searchTerms = {"airplane", "flight", "aeroplane", "विमान", "एयरप्लेन", "फ्लाइट"};
        
        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        collectAllNodes(root, allNodes);

        for (AccessibilityNodeInfo node : allNodes) {
            String text = getNodeTextFull(node).toLowerCase();
            
            // Must contain airplane keyword AND NOT WiFi
            boolean isAirplane = false;
            for (String term : searchTerms) {
                if (text.contains(term)) {
                    isAirplane = true;
                    break;
                }
            }
            
            boolean isWifi = text.contains("wifi") || text.contains("wi-fi") || text.contains("wlan");

            if (isAirplane && !isWifi) {
                if (isNodeCurrentlyOn(node) == targetState) return true;
                
                if (clickNode(node)) return true;
                if (clickNode(node.getParent())) return true;
                if (clickNode(findClickableAncestor(node))) return true;
            }
        }
        return false;
    }

    // ✅ STRATEGY 4: Open Settings App
    private boolean openSettingsAndToggleAirplane(boolean targetState) {
        if (airplaneRetryCount <= 2) return false; // Try this only after 2 retries

        try {
            // Open Airplane mode settings directly
            Intent intent = new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // Wait for settings to open, then try to toggle
            handler.postDelayed(() -> {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    // Find the switch in settings
                    String[] ids = {
                            "android:id/switch_widget",
                            "com.android.settings:id/switch_widget",
                            "com.android.settings:id/switch"
                    };

                    for (String id : ids) {
                        List<AccessibilityNodeInfo> switches = root.findAccessibilityNodeInfosByViewId(id);
                        if (switches != null && !switches.isEmpty()) {
                            for (AccessibilityNodeInfo sw : switches) {
                                if (sw.isCheckable()) {
                                    if (sw.isChecked() != targetState) {
                                        clickNode(sw);
                                    }
                                    ACTION = "";
                                    handler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 1000);
                                    return;
                                }
                            }
                        }
                    }

                    // Try by text as fallback
                    String[] qsTerms = {"Airplane mode", "Flight mode", "Aeroplane mode", "airplane", "flight"};
                    for (String term : qsTerms) {
                        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(term);
                        if (nodes != null && !nodes.isEmpty()) {
                            for (AccessibilityNodeInfo node : nodes) {
                                AccessibilityNodeInfo toggle = findToggleNearby(node);
                                if (toggle != null) {
                                    if (toggle.isCheckable() && toggle.isChecked() != targetState) {
                                        clickNode(toggle);
                                    }
                                    ACTION = "";
                                    handler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 1000);
                                    return;
                                }
                            }
                        }
                    }
                }
            }, 1500);

            return true; // Return true to stop retrying, we handled it
        } catch (Exception e) {
            android.util.Log.e("AirplaneMode", "Settings intent failed: " + e.getMessage());
            return false;
        }
    }

    // ✅ STRATEGY 5: Brute Force - Click all clickable tiles except WiFi
    private boolean bruteForceFindAirplaneTile(AccessibilityNodeInfo root, boolean targetState) {
        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        collectAllNodes(root, allNodes);

        String[] wifiKeywords = {"wifi", "wi-fi", "wlan", "वाईफाई"};

        // Find all clickable tiles
        List<AccessibilityNodeInfo> clickableTiles = new ArrayList<>();
        for (AccessibilityNodeInfo node : allNodes) {
            if (node.isClickable() && node.getChildCount() > 0 && node.getChildCount() < 10) {
                String text = getNodeTextFull(node);
                if (text == null) text = "";
                String lower = text.toLowerCase();

                boolean isWifi = false;
                for (String wifi : wifiKeywords) {
                    if (lower.contains(wifi)) {
                        isWifi = true;
                        break;
                    }
                }

                if (!isWifi) {
                    clickableTiles.add(node);
                }
            }
        }

        android.util.Log.d("AirplaneMode", "Found " + clickableTiles.size() + " clickable tiles");

        // Try each tile
        for (int i = 0; i < clickableTiles.size(); i++) {
            AccessibilityNodeInfo tile = clickableTiles.get(i);
            String text = getNodeTextFull(tile);

            android.util.Log.d("AirplaneMode", "Trying tile " + i + ": " + text);

            // Check if it might be airplane mode
            if (text != null) {
                String lower = text.toLowerCase();
                if (lower.contains("airplane") || lower.contains("flight") ||
                        lower.contains("aeroplane") || lower.contains("विमान")) {
                    clickNode(tile);
                    return true;
                }
            }
        }

        return false;
    }

    // ✅ Helper: Collect all nodes
    private void collectAllNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> list) {
        if (node == null) return;
        list.add(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            collectAllNodes(node.getChild(i), list);
        }
    }

    // ✅ Helper: Get full text from node and ancestors
    private String getNodeTextFull(AccessibilityNodeInfo node) {
        if (node == null) return null;

        StringBuilder sb = new StringBuilder();

        if (node.getText() != null) sb.append(node.getText()).append(" ");
        if (node.getContentDescription() != null) sb.append(node.getContentDescription()).append(" ");

        // Check parent for context
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            if (parent.getText() != null) sb.append(parent.getText()).append(" ");
            if (parent.getContentDescription() != null) sb.append(parent.getContentDescription()).append(" ");
        }

        return sb.toString().trim();
    }

    // ✅ Helper: Check if node is currently ON
    private boolean isNodeCurrentlyOn(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // Check content description
        String desc = node.getContentDescription() != null ?
                node.getContentDescription().toString().toLowerCase() : "";
        if (desc.contains("on") || desc.contains("चालू") || desc.contains("active")) {
            return true;
        }
        if (desc.contains("off") || desc.contains("बंद") || desc.contains("inactive")) {
            return false;
        }

        // Check if node itself is checkable
        if (node.isCheckable()) return node.isChecked();

        // Check parent
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null && parent.isCheckable()) return parent.isChecked();

        return false;
    }

    // ✅ Helper: Find clickable ancestor
    private AccessibilityNodeInfo findClickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        int depth = 0;
        while (current != null && depth < 6) {
            if (current.isClickable()) return current;
            current = current.getParent();
            depth++;
        }
        return null;
    }

    // ✅ Helper: Click node with multiple methods
    private boolean clickNode(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // Method 1: ACTION_CLICK
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }

        // Method 2: ACTION_SELECT
        if (node.performAction(AccessibilityNodeInfo.ACTION_SELECT)) {
            return true;
        }

        // Method 3: Focus then click
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }

        return false;
    }

    // ===== EXISTING METHODS =====

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
                    if (key.contains("airplane") || key.contains("flight")) {
                        String nodeText = (node.getText() != null) ? node.getText().toString().toLowerCase() : "";
                        if (nodeText.contains("wi-fi") || nodeText.contains("wifi")) continue;
                    }

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

                boolean lookingForAirplane = false;
                for (String key : keywords) {
                    if (key.toLowerCase().contains("airplane") || key.toLowerCase().contains("flight")) {
                        lookingForAirplane = true;
                        break;
                    }
                }

                if (lookingForAirplane && (text.contains("wi-fi") || text.contains("wifi") || text.contains("wlan"))) {
                    return false;
                }

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