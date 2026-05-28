package com.example.pinit.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.adapter.ScheduleDetailAdapter;
import com.example.pinit.data.MyScrap;
import com.example.pinit.model.Schedule;
import com.example.pinit.model.DailySchedule; // 추가됨
import com.example.pinit.model.MyPlan; // 추가됨

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

    private RecyclerView rvPlacesDay1;
    private RecyclerView rvPlacesDay2;
    private Button btnShowMore;
    private Button btnShowMore2;

    // [수정됨] 담기 버튼에서 접근할 수 있도록 데이터를 클래스 멤버 변수로 선언
    private List<Schedule> day1Schedules = new ArrayList<>();
    private List<Schedule> day2Schedules = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_post_detail, container, false);

        // 뷰 바인딩
        rvPlacesDay1 = view.findViewById(R.id.rvPlacesDay1);
        rvPlacesDay2 = view.findViewById(R.id.rvPlacesDay2);
        btnShowMore = view.findViewById(R.id.btnShowMore);
        btnShowMore2 = view.findViewById(R.id.btnShowMore2);

        // 상단 뒤로가기 및 프로필 버튼 이벤트
        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

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

        // [핵심] 서버에서 데이터를 불러오는(척하는) 더미 데이터 세팅 함수 호출
        // (버튼 클릭보다 먼저 데이터가 세팅되어 있어야 합니다)
        fetchPostDetailDataFromServer();

        // [수정됨] 일정 담기 이벤트 리스너 분리 적용
        Button btnSaveAllSchedule = view.findViewById(R.id.btnSaveAllSchedule);
        if (btnSaveAllSchedule != null) {
            btnSaveAllSchedule.setOnClickListener(v -> saveAllSchedulesToMyTravel());
        }

        Button btnSaveDay1Schedule = view.findViewById(R.id.btnSaveDay1Schedule);
        if (btnSaveDay1Schedule != null) {
            btnSaveDay1Schedule.setOnClickListener(v -> saveSingleDayToMyTravel(1));
        }

        Button btnSaveDay2Schedule = view.findViewById(R.id.btnSaveDay2Schedule);
        if (btnSaveDay2Schedule != null) {
            btnSaveDay2Schedule.setOnClickListener(v -> saveSingleDayToMyTravel(2));
        }

        // 하단 액션바 이벤트 (공유, 댓글, 스크랩)
        setupBottomActions(view);

        // 지도 초기화
        setupMapFragments(view);

        // 더보기 버튼 토글 로직
        btnShowMore.setOnClickListener(v -> toggleRecyclerViewVisibility(rvPlacesDay1, btnShowMore));
        btnShowMore2.setOnClickListener(v -> toggleRecyclerViewVisibility(rvPlacesDay2, btnShowMore2));

        return view;
    }

    // ==========================================
    // [내 여행에 담기] 로직 구현부
    // ==========================================
    private void saveAllSchedulesToMyTravel() {
        MyPlan scrapedPlan = new MyPlan(); // 빈 바구니 생성
        scrapedPlan.setTitle("1박 2일 상하이 여행기 (스크랩)");
        scrapedPlan.setDate("2026.05.23 ~ 2026.05.24");
        scrapedPlan.setCountry("상하이");

        List<DailySchedule> allDays = new ArrayList<>();

        // DAY 1 데이터 조립
        DailySchedule day1 = new DailySchedule();
        day1.setDayTitle("DAY 1");
        day1.setDate("2026.05.23");
        day1.setScheduleObjects(day1Schedules); // 미리 만들어둔 더미 데이터 넣기
        allDays.add(day1);

        // DAY 2 데이터 조립
        DailySchedule day2 = new DailySchedule();
        day2.setDayTitle("DAY 2");
        day2.setDate("2026.05.24");
        day2.setScheduleObjects(day2Schedules);
        allDays.add(day2);

        // 완성된 날짜 리스트를 최종 바구니에 담기
        scrapedPlan.setSchedules(allDays);

        // TODO: DB 또는 서버에 scrapedPlan 객체 저장하는 코드 작성 위치

        Toast.makeText(getContext(), "전체 일정이 내 여행에 담겼습니다!", Toast.LENGTH_SHORT).show();
    }

    private void saveSingleDayToMyTravel(int dayNumber) {
        MyPlan singleDayPlan = new MyPlan();
        singleDayPlan.setTitle("상하이 여행기 - DAY " + dayNumber + " (스크랩)");
        singleDayPlan.setCountry("상하이");

        List<DailySchedule> selectedDayList = new ArrayList<>();
        DailySchedule targetDay = new DailySchedule();

        if (dayNumber == 1) {
            targetDay.setDayTitle("DAY 1");
            targetDay.setDate("2026.05.23");
            targetDay.setScheduleObjects(day1Schedules);
            singleDayPlan.setDate("2026.05.23"); // 하루짜리 여행이므로 날짜 고정
        } else if (dayNumber == 2) {
            targetDay.setDayTitle("DAY 2");
            targetDay.setDate("2026.05.24");
            targetDay.setScheduleObjects(day2Schedules);
            singleDayPlan.setDate("2026.05.24");
        }

        selectedDayList.add(targetDay);
        singleDayPlan.setSchedules(selectedDayList);

        // TODO: DB 또는 서버에 singleDayPlan 객체 저장하는 코드 작성 위치

        Toast.makeText(getContext(), "DAY " + dayNumber + " 일정이 내 여행에 담겼습니다!", Toast.LENGTH_SHORT).show();
    }
    // ==========================================


    private void setupBottomActions(View view) {
        ImageView btnActionShare = view.findViewById(R.id.btnActionShare);
        ImageView btnActionComment = view.findViewById(R.id.btnActionComment);
        ImageView btnActionScrap = view.findViewById(R.id.btnActionScrap);

        btnActionShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "[Pin It] 1박 2일 상하이 여행기\n아래 링크에서 확인해보세요!");
            startActivity(Intent.createChooser(shareIntent, "게시물 공유하기"));
        });

        btnActionComment.setOnClickListener(v -> {
            CommentBottomSheetFragment commentSheet = new CommentBottomSheetFragment();
            commentSheet.show(getChildFragmentManager(), "CommentBottomSheet");
        });

        final boolean[] isScraped = { MyScrap.isScraped(requireContext(), MyScrap.POST_ID_SHANGHAI) };
        btnActionScrap.setColorFilter(isScraped[0] ? 0xFFFFD54F : 0xFF888888);
        btnActionScrap.setOnClickListener(v -> {
            isScraped[0] = !isScraped[0];
            MyScrap.setScraped(requireContext(), MyScrap.POST_ID_SHANGHAI, isScraped[0]);
            btnActionScrap.setColorFilter(isScraped[0] ? 0xFFFFD54F : 0xFF888888);
            Toast.makeText(getContext(), isScraped[0] ? "스크랩 완료!" : "스크랩이 취소되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupMapFragments(View view) {
        SupportMapFragment mapFragment1 = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapViewDetail);
        if (mapFragment1 != null) {
            mapFragment1.getMapAsync(this::setupDay1Map);
        }
        View map1View = view.findViewById(R.id.mapViewDetail);
        if (map1View != null) {
            map1View.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
        }

        SupportMapFragment mapFragment2 = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapViewDetail2);
        if (mapFragment2 != null) {
            mapFragment2.getMapAsync(this::setupDay2Map);
        }
        View map2View = view.findViewById(R.id.mapViewDetail2);
        if (map2View != null) {
            map2View.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
        }
    }

    // 서버 데이터를 받아오는 더미 환경 구성
    private void fetchPostDetailDataFromServer() {
        // [수정됨] 기존 리스트를 클리어하고 멤버 변수에 담기
        day1Schedules.clear();
        String[] day1Names = {"상하이 푸동 국제 공항", "Shanghai Royal Garden Hotel", "Haidilao (Gaoke East Rd Branch)", "난징동루 보행자 거리", "와이탄 야경"};
        for (String name : day1Names) {
            Schedule obj = new Schedule();
            obj.setPlaceName(name); // TODO: 모델 클래스의 Setter 확인 (예: setName)
            day1Schedules.add(obj);
        }

        // [수정됨] 기존 리스트를 클리어하고 멤버 변수에 담기
        day2Schedules.clear();
        String[] day2Names = {"신천지 거리", "상하이 디즈니랜드", "예원 야경"};
        for (String name : day2Names) {
            Schedule obj = new Schedule();
            obj.setPlaceName(name); // TODO: 모델 클래스의 Setter 확인
            day2Schedules.add(obj);
        }

        // 리사이클러뷰에 데이터 렌더링
        renderRecyclerView(rvPlacesDay1, day1Schedules);
        renderRecyclerView(rvPlacesDay2, day2Schedules);
    }

    // 리사이클러뷰 어댑터 연결 공통 함수
    private void renderRecyclerView(RecyclerView recyclerView, List<Schedule> schedules) {
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);

        // CreatePostFragment와 완전히 동일한 어댑터 구조 적용
        recyclerView.setAdapter(new ScheduleDetailAdapter(
                getContext(),
                schedules,
                schedule -> {}, // 상세 조회 화면이므로 클릭 이벤트 비활성화
                id -> {},
                schedule -> {}
        ));
    }

    // 더보기 / 접기 토글 함수
    private void toggleRecyclerViewVisibility(RecyclerView rv, Button btn) {
        if (rv.getVisibility() == View.VISIBLE) {
            rv.setVisibility(View.GONE);
            btn.setText("더보기 ▼");
        } else {
            rv.setVisibility(View.VISIBLE);
            btn.setText("접기 ▲");
        }
    }

    // ==========================================
    // 지도 그리기 (기존 코드 유지)
    // ==========================================
    private void setupDay1Map(GoogleMap googleMap) {
        List<LatLng> routePoints = new ArrayList<>();
        routePoints.add(new LatLng(31.1443, 121.8083));
        routePoints.add(new LatLng(31.2000, 121.6000));
        routePoints.add(new LatLng(31.2150, 121.5500));
        routePoints.add(new LatLng(31.2350, 121.4800));
        routePoints.add(new LatLng(31.2397, 121.4898));

        PolylineOptions polylineOptions = new PolylineOptions().color(Color.parseColor("#FFDA44")).width(8f);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < routePoints.size(); i++) {
            LatLng point = routePoints.get(i);
            polylineOptions.add(point);
            builder.include(point);
            googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        }
        googleMap.addPolyline(polylineOptions);
        googleMap.setOnMapLoadedCallback(() ->
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
        );
    }

    private void setupDay2Map(GoogleMap googleMap) {
        List<LatLng> routePoints = new ArrayList<>();
        routePoints.add(new LatLng(31.2222, 121.4744));
        routePoints.add(new LatLng(31.1433, 121.6580));
        routePoints.add(new LatLng(31.2272, 121.4921));

        PolylineOptions polylineOptions = new PolylineOptions().color(Color.parseColor("#FFDA44")).width(8f);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < routePoints.size(); i++) {
            LatLng point = routePoints.get(i);
            polylineOptions.add(point);
            builder.include(point);
            googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        }
        googleMap.addPolyline(polylineOptions);
        googleMap.setOnMapLoadedCallback(() ->
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
        );
    }
}