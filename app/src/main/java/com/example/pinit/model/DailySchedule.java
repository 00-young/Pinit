package com.example.pinit.model;

import java.io.Serializable;
import java.util.List;

// 💡 Serializable: 데이터를 뭉치로 묶어서 다른 화면으로 던져줄 수 있게 해주는 마법의 단어입니다.
public class DailySchedule implements Serializable {
    private String dayTitle; // 예: "DAY 1"
    private String date; // 예: "5월 1일"
    private List<String> places; // 예: ["상하이 공항", "호텔", "디즈니랜드"]

    public DailySchedule(String dayTitle, String date, List<String> places) {
        this.dayTitle = dayTitle;
        this.date = date;
        this.places = places;
    }

    public String getDayTitle() { return dayTitle; }
    public String getDate() { return date; }
    public List<String> getPlaces() { return places; }
}