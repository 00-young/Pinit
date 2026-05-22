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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class PlaceFragment extends Fragment {

    private PlacesApiHelper apiHelper;
    private DatabaseHelper dbHelper;
    private PlaceAdapter searchAdapter;
    private PlaceAdapter nearbyAdapter;
    private ProgressBar progressBar, progressBarNearby;
    private List<Map<String, String>> allPlaces = new ArrayList<>();
    private String selectedCategory = "전체";
    private String selectedSort = "별점순";

    private final String[] categories = {"전체", "음식", "카페", "관광", "쇼핑", "액티비티"};
    private final String[] categoryTypes = {"", "restaurant", "cafe", "tourist_attraction", "shopping_mall", "amusement_park"};
    private final String[] sorts = {"별점순", "리뷰순", "맛집추천순"};

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0, currentLng = 0;
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

        // 장소검색 RecyclerView - 클릭: 상세보기 / 롱클릭: 일정 추가
        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchAdapter = new PlaceAdapter(requireContext(), new ArrayList<>(), place -> {
            showPlaceOptions(place);
        });
        recyclerView.setAdapter(searchAdapter);

        // 주변찾기 RecyclerView
        RecyclerView recyclerViewNearby = rootView.findViewById(R.id.recyclerViewNearby);
        recyclerViewNearby.setLayoutManager(new LinearLayoutManager(requireContext()));
        nearbyAdapter = new PlaceAdapter(requireContext(), new ArrayList<>(), place -> {
            showPlaceOptions(place);
        });
        recyclerViewNearby.setAdapter(nearbyAdapter);

        // 모드 탭 전환 - 처음엔 장소검색 모드
        switchMode(true);
        tabSearch.setOnClickListener(v -> switchMode(true));
        tabNearby.setOnClickListener(v -> switchMode(false));

        // 카테고리 탭 생성
        LinearLayout categoryGroup = rootView.findViewById(R.id.categoryTabGroup);
        for (String cat : categories) {
            categoryGroup.addView(makeTab(cat, cat.equals(selectedCategory), true));
        }

        // 정렬 탭 생성
        LinearLayout sortGroup = rootView.findViewById(R.id.sortTabGroup);
        for (String sort : sorts) {
            sortGroup.addView(makeTab(sort, sort.equals(selectedSort), false));
        }

        // 검색창
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

        // 주변찾기 버튼들
        rootView.findViewById(R.id.btnRestaurant).setOnClickListener(v -> searchNearby("restaurant", "🍽️ 주변 음식점"));
        rootView.findViewById(R.id.btnCafe).setOnClickListener(v -> searchNearby("cafe", "☕ 주변 카페"));
        rootView.findViewById(R.id.btnAttraction).setOnClickListener(v -> searchNearby("tourist_attraction", "🏛️ 주변 관광지"));
        rootView.findViewById(R.id.btnHotel).setOnClickListener(v -> searchNearby("lodging", "🏨 주변 숙소"));

        getCurrentLocation();
        return rootView;
    }

    // ========== 장소 옵션 팝업 (상세보기 / 일정 추가) ==========

    private void showPlaceOptions(Map<String, String> place) {
        String placeName = place.getOrDefault("name", "");
        new AlertDialog.Builder(requireContext())
                .setTitle(placeName)
                .setItems(new String[]{"상세 정보 보기", "내 여행 일정에 추가"}, (dialog, which) -> {
                    if (which == 0) {
                        // 상세보기
                        Intent intent = new Intent(requireContext(), PlaceDetailActivity.class);
                        intent.putExtra("place_id", place.get("place_id"));
                        intent.putExtra("place_name", placeName);
                        startActivity(intent);
                    } else {
                        // 일정 추가
                        showAddToScheduleDialog(place);
                    }
                }).show();
    }

    private void showAddToScheduleDialog(Map<String, String> place) {
        List<Trip> trips = dbHelper.getAllTrips();
        if (trips.isEmpty()) {
            Toast.makeText(requireContext(), "먼저 여행을 추가해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] tripNames = new String[trips.size()];
        for (int i = 0; i < trips.size(); i++) {
            tripNames[i] = trips.get(i).getTitle() + " (" + trips.get(i).getStartDate() + ")";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("어느 여행에 추가할까요?")
                .setItems(tripNames, (dialog, which) -> {
                    Trip selectedTrip = trips.get(which);
                    addPlaceToSchedule(place, selectedTrip);
                }).show();
    }

    private void addPlaceToSchedule(Map<String, String> place, Trip trip) {
        Schedule schedule = new Schedule();
        schedule.setTripId(trip.getId());
        schedule.setTitle(place.getOrDefault("name", ""));
        schedule.setPlaceName(place.getOrDefault("address", ""));
        schedule.setDate(trip.getStartDate());
        schedule.setTime("");
        schedule.setMemo(place.getOrDefault("rating", "") + "점");
        schedule.setColor("#FFDA44");
        dbHelper.insertSchedule(schedule);
        Toast.makeText(requireContext(),
                "'" + place.getOrDefault("name", "") + "'을(를) '" + trip.getTitle() + "' 일정에 추가했습니다!",
                Toast.LENGTH_SHORT).show();
    }

    // ========== 모드 전환 ==========

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
            applyFilterAndSort();
        });
        return tv;
    }

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

    private void doSearch(String query) {
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);

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
            if (filtered.isEmpty()) filtered.addAll(allPlaces);
        }

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