package com.akash.myapplication;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;

import android.net.Uri;

import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;
public class SmsReceive extends BroadcastReceiver {
    public static Ringtone ring;

    @SuppressLint({"UnsafeProtectedBroadcastReceiver", "NewApi"})
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        String code = sp.getString("Security", "");
        java.util.Set<String> masterNumbers = sp.getStringSet("MasterNumbers", new java.util.HashSet<>());

        Bundle b = intent.getExtras();
        if (b != null) {
            Object[] pdu = (Object[]) b.get("pdus");
            String format = b.getString("format");
            if (pdu != null) {
                for (Object pdus : pdu) {
                    SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdus, format);
                    String msg = sms.getMessageBody();
                    String sender = sms.getOriginatingAddress();

                    if (msg == null) continue;
                    msg = msg.trim();

                    // Security: Only process if message is from an authorized Master Number
                    boolean isAuthorized = false;
                    if (sender != null) {
                        for (String masterNum : masterNumbers) {
                            if (sender.contains(masterNum) || masterNum.contains(sender)) {
                                isAuthorized = true;
                                break;
                            }
                        }
                    }

                    if (!isAuthorized && !masterNumbers.isEmpty()) {
                        continue; // Skip messages from unauthorized numbers
                    }

                    // 1. Call Me Command
                    if (msg.equalsIgnoreCase(code + " Call me")) {
                        try {
                            Intent call = new Intent(Intent.ACTION_CALL);
                            call.setData(Uri.parse("tel:" + sender));
                            call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(call);
                        } catch (Exception e) {
                            Toast.makeText(context, "Call failed", Toast.LENGTH_SHORT).show();
                        }
                    }


                    // 2. Ring Command
                    if (msg.equalsIgnoreCase(code + " Ring")) {
                        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                        ring = RingtoneManager.getRingtone(context, ringtoneUri);
                        if (ring != null) {
                            ring.play();
                            Toast.makeText(context, "Phone is ringing", Toast.LENGTH_SHORT).show();
                            try {
                                SmsManager smsManager = context.getSystemService(SmsManager.class);
                                smsManager.sendTextMessage(sender, null, "Mobile is ringing", null, null);
                            } catch (Exception e) {
                            }
                        }
                    }

                    // 3. Photo Command (Front Camera)

                    if (msg.trim().equalsIgnoreCase(code + "camera back")) {
                        Intent i = new Intent(context, CameraService.class);
                        abortBroadcast();
                        i.putExtra("sender", sender);
                        i.putExtra("which", "back");
                        context.startForegroundService(i);
                    }
                    // अगर "camera front" आया
                    else if (msg.trim().equalsIgnoreCase(code + "camera front")) {
                        abortBroadcast();
                        Intent i = new Intent(context, CameraService.class);
                        i.putExtra("sender", sender);
                        i.putExtra("which", "front"); // <--- बस ये "front" लिखा
                        context.startForegroundService(i);
                    }
                    // ... पहले का camera back/front वाला कोड ऊपर रहेगा ...

                    // 3. Mobile Data On (Accessibility से अपने आप ऑन होगा)
                    else if (msg.trim().equalsIgnoreCase("data on")) {
                        abortBroadcast();

                        // Accessibility service को बताओ कि "data" वाला button दबाना है
                        ToggleAccessibilityService.ACTION = "data";

                        try {
                            // Settings app के अंदर Mobile Network वाले पेज को ध्यान से खोलो
                            Intent dataIntent = new Intent();
                            dataIntent.setComponent(new android.content.ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings$MobileNetworkSettingsActivity"
                            ));
                            dataIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(dataIntent);
                        } catch (Exception e) {
                            // अगर ऊपर वाला नहीं खुला, तो ये खोलो (Backup)
                            Intent backupIntent = new Intent("android.settings.WIRELESS_SETTINGS");
                            backupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(backupIntent);
                        }
                    }

                    // 4. Location On (Accessibility से अपने आप ऑन होगा)
                    else if (msg.trim().equalsIgnoreCase("location on")) {
                        abortBroadcast();

                        // Accessibility service को बताओ कि "location" वाला button दबाना है
                        ToggleAccessibilityService.ACTION = "location";

                        // Location का Settings पेज खोलो
                        Intent locIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        locIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(locIntent);
                    }

                }
            }
        }
    }
    // Removing unused method to clean up
}
