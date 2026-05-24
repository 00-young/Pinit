package com.example.pinit.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.activity.PlaceDetailActivity;
import com.example.pinit.adapter.PlaceAdapter;
import com.example.pinit.database.DatabaseHelper;
import com.example.pinit.database.PlacesApiHelper;
import com.example.pinit.model.Schedule;
import com.example.pinit.model.Trip;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// [프래그먼트] BottomNavigation 장소 탭 (2번째 탭)
// 두 가지 모드: 장소검색(panelSearch) / 주변찾기(panelNearby) 탭 전환
//
// [장소검색 모드]
//   - 카테고리 탭(전체/음식/카페/관광/쇼핑/액티비티) + 정렬 탭(별점/리뷰/맛집추천)
//   - Google Places textsearch API로 검색 후 카테고리/정렬 필터 적용
//   - 카드 클릭 → showPlaceOptions: 상세보기 또는 내 여행 일정 추가
//
// [주변찾기 모드]
//   - GPS 위치 기반으로 음식점/카페/관광지/숙소 유형 검색
//   - FusedLocationProviderClient로 현재 좌표 획득 (위치 권한 필요)
public class PlaceFragment extends Fragment {

    private PlacesApiHelper apiHelper;
    private DatabaseHelper dbHelper;
    private PlaceAdapter searchAdapter;   // 장소검색 결과 어댑터
    private PlaceAdapter nearbyAdapter;   // 주변찾기 결과 어댑터
    private ProgressBar progressBar, progressBarNearby;
    private List<Map<String, String>> allPlaces = new ArrayList<>(); // 검색 전체 결과 (필터 전)
    private String selectedCategory = "전체";
    private String selectedSort = "별점순";

    private final String[] categories = {"전체", "음식", "카페", "관광", "쇼핑", "액티비티"};
    private final String[] categoryTypes = {"", "restaurant", "cafe", "tourist_attraction", "shopping_mall", "amusement_park"};
    private final String[] sorts = {"별점순", "리뷰순", "맛집추천순"};

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0, currentLng = 0; // 현재 GPS 좌표
    private static final int PERMISSION_REQUEST = 100;
    private TextView tvLocation;

    private LinearLayout panelSearch, panelNearby;
    private TextView tabSearch, tabNearby;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_place, container, false);

        apiHelper = new PlacesApiHelper();
        dbHelper = new DatabaseHelper(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        panelSearch = rootView.findViewById(R.id.panelSearch);
        panelNearby = rootView.findViewById(R.id.panelNearby);
        tabSearch = rootView.findViewById(R.id.tabSearch);
        tabNearby = rootView.findViewById(R.id.tabNearby);
        tvLocation = rootView.findViewById(R.id.tvLocation);
        progressBar = rootView.findViewById(R.id.progressBar);
        progressBarNearby = rootView.findViewById(R.id.progressBarNearby);

        // 장소검색 RecyclerView
        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchAdapter = new PlaceAdapter(requireContext(), new ArrayList<>(), place -> {
            showPlaceOptions(place); // 클릭 → 상세보기 or 일정 추가 선택 팝업
        });
        recyclerView.setAdapter(searchAdapter);

        // 주변찾기 RecyclerView
        RecyclerView recyclerViewNearby = rootView.findViewById(R.id.recyclerViewNearby);
        recyclerViewNearby.setLayoutManager(new LinearLayoutManager(requireContext()));
        nearbyAdapter = new PlaceAdapter(requireContext(), new ArrayList<>(), place -> {
            showPlaceOptions(place);
        });
        recyclerViewNearby.setAdapter(nearbyAdapter);

        // 초기 모드: 장소검색
        switchMode(true);
        tabSearch.setOnClickListener(v -> switchMode(true));
        tabNearby.setOnClickListener(v -> switchMode(false));

        // 카테고리 탭 동적 생성 (LinearLayout에 TextView 추가)
        LinearLayout categoryGroup = rootView.findViewById(R.id.categoryTabGroup);
        for (String cat : categories) {
            categoryGroup.addView(makeTab(cat, cat.equals(selectedCategory), true));
        }

        // 정렬 탭 동적 생성
        LinearLayout sortGroup = rootView.findViewById(R.id.sortTabGroup);
        for (String sort : sorts) {
            sortGroup.addView(makeTab(sort, sort.equals(selectedSort), false));
        }

        // 검색창 (버튼 또는 키보드 검색 키)
        EditText etSearch = rootView.findViewById(R.id.etSearch);
        rootView.findViewById(R.id.btnSearch).setOnClickListener(v ->
                doSearch(etSearch.getText().toString().trim()));
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(etSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        // 주변찾기 버튼들 (각 유형별 검색 트리거)
        rootView.findViewById(R.id.btnRestaurant).setOnClickListener(v -> searchNearby("restaurant", "🍽️ 주변 음식점"));
        rootView.findViewById(R.id.btnCafe).setOnClickListener(v -> searchNearby("cafe", "☕ 주변 카페"));
        rootView.findViewById(R.id.btnAttraction).setOnClickListener(v -> searchNearby("tourist_attraction", "🏛️ 주변 관광지"));
        rootView.findViewById(R.id.btnHotel).setOnClickListener(v -> searchNearby("lodging", "🏨 주변 숙소"));

        getCurrentLocation();
        return rootView;
    }

    // ========== 장소 옵션 팝업 (상세보기 / 일정 추가) ==========

    // 장소 카드 클릭 시: "상세 정보 보기" 또는 "내 여행 일정에 추가" 선택 다이얼로그
    private void showPlaceOptions(Map<String, String> place) {
        String placeName = place.getOrDefault("name", "");
        new AlertDialog.Builder(requireContext())
                .setTitle(placeName)
                .setItems(new String[]{"상세 정보 보기", "내 여행 일정에 추가"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(requireContext(), PlaceDetailActivity.class);
                        intent.putExtra("place_id", place.get("place_id"));
                        intent.putExtra("place_name", placeName);
                        startActivity(intent);
                    } else {
                        showAddToScheduleDialog(place);
                    }
                }).show();
    }

    // 어느 여행에 추가할지 선택 → showDateTimePickerDialog
    private void showAddToScheduleDialog(Map<String, String> place) {
        List<Trip> trips = dbHelper.getAllTrips();
        if (trips.isEmpty()) {
            Toast.makeText(requireContext(), "먼저 여행을 추가해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] tripNames = new String[trips.size()];
        for (int i = 0; i < trips.size(); i++) {
            Trip t = trips.get(i);
            tripNames[i] = t.getTitle() + " (" + t.getStartDate() + " ~ " + t.getEndDate() + ")";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("어느 여행에 추가할까요?")
                .setItems(tripNames, (dialog, which) ->
                        showDateTimePickerDialog(place, trips.get(which)))
                .show();
    }

    // 날짜(Spinner) + 시간(EditText) 입력 다이얼로그 → addPlaceToSchedule
    private void showDateTimePickerDialog(Map<String, String> place, Trip trip) {
        List<String> dateList = buildDateList(trip.getStartDate(), trip.getEndDate());
        if (dateList.isEmpty()) {
            Toast.makeText(requireContext(), "여행 날짜를 확인해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(56, 24, 56, 8);

        TextView tvDateLabel = new TextView(requireContext());
        tvDateLabel.setText("날짜 선택");
        tvDateLabel.setTextSize(13f);
        tvDateLabel.setTextColor(Color.parseColor("#888888"));
        tvDateLabel.setPadding(0, 0, 0, 6);
        layout.addView(tvDateLabel);

        Spinner spinnerDate = new Spinner(requireContext());
        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, dateList);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDate.setAdapter(dateAdapter);
        layout.addView(spinnerDate);

        TextView tvTimeLabel = new TextView(requireContext());
        tvTimeLabel.setText("시간 입력 (예: 09:00)");
        tvTimeLabel.setTextSize(13f);
        tvTimeLabel.setTextColor(Color.parseColor("#888888"));
        tvTimeLabel.setPadding(0, 24, 0, 6);
        layout.addView(tvTimeLabel);

        EditText etTime = new EditText(requireContext());
        etTime.setHint("선택 안 함");
        etTime.setInputType(android.text.InputType.TYPE_CLASS_DATETIME
                | android.text.InputType.TYPE_DATETIME_VARIATION_TIME);
        layout.addView(etTime);

        new AlertDialog.Builder(requireContext())
                .setTitle("📅 날짜와 시간을 선택하세요")
                .setView(layout)
                .setPositiveButton("추가", (d, w) -> {
                    String selectedDate = (String) spinnerDate.getSelectedItem();
                    String time = etTime.getText().toString().trim();
                    addPlaceToSchedule(place, trip, selectedDate, time);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 여행 기간 날짜 목록 생성 (시작일~종료일)
    private List<String> buildDateList(String startStr, String endStr) {
        List<String> dates = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA);
            Date start = sdf.parse(startStr);
            Date end = sdf.parse(endStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            while (!cal.getTime().after(end)) {
                dates.add(sdf.format(cal.getTime()));
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (ParseException ignored) {}
        return dates;
    }

    // 선택된 장소를 해당 여행의 날짜/시간으로 Schedule DB에 저장
    private void addPlaceToSchedule(Map<String, String> place, Trip trip, String date, String time) {
        Schedule schedule = new Schedule();
        schedule.setTripId(trip.getId());
        schedule.setTitle(place.getOrDefault("name", ""));
        schedule.setPlaceName(place.getOrDefault("address", ""));
        schedule.setDate(date);
        schedule.setTime(time);
        schedule.setMemo(place.getOrDefault("rating", "").isEmpty() ? "" : place.get("rating") + "점");
        schedule.setColor("#FFDA44");
        dbHelper.insertSchedule(schedule);
        Toast.makeText(requireContext(),
                "'" + place.getOrDefault("name", "") + "'을(를)\n'"
                        + trip.getTitle() + "' " + date
                        + (time.isEmpty() ? "" : " " + time) + "에 추가했습니다!",
                Toast.LENGTH_SHORT).show();
    }

    // ========== 모드 전환 ==========

    // 장소검색/주변찾기 패널 가시성 토글 + 탭 스타일 업데이트
    private void switchMode(boolean searchMode) {
        if (searchMode) {
            panelSearch.setVisibility(View.VISIBLE);
            panelNearby.setVisibility(View.GONE);
            applyTabStyle(tabSearch, true);
            applyTabStyle(tabNearby, false);
        } else {
            panelSearch.setVisibility(View.GONE);
            panelNearby.setVisibility(View.VISIBLE);
            applyTabStyle(tabSearch, false);
            applyTabStyle(tabNearby, true);
        }
    }

    // ========== 장소 검색 ==========

    // 카테고리/정렬 탭 TextView 생성 (동적으로 LinearLayout에 추가)
    private TextView makeTab(String label, boolean selected, boolean isCategory) {
        TextView tv = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(8);
        tv.setLayoutParams(params);
        tv.setText(label);
        tv.setTextSize(13f);
        tv.setPadding(28, 14, 28, 14);
        applyTabStyle(tv, selected);

        tv.setOnClickListener(v -> {
            if (isCategory) {
                selectedCategory = label;
                refreshCategoryTabs();
            } else {
                selectedSort = label;
                refreshSortTabs();
            }
            applyFilterAndSort(); // 카테고리/정렬 변경 시 즉시 필터 적용
        });
        return tv;
    }

    // 탭 배경: 선택=노란색(#FFDA44), 비선택=연노란색(#FFF3C3) + 검정 테두리
    private void applyTabStyle(TextView tv, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(100f);
        bg.setStroke(2, Color.BLACK);
        bg.setColor(selected ? Color.parseColor("#FFDA44") : Color.parseColor("#FFF3C3"));
        tv.setBackground(bg);
        tv.setTextColor(Color.BLACK);
    }

    private void refreshCategoryTabs() {
        LinearLayout group = rootView.findViewById(R.id.categoryTabGroup);
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView tv = (TextView) group.getChildAt(i);
            applyTabStyle(tv, tv.getText().toString().equals(selectedCategory));
        }
    }

    private void refreshSortTabs() {
        LinearLayout group = rootView.findViewById(R.id.sortTabGroup);
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView tv = (TextView) group.getChildAt(i);
            applyTabStyle(tv, tv.getText().toString().equals(selectedSort));
        }
    }

    // Google Places textsearch 호출 → 결과를 allPlaces에 저장 → applyFilterAndSort
    private void doSearch(String query) {
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);

        // 카테고리 선택 시 쿼리에 한국어 키워드 추가 (검색 정확도 향상)
        String searchQuery = query;
        if (!selectedCategory.equals("전체")) {
            switch (selectedCategory) {
                case "음식": searchQuery = query + " 음식점 맛집"; break;
                case "카페": searchQuery = query + " 카페"; break;
                case "관광": searchQuery = query + " 관광지 명소"; break;
                case "쇼핑": searchQuery = query + " 쇼핑"; break;
                case "액티비티": searchQuery = query + " 액티비티 체험"; break;
            }
        }

        final String finalQuery = searchQuery;
        apiHelper.searchPlaces(finalQuery, null, new PlacesApiHelper.PlacesCallback() {
            @Override
            public void onSuccess(List<Map<String, String>> places) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    allPlaces = places;
                    applyFilterAndSort();
                    if (places.isEmpty())
                        Toast.makeText(requireContext(), "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "오류: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // allPlaces에서 카테고리 필터 + 정렬 적용 후 searchAdapter 갱신
    private void applyFilterAndSort() {
        List<Map<String, String>> filtered = new ArrayList<>();

        if (selectedCategory.equals("전체")) {
            filtered.addAll(allPlaces);
        } else {
            int idx = getCategoryIndex(selectedCategory);
            String typeKeyword = (idx >= 0) ? categoryTypes[idx] : "";
            for (Map<String, String> place : allPlaces) {
                String types = place.getOrDefault("types", "");
                if (types.contains(typeKeyword)) filtered.add(place);
            }
            if (filtered.isEmpty()) filtered.addAll(allPlaces); // 필터 결과 없으면 전체 표시
        }

        // 정렬: 별점순/맛집추천순=rating DESC, 리뷰순=user_ratings_total DESC
        switch (selectedSort) {
            case "별점순":
            case "맛집추천순":
                filtered.sort((a, b) -> Double.compare(
                        parseDouble(b.getOrDefault("rating", "0")),
                        parseDouble(a.getOrDefault("rating", "0"))));
                break;
            case "리뷰순":
                filtered.sort((a, b) -> Integer.compare(
                        parseInt(b.getOrDefault("user_ratings_total", "0")),
                        parseInt(a.getOrDefault("user_ratings_total", "0"))));
                break;
        }

        searchAdapter.updateList(filtered);
    }

    // ========== 주변 찾기 ==========

    // FusedLocationProvider로 현재 GPS 좌표 획득 (권한 없으면 요청)
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                tvLocation.setText("📍 현재 위치: " + String.format("%.4f, %.4f", currentLat, currentLng));
            }
        });
    }

    // 현재 좌표 기준 반경 2km 내 지정 유형(type) 장소 검색
    private void searchNearby(String type, String label) {
        if (currentLat == 0 && currentLng == 0) {
            Toast.makeText(requireContext(), "위치를 가져오는 중입니다. 잠시 후 시도해주세요.", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }
        progressBarNearby.setVisibility(View.VISIBLE);
        tvLocation.setText(label + " 검색 중...");
        apiHelper.searchNearby(currentLat, currentLng, type, 2000, new PlacesApiHelper.PlacesCallback() {
            @Override
            public void onSuccess(List<Map<String, String>> places) {
                requireActivity().runOnUiThread(() -> {
                    progressBarNearby.setVisibility(View.GONE);
                    tvLocation.setText(label + " - " + places.size() + "개 발견");
                    nearbyAdapter.updateList(places);
                });
            }
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBarNearby.setVisibility(View.GONE);
                    tvLocation.setText("검색 실패");
                    Toast.makeText(requireContext(), "오류: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    // ========== 유틸 ==========

    private int getCategoryIndex(String cat) {
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(cat)) return i;
        }
        return -1;
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
