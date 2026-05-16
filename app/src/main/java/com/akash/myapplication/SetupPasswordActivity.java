package com.akash.myapplication;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SetupPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private Button btnSetup;
    private PrefsHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Aapka XML layout yahan hoga

        prefsHelper = new PrefsHelper(this);

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSetup = findViewById(R.id.btnSetup);

        btnSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPass = etNewPassword.getText().toString().trim();
                String confirmPass = etConfirmPassword.getText().toString().trim();

                if (newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(SetupPasswordActivity.this, "Please fill both fields", Toast.LENGTH_SHORT).show();
                } else if (!newPass.equals(confirmPass)) {
                    Toast.makeText(SetupPasswordActivity.this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                } else {
                    // Password match kar gaya, save karo
                    prefsHelper.setPassword(newPass);
                    Toast.makeText(SetupPasswordActivity.this, "Password Set Successfully!", Toast.LENGTH_SHORT).show();

                    // Ab Dashboard ya Security Setup (Trusted number wala) par bhej do
                    Intent intent = new Intent(SetupPasswordActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}