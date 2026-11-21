package com.example.dscenavigator;




import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Campus3DActivity extends AppCompatActivity {

    private WebView web;
    private TextView infoText;
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
        places.addAll(CampusData.getPlaces());
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

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void startLocation() {
        if (!hasLocationPermission()) {
            return;
        }
        locationManager.removeUpdates(locationListener);

        String provider = null;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }

        if (provider == null) {
            Toast.makeText(this, "Turn on Location (GPS) on your phone",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Location last = locationManager.getLastKnownLocation(provider);
        if (last != null) {
            handleLocation(last);
        }
        locationManager.requestLocationUpdates(provider, 1000, 1, locationListener);
    }

    private void handleLocation(Location loc) {
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
                infoText.setText("Location found. Search or tap a place");
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

    // The name shown to the user: what they searched, plus the building if different
    private String displayName() {
        if (selectedLabel.isEmpty() || selectedLabel.equals(selectedPlace.name)) {
            return selectedPlace.name;
        }
        return selectedLabel + " (" + selectedPlace.name + ")";
    }

    private void updateRoute() {
        if (selectedPlace == null) {
            return;
        }
        if (lastLocation == null) {
            infoText.setText(displayName() + ": waiting for your location...");
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

        // First update after choosing a place: follow if we are on campus,
        // otherwise just show the whole route
        boolean onCampus = me.distanceToAsDouble(graph.getPoint(startNode)) < 300;
        if (justSelected) {
            justSelected = false;
            following = onCampus;
            zoomNext = true;
            camBearing = Double.NaN;
            if (!onCampus && pageLoaded) {
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

    // Marks a place as the destination, shows its name on the map, and draws the route.
    // The name can be a building or a department inside a building.
    private void selectByName(String name) {
        String placeName = aliasOf.containsKey(name) ? aliasOf.get(name) : name;

        Place found = null;
        for (Place p : places) {
            if (p.name.equals(placeName)) {
                found = p;
                break;
            }
        }
        if (found == null) {
            return;
        }

        selectedPlace = found;
        selectedLabel = name;
        infoText.setText(displayName());
        justSelected = true;

        // Without a location we can only fly to the place
        boolean flyToPlace = (lastLocation == null);
        web.evaluateJavascript("selectPlace(" + JSONObject.quote(selectedPlace.name)
                + ", " + flyToPlace + ", " + JSONObject.quote(selectedLabel) + ");", null);
        updateRoute();
    }

    // The page calls these through the "Android" bridge
    public class Bridge {
        @JavascriptInterface
        public void onPlaceTapped(String name) {
            runOnUiThread(() -> selectByName(name));
        }

        @JavascriptInterface
        public void onUserMoved() {
            // The user dragged the map, so stop following
            runOnUiThread(() -> following = false);
        }

        @JavascriptInterface
        public void onRecenter() {
            // The user pressed the target button, so follow again
            runOnUiThread(() -> {
                following = true;
                zoomNext = true;
                camBearing = Double.NaN;
                updateRoute();
            });
        }
    }

    // ---------- Helpers ----------

    private String buildPlacesJson() {
        JSONArray array = new JSONArray();
        try {
            for (Place p : places) {
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