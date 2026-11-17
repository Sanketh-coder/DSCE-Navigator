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

    private final List<Place> all;
    private final List<Place> shown = new ArrayList<>();
    private final OnPlaceClick listener;
    private Location userLocation;

    public PlaceAdapter(List<Place> places, OnPlaceClick listener) {
        this.all = places;
        this.shown.addAll(places);
        this.listener = listener;
    }

    public void filter(String text) {
        String q = text.trim().toLowerCase();
        shown.clear();
        for (Place p : all) {
            if (q.isEmpty() || p.name.toLowerCase().contains(q)) {
                shown.add(p);
            }
        }
        notifyDataSetChanged();
    }

    // Sorts the list so the closest place comes first and shows the distance
    public void sortByDistance(Location me) {
        userLocation = me;
        Collections.sort(all, (a, b) -> Float.compare(distanceTo(a), distanceTo(b)));
        filter("");
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
        h.details.setText(p.details);

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