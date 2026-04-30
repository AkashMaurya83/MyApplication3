package com.akash.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.card.MaterialCardView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    Button btnstopRing, btnAccessibility;
    TextView Receive;
    MaterialCardView cardSettings, cardManual;
    String number = "";
    SharedPreferences sp;

    private void updateLogs(String number, String message) {
        if (Receive == null) return;
        String log = "\n" + (number != null ? number : "Unknown") + " : " + message + "\n";
        Receive.append(log);
        
        if (Receive.getText().length() > 5000) {
            String current = Receive.getText().toString();
            Receive.setText(current.substring(current.length() - 2000));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            number = intent.getStringExtra("number");
            String message = intent.getStringExtra("message");
            if (message != null) updateLogs(number, message);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("MyPref", MODE_PRIVATE);

        btnstopRing = findViewById(R.id.btnstopRing);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        Receive = findViewById(R.id.Receive);
        cardSettings = findViewById(R.id.cardSettings);
        cardManual = findViewById(R.id.cardManual);

        cardSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        cardManual.setOnClickListener(v -> {
            startActivity(new Intent(this, ManualActivity.class));
        });

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS,
                Manifest.permission.CALL_PHONE, Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, 1);

        Intent intent = getIntent();
        if (intent != null) {
            number = intent.getStringExtra("number");
            String message = intent.getStringExtra("message");
            if (message != null) updateLogs(number, message);
        }

        clearAppStorage();

        btnstopRing.setOnClickListener(v -> {
            if (SmsReceive.ring != null && SmsReceive.ring.isPlaying()) {
                SmsReceive.ring.stop();
                Toast.makeText(MainActivity.this, "Ring stopped", Toast.LENGTH_SHORT).show();
                if (number != null && !number.isEmpty()) {
                    try {
                        SmsManager smsManager = getSystemService(SmsManager.class);
                        smsManager.sendTextMessage(number, null, "Ringing Stopped", null, null);
                    } catch (Exception e) {}
                }
            }
        });

        btnAccessibility.setOnClickListener(v -> {
            startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this, "Enable 'Security Guard' in Accessibility Settings", Toast.LENGTH_LONG).show();
        });
    }

    private void clearAppStorage() {
        try {
            File filesDir = getExternalFilesDir(null);
            if (filesDir != null) {
                File[] files = filesDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (System.currentTimeMillis() - f.lastModified() > 1800000) f.delete();
                    }
                }
            }
            File cacheDir = getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                File[] cacheFiles = cacheDir.listFiles();
                if (cacheFiles != null) {
                    for (File f : cacheFiles) f.delete();
                }
            }
        } catch (Exception e) {}
    }
}
