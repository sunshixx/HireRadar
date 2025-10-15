package com.example.demo.model;

import java.util.List;

public class NearbyResponse {
    private List<Place> items;
    private Meta meta;

    public NearbyResponse() {}

    public NearbyResponse(List<Place> items, Meta meta) {
        this.items = items;
        this.meta = meta;
    }

    public List<Place> getItems() { return items; }
    public void setItems(List<Place> items) { this.items = items; }

    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }

    public static class Meta {
        private double requestLat;
        private double requestLng;
        private int radius;
        private String provider;
        private String keyword;

        public Meta() {}

        public Meta(double requestLat, double requestLng, int radius, String provider, String keyword) {
            this.requestLat = requestLat;
            this.requestLng = requestLng;
            this.radius = radius;
            this.provider = provider;
            this.keyword = keyword;
        }

        public double getRequestLat() { return requestLat; }
        public void setRequestLat(double requestLat) { this.requestLat = requestLat; }

        public double getRequestLng() { return requestLng; }
        public void setRequestLng(double requestLng) { this.requestLng = requestLng; }

        public int getRadius() { return radius; }
        public void setRadius(int radius) { this.radius = radius; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
    }
}