package com.akash.myapplication;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class SmsReceive extends BroadcastReceiver {
    public static Ringtone ring;

    @SuppressLint({"UnsafeProtectedBroadcastReceiver", "NewApi"})
    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sp = context.getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        String code = sp.getString("Security", "");
        Set<String> masterNumbers = sp.getStringSet("MasterNumbers", new HashSet<>());

        Bundle b = intent.getExtras();
        if (b == null) return;

        Object[] pdu = (Object[]) b.get("pdus");
        String format = b.getString("format");
        if (pdu == null) return;

        for (Object pdus : pdu) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdus, format);
            String msg = sms.getMessageBody();
            String sender = sms.getOriginatingAddress();

            if (msg == null) continue;

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
                continue;
            }

            String fullMsg = msg.trim().toLowerCase();
            String lowerCode = code.trim().toLowerCase();

            if (fullMsg.equals(lowerCode + " call me") || fullMsg.equals(lowerCode + "call me")) {
                abortBroadcast();
                try {
                    Intent call = new Intent(Intent.ACTION_CALL);
                    call.setData(Uri.parse("tel:" + sender));
                    call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(call);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            else if (fullMsg.contains(lowerCode + " ring") || fullMsg.contains(lowerCode + "ring")) {
                abortBroadcast();
                try {
                    if (ring != null && ring.isPlaying()) ring.stop();

                    Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    ring = RingtoneManager.getRingtone(context, ringtoneUri);

                    if (ring != null) {
                        ring.setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build());
                        ring.play();

                        try {
                            android.telephony.SmsManager smsManager = context.getSystemService(android.telephony.SmsManager.class);
                            smsManager.sendTextMessage(sender, null, "Security Guard: Mobile is ringing!", null, null);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            else if (fullMsg.contains(lowerCode + " camera back") || fullMsg.contains(lowerCode + "camera back")) {
                abortBroadcast();
                Intent i = new Intent(context, CameraService.class);
                i.putExtra("sender", sender);
                i.putExtra("which", "back");
                context.startForegroundService(i);
            }
            else if (fullMsg.contains(lowerCode + " camera front") || fullMsg.contains(lowerCode + "camera front")) {
                abortBroadcast();
                Intent i = new Intent(context, CameraService.class);
                i.putExtra("sender", sender);
                i.putExtra("which", "front");
                context.startForegroundService(i);
            }

            else if (fullMsg.equals(lowerCode + " location") || fullMsg.equals(lowerCode + "location")) {
                abortBroadcast();
                Intent i = new Intent(context, LocationService.class);
                i.putExtra("sender", sender);
                context.startService(i);
            }

            else if (fullMsg.contains(lowerCode + " data on") || fullMsg.contains(lowerCode + "data on")) {
                abortBroadcast();
                ToggleAccessibilityService.ACTION = "data";
                try {
                    Intent dataIntent = new Intent();
                    dataIntent.setComponent(new android.content.ComponentName("com.android.settings", "com.android.settings.Settings$MobileNetworkSettingsActivity"));
                    dataIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(dataIntent);
                } catch (Exception e) {
                    context.startActivity(new Intent("android.settings.WIRELESS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            }

            else if (fullMsg.contains(lowerCode + " location on") || fullMsg.contains(lowerCode + "location on")) {
                abortBroadcast();
                ToggleAccessibilityService.ACTION = "location";
                context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }

            else if (fullMsg.contains(lowerCode + " airplane on") || fullMsg.contains(lowerCode + "airplane on")) {
                abortBroadcast();
                ToggleAccessibilityService.ACTION = "airplane_on";
                context.startActivity(new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }

            else if (fullMsg.contains(lowerCode + " airplane off") || fullMsg.contains(lowerCode + "airplane off")) {
                abortBroadcast();
                ToggleAccessibilityService.ACTION = "airplane_off";
                context.startActivity(new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }
    }
}
