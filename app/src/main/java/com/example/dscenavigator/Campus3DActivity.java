package com.example.dscenavigator;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Campus3DActivity extends AppCompatActivity {

    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus3d);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        web = findViewById(R.id.web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Send our place list to the page
                view.evaluateJavascript("setPlaces(" + buildPlacesJson() + ");", null);
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

    // Turns the places from CampusData into a JSON list for the page
    private String buildPlacesJson() {
        JSONArray array = new JSONArray();
        try {
            for (Place p : CampusData.getPlaces()) {
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