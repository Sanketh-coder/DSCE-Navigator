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
    private final CampusGraph graph = new CampusGraph();
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

        // Walkway network (junctions and bends only)
        buildGraph();

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

        findViewById(R.id.btn3d).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, Campus3DActivity.class)));
    }

    private void onPlaceTapped(Place p, Marker dotMarker) {
        selectedPlace = p;
        if (myLocationOverlay != null) {
            myLocationOverlay.disableFollowLocation();
        }
        dotMarker.showInfoWindow();
        updateRoute();
    }

    // Junctions and bends only. Buildings are NOT part of this network.
    private void buildGraph() {
        int j1 = graph.addNode("Junction 1", 12.909452163940593, 77.56690626199226);
        int lj = graph.addNode("Library Junction", 12.909259042792195, 77.56689228934977);
        int b1 = graph.addNode("Bend 1", 12.90899716750334, 77.56685518765237);
        int b2 = graph.addNode("Bend 2", 12.908977677953816, 77.56670921161641);
        int j2 = graph.addNode("Junction 2", 12.908654348988824, 77.5666988303368);
        int j3 = graph.addNode("Junction 3", 12.908611412472947, 77.56639263782078);
        int round = graph.addNode("Roundabout", 12.908603148965359, 77.56629260024972);
        int jc4 = graph.addNode("Junction CSE 4", 12.908466933920192, 77.56597023975043);
        int jc5 = graph.addNode("Junction CSE 5", 12.908127864653343, 77.56602445273128);
        int j6 = graph.addNode("Junction 6", 12.908410092961743, 77.56573585382016);
        int b3 = graph.addNode("Bend 3", 12.908487268488631, 77.56547652597578);
        int b4 = graph.addNode("Bend 4", 12.908015967226282, 77.56630316223328);
        int b5 = graph.addNode("Bend 5", 12.90825772641125, 77.56637938160114);
        int b6 = graph.addNode("Bend 6", 12.908231023602807, 77.56652277641766);
        int j7 = graph.addNode("Junction 7", 12.908032786402046, 77.56661797398587);
        int b7 = graph.addNode("Bend 7", 12.908148256239866, 77.56676892537583);
        int b8 = graph.addNode("Bend 8", 12.908180610068786, 77.56692382429856);
        int b9 = graph.addNode("Bend 9", 12.908342692341485, 77.56693862759742);
        int b10 = graph.addNode("Bend 10", 12.908370032846431, 77.56700033616666);
        int j8 = graph.addNode("Junction 8", 12.9085134080523, 77.56702108061448);
        int b11 = graph.addNode("Bend 11", 12.908525642675148, 77.56680116629576);
        int j9 = graph.addNode("Junction 9", 12.909474417937995, 77.56651576600096);
        int b12 = graph.addNode("Bend 12", 12.90918721029665, 77.56628221040849);
        int j10 = graph.addNode("Junction 10", 12.909430773933972, 77.56753317493545);
        int j11 = graph.addNode("Junction 11", 12.908675115725668, 77.5675874778338);
        int j13 = graph.addNode("Junction 13", 12.908485027281984, 77.56758184516191);
        int j14 = graph.addNode("Junction 14", 12.907550976027444, 77.56700658479666);
        int j15 = graph.addNode("Junction 15", 12.907484691708188, 77.56726694186878);
        int j16 = graph.addNode("Junction 16", 12.907526671452993, 77.56679350613977);
        int j17 = graph.addNode("Junction 17", 12.907454374275545, 77.56639465658326);
        int b13 = graph.addNode("Bend 13", 12.90741437353605, 77.5659829702111);
        int b14 = graph.addNode("Bend 14", 12.90896237999502, 77.56758593870494);
        int b15 = graph.addNode("Bend 15", 12.908146289492963, 77.56757043409074);
        int b16 = graph.addNode("Bend 16", 12.907738243246898, 77.5674339934193);
        int b17 = graph.addNode("Bend 17", 12.907785554815762, 77.56671369618405);
        int b18 = graph.addNode("Bend 18", 12.907087245438182, 77.5663823243488);
        int b19 = graph.addNode("Bend 19", 12.90721005638239, 77.5670182311133);
        int j18 = graph.addNode("Junction 18", 12.907969324322213, 77.56568729641981);
        int b20 = graph.addNode("Bend 20", 12.908545053494494, 77.56638100930793);
        int b21 = graph.addNode("Bend 21", 12.908339050476645, 77.56637569308572);
        int b22 = graph.addNode("Bend 22", 12.906732947688875, 77.56697620089976);

        // Top road along the gate
        graph.connect(j9, j1);
        graph.connect(j1, j10);
        // Gate road down to the roundabout
        graph.connect(j1, lj);
        graph.connect(lj, b1);
        graph.connect(b1, b2);
        graph.connect(b2, j2);
        graph.connect(j2, j3);
        graph.connect(j3, round);
        // Road from top-left down to the roundabout
        graph.connect(j9, b12);
        graph.connect(b12, round);
        // West of the roundabout
        graph.connect(round, jc4);
        graph.connect(jc4, jc5);
        graph.connect(jc4, j6);
        graph.connect(j6, b3);
        // Inner ring road
        graph.connect(jc5, b4);
        graph.connect(b4, b5);
        graph.connect(b5, b6);
        graph.connect(b6, j7);
        graph.connect(j7, b7);
        graph.connect(b7, b8);
        graph.connect(b8, b9);
        graph.connect(b9, b10);
        graph.connect(b10, j8);
        graph.connect(j8, b11);
        graph.connect(b11, j2);
        // East road, top to bottom
        graph.connect(j10, b14);
        graph.connect(b14, j11);
        graph.connect(j11, j13);
        graph.connect(j13, b15);
        graph.connect(b15, b16);
        graph.connect(b16, j15);
        // South road
        graph.connect(b13, j17);
        graph.connect(j17, j16);
        graph.connect(j16, j14);
        graph.connect(j14, j15);
        // Roundabout road down to the inner ring road
        graph.connect(j3, b20);
        graph.connect(b20, b21);
        graph.connect(b21, b5);
        // Inner ring road to the south road
        graph.connect(j7, b17);
        graph.connect(b17, j16);
        // South spurs
        graph.connect(j17, b18);
        graph.connect(j14, b19);
        graph.connect(b19,b22);
        // West road
        graph.connect(j6, j18);
        graph.connect(j18, b13);

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