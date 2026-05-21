package com.example.pinit.model;

import java.io.Serializable;
import java.util.List;

public class MyPlan implements Serializable {
    private String planId;
    private String title;
    private String date;
    private String country;

    private List<DailySchedule> schedules;

    public MyPlan(String planId, String title, String date, String country, List<DailySchedule> schedules) {
        this.planId = planId;
        this.title = title;
        this.date = date;
        this.country = country;
        this.schedules = schedules;
    }

    public String getPlanId() { return planId; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getCountry() { return country; }
    public List<DailySchedule> getSchedules() { return schedules; } // 일정 목록 꺼내기
}