package com.example.dscenavigator;


import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.Holder> {

    public interface OnPlaceClick {
        void onClick(Place place);
    }

    // Categories used by the filter chips
    public static final String CAT_ALL = "All";
    public static final String CAT_DEPT = "Departments";
    public static final String CAT_HOSTEL = "Hostels";
    public static final String CAT_FOOD = "Food";
    public static final String CAT_SPORTS = "Sports";
    public static final String CAT_GATES = "Gates & Parking";
    public static final String CAT_FACILITIES = "Facilities";

    private final List<Place> all;
    private final List<Place> shown = new ArrayList<>();
    private final OnPlaceClick listener;
    private Location userLocation;
    private String query = "";
    private String category = CAT_ALL;

    public PlaceAdapter(List<Place> places, OnPlaceClick listener) {
        this.all = places;
        this.shown.addAll(places);
        this.listener = listener;
    }

    public void filter(String text) {
        query = text.trim().toLowerCase();
        applyFilter();
    }

    public void setCategory(String newCategory) {
        category = newCategory;
        applyFilter();
    }

    private void applyFilter() {
        shown.clear();
        for (Place p : all) {
            boolean textOk = query.isEmpty() || p.name.toLowerCase().contains(query);
            boolean catOk = category.equals(CAT_ALL) || categoryOf(p.name).equals(category);
            if (textOk && catOk) {
                shown.add(p);
            }
        }
        notifyDataSetChanged();
    }

    // Works out which chip a place belongs to, from its name
    public static String categoryOf(String name) {
        String n = name.toLowerCase();
        if (n.contains("canteen") || n.contains("mess")) return CAT_FOOD;
        if (n.contains("hostel")) return CAT_HOSTEL;
        if (n.contains("ground") || n.contains("court")) return CAT_SPORTS;
        if (n.contains("gate") || n.contains("parking")) return CAT_GATES;
        if (n.contains("library") || n.contains("temple") || n.contains("auditorium")
                || n.contains("hall") || n.contains("amphitheatre") || n.contains("rock garden")
                || n.contains("conveno")) return CAT_FACILITIES;
        return CAT_DEPT;
    }

    // Sorts the list so the closest place comes first and shows the distance
    public void sortByDistance(Location me) {
        userLocation = me;
        Collections.sort(all, (a, b) -> Float.compare(distanceTo(a), distanceTo(b)));
        applyFilter();
    }

    private float distanceTo(Place p) {
        float[] result = new float[1];
        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                p.lat, p.lon, result);
        return result[0];
    }

    private String formatDistance(float meters) {
        if (meters < 1000) {
            return Math.round(meters) + " m";
        }
        return String.format("%.1f km", meters / 1000);
    }

    private String iconFor(String name) {
        String n = name.toLowerCase();
        if (n.contains("hostel") || n.contains("mess")) return "🛏️";
        if (n.contains("canteen")) return "🍽️";
        if (n.contains("library")) return "📚";
        if (n.contains("ground") || n.contains("court")) return "⚽";
        if (n.contains("gate")) return "🚪";
        if (n.contains("parking")) return "🅿️";
        if (n.contains("temple")) return "🛕";
        if (n.contains("auditorium") || n.contains("hall") || n.contains("amphitheatre")) return "🎭";
        if (n.contains("department") || n.contains("engineering") || n.contains("block")
                || n.contains("college") || n.contains("polytechnic") || n.contains("building")) return "🏛️";
        return "📍";
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Place p = shown.get(position);
        h.icon.setText(iconFor(p.name));
        h.name.setText(p.name);

        // Different tile colour for each category
        int[] tiles = {R.drawable.bg_icon_blue, R.drawable.bg_icon_green,
                R.drawable.bg_icon_orange, R.drawable.bg_icon_pink};
        h.icon.setBackgroundResource(tiles[(categoryOf(p.name).hashCode() & 0x7fffffff) % tiles.length]);

        // Show the category instead of the same placeholder text on every card
        if (p.details == null || p.details.equals("Floor and block details")) {
            h.details.setText(categoryOf(p.name) + "  •  Tap to navigate");
        } else {
            h.details.setText(p.details);
        }

        if (userLocation != null) {
            h.distance.setVisibility(View.VISIBLE);
            h.distance.setText(formatDistance(distanceTo(p)));
        } else {
            h.distance.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(p));
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView icon, name, details, distance;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.tvIcon);
            name = v.findViewById(R.id.tvName);
            details = v.findViewById(R.id.tvDetails);
            distance = v.findViewById(R.id.tvDistance);
        }
    }
}