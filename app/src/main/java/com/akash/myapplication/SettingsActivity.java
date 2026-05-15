package com.akash.myapplication;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    EditText edtCode, edtMasterNumber;
    Button btnSave, btnAddNumber;
    LinearLayout numberListLayout;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        sp = getSharedPreferences("MyPref", MODE_PRIVATE);

        edtCode = findViewById(R.id.edtCode);
        edtMasterNumber = findViewById(R.id.edtMasterNumber);
        btnSave = findViewById(R.id.btnSave);
        btnAddNumber = findViewById(R.id.btnAddNumber);
        numberListLayout = findViewById(R.id.numberListLayout);

        edtCode.setText(sp.getString("Security", ""));
        refreshNumberList();

        btnSave.setOnClickListener(v -> {
            String code = edtCode.getText().toString();
            sp.edit().putString("Security", code).apply();
            Toast.makeText(this, "Security Code Saved", Toast.LENGTH_SHORT).show();
        });

        btnAddNumber.setOnClickListener(v -> {
            String num = edtMasterNumber.getText().toString().trim();
            if (num.length() < 10) {
                Toast.makeText(this, "Enter valid number!", Toast.LENGTH_SHORT).show();
                return;
            }
            Set<String> numbers = new HashSet<>(sp.getStringSet("MasterNumbers", new HashSet<>()));
            numbers.add(num);
            sp.edit().putStringSet("MasterNumbers", numbers).apply();
            edtMasterNumber.setText("");
            refreshNumberList();
            Toast.makeText(this, "Number Added", Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshNumberList() {
        numberListLayout.removeAllViews();
        Set<String> numbers = sp.getStringSet("MasterNumbers", new HashSet<>());
        for (String num : numbers) {
            TextView tv = new TextView(this);
            String last4 = num.length() > 4 ? "****" + num.substring(num.length() - 4) : num;
            tv.setText(getString(R.string.number_list_item, last4));
            tv.setTextSize(16);
            tv.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
            tv.setPadding(16, 16, 16, 16);
            tv.setBackgroundResource(android.R.drawable.list_selector_background);
            tv.setOnClickListener(v -> {
                Set<String> currentNumbers = new HashSet<>(sp.getStringSet("MasterNumbers", new HashSet<>()));
                currentNumbers.remove(num);
                sp.edit().putStringSet("MasterNumbers", currentNumbers).apply();
                refreshNumberList();
                Toast.makeText(this, "Number Removed", Toast.LENGTH_SHORT).show();
            });
            numberListLayout.addView(tv);
        }
    }
}
