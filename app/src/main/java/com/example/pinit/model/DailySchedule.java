package com.example.pinit.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DailySchedule implements Serializable {
    private String dayTitle;
    private String date;
    private List<Schedule> scheduleObjects;

    public DailySchedule() {
    }
    public DailySchedule(String dayTitle, String date, List<Schedule> scheduleObjects) {
        this.dayTitle = dayTitle;
        this.date = date;
        this.scheduleObjects = scheduleObjects;
    }

    public String getDayTitle() { return dayTitle; }
    public String getDate() { return date; }
    public List<Schedule> getScheduleObjects() { return scheduleObjects; }

    public void setDayTitle(String dayTitle) {
        this.dayTitle = dayTitle;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setScheduleObjects(List<Schedule> scheduleObjects) {
        this.scheduleObjects = scheduleObjects;
    }

    // 지도를 그릴 장소 리스트만 뽑아주는 도우미 함수 (CreatePostFragment 오류 방지용)
    public List<String> getPlaces() {
        List<String> places = new ArrayList<>();
        for(Schedule s : scheduleObjects) {
            if(s.getPlaceName() != null && !s.getPlaceName().isEmpty()) {
                places.add(s.getPlaceName());
            } else {
                places.add(s.getTitle());
            }
        }
        return places;
    }
}