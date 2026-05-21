package com.example.pinit.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.pinit.R;
import com.example.pinit.fragment.BudgetFragment;
import com.example.pinit.fragment.FeedFragment;
import com.example.pinit.fragment.HomeFragment;
import com.example.pinit.fragment.PlaceFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        int selectedNav = getIntent().getIntExtra("selected_nav", R.id.nav_home);
        String postSearchQuery = getIntent().getStringExtra(PostSearchActivity.EXTRA_SEARCH_QUERY);
        bottomNav.setSelectedItemId(selectedNav);
        loadFragment(fragmentForNavId(selectedNav, postSearchQuery));

        bottomNav.setOnItemSelectedListener(item -> {
            loadFragment(fragmentForNavId(item.getItemId(), null));
            return true;
        });
    }

    private Fragment fragmentForNavId(int id, String postSearchQuery) {
        if (id == R.id.nav_home) return new HomeFragment();
        if (id == R.id.nav_place) return new PlaceFragment();
        if (id == R.id.nav_budget) return new BudgetFragment();
        if (id == R.id.nav_community) {
            FeedFragment fragment = new FeedFragment();
            if (postSearchQuery != null) {
                Bundle args = new Bundle();
                args.putString(PostSearchActivity.EXTRA_SEARCH_QUERY, postSearchQuery);
                fragment.setArguments(args);
            }
            return fragment;
        }
        return new HomeFragment();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment).commit();
    }
}
