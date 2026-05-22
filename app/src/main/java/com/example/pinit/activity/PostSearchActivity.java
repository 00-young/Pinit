package com.example.pinit.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.HorizontalScrollView;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    // 기존 해시태그용 변수
    private HorizontalScrollView selectedTagScroller;
    private ChipGroup selectedTagContainer;
    private final Set<String> selectedTags = new LinkedHashSet<>();

    // 여행설정 태그용 변수
    private HorizontalScrollView travelSettingTagScroller;
    private ChipGroup travelSettingTagContainer;
    private final List<String> travelSettingTags = new ArrayList<>();
    private ActivityResultLauncher<Intent> travelSettingLauncher;

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

        // 새로 추가된 XML 아이디 연결
        travelSettingTagScroller = findViewById(R.id.travelSettingTagScroller);
        travelSettingTagContainer = findViewById(R.id.travelSettingTagContainer);

        ImageButton btnRunPostSearch = findViewById(R.id.btnRunPostSearch);

        // 여행 설정 화면 런처
        travelSettingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String date = data.getStringExtra("selectedDate");
                        String country = data.getStringExtra("selectedCountry");
                        String people = data.getStringExtra("selectedPeople");

                        // 기존 여행설정 태그 초기화 후 새로 담기
                        travelSettingTags.clear();
                        if (date != null && !date.trim().isEmpty() && !date.equals("날짜를 선택하세요")) {
                            travelSettingTags.add(date);
                        }
                        if (country != null && !country.trim().isEmpty()) {
                            travelSettingTags.add(country);
                        }
                        if (people != null && !people.trim().isEmpty()) {
                            travelSettingTags.add(people);
                        }

                        renderTravelSettingTags();
                    }
                }
        );

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

    private void renderTravelSettingTags() {
        travelSettingTagContainer.removeAllViews();

        for (String tag : travelSettingTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setTextColor(Color.rgb(34, 34, 34));
            chip.setTextSize(14);

            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.WHITE));
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.rgb(221, 221, 221)));
            chip.setChipStrokeWidth(1);

            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ColorStateList.valueOf(Color.rgb(120, 100, 70)));
            chip.setOnCloseIconClickListener(v -> {
                travelSettingTags.remove(tag);
                renderTravelSettingTags();
            });
            travelSettingTagContainer.addView(chip);
        }

        travelSettingTagScroller.post(() -> travelSettingTagScroller.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
    }

    private void renderSelectedTags() {
        selectedTagContainer.removeAllViews();

        for (String tag : selectedTags) {
            Chip chip = baseChip();
            if (tag.startsWith("#")) {
                chip.setText(tag);
            } else {
                chip.setText("#" + tag);
            }
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
        chip.setOnClickListener(v -> {
            if (selectedTags.add(tag)) {
                renderSelectedTags();
            }
        });
        return chip;
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

    //  일반 해시태그와 입력된 텍스트만 문자열로 만듭니다. (여행 설정 태그는 제외됨)
    private String buildSearchQuery() {
        String typedQuery = searchEditText.getText().toString().trim();
        StringBuilder queryBuilder = new StringBuilder(typedQuery);

        for (String tag : selectedTags) {
            if (queryBuilder.length() > 0) queryBuilder.append(' ');
            queryBuilder.append(tag);
        }

        return queryBuilder.toString().trim();
    }

    // 일반 검색어와 여행 설정 데이터를 따로따로 분리해서 전송합니다.
    private void openSearchResults() {
        String query = buildSearchQuery();

        // 둘 다 비어있을 때만 막도록 조건을 수정했습니다. (장소만 선택하고 검색해도 검색되도록)
        if (query.isEmpty() && travelSettingTags.isEmpty()) {
            Toast.makeText(this, "검색어 또는 여행 설정을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selected_nav", R.id.nav_community);

        // 1. 일반 검색어 텍스트 전송
        intent.putExtra(EXTRA_SEARCH_QUERY, query);

        // 2. 여행 설정 태그(리스트) 별도 전송
        intent.putStringArrayListExtra("travel_settings", new ArrayList<>(travelSettingTags));

        startActivity(intent);
        finish();
    }

    private void openTravelSetting() {
        Intent intent = new Intent(this, PostTravelSettingActivity.class);
        intent.putExtra(EXTRA_SEARCH_QUERY, buildSearchQuery());
        travelSettingLauncher.launch(intent);
    }

    private void openMainTab(int navId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selected_nav", navId);
        startActivity(intent);
        finish();
    }
}