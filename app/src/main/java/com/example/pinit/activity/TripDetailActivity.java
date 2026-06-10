package com.example.pinit.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.adapter.ScheduleDetailAdapter;
import com.example.pinit.model.Schedule;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class TripDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_ADD_SCHEDULE = 100;
    private static final int REQUEST_EDIT_SCHEDULE = 101;

    private FirebaseFirestore db;
    private String firestoreDocId;
    private GoogleMap googleMap;
    private String selectedDate = "";
    private List<String> dateList = new ArrayList<>();
    private LinearLayout dateTabs;
    private RecyclerView rvSchedule;
    private View layoutEmpty;
    private ScheduleDetailAdapter scheduleAdapter;

    private Map<Integer, String> itemDocIdMap = new HashMap<>();
    private List<Schedule> currentSchedules = new ArrayList<>();

    private static final String API_KEY = com.example.pinit.database.PlacesApiHelper.API_KEY;

    interface AddressCallback { void onResult(String address); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        db = FirebaseFirestore.getInstance();
        firestoreDocId = getIntent().getStringExtra("firestore_doc_id");

        if (firestoreDocId == null) {
            int tripId = getIntent().getIntExtra("trip_id", -1);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && tripId != -1) {
                firestoreDocId = user.getUid() + "_" + tripId;
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dateTabs = findViewById(R.id.dateTabs);
        rvSchedule = findViewById(R.id.rvSchedule);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        scheduleAdapter = new ScheduleDetailAdapter(this, new ArrayList<>(),
                schedule -> {
                    if (schedule.getPlaceName() != null && !schedule.getPlaceName().isEmpty()) {
                        Uri webUri = Uri.parse("https://maps.google.com/?q=" + Uri.encode(schedule.getPlaceName()));
                        startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                    }
                },
                id -> {
                    String itemDocId = itemDocIdMap.get(id);
                    if (itemDocId != null && firestoreDocId != null && !selectedDate.isEmpty()) {
                        db.collection("schedules").document(firestoreDocId)
                                .collection("days").document(selectedDate)
                                .collection("items").document(itemDocId)
                                .delete().addOnSuccessListener(aVoid -> {
                                    buildDateTabs();
                                    loadSchedulesForDate(selectedDate);
                                });
                    }
                },
                schedule -> {
                    Intent intent = new Intent(this, AddScheduleActivity.class);
                    intent.putExtra("schedule_title", schedule.getTitle());
                    intent.putExtra("schedule_date", schedule.getDate());
                    intent.putExtra("schedule_time", schedule.getTime());
                    intent.putExtra("schedule_place", schedule.getPlaceName());
                    intent.putExtra("schedule_memo", schedule.getMemo());
                    intent.putExtra("schedule_latitude", schedule.getLatitude());
                    intent.putExtra("schedule_longitude", schedule.getLongitude());
                    intent.putExtra("schedule_color", schedule.getColor());
                    intent.putExtra("schedule_category", schedule.getCategory());
                    intent.putExtra("schedule_google_place_id", schedule.getGooglePlaceId());
                    startActivityForResult(intent, REQUEST_EDIT_SCHEDULE);
                }
        );
        rvSchedule.setAdapter(scheduleAdapter);

        findViewById(R.id.btnAddSchedule).setOnClickListener(v -> openAddSchedule());
        findViewById(R.id.btnAddScheduleEmpty).setOnClickListener(v -> openAddSchedule());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        loadTripFromFirebase();
    }

    private void loadTripFromFirebase() {
        if (firestoreDocId == null || firestoreDocId.isEmpty()) { finish(); return; }

        db.collection("schedules").document(firestoreDocId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) { finish(); return; }

            String title = doc.getString("title");
            String country = doc.getString("country");
            String startDate = doc.getString("startDate");
            String endDate = doc.getString("endDate");

            if (title == null || title.isEmpty()) title = "이름 없는 일정";
            if (country == null || country.isEmpty()) country = "미정";
            if (startDate == null || startDate.isEmpty()) startDate = "2026-06-10";
            if (endDate == null || endDate.isEmpty()) endDate = "2026-06-10";

            startDate = startDate.replace(".", "-").replaceAll(" ", "");
            endDate = endDate.replace(".", "-").replaceAll(" ", "");

            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
            ((TextView) findViewById(R.id.tvDestination)).setText("📍 " + country);
            ((TextView) findViewById(R.id.tvDate)).setText("📅 " + startDate + " - " + endDate);

            dateList = generateDateList(startDate, endDate);
            selectedDate = dateList.isEmpty() ? startDate : dateList.get(0);

            buildDateTabs();
            loadSchedulesForDate(selectedDate);
        }).addOnFailureListener(e -> finish());
    }

    private List<String> generateDateList(String startStr, String endStr) {
        List<String> dates = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date start = sdf.parse(startStr);
            Date end = sdf.parse(endStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            while (!cal.getTime().after(end)) {
                dates.add(sdf.format(cal.getTime()));
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (ParseException e) {
            dates.add(startStr);
        }
        return dates;
    }

    private void buildDateTabs() {
        dateTabs.removeAllViews();
        SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        SimpleDateFormat displaySdf = new SimpleDateFormat("M월 d일", Locale.KOREA);

        for (int i = 0; i < dateList.size(); i++) {
            String date = dateList.get(i);

            LinearLayout tab = new LinearLayout(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            tab.setLayoutParams(params);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setPadding(20, 12, 20, 12);

            TextView tvDateLabel = new TextView(this);
            String displayDate = date;
            try { displayDate = displaySdf.format(inputSdf.parse(date)); } catch (ParseException ignored) {}

            tvDateLabel.setText(displayDate + "\n일정 보기");
            tvDateLabel.setTextSize(12f);
            tvDateLabel.setGravity(android.view.Gravity.CENTER);
            tvDateLabel.setTextColor(Color.BLACK);
            tab.addView(tvDateLabel);
            applyDateTabStyle(tab, date.equals(selectedDate));

            tab.setOnClickListener(v -> {
                selectedDate = date;
                refreshDateTabStyles();
                loadSchedulesForDate(date);
            });

            dateTabs.addView(tab);
        }
    }

    private void applyDateTabStyle(LinearLayout tab, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12f);
        bg.setColor(selected ? Color.parseColor("#FFDA44") : Color.parseColor("#FFF3C3"));
        bg.setStroke(1, Color.parseColor("#DDDDDD"));
        tab.setBackground(bg);
    }

    private void refreshDateTabStyles() {
        for (int i = 0; i < dateTabs.getChildCount(); i++) {
            LinearLayout tab = (LinearLayout) dateTabs.getChildAt(i);
            applyDateTabStyle(tab, dateList.get(i).equals(selectedDate));
        }
    }

    private void loadSchedulesForDate(String date) {
        if (date == null || date.isEmpty() || firestoreDocId == null || firestoreDocId.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvSchedule.setVisibility(View.GONE);
            return;
        }

        db.collection("schedules").document(firestoreDocId)
                .collection("days").document(date)
                .collection("items").get().addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Schedule> filtered = new ArrayList<>();
                    itemDocIdMap.clear();

                    for (QueryDocumentSnapshot d : queryDocumentSnapshots) {
                        Schedule s = d.toObject(Schedule.class);
                        if (s != null) {
                            // 💡 초강력 방어막: 파이어베이스 null 필드를 전량 빈 공백문자("")로 수동 교체하여 어댑터 NPE 완전 차단!
                            if (s.getTitle() == null) s.setTitle("");
                            if (s.getPlaceName() == null) s.setPlaceName("");
                            if (s.getTime() == null) s.setTime("");
                            if (s.getMemo() == null) s.setMemo("");
                            if (s.getColor() == null) s.setColor("#FFDA44");
                            if (s.getCategory() == null) s.setCategory("");
                            if (s.getGooglePlaceId() == null) s.setGooglePlaceId("");
                            if (s.getDate() == null) s.setDate(date);

                            int localId = d.getId().hashCode();
                            s.setId(localId);
                            itemDocIdMap.put(localId, d.getId());
                            filtered.add(s);
                        }
                    }

                    // 💡 API 호환성 방어: 하위 버전 기기 크래시 차단을 위해 Collections.sort 문법 변경
                    Collections.sort(filtered, (a, b) -> {
                        String ta = a.getTime() == null ? "" : a.getTime();
                        String tb = b.getTime() == null ? "" : b.getTime();
                        return ta.compareTo(tb);
                    });

                    currentSchedules = filtered;

                    if (filtered.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvSchedule.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvSchedule.setVisibility(View.VISIBLE);
                    }

                    scheduleAdapter.updateList(filtered);
                    if (googleMap != null) showPinsForSchedules(filtered);
                }).addOnFailureListener(e -> {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvSchedule.setVisibility(View.GONE);
                });
    }

    private void showPinsForSchedules(List<Schedule> schedules) {
        if (googleMap == null) return;
        googleMap.clear();
        if (schedules.isEmpty()) return;

        List<LatLng> positions = new ArrayList<>();
        for (int i = 0; i < schedules.size(); i++) {
            Schedule s = schedules.get(i);
            if (s.getLatitude() == 0 && s.getLongitude() == 0) continue;

            LatLng pos = new LatLng(s.getLatitude(), s.getLongitude());
            positions.add(pos);

            googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title((i + 1) + ". " + (s.getTitle() != null ? s.getTitle() : ""))
                    .snippet(s.getPlaceName() != null ? s.getPlaceName() : "")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        }

        if (positions.size() >= 2) {
            googleMap.addPolyline(new PolylineOptions().addAll(positions).width(8f).color(Color.parseColor("#FF9800")));
        }
        fitCameraToPins(positions);
    }

    private void fitCameraToPins(List<LatLng> positions) {
        if (googleMap == null || positions.isEmpty()) return;
        if (positions.size() == 1) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(positions.get(0), 15f));
        } else {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng pos : positions) builder.include(pos);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnMapClickListener(latLng -> reverseGeocode(latLng, address ->
                runOnUiThread(() -> showMapTapDialog(latLng, address))));
        loadSchedulesForDate(selectedDate);
    }

    private void reverseGeocode(LatLng latLng, AddressCallback callback) {
        new Thread(() -> {
            try {
                String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                        + latLng.latitude + "," + latLng.longitude + "&language=ko&key=" + API_KEY;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                JSONObject json = new JSONObject(response.body().string());
                JSONArray results = json.optJSONArray("results");
                if (results != null && results.length() > 0) {
                    callback.onResult(results.getJSONObject(0).optString("formatted_address", ""));
                } else { callback.onResult(""); }
            } catch (Exception e) { callback.onResult(""); }
        }).start();
    }

    private void showMapTapDialog(LatLng latLng, String address) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 0);

        TextView tvAddress = new TextView(this);
        tvAddress.setText(address.isEmpty() ? String.format("%.5f, %.5f", latLng.latitude, latLng.longitude) : address);
        tvAddress.setTextColor(Color.GRAY);
        tvAddress.setPadding(0, 0, 0, 16);
        layout.addView(tvAddress);

        EditText etTitle = new EditText(this);
        etTitle.setHint("일정 제목 입력");
        layout.addView(etTitle);

        EditText etTime = new EditText(this);
        etTime.setHint("시간 (예: 09:00)");
        layout.addView(etTime);

        new AlertDialog.Builder(this)
                .setTitle("📍 이 위치에 일정 추가")
                .setView(layout)
                .setPositiveButton("추가", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "일정 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addScheduleFromMap(title, etTime.getText().toString().trim(), address, latLng);
                })
                .setNegativeButton("취소", null).show();
    }

    private void addScheduleFromMap(String title, String time, String address, LatLng latLng) {
        if (firestoreDocId == null || selectedDate == null || selectedDate.isEmpty()) return;

        Schedule s = new Schedule();
        s.setTitle(title);
        s.setDate(selectedDate);
        s.setTime(time);
        s.setPlaceName(address.isEmpty() ? String.format("%.5f, %.5f", latLng.latitude, latLng.longitude) : address);
        s.setColor("#FFDA44");
        s.setLatitude(latLng.latitude);
        s.setLongitude(latLng.longitude);

        DocumentReference dayRef = db.collection("schedules").document(firestoreDocId)
                .collection("days").document(selectedDate);

        Map<String, Object> dayData = new HashMap<>();
        dayData.put("dayNumber", 1);
        dayData.put("dayTitle", selectedDate);

        dayRef.set(dayData).addOnSuccessListener(aVoid -> {
            dayRef.collection("items").document().set(s).addOnSuccessListener(aVoid2 -> {
                buildDateTabs();
                loadSchedulesForDate(selectedDate);
                Toast.makeText(this, "'" + title + "' 일정이 추가되었습니다!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void openAddSchedule() {
        Intent intent = new Intent(this, AddScheduleActivity.class);
        intent.putExtra("default_date", selectedDate);
        startActivityForResult(intent, REQUEST_ADD_SCHEDULE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && (requestCode == REQUEST_ADD_SCHEDULE || requestCode == REQUEST_EDIT_SCHEDULE)) {
            buildDateTabs();
            loadSchedulesForDate(selectedDate);
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}