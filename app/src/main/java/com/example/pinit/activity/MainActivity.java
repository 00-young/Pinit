package com.example.pinit.activity;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.pinit.R;
import com.example.pinit.fragment.BudgetFragment;
import com.example.pinit.fragment.FeedFragment;
import com.example.pinit.fragment.HomeFragment;
import com.example.pinit.fragment.PlaceFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        int selectedNav = getIntent().getIntExtra("selected_nav", R.id.nav_home);
        String postSearchQuery = getIntent().getStringExtra(PostSearchActivity.EXTRA_SEARCH_QUERY);

        //  [추가] PostSearchActivity에서 보낸 여행 설정(태그 리스트) 꺼내기
        ArrayList<String> travelSettings = getIntent().getStringArrayListExtra("travel_settings");

        bottomNav.setSelectedItemId(selectedNav);

        //  [수정] 프래그먼트를 부를 때 여행 설정 리스트도 같이 넘겨줍니다.
        loadFragment(fragmentForNavId(selectedNav, postSearchQuery, travelSettings));

        bottomNav.setOnItemSelectedListener(item -> {
            loadFragment(fragmentForNavId(item.getItemId(), null, null));

            // [커뮤니티 프론트엔드 테스트용]
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("nickname", "냉동된 블루베리");
            editor.apply();

            return true;
        });
    }

    // [수정] travelSettings 리스트를 매개변수로 받도록 변경
    private Fragment fragmentForNavId(int id, String postSearchQuery, ArrayList<String> travelSettings) {
        if (id == R.id.nav_home) return new HomeFragment();
        if (id == R.id.nav_place) return new PlaceFragment();
        if (id == R.id.nav_budget) return new BudgetFragment();

        if (id == R.id.nav_community) {
            FeedFragment fragment = new FeedFragment();
            Bundle args = new Bundle();

            // 검색어가 있으면 주머니에 넣기
            if (postSearchQuery != null) {
                args.putString(PostSearchActivity.EXTRA_SEARCH_QUERY, postSearchQuery);
            }
            // 🌟 [추가] 여행 설정 태그가 있으면 주머니에 넣기
            if (travelSettings != null) {
                args.putStringArrayList("travel_settings", travelSettings);
            }

            fragment.setArguments(args);
            return fragment;
        }
        return new HomeFragment();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment).commit();
    }
}