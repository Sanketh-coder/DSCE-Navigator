package com.example.dscenavigator;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CampusGraph {

    private final List<String> names = new ArrayList<>();
    private final List<GeoPoint> points = new ArrayList<>();
    private final List<List<Integer>> neighbors = new ArrayList<>();

    public int addNode(String name, double lat, double lon) {
        names.add(name);
        points.add(new GeoPoint(lat, lon));
        neighbors.add(new ArrayList<>());
        return names.size() - 1;
    }

    public String getName(int i) {
        return names.get(i);
    }

    public void connect(int a, int b) {
        neighbors.get(a).add(b);
        neighbors.get(b).add(a);
    }

    public int size() {
        return names.size();
    }

    public GeoPoint getPoint(int i) {
        return points.get(i);
    }

    public List<Integer> getNeighbors(int i) {
        return neighbors.get(i);
    }



    public int nearestNode(GeoPoint p) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < points.size(); i++) {
            double d = p.distanceToAsDouble(points.get(i));
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    // Dijkstra's shortest path. Returns an empty list if there is no route.
    public List<Integer> shortestPath(int from, int to) {
        int n = size();
        double[] dist = new double[n];
        int[] prev = new int[n];
        boolean[] done = new boolean[n];
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(prev, -1);
        dist[from] = 0;

        for (int step = 0; step < n; step++) {
            int u = -1;
            for (int i = 0; i < n; i++) {
                if (!done[i] && (u == -1 || dist[i] < dist[u])) {
                    u = i;
                }
            }
            if (u == -1 || dist[u] == Double.MAX_VALUE) {
                break;
            }
            done[u] = true;
            for (int v : neighbors.get(u)) {
                double w = points.get(u).distanceToAsDouble(points.get(v));
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    prev[v] = u;
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        if (dist[to] == Double.MAX_VALUE) {
            return path;
        }
        for (int at = to; at != -1; at = prev[at]) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }
}