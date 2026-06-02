package com.example.pinit.database;

import android.util.Log;

import com.example.pinit.model.Schedule;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreRepository {

    private static final String TAG = "FirestoreRepository";

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * schedules 컬렉션 업로드
     */
    public void uploadSchedule(
            String scheduleId,
            String userId,
            String title,
            String country,
            String city,
            int travelerCount,
            double totalBudget,
            String startDate,
            String endDate,
            double latitude,
            double longitude
    ) {

        Map<String, Object> schedule = new HashMap<>();

        schedule.put("userId", userId);
        schedule.put("title", title);
        schedule.put("country", country);
        schedule.put("city", city);
        schedule.put("latitude", latitude);
        schedule.put("longitude", longitude);
        schedule.put("travelerCount", travelerCount);
        schedule.put("totalBudget", totalBudget);
        schedule.put("startDate", startDate);
        schedule.put("endDate", endDate);

        // 서버 시간 대신 일단 timestamp
        schedule.put("createdAt",
                System.currentTimeMillis());

        db.collection("schedules")
                .document(scheduleId)
                .set(schedule)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG,
                            "Schedule 업로드 성공");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG,
                            "Schedule 업로드 실패", e);
                });
    }

    public void uploadBudget(
            String budgetId,
            long tripId,
            String title,
            double amount,
            String category,
            String date,
            String type,
            String memo
    ) {

        Map<String, Object> budget = new HashMap<>();

        budget.put("id", budgetId);
        budget.put("trip_id", tripId);

        budget.put("title", title);
        budget.put("amount", amount);
        budget.put("category", category);
        budget.put("date", date);
        budget.put("type", type);
        budget.put("memo", memo);

        db.collection("budgets")
                .document(budgetId)
                .set(budget)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG,
                            "Budget 업로드 성공");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG,
                            "Budget 업로드 실패", e);
                });

    }


    public void uploadDay(
            String scheduleId,
            String dayId,
            int dayNumber,
            String date
    ) {

        Map<String, Object> day = new HashMap<>();

        day.put("dayNumber", dayNumber);
        day.put("date", date);

        db.collection("schedules")
                .document(scheduleId)
                .collection("days")
                .document(dayId)
                .set(day);
    }

    public void uploadItem(
            String scheduleId,
            String dayId,
            String itemId,
            Schedule schedule
    ) {

        Map<String, Object> item = new HashMap<>();

        item.put("title", schedule.getTitle());
        item.put("time", schedule.getTime());

        // 현재 구조:
        // title = 장소명
        item.put("placeName", schedule.getTitle());

        item.put("memo", schedule.getMemo());
        item.put("color", schedule.getColor());

        // 현재 SQLite의 place_name은 주소 역할
        item.put("address", schedule.getPlaceName());

        item.put("latitude", schedule.getLatitude());
        item.put("longitude", schedule.getLongitude());

        item.put("category", schedule.getCategory());
        item.put("googlePlaceId", schedule.getGooglePlaceId());

        item.put("cost", 0);

        db.collection("schedules")
                .document(scheduleId)
                .collection("days")
                .document(dayId)
                .collection("items")
                .document(itemId)
                .set(item)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG,
                            "Item 업로드 성공");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG,
                            "Item 업로드 실패", e);
                });
    }

    public void deleteItem(
            String scheduleId,
            String dayId,
            String itemId
    ) {

        db.collection("schedules")
                .document(scheduleId)
                .collection("days")
                .document(dayId)
                .collection("items")
                .document(itemId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Item 삭제 성공");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Item 삭제 실패", e);
                });
    }

    public void updateSchedule(
            String scheduleId,
            String title,
            String startDate,
            String endDate,
            double totalBudget
    ) {

        Map<String, Object> updates = new HashMap<>();

        updates.put("title", title);
        updates.put("startDate", startDate);
        updates.put("endDate", endDate);
        updates.put("totalBudget", totalBudget);

        db.collection("schedules")
                .document(scheduleId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Schedule 수정 성공");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Schedule 수정 실패", e);
                });
    }

}