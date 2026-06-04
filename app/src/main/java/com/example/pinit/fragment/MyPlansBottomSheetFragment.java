package com.example.pinit.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.model.DailySchedule;
import com.example.pinit.model.MyPlan;
import com.example.pinit.model.Schedule;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 내 일정(여행) 목록 바텀시트 - Firestore 버전.
 *
 * Firestore 구조:
 *   schedules/{scheduleId}                여행 (title, city, country, startDate, endDate, userId ...)
 *     └ days/{날짜}                        날짜별 (date, dayNumber)
 *         └ items/{itemId}                 장소 (placeName, title, latitude, longitude, time, memo ...)
 *
 * 작성자(로그인 사용자)의 여행만 표시하고, 선택 시 좌표까지 담아 planResult 로 전달한다.
 */
public class MyPlansBottomSheetFragment extends BottomSheetDialogFragment {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView tvEmpty;
    private RecyclerView rv;
    private final List<DocumentSnapshot> tripDocs = new ArrayList<>();
    private TripAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_my_plans, container, false);

        rv = view.findViewById(R.id.recyclerViewMyPlans);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TripAdapter();
        rv.setAdapter(adapter);

        // 빈 목록 안내용 (레이아웃에 tvEmpty 가 없으면 null 이어도 무방)
        tvEmpty = view.findViewById(R.id.tvEmptyPlans);

        loadTrips();
        return view;
    }

    /** 작성자(로그인 사용자)의 여행 목록 조회 */
    private void loadTrips() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (uid.isEmpty()) {
            Toast.makeText(getContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("schedules")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    tripDocs.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        tripDocs.add(doc);
                    }
                    adapter.notifyDataSetChanged();
                    if (tvEmpty != null) {
                        tvEmpty.setVisibility(tripDocs.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "일정을 불러오지 못했습니다", Toast.LENGTH_SHORT).show());
    }

    /**
     * 선택한 여행의 days + items 를 모두 읽어 MyPlan 으로 만들고 planResult 로 전달.
     * days 는 날짜순(문서 ID 가 "2026-05-27" 형태라 정렬 가능),
     * items 는 time 순으로 정렬한다.
     */
    private void selectTrip(DocumentSnapshot tripDoc) {
        final String title = getString(tripDoc, "title", "제목 없음");
        final String startDate = getString(tripDoc, "startDate", "");
        final String endDate = getString(tripDoc, "endDate", "");
        final String country = getString(tripDoc, "country", "");

        tripDoc.getReference().collection("days")
                .orderBy("dayNumber", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(daySnap -> {
                    List<DocumentSnapshot> dayDocs = new ArrayList<>(daySnap.getDocuments());
                    if (dayDocs.isEmpty()) {
                        Toast.makeText(getContext(), "일정 내용이 비어있습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 각 day 의 items 를 비동기로 모두 읽어야 하므로 카운터로 완료를 기다린다
                    final DailySchedule[] results = new DailySchedule[dayDocs.size()];
                    final int[] remaining = { dayDocs.size() };

                    for (int i = 0; i < dayDocs.size(); i++) {
                        final int index = i;
                        DocumentSnapshot dayDoc = dayDocs.get(i);
                        final String date = getString(dayDoc, "date", dayDoc.getId());
                        final long dayNumber = dayDoc.getLong("dayNumber") != null ? dayDoc.getLong("dayNumber") : (index + 1);

                        dayDoc.getReference().collection("items")
                                .get()
                                .addOnSuccessListener(itemSnap -> {
                                    List<Schedule> schedules = new ArrayList<>();
                                    for (QueryDocumentSnapshot itemDoc : itemSnap) {
                                        schedules.add(toSchedule(itemDoc));
                                    }
                                    sortByTime(schedules);

                                    results[index] = new DailySchedule(
                                            "DAY " + dayNumber, date, schedules);

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        finishAndSend(title, startDate, endDate, country, results);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // 실패해도 빈 day 로 채워 진행 (전체가 멈추지 않도록)
                                    results[index] = new DailySchedule(
                                            "DAY " + dayNumber, date, new ArrayList<>());
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        finishAndSend(title, startDate, endDate, country, results);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "일정을 불러오지 못했습니다", Toast.LENGTH_SHORT).show());
    }

    /** 결과를 MyPlan 으로 묶어 CreatePostFragment 로 전달 */
    private void finishAndSend(String title, String startDate, String endDate,
                               String country, DailySchedule[] results) {
        List<DailySchedule> dailySchedules = new ArrayList<>();
        for (DailySchedule d : results) {
            if (d != null) dailySchedules.add(d);
        }

        String dateRange = startDate + " ~ " + endDate;
        MyPlan plan = new MyPlan(title, dateRange, country, dailySchedules);

        Bundle bundle = new Bundle();
        bundle.putSerializable("selectedPlan", plan);
        getParentFragmentManager().setFragmentResult("planResult", bundle);
        dismiss();
    }

    /** Firestore item 문서 -> Schedule (좌표 포함) */
    private Schedule toSchedule(DocumentSnapshot doc) {
        Schedule s = new Schedule();
        s.setTitle(getString(doc, "title", ""));
        s.setPlaceName(getString(doc, "placeName", ""));
        s.setDate(getString(doc, "date", ""));
        s.setTime(getString(doc, "time", ""));
        s.setMemo(getString(doc, "memo", ""));
        s.setGooglePlaceId(getString(doc, "googlePlaceId", ""));
        Double lat = doc.getDouble("latitude");
        Double lng = doc.getDouble("longitude");
        s.setLatitude(lat != null ? lat : 0.0);
        s.setLongitude(lng != null ? lng : 0.0);
        return s;
    }

    /** items 를 time(예: "9:00") 순으로 정렬 */
    private void sortByTime(List<Schedule> list) {
        list.sort((a, b) -> {
            int[] ta = parseTime(a.getTime());
            int[] tb = parseTime(b.getTime());
            return ta[0] != tb[0] ? ta[0] - tb[0] : ta[1] - tb[1];
        });
    }

    private int[] parseTime(String time) {
        if (time == null || time.trim().isEmpty()) return new int[]{0, 0};
        try {
            String[] p = time.trim().split(":");
            int h = p.length > 0 ? Integer.parseInt(p[0].trim()) : 0;
            int m = p.length > 1 ? Integer.parseInt(p[1].trim()) : 0;
            return new int[]{h, m};
        } catch (NumberFormatException e) {
            return new int[]{0, 0};
        }
    }

    private String getString(DocumentSnapshot doc, String key, String def) {
        String v = doc.getString(key);
        return v != null ? v : def;
    }

    // =====================================================
    // 여행 목록 어댑터
    // =====================================================
    private class TripAdapter extends RecyclerView.Adapter<PlanViewHolder> {
        @NonNull
        @Override
        public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new PlanViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
            DocumentSnapshot trip = tripDocs.get(position);
            String title = getString(trip, "title", "제목 없음");
            String start = getString(trip, "startDate", "");
            String end = getString(trip, "endDate", "");
            holder.textView.setText(title + "\n(" + start + " ~ " + end + ")");
            holder.itemView.setOnClickListener(v -> selectTrip(trip));
        }

        @Override
        public int getItemCount() { return tripDocs.size(); }
    }

    private static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}