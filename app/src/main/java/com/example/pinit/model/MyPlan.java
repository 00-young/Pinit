package com.example.pinit.model;

import java.io.Serializable;
import java.util.List;

public class MyPlan implements Serializable {
    private String title;
    private String date;
    private String country;
    private List<DailySchedule> schedules;

    public MyPlan() {
    }

    public MyPlan(String title, String date, String country, List<DailySchedule> schedules) {
        this.title = title;
        this.date = date;
        this.country = country;
        this.schedules = schedules;
    }

    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getCountry() { return country; }
    public List<DailySchedule> getSchedules() { return schedules; }

    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setCountry(String country) { this.country = country; }
    public void setSchedules(List<DailySchedule> schedules) { this.schedules = schedules; }
}