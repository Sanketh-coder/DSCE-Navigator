package com.example.dscenavigator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable goHome = () -> {
        startActivity(new Intent(SplashActivity.this, HomeActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.splashLogo);
        View title = findViewById(R.id.splashTitle);
        View tagline = findViewById(R.id.splashTagline);
        View footer = findViewById(R.id.splashFooter);

        float shift = 24 * getResources().getDisplayMetrics().density;

        // Logo pops in
        logo.setScaleX(0.3f);
        logo.setScaleY(0.3f);
        logo.setAlpha(0f);
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(700)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .start();

        // Texts slide up one after another
        View[] texts = {title, tagline, footer};
        for (int i = 0; i < texts.length; i++) {
            texts[i].setAlpha(0f);
            texts[i].setTranslationY(shift);
            texts[i].animate().alpha(1f).translationY(0f)
                    .setStartDelay(400L + i * 200L)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        handler.postDelayed(goHome, 1900);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(goHome);
        super.onDestroy();
    }
}