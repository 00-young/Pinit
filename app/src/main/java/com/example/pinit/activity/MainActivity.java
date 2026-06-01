package com.example.pinit.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.pinit.R;
import com.example.pinit.fragment.BudgetFragment;
import com.example.pinit.fragment.FeedFragment;
import com.example.pinit.fragment.HomeFragment;
import com.example.pinit.fragment.MyPageFragment;
import com.example.pinit.fragment.PlaceFragment;
import com.example.pinit.model.RecommendedPlace;
import com.example.pinit.model.User;
import com.example.pinit.service.RecommendationManager;
import com.example.pinit.service.UserService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_MY_PAGE = "open_my_page";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // XML에서 툴바가 지워졌을 때를 대비해 findViewById 결과가 null이 아닐 때만 세팅하도록 코드 적용
        // TODO 로그아웃 버튼 삭제하는 과정에서 에러가 생겼는데 왜인지 잘 모르겠습니다. 그냥 지우기는 좀 그래서 일단 무시 하는 코드 작성 했습니다.
        Toolbar toolbar = findViewById(R.id.toolbar);
       if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        int selectedNav = getIntent().getIntExtra("selected_nav", R.id.nav_home);
        String postSearchQuery = getIntent().getStringExtra(PostSearchActivity.EXTRA_SEARCH_QUERY);
        ArrayList<String> travelSettings = getIntent().getStringArrayListExtra(PostSearchActivity.EXTRA_TRAVEL_SETTINGS);
        boolean openMyPage = getIntent().getBooleanExtra(EXTRA_OPEN_MY_PAGE, false);

        bottomNav.setSelectedItemId(openMyPage ? R.id.nav_community : selectedNav);
        if (openMyPage) {
            loadFragment(new MyPageFragment());
        } else {
            loadFragment(fragmentForNavId(selectedNav, postSearchQuery, travelSettings));
        }

        bottomNav.setOnItemSelectedListener(item -> {
            loadFragment(fragmentForNavId(item.getItemId(), null, null));
            return true;
        });

        UserService userService = new UserService();
        userService.getUser(
                new UserService.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        startRecommendation(user);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("FINAL_RECOMMEND", error);
                    }
                }
        );
    }

    private Fragment fragmentForNavId(int id, String postSearchQuery, ArrayList<String> travelSettings) {
        if (id == R.id.nav_home) return new HomeFragment();
        if (id == R.id.nav_place) return new PlaceFragment();
        if (id == R.id.nav_budget) return new BudgetFragment();

        if (id == R.id.nav_community) {
            FeedFragment fragment = new FeedFragment();
            Bundle args = new Bundle();
            if (postSearchQuery != null) {
                args.putString(PostSearchActivity.EXTRA_SEARCH_QUERY, postSearchQuery);
            }
            if (travelSettings != null) {
                args.putStringArrayList(PostSearchActivity.EXTRA_TRAVEL_SETTINGS, travelSettings);
            }
            fragment.setArguments(args);
            return fragment;
        }
        return new HomeFragment();
    }

    // [삭제] R.id.action_logout 컴파일 에러 유발하던 onCreateOptionsMenu 및 onOptionsItemSelected 메서드 전면 제거 완료

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void startRecommendation(User user) {
        RecommendationManager recommendationManager = new RecommendationManager();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("schedules")
                .whereEqualTo("userId", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        Log.e("FINAL_RECOMMEND", "여행 일정 없음");
                        return;
                    }

                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    Double lat = doc.getDouble("latitude");
                    Double lng = doc.getDouble("longitude");

                    if (lat == null || lng == null) {
                        Log.e("FINAL_RECOMMEND", "좌표 없음");
                        return;
                    }

                    Log.d("FINAL_RECOMMEND", "좌표 : " + lat + ", " + lng);

                    recommendationManager.getRecommendations(
                            user,
                            lat,
                            lng,
                            new RecommendationManager.RecommendationCallback() {
                                @Override
                                public void onSuccess(List<RecommendedPlace> recommendedPlaces) {
                                    for (RecommendedPlace place : recommendedPlaces) {
                                        Log.d("FINAL_RECOMMEND", place.getName() + " / 점수: " + place.getScore());
                                    }
                                }

                                @Override
                                public void onFailure(String error) {
                                    Log.e("FINAL_RECOMMEND", error);
                                }
                            }
                    );
                });
    }
}