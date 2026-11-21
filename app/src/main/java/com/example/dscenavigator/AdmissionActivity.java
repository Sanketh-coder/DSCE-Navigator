package com.example.dscenavigator;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AdmissionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admission);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        LinearLayout container = findViewById(R.id.admissionContainer);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (AdmissionInfo info : AdmissionInfo.getAll()) {
            View card = inflater.inflate(R.layout.item_admission, container, false);

            ((TextView) card.findViewById(R.id.tvIcon)).setText(info.icon);
            ((TextView) card.findViewById(R.id.tvTitle)).setText(info.title);
            ((TextView) card.findViewById(R.id.tvLocation)).setText("📍 " + info.placeName);
            ((TextView) card.findViewById(R.id.tvDescription)).setText(info.description);
            ((TextView) card.findViewById(R.id.tvPhone)).setText("📞 " + info.phone);

            // Opens the map with this place already selected
            card.findViewById(R.id.btnMap).setOnClickListener(v -> {
                Intent i = new Intent(AdmissionActivity.this, Campus3DActivity.class);
                i.putExtra("place_name", info.placeName);
                startActivity(i);
            });

            // Opens the dialer with the number filled in; the user presses call
            card.findViewById(R.id.btnCall).setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_DIAL,
                            Uri.parse("tel:" + info.phone))));

            container.addView(card);
        }
    }
}