package com.example.demo.model;

import java.util.List;

public class Place {
    private String id;
    private String name;
    private String address;
    private double lat;
    private double lng;
    private double distance;
    private List<String> categories;
    private String source;
    private String url;

    public Place() {}

    public Place(String id, String name, String address, double lat, double lng,
                 double distance, List<String> categories, String source, String url) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.distance = distance;
        this.categories = categories;
        this.source = source;
        this.url = url;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}