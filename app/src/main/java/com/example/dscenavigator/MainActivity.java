package com.example.dscenavigator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private MyLocationNewOverlay myLocationOverlay;
    private final List<Place> places = new ArrayList<>();
    private final CampusGraph graph = CampusData.buildGraph();
    private final HashMap<String, String> viaOf = new HashMap<>();
    private Place selectedPlace;
    private Polyline routeLine;
    private TextView distanceText;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updater = new Runnable() {
        @Override
        public void run() {
            updateRoute();
            handler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid setup (must come before setContentView)
        Configuration.getInstance().load(getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(
                "DSCENavigator/1.0 (college lab project; patilmohan8111@gmail.com)");
        File base = new File(getCacheDir(), "osmdroid_new");
        Configuration.getInstance().setOsmdroidBasePath(base);
        Configuration.getInstance().setOsmdroidTileCache(new File(base, "tiles"));

        setContentView(R.layout.activity_main);

        distanceText = findViewById(R.id.distanceText);

        // Map
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.OpenTopo);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(
                CustomZoomButtonsController.Visibility.ALWAYS);
        map.getController().setZoom(18.0);
        map.getController().setCenter(new GeoPoint(12.909473, 77.566848));



        // Gray path lines, only for checking new points. Set to true to see them.
        boolean showPaths = false;
        if (showPaths) {
            for (int a = 0; a < graph.size(); a++) {
                for (int b : graph.getNeighbors(a)) {
                    if (a < b) {
                        Polyline seg = new Polyline();
                        seg.getOutlinePaint().setColor(Color.GRAY);
                        seg.getOutlinePaint().setStrokeWidth(5f);
                        List<GeoPoint> pts = new ArrayList<>();
                        pts.add(graph.getPoint(a));
                        pts.add(graph.getPoint(b));
                        seg.setPoints(pts);
                        map.getOverlays().add(seg);
                    }
                }
            }
        }

        // Route line
        routeLine = new Polyline();
        routeLine.getOutlinePaint().setColor(Color.BLUE);
        routeLine.getOutlinePaint().setStrokeWidth(8f);
        map.getOverlays().add(routeLine);

        // Places (buildings) and special route rules now live in CampusData
        places.addAll(CampusData.getPlaces());
        viaOf.putAll(CampusData.getViaRules());

        // Dots and name labels
        int dotSize = (int) (18 * getResources().getDisplayMetrics().density);

        for (Place p : places) {
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(Color.parseColor("#D32F2F"));
            dot.setStroke(3, Color.WHITE);
            dot.setSize(dotSize, dotSize);

            Marker m = new Marker(map);
            m.setPosition(new GeoPoint(p.lat, p.lon));
            m.setTitle(p.name);
            m.setSnippet(p.details);
            m.setIcon(dot);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            m.setOnMarkerClickListener((marker, mapView) -> {
                onPlaceTapped(p, marker);
                return true;
            });

            // Name label shown under the dot
            Marker label = new Marker(map);
            label.setPosition(new GeoPoint(p.lat, p.lon));
            label.setTextIcon(p.name);
            label.setTextLabelFontSize(24);
            label.setTextLabelBackgroundColor(Color.argb(200, 255, 255, 255));
            label.setTextLabelForegroundColor(Color.BLACK);
            label.setAnchor(Marker.ANCHOR_CENTER, -0.9f);
            label.setInfoWindow(null);
            label.setOnMarkerClickListener((marker, mapView) -> {
                onPlaceTapped(p, m);
                return true;
            });

            map.getOverlays().add(label);
            map.getOverlays().add(m);
        }

        // Live location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        // Search box
        AutoCompleteTextView searchBox = findViewById(R.id.searchBox);

        List<String> names = new ArrayList<>();
        for (Place p : places) {
            names.add(p.name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        searchBox.setAdapter(adapter);

        searchBox.setOnItemClickListener((parent, view, position, id) -> {
            String chosen = (String) parent.getItemAtPosition(position);
            for (Place p : places) {
                if (p.name.equals(chosen)) {
                    selectedPlace = p;
                    break;
                }
            }
            if (selectedPlace != null) {
                if (myLocationOverlay != null) {
                    myLocationOverlay.disableFollowLocation();
                }
                map.getController().animateTo(
                        new GeoPoint(selectedPlace.lat, selectedPlace.lon));
                updateRoute();
            }
        });

        // If another screen sent a place name, select it
        String wanted = getIntent().getStringExtra("place_name");
        if (wanted != null) {
            for (Place p : places) {
                if (p.name.equals(wanted)) {
                    selectedPlace = p;
                    if (myLocationOverlay != null) {            // NEW
                        myLocationOverlay.disableFollowLocation(); // NEW
                    }                                           // NEW
                    map.getController().setCenter(new GeoPoint(p.lat, p.lon));
                    break;
                }
            }
        }

    }

    private void onPlaceTapped(Place p, Marker dotMarker) {
        selectedPlace = p;
        if (myLocationOverlay != null) {
            myLocationOverlay.disableFollowLocation();
        }
        dotMarker.showInfoWindow();
        updateRoute();
    }



    private void updateRoute() {
        if (selectedPlace == null) {
            return;
        }
        if (myLocationOverlay == null) {
            distanceText.setText("Allow location to see the distance");
            return;
        }
        GeoPoint me = myLocationOverlay.getMyLocation();
        if (me == null) {
            distanceText.setText("Waiting for your location...");
            return;
        }

        GeoPoint dest = new GeoPoint(selectedPlace.lat, selectedPlace.lon);

        // Joint building: go through the other building first
        GeoPoint via = null;
        String viaName = viaOf.get(selectedPlace.name);
        if (viaName != null) {
            for (Place p : places) {
                if (p.name.equals(viaName)) {
                    via = new GeoPoint(p.lat, p.lon);
                    break;
                }
            }
        }
        GeoPoint pathEnd = (via != null) ? via : dest;

        int startNode = graph.nearestNode(me);
        int endNode = graph.nearestNode(pathEnd);
        List<Integer> nodePath = graph.shortestPath(startNode, endNode);

        if (nodePath.isEmpty()) {
            routeLine.setPoints(new ArrayList<>());
            map.invalidate();
            distanceText.setText("No walkway connection found to " + selectedPlace.name);
            return;
        }

        List<GeoPoint> points = new ArrayList<>();
        points.add(me);
        for (int idx : nodePath) {
            points.add(graph.getPoint(idx));
        }
        if (via != null) {
            points.add(via);
        }
        points.add(dest);

        routeLine.setPoints(points);
        map.invalidate();

        double meters = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            meters += points.get(i).distanceToAsDouble(points.get(i + 1));
        }

        if (meters < 1000) {
            distanceText.setText(selectedPlace.name + ": " + Math.round(meters) + " m");
        } else {
            distanceText.setText(selectedPlace.name + ": "
                    + String.format("%.2f", meters / 1000) + " km");
        }
    }

    private void enableLocation() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        myLocationOverlay.enableMyLocation();
        if (selectedPlace == null) {
            myLocationOverlay.enableFollowLocation();
        }
        map.getOverlays().add(myLocationOverlay);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        handler.post(updater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        handler.removeCallbacks(updater);
    }
}