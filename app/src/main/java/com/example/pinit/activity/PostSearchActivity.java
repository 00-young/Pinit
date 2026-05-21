package com.example.pinit.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.HorizontalScrollView;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pinit.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PostSearchActivity extends AppCompatActivity {

    public static final String EXTRA_SEARCH_QUERY = "post_search_query";

    private EditText searchEditText;
    private HorizontalScrollView selectedTagScroller;
    private ChipGroup selectedTagContainer;
    private final Set<String> selectedTags = new LinkedHashSet<>();

    private final String[] togetherTags = {
            "#아이와 함께", "#부모님과 함께", "#친구와 함께",
            "#가족들과 함께", "#신혼여행 맞춤", "#커플 여행",
            "#혼자 놀아도 좋은"
    };

    private final String[] durationTags = {
            "#당일치기", "#1박 2일", "#2박 3일",
            "#3박 4일", "#4박 5일", "#5일 이상",
            "#10일 이상", "#한 달 살기", "#장기 여행"
    };

    private final String[] themeTags = {
            "#관광 명소", "#맛집 투어", "#카페 투어",
            "#귀여운 캐릭터를 찾아", "#자연 경관", "#야경"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_search);

        searchEditText = findViewById(R.id.postSearchEditText);
        selectedTagScroller = findViewById(R.id.selectedTagScroller);
        selectedTagContainer = findViewById(R.id.selectedTagContainer);
        ImageButton btnRunPostSearch = findViewById(R.id.btnRunPostSearch);

        setInitialQuery(getIntent().getStringExtra(EXTRA_SEARCH_QUERY));

        addTags(findViewById(R.id.tagContainerTogether), togetherTags);
        addTags(findViewById(R.id.tagContainerDuration), durationTags);
        addTags(findViewById(R.id.tagContainerTheme), themeTags);

        findViewById(R.id.btnTravelSetting).setOnClickListener(v -> openTravelSetting());
        btnRunPostSearch.setOnClickListener(v -> openSearchResults());
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                openSearchResults();
                return true;
            }
            return false;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_community);
        bottomNav.setOnItemSelectedListener(item -> {
            openMainTab(item.getItemId());
            return true;
        });
    }

    private void setInitialQuery(String incomingQuery) {
        String visibleQuery = incomingQuery == null ? "" : incomingQuery.trim();

        for (String tag : collectKnownTags()) {
            if (visibleQuery.contains(tag)) {
                selectedTags.add(tag);
                visibleQuery = visibleQuery.replace(tag, " ");
            }
        }

        visibleQuery = visibleQuery.replaceAll("\\s+", " ").trim();
        searchEditText.setText(visibleQuery);
        searchEditText.setSelection(searchEditText.length());
        renderSelectedTags();
    }

    private List<String> collectKnownTags() {
        List<String> tags = new ArrayList<>();
        addAll(tags, togetherTags);
        addAll(tags, durationTags);
        addAll(tags, themeTags);
        return tags;
    }

    private void addAll(List<String> target, String[] source) {
        for (String item : source) {
            target.add(item);
        }
    }

    private void addTags(ChipGroup container, String[] tags) {
        for (String tag : tags) {
            Chip chip = createTagChip(tag);
            container.addView(chip);
        }
    }

    private Chip createTagChip(String tag) {
        Chip chip = baseChip();
        chip.setText(tag);
        chip.setOnClickListener(v -> addSelectedTag(tag));
        return chip;
    }

    private void addSelectedTag(String tag) {
        if (selectedTags.add(tag)) {
            renderSelectedTags();
        }
    }

    private void renderSelectedTags() {
        selectedTagContainer.removeAllViews();

        for (String tag : selectedTags) {
            Chip chip = baseChip();
            chip.setText(tag);
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ColorStateList.valueOf(Color.rgb(120, 100, 70)));
            chip.setOnCloseIconClickListener(v -> {
                selectedTags.remove(tag);
                renderSelectedTags();
            });
            selectedTagContainer.addView(chip);
        }

        selectedTagScroller.post(() -> selectedTagScroller.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
    }

    private Chip baseChip() {
        Chip chip = new Chip(this);
        chip.setTextColor(Color.rgb(34, 34, 34));
        chip.setTextSize(14);
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.rgb(255, 248, 232)));
        chip.setChipStrokeColor(ColorStateList.valueOf(Color.rgb(210, 180, 140)));
        chip.setChipStrokeWidth(1);
        chip.setSingleLine(true);
        chip.setCheckable(false);
        return chip;
    }

    private String buildSearchQuery() {
        String typedQuery = searchEditText.getText().toString().trim();
        StringBuilder queryBuilder = new StringBuilder(typedQuery);

        for (String tag : selectedTags) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append(' ');
            }
            queryBuilder.append(tag);
        }

        return queryBuilder.toString().trim();
    }

    private void openSearchResults() {
        String query = buildSearchQuery();
        if (query.isEmpty()) {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selected_nav", R.id.nav_community);
        intent.putExtra(EXTRA_SEARCH_QUERY, query);
        startActivity(intent);
        finish();
    }

    private void openTravelSetting() {
        Intent intent = new Intent(this, PostTravelSettingActivity.class);
        intent.putExtra(EXTRA_SEARCH_QUERY, buildSearchQuery());
        startActivity(intent);
    }

    private void openMainTab(int navId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selected_nav", navId);
        startActivity(intent);
        finish();
    }
}
