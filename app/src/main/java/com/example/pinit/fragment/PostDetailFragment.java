package com.example.pinit.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pinit.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class PostDetailFragment extends Fragment {

    private LinearLayout layoutPlacesList;
    private Button btnShowMore;
    private boolean isExpanded = false;

    // DAY 1 장소 리스트
    private String[] placeNamesDay1 = {
            "상하이 푸동 국제 공항",
            "Shanghai Royal Garden Hotel",
            "Haidilao (Gaoke East Rd Branch)",
            "난징동루 보행자 거리",
            "와이탄 야경"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_post_detail, container, false);

        layoutPlacesList = view.findViewById(R.id.layoutPlacesList);
        btnShowMore = view.findViewById(R.id.btnShowMore);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            // 백스택(이전 화면들 모아둔 상자)에서 현재 화면을 빼고 이전으로 돌아가라!
            getParentFragmentManager().popBackStack();
        });

        view.findViewById(R.id.btnOpenMyPage).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new MyPageFragment())
                        .addToBackStack(null)
                        .commit());

        // ==========================================
        // [1] DAY 1 지도 세팅
        // ==========================================
        SupportMapFragment mapFragment1 = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapViewDetail);
        if (mapFragment1 != null) {
            mapFragment1.getMapAsync(googleMap -> setupDay1Map(googleMap));
        }
        // DAY 1 지도 스크롤 충돌 방지
        View map1View = view.findViewById(R.id.mapViewDetail);
        if(map1View != null) {
            map1View.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
        }

        // ==========================================
        // [2] DAY 2 지도 세팅 (새로 추가된 부분!)
        // ==========================================
        SupportMapFragment mapFragment2 = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapViewDetail2);
        if (mapFragment2 != null) {
            mapFragment2.getMapAsync(googleMap -> setupDay2Map(googleMap));
        }
        // DAY 2 지도 스크롤 충돌 방지
        View map2View = view.findViewById(R.id.mapViewDetail2);
        if(map2View != null) {
            map2View.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
        }

        // DAY 1 텍스트 리스트 렌더링
        renderPlacesListDay1();

        btnShowMore.setOnClickListener(v -> {
            isExpanded = !isExpanded;
            btnShowMore.setText(isExpanded ? "접기 ▲" : "더보기 ▼");
            renderPlacesListDay1();
        });

        return view;
    }

    // DAY 1 텍스트 리스트를 그려주는 함수
    private void renderPlacesListDay1() {
        if(layoutPlacesList == null) return;
        layoutPlacesList.removeAllViews();
        int limit = isExpanded ? placeNamesDay1.length : Math.min(3, placeNamesDay1.length);

        for (int i = 0; i < limit; i++) {
            TextView tvPlace = new TextView(getContext());
            String numberCircle = String.valueOf((char) ('①' + i));
            tvPlace.setText(numberCircle + " " + placeNamesDay1[i]);
            tvPlace.setTextSize(16f);
            tvPlace.setTextColor(Color.BLACK);
            tvPlace.setPadding(0, 8, 0, 8);
            layoutPlacesList.addView(tvPlace);
        }

        if (placeNamesDay1.length <= 3) {
            btnShowMore.setVisibility(View.GONE);
        }
    }

    // DAY 1 지도에 핀과 선을 그리는 함수
    private void setupDay1Map(GoogleMap googleMap) {
        List<LatLng> routePoints = new ArrayList<>();
        routePoints.add(new LatLng(31.1443, 121.8083)); // 푸동 공항
        routePoints.add(new LatLng(31.2000, 121.6000)); // 호텔
        routePoints.add(new LatLng(31.2150, 121.5500)); // 하이디라오
        routePoints.add(new LatLng(31.2350, 121.4800)); // 난징동루
        routePoints.add(new LatLng(31.2397, 121.4898)); // 와이탄

        PolylineOptions polylineOptions = new PolylineOptions().color(Color.parseColor("#FFDA44")).width(8f);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < routePoints.size(); i++) {
            LatLng point = routePoints.get(i);
            polylineOptions.add(point);
            builder.include(point);

            googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title(i < placeNamesDay1.length ? placeNamesDay1[i] : "장소")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        }

        googleMap.addPolyline(polylineOptions);
        googleMap.setOnMapLoadedCallback(() ->
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
        );
    }

    // DAY 2 지도에 핀과 선을 그리는 함수
    private void setupDay2Map(GoogleMap googleMap) {
        List<LatLng> routePoints = new ArrayList<>();
        // DAY 2의 실제 예상 좌표 (신천지 -> 디즈니랜드 -> 예원)
        routePoints.add(new LatLng(31.2222, 121.4744)); // 신천지
        routePoints.add(new LatLng(31.1433, 121.6580)); // 디즈니랜드
        routePoints.add(new LatLng(31.2272, 121.4921)); // 예원

        String[] placeNamesDay2 = {"신천지 거리", "상하이 디즈니랜드", "예원 야경"};

        PolylineOptions polylineOptions = new PolylineOptions().color(Color.parseColor("#FFDA44")).width(8f);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < routePoints.size(); i++) {
            LatLng point = routePoints.get(i);
            polylineOptions.add(point);
            builder.include(point);

            googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title(placeNamesDay2[i])
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        }

        googleMap.addPolyline(polylineOptions);
        googleMap.setOnMapLoadedCallback(() ->
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
        );
    }
}
