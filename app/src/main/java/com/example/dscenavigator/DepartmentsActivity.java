package com.example.dscenavigator;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DepartmentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_departments);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.placeList);
        TextView empty = findViewById(R.id.emptyText);
        EditText search = findViewById(R.id.searchPlaces);

        PlaceAdapter adapter = new PlaceAdapter(CampusData.getPlaces(), place -> {
            Intent i = new Intent(DepartmentsActivity.this, Campus3DActivity.class);
            i.putExtra("place_name", place.name);
            startActivity(i);
        });

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}