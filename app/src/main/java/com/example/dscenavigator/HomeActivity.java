package com.example.dscenavigator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Calendar;

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

        // Time-based greeting
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hour < 12 ? "Good morning" : (hour < 17 ? "Good afternoon" : "Good evening");
        ((TextView) findViewById(R.id.tvGreeting)).setText(greet + " \uD83D\uDC4B");

        // Cards slide up one after another (skipped when the theme toggle recreates the screen)
        if (savedInstanceState == null) {
            View[] cards = {
                    findViewById(R.id.cardFind),
                    findViewById(R.id.cardDepartments),
                    findViewById(R.id.cardAdmission),
                    findViewById(R.id.cardReport),
                    findViewById(R.id.cardAbout)
            };
            float shift = 28 * getResources().getDisplayMetrics().density;
            for (int i = 0; i < cards.length; i++) {
                cards[i].setAlpha(0f);
                cards[i].setTranslationY(shift);
                cards[i].animate().alpha(1f).translationY(0f)
                        .setStartDelay(120L * i + 100)
                        .setDuration(450)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }

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

        // Buttons
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
}