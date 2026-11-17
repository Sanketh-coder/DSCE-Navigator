package com.example.dscenavigator;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends AppCompatActivity {

    private static final String REPORT_EMAIL = "mohanpatil8455@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        AutoCompleteTextView placeBox = findViewById(R.id.reportPlace);
        EditText detailsBox = findViewById(R.id.reportDetails);
        RadioGroup issueGroup = findViewById(R.id.issueGroup);

        // Suggestions come from the same list the map uses
        List<String> names = new ArrayList<>();
        for (Place p : CampusData.getPlaces()) {
            names.add(p.name);
        }
        placeBox.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names));
        placeBox.setThreshold(1);

        findViewById(R.id.btnSend).setOnClickListener(v -> {
            String details = detailsBox.getText().toString().trim();
            if (details.isEmpty()) {
                detailsBox.setError("Please describe the problem");
                return;
            }

            String place = placeBox.getText().toString().trim();
            if (place.isEmpty()) {
                place = "Not specified";
            }

            String type = "Other";
            RadioButton selected = findViewById(issueGroup.getCheckedRadioButtonId());
            if (selected != null) {
                type = selected.getText().toString();
            }

            String body = "Place: " + place + "\n"
                    + "Issue type: " + type + "\n\n"
                    + "Details:\n" + details + "\n\n"
                    + "Sent from DSCE Navigator";

            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:"));
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{REPORT_EMAIL});
            email.putExtra(Intent.EXTRA_SUBJECT, "DSCE Navigator: " + type + " - " + place);
            email.putExtra(Intent.EXTRA_TEXT, body);

            try {
                startActivity(Intent.createChooser(email, "Send report using"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No email app found on this phone",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}