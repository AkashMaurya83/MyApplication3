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
                    smartToggle("mobile data"); // finding in small letter (mobile data)
                    ACTION = "";
                    new Handler().postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 1000);
                }
            }

            else if (ACTION.equals("location")) {
                if (event.getClassName().toString().contains("Settings")) {
                    smartToggle("use location"); // finding in small letter ( location )
                    ACTION = "";
                    new Handler().postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 1000);
                }
            }
        }
    }

    private void smartToggle(String textToFind) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            findAndSmartClick(rootNode, textToFind);
        }
    }

    private boolean findAndSmartClick(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;

        // if text is match
        if (node.getText() != null && node.getText().toString().toLowerCase().contains(text)) {

            // find toggle
            AccessibilityNodeInfo toggleNode = findToggleNearby(node);

            if (toggleNode != null) {
                // checking Is toggle is already on ?
                if (toggleNode.isChecked()) {
                    return true; // if already on then do nothing
                } else {
                    // if toggle is off then click to on
                    toggleNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }

        // search on whole screen
        for (int i = 0; i < node.getChildCount(); i++) {
            if (findAndSmartClick(node.getChild(i), text)) {
                return true;
            }
        }
        return false;
    }

    // toggle (Sibling Search)
    private AccessibilityNodeInfo findToggleNearby(AccessibilityNodeInfo textNode) {
        // is toggle
        if (isToggle(textNode)) return textNode;

        // 2. इसके Parent (पूरी लाइन वाला बॉक्स) को देखो
        AccessibilityNodeInfo parent = textNode.getParent();
        if (parent != null) {

            // क्या Parent खुद Switch है?
            if (isToggle(parent)) return parent;

            // 3. Parent के अंदर बच्चों (Siblings) में ढूंढो
            // (क्योंकि बाएं तरफ Text होता है, दाएं तरफ Switch होता है)
            for (int i = 0; i < parent.getChildCount(); i++) {
                AccessibilityNodeInfo child = parent.getChild(i);
                if (child != null && isToggle(child)) {
                    return child;
                }
            }

            // 4. अगर नहीं मिला, तो Grandparent (उससे ऊपर वाले बॉक्स) में ढूंढो
            AccessibilityNodeInfo grandParent = parent.getParent();
            if (grandParent != null) {
                if (isToggle(grandParent)) return grandParent;
                for (int i = 0; i < grandParent.getChildCount(); i++) {
                    AccessibilityNodeInfo child = grandParent.getChild(i);
                    if (child != null && isToggle(child)) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    // ये check करेगा कि ये Switch है या CheckBox
    private boolean isToggle(AccessibilityNodeInfo node) {
        if (node == null) return false;
        CharSequence className = node.getClassName();
        if (className != null) {
            String cn = className.toString().toLowerCase();
            return cn.contains("switch") || cn.contains("checkbox");
        }
        return false;
    }

    @Override
    public void onInterrupt() {}
}