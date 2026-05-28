package com.example.pinit.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.pinit.R;
import com.example.pinit.fragment.BudgetFragment;
import com.example.pinit.fragment.FeedFragment;
import com.example.pinit.fragment.HomeFragment;
import com.example.pinit.fragment.MyPageFragment;
import com.example.pinit.fragment.PlaceFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_MY_PAGE = "open_my_page";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
