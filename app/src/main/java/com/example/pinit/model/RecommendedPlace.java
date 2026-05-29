package com.example.pinit.model;

public class RecommendedPlace {

    private String name;

    private String category;

    private String address;

    private double rating;

    private String congestionLevel;

    private double score;

    private String detailScore;

    public RecommendedPlace(
            String name,
            String category,
            String address,
            double rating,
            String congestionLevel,
            double score,
            String detailScore
    ) {

        this.name = name;

        this.category = category;

        this.address = address;

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