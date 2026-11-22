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

import com.google.android.material.chip.ChipGroup;

public class DepartmentsActivity extends AppCompatActivity {

    private PlaceAdapter adapter;
    private TextView empty;
    private TextView count;
    private boolean searching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_departments);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.placeList);
        empty = findViewById(R.id.emptyText);
        count = findViewById(R.id.tvCount);
        EditText search = findViewById(R.id.searchPlaces);
        ChipGroup chips = findViewById(R.id.chipGroup);

        adapter = new PlaceAdapter(CampusData.getPlaces(), place -> {
            Intent i = new Intent(DepartmentsActivity.this, Campus3DActivity.class);
            i.putExtra("place_name", place.name);
            startActivity(i);
        });

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        updateCount();

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searching = s.length() > 0;
                adapter.filter(s.toString());
                updateCount();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Category chips
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            String category = PlaceAdapter.CAT_ALL;
            if (id == R.id.chipDept) category = PlaceAdapter.CAT_DEPT;
            else if (id == R.id.chipHostel) category = PlaceAdapter.CAT_HOSTEL;
            else if (id == R.id.chipFood) category = PlaceAdapter.CAT_FOOD;
            else if (id == R.id.chipSports) category = PlaceAdapter.CAT_SPORTS;
            else if (id == R.id.chipGates) category = PlaceAdapter.CAT_GATES;
            else if (id == R.id.chipFacilities) category = PlaceAdapter.CAT_FACILITIES;
            adapter.setCategory(category);
            updateCount();
        });
    }

    private void updateCount() {
        int n = adapter.getItemCount();
        count.setText(n + (n == 1 ? " place" : " places")
                + (searching ? " found" : " on campus"));
        empty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
    }
}