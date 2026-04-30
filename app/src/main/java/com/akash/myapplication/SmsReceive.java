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

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
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
                            } catch (Exception e) {}
                        }
                    }

                    // 3. Photo Command (Front Camera)
                    if (msg.equalsIgnoreCase(code + " Photo")) {
                        sp.edit().putLong("LastCommandTime", System.currentTimeMillis()).apply();
                        Intent intentPic = new Intent(context, CameraService.class);
                        intentPic.putExtra("sender", sender);
                        intentPic.putExtra("back_camera", false);
                        context.startService(intentPic);
                        Toast.makeText(context, "Front Photo started", Toast.LENGTH_SHORT).show();
                    }

                    // 3b. Back Photo Command
                    if (msg.equalsIgnoreCase(code + " Photo Back")) {
                        sp.edit().putLong("LastCommandTime", System.currentTimeMillis()).apply();
                        Intent intentPic = new Intent(context, CameraService.class);
                        intentPic.putExtra("sender", sender);
                        intentPic.putExtra("back_camera", true);
                        context.startService(intentPic);
                        Toast.makeText(context, "Back Photo started", Toast.LENGTH_SHORT).show();
                    }

                    // 4. Location On Command (Accessibility)
                    if (msg.equalsIgnoreCase(code + " Location On")) {
                        sp.edit().putLong("LastCommandTime", System.currentTimeMillis()).apply();
                        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                        boolean gps_enabled = false;
                        try {
                            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                        } catch (Exception e) {}

                        if (!gps_enabled) {
                            // Focus on the specific toggle screen
                            Intent locIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            locIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            try {
                                context.startActivity(locIntent);
                            } catch (Exception e) {
                                Intent fallback = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                fallback.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(fallback);
                            }
                        } else {
                            Toast.makeText(context, "Location is already ON", Toast.LENGTH_SHORT).show();
                        }
                    }

                    // 5. Data On Command (Accessibility)
                    if (msg.equalsIgnoreCase(code + " Data on")) {
                        sp.edit().putLong("LastCommandTime", System.currentTimeMillis()).apply();
                        
                        // Action 1: Data Usage Settings (Requested by user)
                        Intent dataIntent = new Intent("android.settings.DATA_USAGE_SETTINGS");
                        dataIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        
                        try {
                            context.startActivity(dataIntent);
                        } catch (Exception e) {
                            try {
                                // Fallback: Network Operator Settings
                                Intent networkIntent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
                                networkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(networkIntent);
                            } catch (Exception e2) {
                                Toast.makeText(context, "Could not open Data Settings", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }
    }

    // Removing unused method to clean up
}
