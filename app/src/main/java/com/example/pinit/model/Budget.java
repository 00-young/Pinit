package com.example.pinit.model;

public class Budget {
    private int id;
    private int tripId;
    private String title;
    private double amount;
    private String category;
    private String date;
    private String type; // income / expense
    private String memo;

    public Budget() {}
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTripId() { return tripId; }
    public void setTripId(int tripId) { this.tripId = tripId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
