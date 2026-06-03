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
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.model.Schedule;
import com.example.pinit.model.Trip;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.example.pinit.database.FirestoreRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TripDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_ADD_SCHEDULE = 100;
    private static final int REQUEST_EDIT_SCHEDULE = 101;

    private DatabaseHelper dbHelper;
    private int tripId;
    private Trip trip;
    private GoogleMap googleMap;
    private String selectedDate;
    private List<String> dateList = new ArrayList<>();
    private LinearLayout dateTabs;
    private RecyclerView rvSchedule;
    private View layoutEmpty;
    private ScheduleDetailAdapter scheduleAdapter;
    private List<Schedule> currentSchedules = new ArrayList<>();

    private static final String API_KEY = com.example.pinit.database.PlacesApiHelper.API_KEY;

    // ========== 콜백 인터페이스 ==========
    interface GeocodeCallback { void onResult(LatLng latLng); }
    interface AddressCallback { void onResult(String address); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new DatabaseHelper(this);
        tripId = getIntent().getIntExtra("trip_id", -1);

        dateTabs = findViewById(R.id.dateTabs);
        rvSchedule = findViewById(R.id.rvSchedule);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        scheduleAdapter = new ScheduleDetailAdapter(
                this,
                new ArrayList<>(),

                // 지도 열기
                schedule -> {

                    if (schedule.getPlaceName() != null &&
                            !schedule.getPlaceName().isEmpty()) {

                        Uri webUri = Uri.parse(
                                "https://maps.google.com/?q="
                                        + Uri.encode(schedule.getPlaceName()));

                        startActivity(
                                new Intent(Intent.ACTION_VIEW, webUri));
                    }
                },

                // 삭제
                id -> {

                    dbHelper.deleteSchedule(id);

                    buildDateTabs();

                    loadSchedulesForDate(selectedDate);
                },

                // 수정
                schedule -> {
                    Intent intent = new Intent(this, AddScheduleActivity.class);
                    intent.putExtra("schedule_id", schedule.getId());
                    intent.putExtra("schedule_title", schedule.getTitle());
                    intent.putExtra("schedule_date", schedule.getDate());
                    intent.putExtra("schedule_time", schedule.getTime());
                    intent.putExtra("schedule_place", schedule.getPlaceName());
                    intent.putExtra("schedule_memo", schedule.getMemo());
                    startActivityForResult(intent, REQUEST_EDIT_SCHEDULE);
                }
        );
        rvSchedule.setAdapter(scheduleAdapter);

        findViewById(R.id.btnAddSchedule).setOnClickListener(v -> openAddSchedule());
        /*
        findViewById(R.id.btnUploadFirestore)
                .setOnClickListener(v -> {
                    uploadScheduleToFirestore();
                });
         */
        findViewById(R.id.btnAddScheduleEmpty).setOnClickListener(v -> openAddSchedule());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        loadTrip();
    }

    private void loadTrip() {
        trip = dbHelper.getTripById(tripId);
        if (trip == null) { finish(); return; }

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(trip.getTitle());
        ((TextView) findViewById(R.id.tvDestination)).setText("📍 " + trip.getDestination());
        ((TextView) findViewById(R.id.tvDate)).setText("📅 " + trip.getStartDate() + " - " + trip.getEndDate());

        dateList = generateDateList(trip.getStartDate(), trip.getEndDate());
        selectedDate = dateList.isEmpty() ? "" : dateList.get(0);

        buildDateTabs();
        loadSchedulesForDate(selectedDate);
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
            e.printStackTrace();
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
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(8);
            tab.setLayoutParams(params);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setPadding(20, 12, 20, 12);

            TextView tvDateLabel = new TextView(this);
            String displayDate = date;
            try { displayDate = displaySdf.format(inputSdf.parse(date)); } catch (ParseException ignored) {}

            List<Schedule> allSchedules = dbHelper.getSchedulesByTrip(tripId);
            int count = 0;
            for (Schedule s : allSchedules) {
                if (date.equals(s.getDate())) count++;
            }

            tvDateLabel.setText(displayDate + "\n" + count + "개 일정");
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
        if (date == null || date.isEmpty()) return;

        List<Schedule> allSchedules = dbHelper.getSchedulesByTrip(tripId);
        List<Schedule> filtered = new ArrayList<>();
        for (Schedule s : allSchedules) {
            if (date.equals(s.getDate())) filtered.add(s);
        }
        filtered.sort((a, b) -> {
            String ta = a.getTime() == null ? "" : a.getTime();
            String tb = b.getTime() == null ? "" : b.getTime();
            String[] pa = ta.split(":");
            String[] pb = tb.split(":");
            try {
                int ha = pa.length > 0 ? Integer.parseInt(pa[0]) : 0;
                int ma = pa.length > 1 ? Integer.parseInt(pa[1]) : 0;
                int hb = pb.length > 0 ? Integer.parseInt(pb[0]) : 0;
                int mb = pb.length > 1 ? Integer.parseInt(pb[1]) : 0;
                return ha != hb ? ha - hb : ma - mb;
            } catch (NumberFormatException e) {
                return ta.compareTo(tb);
            }
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

        if (googleMap != null) {
            showPinsForSchedules(filtered);
        }
    }

    // ========== 지도 핀 표시 ==========

    private final java.util.Map<String, LatLng> geocodeCache =
            new java.util.HashMap<>();

    private void showPinsForSchedules(List<Schedule> schedules) {

        if (googleMap == null) return;

        googleMap.clear();

        if (schedules.isEmpty()) return;

        int total = schedules.size();

        LatLng[] orderedPositions = new LatLng[total];

        int[] done = {0};

        for (int i = 0; i < total; i++) {

            Schedule s = schedules.get(i);

            String query =
                    (s.getPlaceName() != null && !s.getPlaceName().isEmpty())
                            ? s.getPlaceName()
                            : s.getTitle();

            if (query == null || query.isEmpty()) {

                done[0]++;

                if (done[0] == total) {
                    onAllGeocodeDone(orderedPositions);
                }

                continue;
            }

            final int index = i;

            final String markerTitle =
                    (i + 1) + ". " + s.getTitle();

            final String snippet = query;

            // =========================
            // cache 사용
            // =========================

            if (geocodeCache.containsKey(query)) {

                LatLng cached = geocodeCache.get(query);

                runOnUiThread(() -> {

                    if (cached != null && googleMap != null) {

                        orderedPositions[index] = cached;

                        googleMap.addMarker(
                                new MarkerOptions()
                                        .position(cached)
                                        .title(markerTitle)
                                        .snippet(snippet)
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_YELLOW))
                        );
                    }

                    done[0]++;

                    if (done[0] == total) {
                        onAllGeocodeDone(orderedPositions);
                    }
                });

            } else {

                geocode(query, latLng -> {

                    if (latLng != null) {
                        geocodeCache.put(query, latLng);
                    }

                    runOnUiThread(() -> {

                        if (latLng != null && googleMap != null) {

                            orderedPositions[index] = latLng;

                            googleMap.addMarker(
                                    new MarkerOptions()
                                            .position(latLng)
                                            .title(markerTitle)
                                            .snippet(snippet)
                                            .icon(BitmapDescriptorFactory.defaultMarker(
                                                    BitmapDescriptorFactory.HUE_YELLOW))
                            );
                        }

                        done[0]++;

                        if (done[0] == total) {
                            onAllGeocodeDone(orderedPositions);
                        }
                    });
                });
            }
        }
    }
    private void fitCameraToPins(List<LatLng> positions) {
        if (googleMap == null) return;
        if (positions.isEmpty()) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(37.5665, 126.9780), 10f));
            return;
        }
        if (positions.size() == 1) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(positions.get(0), 15f));
        } else {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng pos : positions) builder.include(pos);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
        }
    }

    private void onAllGeocodeDone(LatLng[] orderedPositions) {

        List<LatLng> validPositions = new ArrayList<>();

        for (LatLng pos : orderedPositions) {

            if (pos != null) {
                validPositions.add(pos);
            }
        }

        if (validPositions.size() >= 2) {

            PolylineOptions polylineOptions =
                    new PolylineOptions()
                            .addAll(validPositions)
                            .width(8f)
                            .color(Color.parseColor("#FF9800"));

            googleMap.addPolyline(polylineOptions);
        }

        fitCameraToPins(validPositions);
    }

    private void geocode(String address, GeocodeCallback callback) {
        new Thread(() -> {
            try {
                String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                        + java.net.URLEncoder.encode(address, "UTF-8")
                        + "&language=ko&key=" + API_KEY;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                JSONArray results = json.optJSONArray("results");
                if (results != null && results.length() > 0) {
                    JSONObject loc = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location");
                    callback.onResult(new LatLng(loc.getDouble("lat"), loc.getDouble("lng")));
                } else {
                    callback.onResult(null);
                }
            } catch (Exception e) {
                callback.onResult(null);
            }
        }).start();
    }

    // ========== 지도 탭 → 역지오코딩 → 일정 추가 ==========

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(37.5665, 126.9780), 10f));

        googleMap.setOnMapClickListener(latLng -> reverseGeocode(latLng, address ->
                runOnUiThread(() -> showMapTapDialog(latLng, address))));

        googleMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        loadSchedulesForDate(selectedDate);
    }

    // ★ 수정: AddressCallback 사용으로 타입 오류 해결
    private void reverseGeocode(LatLng latLng, AddressCallback callback) {
        new Thread(() -> {
            try {
                String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                        + latLng.latitude + "," + latLng.longitude
                        + "&language=ko&key=" + API_KEY;
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                JSONArray results = json.optJSONArray("results");
                if (results != null && results.length() > 0) {
                    callback.onResult(results.getJSONObject(0).optString("formatted_address", ""));
                } else {
                    callback.onResult("");
                }
            } catch (Exception e) {
                callback.onResult("");
            }
        }).start();
    }

    private void showMapTapDialog(LatLng latLng, String address) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 0);

        TextView tvAddress = new TextView(this);
        tvAddress.setText(address.isEmpty()
                ? String.format("%.5f, %.5f", latLng.latitude, latLng.longitude)
                : address);
        tvAddress.setTextSize(12f);
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
                .setNegativeButton("취소", null)
                .show();
    }

    private void addScheduleFromMap(String title, String time, String address, LatLng latLng) {
        Schedule s = new Schedule();
        s.setTripId(tripId);
        s.setTitle(title);
        s.setDate(selectedDate);
        s.setTime(time);
        s.setPlaceName(address.isEmpty()
                ? String.format("%.5f, %.5f", latLng.latitude, latLng.longitude)
                : address);
        s.setMemo("");
        s.setColor("#FFDA44");
        dbHelper.insertSchedule(s);

        if (googleMap != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .snippet(s.getPlaceName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        }

        buildDateTabs();
        loadSchedulesForDate(selectedDate);
        Toast.makeText(this, "'" + title + "' 일정이 추가되었습니다!", Toast.LENGTH_SHORT).show();
    }

    private void openAddSchedule() {
        Intent intent = new Intent(this, AddScheduleActivity.class);
        intent.putExtra("trip_id", tripId);
        intent.putExtra("default_date", selectedDate);
        startActivityForResult(intent, REQUEST_ADD_SCHEDULE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK &&
                (requestCode == REQUEST_ADD_SCHEDULE || requestCode == REQUEST_EDIT_SCHEDULE)) {
            buildDateTabs();
            loadSchedulesForDate(selectedDate);
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}