package com.example.pinit.model;

public class Schedule {
    private int id;
    private int tripId;
    private String title;
    private String date;
    private String time;
    private String placeName;
    private String memo;
    private String color;

    public Schedule() {}
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTripId() { return tripId; }
    public void setTripId(int tripId) { this.tripId = tripId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
