package com.example.pinit.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pinit.R;
import com.example.pinit.data.MyScrap;
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
    private LinearLayout layoutPlacesList2;
    private Button btnShowMore;
    private Button btnShowMore2;
    private boolean isExpanded = false;
    private boolean isExpanded2 = false;

    // DAY 1 장소 리스트
    private String[] placeNamesDay1 = {
            "상하이 푸동 국제 공항",
            "Shanghai Royal Garden Hotel",
            "Haidilao (Gaoke East Rd Branch)",
            "난징동루 보행자 거리",
            "와이탄 야경"
    };
    private String[] placeNamesDay2 = {"신천지 거리", "상하이 디즈니랜드", "예원 야경"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_post_detail, container, false);

        layoutPlacesList = view.findViewById(R.id.layoutPlacesList);
        layoutPlacesList2 = view.findViewById(R.id.layoutPlacesList2);
        btnShowMore = view.findViewById(R.id.btnShowMore);
        btnShowMore2 = view.findViewById(R.id.btnShowMore2);

// 1. 일정 담기 기능 (토스트 팝업)
        Button btnSaveAllSchedule = view.findViewById(R.id.btnSaveAllSchedule);
        Button btnSaveDay1Schedule = view.findViewById(R.id.btnSaveDay1Schedule);
        Button btnSaveDay2Schedule = view.findViewById(R.id.btnSaveDay2Schedule);

        View.OnClickListener saveScheduleListener = v -> {
            Toast.makeText(getContext(), "일정이 내 여행에 담겼습니다!", Toast.LENGTH_SHORT).show();
        };

        if (btnSaveAllSchedule != null) btnSaveAllSchedule.setOnClickListener(saveScheduleListener);
        if (btnSaveDay1Schedule != null) btnSaveDay1Schedule.setOnClickListener(saveScheduleListener);
        if (btnSaveDay2Schedule != null) btnSaveDay2Schedule.setOnClickListener(saveScheduleListener);


// 2. 하단 액션바 기능 (공유, 댓글, 스크랩)
        ImageView btnActionShare = view.findViewById(R.id.btnActionShare);
        ImageView btnActionComment = view.findViewById(R.id.btnActionComment);
        ImageView btnActionScrap = view.findViewById(R.id.btnActionScrap);

// [공유 기능]: 안드로이드 기본 인텐트를 호출하여 카톡, 인스타 등으로 보냅니다.
        if (btnActionShare != null) {
            btnActionShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                // 공유될 때 넘어가는 텍스트 문구
                shareIntent.putExtra(Intent.EXTRA_TEXT, "[Pin It] 1박 2일 상하이 여행기\n아래 링크에서 확인해보세요!");

                Intent chooser = Intent.createChooser(shareIntent, "게시물 공유하기");
                startActivity(chooser);
            });
        }

// [댓글 기능]: 새롭게 만든 댓글 바텀시트를 띄웁니다.
        if (btnActionComment != null) {
            btnActionComment.setOnClickListener(v -> {
                CommentBottomSheetFragment commentSheet = new CommentBottomSheetFragment();
                commentSheet.show(getChildFragmentManager(), "CommentBottomSheet");
            });
        }

// [스크랩 기능]: 클릭 시 토스트를 띄우고, 아이콘 색상을 포인트 색상(예: 노란색)으로 바꿉니다.
        if (btnActionScrap != null) {
            final boolean[] isScraped = {
                    MyScrap.isScraped(requireContext(), MyScrap.POST_ID_SHANGHAI)
            };
            btnActionScrap.setColorFilter(isScraped[0] ? 0xFFFFD54F : 0xFF888888);

            btnActionScrap.setOnClickListener(v -> {
                isScraped[0] = !isScraped[0]; // 상태 뒤집기
                MyScrap.setScraped(requireContext(), MyScrap.POST_ID_SHANGHAI, isScraped[0]);

                if (isScraped[0]) {
                    Toast.makeText(getContext(), "스크랩 완료!", Toast.LENGTH_SHORT).show();
                    btnActionScrap.setColorFilter(0xFFFFD54F); // 노란색(또는 테마색)으로 틴트 변경
                } else {
                    Toast.makeText(getContext(), "스크랩이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    btnActionScrap.setColorFilter(0xFF888888); // 원래 회색으로 복구
                }
            });
        }
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            // 백스택(이전 화면들 모아둔 상자)에서 현재 화면을 빼고 이전으로 돌아가라!
            getParentFragmentManager().popBackStack();
        });

        view.findViewById(R.id.btnOpenMyPage).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new MyPageFragment())
                        .addToBackStack(null)
                        .commit());

        view.findViewById(R.id.authorProfileRow).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, OtherMyPageFragment.newInstance("\uD138\uD138\uD55C \uBCF5\uC22D\uC544"))
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
        // [2] DAY 2 지도 세팅
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
        renderPlacesListDay2();

        btnShowMore.setOnClickListener(v -> {
            isExpanded = !isExpanded;
            btnShowMore.setText(isExpanded ? "접기 ▲" : "더보기 ▼");
            renderPlacesListDay1();
        });

        btnShowMore2.setOnClickListener(v -> {
            isExpanded2 = !isExpanded2;
            btnShowMore2.setText(isExpanded2 ? "접기 ▲" : "더보기 ▼");
            renderPlacesListDay2(); // DAY 2 전용 렌더링 함수
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
    // DAY 2용 리스트 렌더링 (placeNamesDay2는 setupDay2Map에 있는 배열을 클래스 멤버 변수로 빼주세요!)
    private void renderPlacesListDay2() {
        if(layoutPlacesList2 == null) return;
        layoutPlacesList2.removeAllViews();
        int limit = isExpanded2 ? placeNamesDay2.length : Math.min(3, placeNamesDay2.length);

        for (int i = 0; i < limit; i++) {
            TextView tvPlace = new TextView(getContext());
            String numberCircle = String.valueOf((char) ('①' + i));
            tvPlace.setText(numberCircle + " " + placeNamesDay2[i]);
            tvPlace.setTextSize(16f);
            tvPlace.setTextColor(Color.BLACK);
            tvPlace.setPadding(0, 8, 0, 8);
            layoutPlacesList2.addView(tvPlace);
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

        PolylineOptions polylineOptions = new PolylineOptions().color(Color.parseColor("#FFDA44")).width(8f);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < routePoints.size(); i++) {
            LatLng point = routePoints.get(i);
            polylineOptions.add(point);
            builder.include(point);

            googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title(i < placeNamesDay2.length ? placeNamesDay2[i] : "장소")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        }

        googleMap.addPolyline(polylineOptions);
        googleMap.setOnMapLoadedCallback(() ->
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
        );
    }
}
