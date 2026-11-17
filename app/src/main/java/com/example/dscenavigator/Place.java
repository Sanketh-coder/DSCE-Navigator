package com.example.dscenavigator;

public class Place {
    public String name;
    public String details;
    public double lat;
    public double lon;

    public Place(String name, String details, double lat, double lon) {
        this.name = name;
        this.details = details;
        this.lat = lat;
        this.lon = lon;
    }
}