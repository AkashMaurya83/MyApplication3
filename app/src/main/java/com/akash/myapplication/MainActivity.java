package com.akash.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.card.MaterialCardView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    Button btnstopRing, btnAccessibility;
    TextView Receive;

    Button cardSettings;
    MaterialCardView cardSettingsCard;
    Button cardManual;

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

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PrefsHelper prefsHelper = new PrefsHelper(this);

        // Check: Kya password pehle se set hai?
        if (prefsHelper.isFirstTime()) {
            // Nahi hai -> Toh Setup (Password banane) wali screen par bhejo
            startActivity(new Intent(MainActivity.this, SetupPasswordActivity.class));
            finish();
            return;
        }

        // Haan hai -> Lekin kya user login hai?
        if (!getIntent().getBooleanExtra("isLoggedIn", false)) {
            // Login nahi hai -> Toh LoginActivity par bhejo
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        sp = getSharedPreferences("MyPref", MODE_PRIVATE);

        Receive = findViewById(R.id.tvReceive); // Restored link
        btnstopRing = findViewById(R.id.btnstopRing);
        btnAccessibility = findViewById(R.id.btnAccessibility);

        cardSettings = findViewById(R.id.btnSecurity);
        cardSettingsCard = findViewById(R.id.cardSettings);
        cardManual = findViewById(R.id.btnlearn);

        cardSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        if (cardSettingsCard != null) {
            cardSettingsCard.setOnClickListener(v -> {
                startActivity(new Intent(this, SetupPasswordActivity.class));
            });
        }
        cardManual.setOnClickListener(v -> {
            startActivity(new Intent(this, ManualActivity.class ));
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
            if (SmsReceive.ring != null) {
                if (SmsReceive.ring.isPlaying()) {
                    SmsReceive.ring.stop();
                    Toast.makeText(MainActivity.this, "Ring stopped", Toast.LENGTH_SHORT).show();
                    if (number != null && !number.isEmpty()) {
                        try {
                            android.telephony.SmsManager smsManager = getSystemService(android.telephony.SmsManager.class);
                            smsManager.sendTextMessage(number, null, "Security Guard: Ringing Stopped", null, null);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "SMS failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Phone is not ringing", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "No active ringtone to stop", Toast.LENGTH_SHORT).show();
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
