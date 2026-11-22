package com.example.dscenavigator;




import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.text.TextUtils;

public class Campus3DActivity extends AppCompatActivity {

    private WebView web;
    private TextView infoText;
    private TextView btnStart;

    private CampusBuildings.Building selectedBuilding;
    private String highlightDept = null;

    // Side panel (building details)
    private DrawerLayout drawer;
    private TextView drawerBadge, drawerTitle, drawerSub, drawerBack;
    private LinearLayout drawerContent;
    private View btnNavigate;
    private final List<Place> places = new ArrayList<>();
    private Place selectedPlace;
    private String selectedLabel = "";
    private String pendingPlace;

    // Walkway network and special route rules
    private final CampusGraph graph = CampusData.buildGraph();
    private final HashMap<String, String> viaOf = CampusData.getViaRules();
    private final HashMap<String, double[]> backGateOf = CampusData.getBackGates();
    private final HashMap<String, String> aliasOf = CampusData.getAliases();

    // Navigation mode
    private boolean justSelected = false;
    private boolean following = false;
    private boolean zoomNext = true;
    private double camBearing = Double.NaN;

    // Live location
    private LocationManager locationManager;
    private Location lastLocation;
    private float lastBearing = 0f;
    private float declination = 0f;
    private boolean pageLoaded = false;
    private boolean firstFix = true;

    // Compass
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private float compassHeading = -1f;
    private float smoothSin = 0f;
    private float smoothCos = 0f;
    private float lastPushedHeading = -1f;
    private long lastCompassPush = 0;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            handleLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            startLocation();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }
    };

    private final SensorEventListener sensorListener = new SensorEventListener() {
        private final float[] rotation = new float[9];
        private final float[] remapped = new float[9];
        private final float[] orientation = new float[3];

        @Override
        public void onSensorChanged(SensorEvent event) {
            SensorManager.getRotationMatrixFromVector(rotation, event.values);
            SensorManager.getOrientation(rotation, orientation);
            // Phone held upright: use the direction the back of the phone faces
            if (Math.abs(orientation[1]) > 0.78f) {
                SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X,
                        SensorManager.AXIS_Z, remapped);
                SensorManager.getOrientation(remapped, orientation);
            }

            double heading = Math.toDegrees(orientation[0]) + declination;
            double rad = Math.toRadians(heading);
            // Smooth the heading so the arrow does not shake
            smoothSin = smoothSin * 0.85f + (float) Math.sin(rad) * 0.15f;
            smoothCos = smoothCos * 0.85f + (float) Math.cos(rad) * 0.15f;
            float smooth = (float) Math.toDegrees(Math.atan2(smoothSin, smoothCos));
            if (smooth < 0) {
                smooth += 360f;
            }
            compassHeading = smooth;

            long now = System.currentTimeMillis();
            if (now - lastCompassPush > 150
                    && (lastPushedHeading < 0 || angleDiff(smooth, lastPushedHeading) > 2)) {
                lastCompassPush = now;
                lastPushedHeading = smooth;
                pushUserToPage();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus3d);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        infoText = findViewById(R.id.infoText);
        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> onStartPressed());
        findViewById(R.id.btnClear).setOnClickListener(v -> clearSelection());
        places.addAll(CampusData.getPlaces());
        setupDrawer();
        pendingPlace = getIntent().getStringExtra("place_name");

        setupSearch();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 3);
        }

        web = findViewById(R.id.web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                view.evaluateJavascript("setBuildings(" + buildBuildingsJson() + ");", null);
                view.evaluateJavascript("setPlaces(" + buildPlacesJson() + ");", null);
                pushUserToPage();
                updateRoute();
                if (pendingPlace != null) {
                    selectByName(pendingPlace);
                    pendingPlace = null;
                }
            }
        });

        try {
            String html = readAsset("campus3d.html");
            web.loadDataWithBaseURL("https://campus3d.local/", html,
                    "text/html", "UTF-8", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- Live location and compass ----------

    private boolean hasFinePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Approximate location is enough to get started, so accept either permission
    private boolean hasLocationPermission() {
        return hasFinePermission() || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean providerOn(String provider) {
        try {
            return locationManager.isProviderEnabled(provider);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocation() {
        if (!hasLocationPermission()) {
            return;
        }
        locationManager.removeUpdates(locationListener);

        // GPS is accurate but slow (and weak indoors). Network location (Wi-Fi / mobile)
        // is quick but rough. Listen to both and use whichever gives the better fix.
        boolean gps = hasFinePermission() && providerOn(LocationManager.GPS_PROVIDER);
        boolean network = providerOn(LocationManager.NETWORK_PROVIDER);

        if (!gps && !network) {
            Toast.makeText(this, "Turn on Location (GPS) on your phone",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (gps) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        1000, 0, locationListener);
            }
            if (network) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        2000, 0, locationListener);
            }

            // Show something straight away from the last known position (if it is recent)
            String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};
            for (String p : providers) {
                if (!providerOn(p)) {
                    continue;
                }
                Location last = locationManager.getLastKnownLocation(p);
                if (last != null && System.currentTimeMillis() - last.getTime() < 10 * 60 * 1000) {
                    handleLocation(last);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Decides if a new fix is better than the one we already have
    private boolean isBetterLocation(Location fresh, Location current) {
        if (current == null) {
            return true;
        }
        long timeDelta = fresh.getTime() - current.getTime();
        if (timeDelta > 10000) {
            return true;
        }
        if (timeDelta < -10000) {
            return false;
        }
        int accuracyDelta = (int) (fresh.getAccuracy() - current.getAccuracy());
        boolean moreAccurate = accuracyDelta < 0;
        boolean lessAccurate = accuracyDelta > 0;
        boolean muchLessAccurate = accuracyDelta > 100;
        boolean sameProvider = fresh.getProvider() != null
                && fresh.getProvider().equals(current.getProvider());

        if (moreAccurate) {
            return true;
        }
        if (timeDelta > 0 && !lessAccurate) {
            return true;
        }
        return timeDelta > 0 && !muchLessAccurate && sameProvider;
    }

    // Used by the Start and target buttons: asks for permission / GPS if needed
    private boolean ensureLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 3);
            return false;
        }
        if (!providerOn(LocationManager.GPS_PROVIDER) && !providerOn(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Please turn on Location", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return false;
        }
        startLocation();
        return lastLocation != null;
    }

    private void handleLocation(Location loc) {
        if (!isBetterLocation(loc, lastLocation)) {
            return;
        }
        lastLocation = loc;
        // GPS only knows the direction while we are moving
        if (loc.hasBearing() && loc.getSpeed() > 0.5f) {
            lastBearing = loc.getBearing();
        }
        // Difference between magnetic north and true north at this place
        GeomagneticField field = new GeomagneticField((float) loc.getLatitude(),
                (float) loc.getLongitude(), (float) loc.getAltitude(),
                System.currentTimeMillis());
        declination = field.getDeclination();

        if (firstFix) {
            firstFix = false;
            if (selectedPlace == null) {
                infoText.setText("Search or tap a place");
            }
        }
        pushUserToPage();
        updateRoute();
    }

    private void pushUserToPage() {
        if (!pageLoaded || lastLocation == null) {
            return;
        }
        float heading = (compassHeading >= 0) ? compassHeading : lastBearing;
        web.evaluateJavascript("setUser(" + lastLocation.getLatitude() + ", "
                + lastLocation.getLongitude() + ", " + heading + ");", null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 3 && hasLocationPermission()) {
            startLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocation();
        if (rotationSensor != null) {
            sensorManager.registerListener(sensorListener, rotationSensor,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        sensorManager.unregisterListener(sensorListener);
    }

    // ---------- Route and navigation ----------

    // The name shown to the user: what they searched, plus the building number
    private String displayName() {
        String base = selectedLabel.isEmpty() ? selectedPlace.name : selectedLabel;
        if (selectedBuilding != null) {
            return base + " (Bldg " + selectedBuilding.no + ")";
        }
        return base;
    }

    private void updateRoute() {
        if (selectedPlace == null) {
            return;
        }
        if (lastLocation == null) {
            infoText.setText(displayName() + ": finding your location...\nKeep GPS on and wait a few seconds");
            return;
        }

        GeoPoint me = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
        GeoPoint dest = new GeoPoint(selectedPlace.lat, selectedPlace.lon);

        // Joint building: go through the other building first (for example AIML via CSE)
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

        // A building with a back gate: use it when the user is closer to it than to the front route
        double[] gate = backGateOf.get(selectedPlace.name);
        if (gate != null && via != null) {
            GeoPoint backGate = new GeoPoint(gate[0], gate[1]);
            if (me.distanceToAsDouble(backGate) < me.distanceToAsDouble(via)) {
                via = backGate;
            }
        }
        GeoPoint pathEnd = (via != null) ? via : dest;

        int startNode = graph.nearestNode(me);
        int endNode = graph.nearestNode(pathEnd);
        List<Integer> nodePath = graph.shortestPath(startNode, endNode);

        if (nodePath.isEmpty()) {
            infoText.setText("No walkway connection found to " + displayName());
            if (pageLoaded) {
                web.evaluateJavascript("clearRoute();", null);
            }
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

        // Drop path points we have already passed
        while (points.size() > 3 && points.get(0).distanceToAsDouble(points.get(2))
                < points.get(1).distanceToAsDouble(points.get(2))) {
            points.remove(1);
        }

        double meters = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            meters += points.get(i).distanceToAsDouble(points.get(i + 1));
        }

        if (meters < 15) {
            infoText.setText("You have arrived at " + displayName());
        } else {
            String distance = (meters < 1000)
                    ? Math.round(meters) + " m"
                    : String.format("%.2f km", meters / 1000);
            int minutes = (int) Math.max(1, Math.ceil(meters / 84.0)); // about 5 km/h
            infoText.setText(displayName() + ": " + distance + " | " + minutes + " min walk");
        }

        sendRouteToPage(points);

        // First route after choosing a place: show the whole route as a preview.
        // Tracking starts only when the user presses Start.
        if (justSelected) {
            justSelected = false;
            following = false;
            updateStartButton();
            if (pageLoaded) {
                web.evaluateJavascript("fitRoute();", null);
            }
        }

        if (following && pageLoaded && meters >= 15) {
            moveCameraAlongRoute(me, points);
        }
    }

    // Puts the camera behind the user with the route pointing up the screen
    private void moveCameraAlongRoute(GeoPoint me, List<GeoPoint> points) {
        // Look ahead to the first route point that is at least 15 m away,
        // so the map starts turning just before a corner
        GeoPoint target = points.get(points.size() - 1);
        for (int i = 1; i < points.size(); i++) {
            if (me.distanceToAsDouble(points.get(i)) >= 15) {
                target = points.get(i);
                break;
            }
        }

        double b = bearingBetween(me, target);
        if (Double.isNaN(camBearing) || angleDiff(b, camBearing) > 6) {
            camBearing = b;
        }

        double zoom = zoomNext ? 18.6 : -1;
        zoomNext = false;
        web.evaluateJavascript("moveCamera(" + me.getLatitude() + ", " + me.getLongitude()
                + ", " + camBearing + ", 60, " + zoom + ");", null);
    }

    private double bearingBetween(GeoPoint a, GeoPoint b) {
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private double angleDiff(double a, double b) {
        double d = Math.abs(a - b) % 360;
        return d > 180 ? 360 - d : d;
    }

    private void sendRouteToPage(List<GeoPoint> points) {
        if (!pageLoaded) {
            return;
        }
        JSONArray coords = new JSONArray();
        try {
            for (GeoPoint p : points) {
                JSONArray c = new JSONArray();
                c.put(p.getLongitude());
                c.put(p.getLatitude());
                coords.put(c);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        web.evaluateJavascript("setRoute(" + coords + ");", null);
    }

    // Start / Stop button on the bottom card
    private void onStartPressed() {
        if (selectedPlace == null) {
            Toast.makeText(this, selectedBuilding != null
                            ? "This building is not placed on the map yet"
                            : "Search or tap a place first",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        drawer.closeDrawer(GravityCompat.START);
        if (following) {
            // Stop tracking and show the whole route again
            following = false;
            updateStartButton();
            if (pageLoaded) {
                web.evaluateJavascript("fitRoute();", null);
            }
            return;
        }
        if (!ensureLocation()) {
            Toast.makeText(this, "Waiting for your location...", Toast.LENGTH_SHORT).show();
            return;
        }
        // Zoom to the user and follow them along the route
        following = true;
        zoomNext = true;
        camBearing = Double.NaN;
        updateStartButton();
        updateRoute();
    }

    private void updateStartButton() {
        btnStart.setText(following ? "\u25A0  Stop" : "\u25B6  Start");
    }

    // Clears the chosen place and the route (the X button on the bottom card)
    private void clearSelection() {
        selectedPlace = null;
        selectedBuilding = null;
        selectedLabel = "";
        highlightDept = null;
        following = false;
        justSelected = false;
        updateStartButton();
        btnStart.setVisibility(View.GONE);
        infoText.setText("Search or tap a place");
        showBuildingList();

        AutoCompleteTextView box = findViewById(R.id.searchBox);
        box.setText("", false);

        if (pageLoaded) {
            web.evaluateJavascript("clearSelection();", null);
        }
    }

    // ---------- Search and selecting a place ----------

    private void setupSearch() {
        AutoCompleteTextView searchBox = findViewById(R.id.searchBox);

        // Building names plus the department names inside buildings
        List<String> names = new ArrayList<>();
        for (Place p : places) {
            names.add(p.name);
        }
        for (String alias : aliasOf.keySet()) {
            if (!names.contains(alias)) {
                names.add(alias);
            }
        }

        // Building numbers and every department from the key map
        for (String n : CampusBuildings.searchNames()) {
            if (!names.contains(n)) {
                names.add(n);
            }
        }

        searchBox.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names));

        searchBox.setOnItemClickListener((parent, view, position, id) -> {
            String chosen = (String) parent.getItemAtPosition(position);
            selectByName(chosen);
            searchBox.clearFocus();
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        });
    }

    // Works out which building / place a search name means, then selects it.
    // The name can be a department, a building number, a building name or a place.
    private void selectByName(String name) {
        String label = name;
        String placeName;
        CampusBuildings.Building b = CampusBuildings.forDept(name);
        int number = CampusBuildings.parseBuildingNumber(name);

        if (b != null) {
            placeName = CampusBuildings.placeForDept(name, b);
        } else if (number > 0 && CampusBuildings.byNumber(number) != null) {
            b = CampusBuildings.byNumber(number);
            label = b.title;
            placeName = b.anchor();
        } else if (CampusBuildings.forTitle(name) != null) {
            b = CampusBuildings.forTitle(name);
            label = b.title;
            placeName = b.anchor();
        } else {
            placeName = aliasOf.containsKey(name) ? aliasOf.get(name) : name;
            b = CampusBuildings.forPlaceName(placeName);
        }
        chooseDestination(b, label, placeName);
    }

    private Place findPlace(String name) {
        if (name == null) {
            return null;
        }
        for (Place p : places) {
            if (p.name.equals(name)) {
                return p;
            }
        }
        return null;
    }

    // Marks the destination: label above the pin, route preview, building details in the panel
    private void chooseDestination(CampusBuildings.Building b, String label, String placeName) {
        Place found = findPlace(placeName);
        if (found == null && b != null) {
            found = findPlace(b.anchor());
        }
        if (found == null && b == null) {
            return; // nothing we know about
        }

        selectedBuilding = b;
        selectedPlace = found;
        selectedLabel = label;
        following = false;
        justSelected = true;
        updateStartButton();

        // The Start button only appears once a place on the map is chosen
        btnStart.setVisibility(found != null ? View.VISIBLE : View.GONE);

        // Is the label a department inside the building? Then highlight it in the panel
        highlightDept = null;
        if (b != null) {
            for (String dept : b.depts) {
                if (dept.equalsIgnoreCase(label)) {
                    highlightDept = dept;
                }
            }
            showBuildingDetail(b, highlightDept);
        } else {
            showBuildingList();
        }

        if (found == null) {
            // On the key map, but it has no GPS position yet
            infoText.setText(label + " (Bldg " + b.no + "): not placed on the map yet");
            if (pageLoaded) {
                web.evaluateJavascript("clearSelection();", null);
            }
            drawer.openDrawer(GravityCompat.START);
            return;
        }

        infoText.setText(displayName());
        if (lastLocation == null) {
            startLocation();
        }
        boolean flyToPlace = (lastLocation == null);
        if (pageLoaded) {
            web.evaluateJavascript("selectPoint(" + found.lat + ", " + found.lon + ", "
                    + JSONObject.quote(label) + ", " + flyToPlace + ");", null);
        }
        updateRoute();
    }

    // ---------- Side panel: buildings and their departments ----------

    private void setupDrawer() {
        drawer = findViewById(R.id.drawerLayout);
        drawerBadge = findViewById(R.id.drawerBadge);
        drawerTitle = findViewById(R.id.drawerTitle);
        drawerSub = findViewById(R.id.drawerSub);
        drawerBack = findViewById(R.id.drawerBack);
        drawerContent = findViewById(R.id.drawerContent);
        btnNavigate = findViewById(R.id.btnNavigate);

        // Keep the map visible behind the panel
        drawer.setScrimColor(Color.TRANSPARENT);

        // Keep the panel header clear of the status bar
        View header = findViewById(R.id.drawerHeader);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        findViewById(R.id.btnBuildings).setOnClickListener(v -> drawer.openDrawer(GravityCompat.START));
        findViewById(R.id.btnInfo).setOnClickListener(v -> drawer.openDrawer(GravityCompat.START));
        drawerBack.setOnClickListener(v -> showBuildingList());
        btnNavigate.setOnClickListener(v -> {
            drawer.closeDrawer(GravityCompat.START);
            if (selectedPlace != null) {
                justSelected = true;
                updateRoute();
            }
        });

        // Back button closes the panel first
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        showBuildingList();
    }

    private boolean isMapped(CampusBuildings.Building b) {
        return b.anchor() != null && findPlace(b.anchor()) != null;
    }

    // All buildings, in number order, with what is inside each one
    private void showBuildingList() {
        drawerBadge.setText("🏢");
        drawerTitle.setText("Campus Buildings");
        drawerSub.setText(CampusBuildings.all().size() + " buildings  •  tap one for details");
        drawerBack.setVisibility(View.GONE);
        btnNavigate.setVisibility(View.GONE);
        drawerContent.removeAllViews();

        for (CampusBuildings.Building b : CampusBuildings.all()) {
            View row = getLayoutInflater().inflate(R.layout.item_building_row, drawerContent, false);
            ((TextView) row.findViewById(R.id.rowNumber)).setText(String.valueOf(b.no));
            ((TextView) row.findViewById(R.id.rowTitle)).setText(b.title);
            ((TextView) row.findViewById(R.id.rowSub)).setText(
                    b.depts.length == 0 ? "Nothing listed yet" : TextUtils.join(", ", b.depts));
            row.setOnClickListener(v -> chooseDestination(b, b.title, b.anchor()));
            drawerContent.addView(row);
        }
    }

    // One building: its number and everything inside it
    private void showBuildingDetail(CampusBuildings.Building b, String dept) {
        drawerBadge.setText(String.valueOf(b.no));
        drawerTitle.setText(b.title);
        int n = b.depts.length;
        drawerSub.setText(n + (n == 1 ? " place inside" : " places inside"));
        drawerBack.setVisibility(View.VISIBLE);
        btnNavigate.setVisibility(isMapped(b) ? View.VISIBLE : View.GONE);
        drawerContent.removeAllViews();

        addSection("INSIDE THIS BUILDING");
        if (n == 0) {
            addInfoLine("Nothing listed yet", "");
        }
        for (String name : b.depts) {
            TextView row = (TextView) getLayoutInflater()
                    .inflate(R.layout.item_dept_row, drawerContent, false);
            boolean match = name.equals(dept);
            row.setText((match ? "📍  " : "•  ") + name);
            if (match) {
                row.setBackgroundResource(R.drawable.bg_icon_blue);
                row.setTypeface(row.getTypeface(), android.graphics.Typeface.BOLD);
            }
            row.setOnClickListener(v -> chooseDestination(b, name, CampusBuildings.placeForDept(name, b)));
            drawerContent.addView(row);
        }

        if (!isMapped(b)) {
            addSection("NOTE");
            addInfoLine("Not placed on the map yet", "");
        }
        if (!b.info.isEmpty()) {
            addSection("MORE");
            addInfoLine(b.info, "");
        }
    }
    private void addSection(String title) {
        TextView t = (TextView) getLayoutInflater()
                .inflate(R.layout.item_section_title, drawerContent, false);
        t.setText(title);
        drawerContent.addView(t);
    }

    private void addInfoLine(String label, String value) {
        TextView t = (TextView) getLayoutInflater()
                .inflate(R.layout.item_dept_row, drawerContent, false);
        t.setText(value.isEmpty() ? label : label + ":  " + value);
        t.setClickable(false);
        t.setFocusable(false);
        drawerContent.addView(t);
    }

    // The page calls these through the "Android" bridge
    public class Bridge {
        @JavascriptInterface
        public void onPlaceTapped(String name) {
            runOnUiThread(() -> selectByName(name));
        }

        @JavascriptInterface
        public void onBuildingTapped(String number) {
            runOnUiThread(() -> {
                int no;
                try {
                    no = Integer.parseInt(number);
                } catch (NumberFormatException e) {
                    return;
                }
                CampusBuildings.Building b = CampusBuildings.byNumber(no);
                if (b != null) {
                    chooseDestination(b, b.title, b.anchor());
                    drawer.openDrawer(GravityCompat.START);
                }
            });
        }

        @JavascriptInterface
        public void onMapLongPress(double lat, double lon) {
            // Handy for adding new buildings: copies the position of the spot you pressed
            runOnUiThread(() -> {
                String text = lat + ", " + lon;
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("position", text));
                }
                Toast.makeText(Campus3DActivity.this, "Copied: " + text, Toast.LENGTH_LONG).show();
            });
        }

        @JavascriptInterface
        public void onUserMoved() {
            // The user dragged the map, so stop following
            runOnUiThread(() -> {
                following = false;
                updateStartButton();
            });
        }

        @JavascriptInterface
        public void onRecenter() {
            // The user pressed the target button, so follow again
            runOnUiThread(() -> {
                if (!ensureLocation()) {
                    Toast.makeText(Campus3DActivity.this,
                            "Finding your location... make sure GPS is on",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedPlace != null) {
                    following = true;
                    zoomNext = true;
                    camBearing = Double.NaN;
                    updateStartButton();
                    updateRoute();
                }
            });
        }
    }

    // ---------- Helpers ----------

    // Places that are NOT part of a numbered building (gates, ground, temple...)
    private String buildPlacesJson() {
        JSONArray array = new JSONArray();
        try {
            for (Place p : places) {
                if (CampusBuildings.forPlaceName(p.name) != null) {
                    continue;
                }
                JSONObject obj = new JSONObject();
                obj.put("name", p.name);
                obj.put("lat", p.lat);
                obj.put("lon", p.lon);
                array.put(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return array.toString();
    }

    // One numbered pin per building that has a position
    private String buildBuildingsJson() {
        JSONArray array = new JSONArray();
        try {
            for (CampusBuildings.Building b : CampusBuildings.all()) {
                Place anchor = findPlace(b.anchor());
                if (anchor == null) {
                    continue;
                }
                JSONObject obj = new JSONObject();
                obj.put("no", b.no);
                obj.put("name", b.title);
                obj.put("lat", anchor.lat);
                obj.put("lon", anchor.lon);
                array.put(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return array.toString();
    }

    private String readAsset(String name) throws IOException {
        InputStream in = getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        in.close();
        return out.toString("UTF-8");
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }
}