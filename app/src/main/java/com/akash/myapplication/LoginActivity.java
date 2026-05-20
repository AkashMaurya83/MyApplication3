package com.akash.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etPassword;
    private Button btnLogin;
    private PrefsHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin); // Aapka original login layout

        prefsHelper = new PrefsHelper(this);

        // Apne XML ke IDs yahan link karein
        etPassword = findViewById(R.id.etpin);
        btnLogin = findViewById(R.id.btnpin);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String enteredPass = etPassword.getText().toString().trim();

                    if (enteredPass.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Password daalein", Toast.LENGTH_SHORT).show();
                    } else if (prefsHelper.checkPassword(enteredPass)) {
                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                        // Check if we came from Power Block
                        String reason = getIntent().getStringExtra("reason");
                        if ("power_block".equals(reason)) {
                            if (ToggleAccessibilityService.instance != null) {
                                ToggleAccessibilityService.instance.showPowerMenu();
                            }
                        } else {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("isLoggedIn", true);
                            startActivity(intent);
                        }

                        finish();
                    } else {
                        // PASSWORD GALAT HAI
                        Toast.makeText(LoginActivity.this, "Galat Password!", Toast.LENGTH_SHORT).show();
                        etPassword.setText(""); // Box clear kar do
                    }
                }
            });
        }
    }
}