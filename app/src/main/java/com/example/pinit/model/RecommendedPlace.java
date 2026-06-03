package com.example.pinit.model;

public class RecommendedPlace {

    private String placeId;

    private String name;

    private String category;

    private String address;

    private double latitude;

    private double longitude;

    private double rating;

    private String congestionLevel;

    private double score;

    private String detailScore;

    public RecommendedPlace(
            String placeId,
            String name,
            String category,
            String address,
            double latitude,
            double longitude,
            double rating,
            String congestionLevel,
            double score,
            String detailScore
    ) {
        this.placeId = placeId;

        this.name = name;

        this.category = category;

        this.address = address;

        this.latitude = latitude;

        this.longitude = longitude;

        this.rating = rating;

        this.congestionLevel = congestionLevel;

        this.score = score;

        this.detailScore = detailScore;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getAddress() {

        return address;
    }
    public String getPlaceId() {
        return placeId;
    }
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    public double getRating() {
        return rating;
    }

    public String getCongestionLevel() {
        return congestionLevel;
    }

    public double getScore() {
        return score;
    }

    public String getDetailScore() {

        return detailScore;
    }
}