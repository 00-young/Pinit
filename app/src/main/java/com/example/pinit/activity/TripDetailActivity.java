package com.example.pinit.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.example.pinit.activity.TripRecordActivity;
import com.example.pinit.adapter.RecordAdapter;
import com.example.pinit.adapter.ScheduleDetailAdapter;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.model.Record;
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

// [화면] 여행 상세 화면 - 날짜 탭별 일정 관리 + 지도 + 여행 기록
// 주요 기능:
//   1) 날짜 탭 - 여행 기간 날짜 목록으로 탭 생성, 탭 선택 시 해당 날짜 일정 필터링
//   2) 일정 목록 - ScheduleDetailAdapter로 표시, 장소 클릭 → Google Maps, 롱클릭 삭제, 클릭 → 수정
//   3) 지도 핀 - 선택된 날짜 일정의 장소명을 Geocoding API로 좌표 변환 후 노란 핀 표시
//   4) 지도 탭 - 지도 클릭 → Reverse Geocoding → 일정 추가 다이얼로그
//   5) 여행 기록 - RecordAdapter로 표시, 추가/수정/삭제
public class TripDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_ADD_SCHEDULE = 100;
    private static final int REQUEST_ADD_RECORD = 200;
    private static final int REQUEST_EDIT_RECORD = 300;

    private DatabaseHelper dbHelper;
    private int tripId;
    private Trip trip;
    private GoogleMap googleMap;
    private String selectedDate;          // 현재 선택된 날짜 탭
    private List<String> dateList = new ArrayList<>();
    private LinearLayout dateTabs;        // 날짜 탭 컨테이너 (HorizontalScrollView 안)
    private RecyclerView rvSchedule;
    private View layoutEmpty;             // 일정이 없을 때 표시되는 빈 화면
    private ScheduleDetailAdapter scheduleAdapter;
    private RecordAdapter recordAdapter;
    private RecyclerView rvRecord;
    private TextView tvRecordEmpty;
    private List<Schedule> currentSchedules = new ArrayList<>();

    private static final String API_KEY = com.example.pinit.database.PlacesApiHelper.API_KEY;

    // 비동기 콜백 인터페이스 (Geocoding/ReverseGeocoding 결과 수신용)
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
        // ScheduleDetailAdapter: 지도 열기 / 삭제 / 수정 3가지 콜백 처리
        scheduleAdapter = new ScheduleDetailAdapter(this, new ArrayList<>(), schedule -> {
            // 장소 클릭 → Google Maps로 이동
            if (schedule.getPlaceName() != null && !schedule.getPlaceName().isEmpty()) {
                Uri webUri = Uri.parse("https://maps.google.com/?q=" + Uri.encode(schedule.getPlaceName()));
                startActivity(new Intent(Intent.ACTION_VIEW, webUri));
            }
        }, id -> {
            // 삭제 콜백
            dbHelper.deleteSchedule(id);
            buildDateTabs();
            loadSchedulesForDate(selectedDate);
        }, schedule -> {
            // 수정 콜백 → AddScheduleActivity로 이동
            Intent intent = new Intent(this, AddScheduleActivity.class);
            intent.putExtra("trip_id", tripId);
            intent.putExtra("schedule_id", schedule.getId());
            startActivityForResult(intent, REQUEST_ADD_SCHEDULE);
        });
        rvSchedule.setAdapter(scheduleAdapter);

        findViewById(R.id.btnAddSchedule).setOnClickListener(v -> openAddSchedule());
        findViewById(R.id.btnAddScheduleEmpty).setOnClickListener(v -> openAddSchedule());

        // 여행 기록 RecyclerView 초기화
        rvRecord = findViewById(R.id.rvRecord);
        tvRecordEmpty = findViewById(R.id.tvRecordEmpty);
        rvRecord.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new RecordAdapter(this, new ArrayList<>(),
                record -> {
                    // 클릭 → 기록 수정
                    Intent intent = new Intent(this, TripRecordActivity.class);
                    intent.putExtra("trip_id", tripId);
                    intent.putExtra("record_id", record.getId());
                    startActivityForResult(intent, REQUEST_EDIT_RECORD);
                },
                id -> {
                    // 롱클릭 → 삭제 확인 다이얼로그
                    new AlertDialog.Builder(this)
                            .setTitle("기록 삭제")
                            .setMessage("이 기록을 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (d, w) -> {
                                dbHelper.deleteRecord(id);
                                loadRecords();
                            })
                            .setNegativeButton("취소", null).show();
                });
        rvRecord.setAdapter(recordAdapter);

        findViewById(R.id.btnAddRecord).setOnClickListener(v -> {
            Intent intent = new Intent(this, TripRecordActivity.class);
            intent.putExtra("trip_id", tripId);
            startActivityForResult(intent, REQUEST_ADD_RECORD);
        });

        // 지도 프래그먼트 비동기 초기화
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        loadTrip();
        loadRecords();
    }

    // 여행 기록 목록 로드 및 빈 상태 처리
    private void loadRecords() {
        if (rvRecord == null) return;
        List<Record> records = dbHelper.getRecordsByTrip(tripId);
        recordAdapter.updateList(records);
        if (records.isEmpty()) {
            tvRecordEmpty.setVisibility(View.VISIBLE);
            rvRecord.setVisibility(View.GONE);
        } else {
            tvRecordEmpty.setVisibility(View.GONE);
            rvRecord.setVisibility(View.VISIBLE);
        }
    }

    // DB에서 여행 정보 로드 → 툴바 제목·날짜·목적지 표시 → 날짜 탭 생성
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

    // 시작일~종료일 사이의 모든 날짜를 "yyyy-MM-dd" 목록으로 생성
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

    // 날짜 탭 UI를 새로 생성 (날짜명 + 일정 개수 표시, 선택 탭은 노란색 강조)
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

            // 해당 날짜의 일정 수 계산
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

    // 탭 배경: 선택된 탭 = #FFDA44, 비선택 = #FFF3C3
    private void applyDateTabStyle(LinearLayout tab, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12f);
        bg.setColor(selected ? Color.parseColor("#FFDA44") : Color.parseColor("#FFF3C3"));
        bg.setStroke(1, Color.parseColor("#DDDDDD"));
        tab.setBackground(bg);
    }

    // 선택 탭 변경 시 모든 탭 스타일 갱신
    private void refreshDateTabStyles() {
        for (int i = 0; i < dateTabs.getChildCount(); i++) {
            LinearLayout tab = (LinearLayout) dateTabs.getChildAt(i);
            applyDateTabStyle(tab, dateList.get(i).equals(selectedDate));
        }
    }

    // 선택된 날짜에 해당하는 일정만 필터링 → RecyclerView + 지도 핀 갱신
    private void loadSchedulesForDate(String date) {
        if (date == null || date.isEmpty()) return;

        List<Schedule> allSchedules = dbHelper.getSchedulesByTrip(tripId);
        List<Schedule> filtered = new ArrayList<>();
        for (Schedule s : allSchedules) {
            if (date.equals(s.getDate())) filtered.add(s);
        }

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

    // 일정 목록의 각 장소를 Geocoding API로 좌표 변환 후 번호+노란 핀 표시, 완료 후 동선 연결
    // 비동기 특성상 순서가 뒤바뀌지 않도록 인덱스 배열로 순서 보장
    private void showPinsForSchedules(List<Schedule> schedules) {
        googleMap.clear();
        if (schedules.isEmpty()) return;

        int total = schedules.size();
        LatLng[] orderedPositions = new LatLng[total];
        int[] done = {0};

        for (int i = 0; i < total; i++) {
            Schedule s = schedules.get(i);
            String query = (s.getPlaceName() != null && !s.getPlaceName().isEmpty())
                    ? s.getPlaceName() : s.getTitle();

            if (query == null || query.isEmpty()) {
                done[0]++;
                if (done[0] == total) onAllGeocodeDone(orderedPositions);
                continue;
            }

            final int index = i;
            final String markerTitle = (i + 1) + ". " + s.getTitle();
            final String snippet = query;
            geocode(query, latLng -> {
                runOnUiThread(() -> {
                    if (latLng != null && googleMap != null) {
                        orderedPositions[index] = latLng;
                        googleMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(markerTitle)
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_YELLOW)));
                    }
                    done[0]++;
                    if (done[0] == total) onAllGeocodeDone(orderedPositions);
                });
            });
        }
    }

    // 모든 Geocoding 완료 후 순서대로 동선 폴리라인 그리기 + 카메라 이동
    private void onAllGeocodeDone(LatLng[] orderedPositions) {
        List<LatLng> validPositions = new ArrayList<>();
        for (LatLng pos : orderedPositions) {
            if (pos != null) validPositions.add(pos);
        }
        if (validPositions.size() >= 2) {
            googleMap.addPolyline(new PolylineOptions()
                    .addAll(validPositions)
                    .width(8f)
                    .color(Color.parseColor("#FF6B35"))
                    .geodesic(true));
        }
        fitCameraToPins(validPositions);
    }

    // 핀이 1개면 해당 위치로 줌, 여러 개면 LatLngBounds로 모두 포함되도록 카메라 이동
    private void fitCameraToPins(List<LatLng> positions) {
        if (googleMap == null) return;
        if (positions.isEmpty()) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(37.5665, 126.9780), 10f)); // 서울 기본 위치
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

    // Google Geocoding API: 주소 문자열 → LatLng (백그라운드 스레드)
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
                new LatLng(37.5665, 126.9780), 10f)); // 서울 기본 위치로 초기화

        // 지도 탭 → Reverse Geocoding → 일정 추가 다이얼로그
        googleMap.setOnMapClickListener(latLng -> reverseGeocode(latLng, address ->
                runOnUiThread(() -> showMapTapDialog(latLng, address))));

        googleMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        loadSchedulesForDate(selectedDate);
    }

    // Google Geocoding API: LatLng → 주소 문자열 (백그라운드 스레드)
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

    // 지도 탭 시 역지오코딩된 주소와 함께 일정 제목/시간 입력 다이얼로그 표시
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

    // 지도 탭으로 입력받은 정보를 Schedule로 만들어 DB 저장 + 지도에 핀 추가
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

        buildDateTabs();
        loadSchedulesForDate(selectedDate);
        Toast.makeText(this, "'" + title + "' 일정이 추가되었습니다!", Toast.LENGTH_SHORT).show();
    }

    // AddScheduleActivity를 현재 선택된 날짜 기본값으로 열기
    private void openAddSchedule() {
        Intent intent = new Intent(this, AddScheduleActivity.class);
        intent.putExtra("trip_id", tripId);
        intent.putExtra("default_date", selectedDate);
        startActivityForResult(intent, REQUEST_ADD_SCHEDULE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 일정 추가/수정 완료 → 탭과 목록 갱신
        if (requestCode == REQUEST_ADD_SCHEDULE && resultCode == RESULT_OK) {
            buildDateTabs();
            loadSchedulesForDate(selectedDate);
        } else if ((requestCode == REQUEST_ADD_RECORD || requestCode == REQUEST_EDIT_RECORD)
                && resultCode == RESULT_OK) {
            loadRecords();
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
