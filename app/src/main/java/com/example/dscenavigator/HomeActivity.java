package com.example.dscenavigator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class HomeActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply the saved theme before the screen is drawn
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean dark = prefs.getBoolean("dark", false);
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_home);

        // Theme toggle: moon in light mode, sun in dark mode
        TextView btnTheme = findViewById(R.id.btnTheme);
        btnTheme.setText(dark ? "\u2600\uFE0F" : "\uD83C\uDF19");
        btnTheme.setOnClickListener(v -> {
            boolean nowDark = prefs.getBoolean("dark", false);
            prefs.edit().putBoolean("dark", !nowDark).apply();
            AppCompatDelegate.setDefaultNightMode(!nowDark
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Working buttons
        findViewById(R.id.cardFind).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MainActivity.class)));
        findViewById(R.id.cardDepartments).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, DepartmentsActivity.class)));


        findViewById(R.id.cardAdmission).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AdmissionActivity.class)));
        findViewById(R.id.cardReport).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ReportActivity.class)));
        findViewById(R.id.cardAbout).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AboutActivity.class)));
    }

    private void soon(int viewId, String name) {
        findViewById(viewId).setOnClickListener(v ->
                Toast.makeText(this, name + " is coming soon", Toast.LENGTH_SHORT).show());
    }
}