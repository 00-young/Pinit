package com.example.pinit.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostSearchActivity extends AppCompatActivity {

    public static final String EXTRA_SEARCH_QUERY = "post_search_query";
    public static final String EXTRA_TRAVEL_SETTINGS = "travel_settings";
    public static final String EXTRA_PEOPLE = "post_setting_people"; // 추가: 인원 설정 전달용 키

    private EditText searchEditText;
    private HorizontalScrollView selectedTagScroller;
    private ChipGroup selectedTagContainer;
    private final Set<String> selectedTags = new LinkedHashSet<>();

    private HorizontalScrollView travelSettingTagScroller;
    private ChipGroup travelSettingTagContainer;
    private final List<String> travelSettingTags = new ArrayList<>();

    // 추가: 인원 설정값을 명확하게 분리해서 담아둘 변수
    private String currentSelectedPeople = "";

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
        travelSettingTagScroller = findViewById(R.id.travelSettingTagScroller);
        travelSettingTagContainer = findViewById(R.id.travelSettingTagContainer);
        ImageButton btnRunPostSearch = findViewById(R.id.btnRunPostSearch);

        travelSettingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        travelSettingTags.clear();
                        addTravelSettingTag(data.getStringExtra("selectedDate"));
                        addTravelSettingTag(data.getStringExtra("selectedCountry"));

                        // 추가: 인원 설정을 낚아채서 따로 저장
                        currentSelectedPeople = data.getStringExtra("selectedPeople");
                        if (currentSelectedPeople != null && !currentSelectedPeople.isEmpty()) {
                            addTravelSettingTag(currentSelectedPeople);
                        }

                        renderTravelSettingTags();
                    }
                }
        );

        restoreTravelSettings(getIntent());
        setInitialQuery(getIntent().getStringExtra(EXTRA_SEARCH_QUERY));
        renderTravelSettingTags();

        addTags(findViewById(R.id.tagContainerTogether), togetherTags);
        addTags(findViewById(R.id.tagContainerDuration), durationTags);
        addTags(findViewById(R.id.tagContainerTheme), themeTags);

        findViewById(R.id.btnTravelSetting).setOnClickListener(v -> openTravelSetting());
        findViewById(R.id.btnOpenMyPage).setOnClickListener(v -> openMyPage());

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

    private void restoreTravelSettings(Intent intent) {
        ArrayList<String> settings = intent.getStringArrayListExtra(EXTRA_TRAVEL_SETTINGS);
        if (settings != null) {
            for (String setting : settings) {
                addTravelSettingTag(setting);
            }
        }

        String startDate = intent.getStringExtra(PostTravelSettingActivity.EXTRA_START_DATE);
        String endDate = intent.getStringExtra(PostTravelSettingActivity.EXTRA_END_DATE);
        if (startDate != null && endDate != null) {
            addTravelSettingTag(startDate + " ~ " + endDate);
        } else {
            addTravelSettingTag(startDate);
        }
        addTravelSettingTag(intent.getStringExtra(PostTravelSettingActivity.EXTRA_COUNTRY));

        // 추가: 인원 설정을 복구할 때도 따로 저장해둡니다.
        currentSelectedPeople = intent.getStringExtra(PostTravelSettingActivity.EXTRA_PEOPLE);
        if (currentSelectedPeople != null && !currentSelectedPeople.isEmpty()) {
            addTravelSettingTag(currentSelectedPeople);
        }
    }

    private void addTravelSettingTag(String tag) {
        if (tag == null) return;

        String trimmed = tag.trim();
        if (trimmed.isEmpty() || trimmed.contains("선택")) return;

        if (!travelSettingTags.contains(trimmed)) {
            travelSettingTags.add(trimmed);
        }
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
                // X버튼으로 태그를 지울 때, 인원 설정이면 변수도 같이 비워줌
                if (tag.equals(currentSelectedPeople)) {
                    currentSelectedPeople = "";
                }
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
            chip.setText(tag.startsWith("#") ? tag : "#" + tag);
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
        String visibleQuery = moveTravelSettingsOutOfQuery(incomingQuery == null ? "" : incomingQuery.trim());
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

    private String moveTravelSettingsOutOfQuery(String query) {
        String withoutDate = moveMatchesToTravelSettings(
                query,
                Pattern.compile("\\d{4}/\\d{2}/\\d{2}\\s*~\\s*\\d{4}/\\d{2}/\\d{2}")
        );
        return moveMatchesToTravelSettings(withoutDate, Pattern.compile("\\d+~\\d+명|\\d+명|혼자|가족"));
    }

    private String moveMatchesToTravelSettings(String query, Pattern pattern) {
        Matcher matcher = pattern.matcher(query);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group();
            addTravelSettingTag(match);

            // 정규식으로 걸러진 게 인원 설정이면 변수에 저장
            if (match.contains("명") || match.equals("혼자") || match.equals("가족")) {
                currentSelectedPeople = match;
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(" "));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
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

    private String buildSearchQuery() {
        String typedQuery = searchEditText.getText().toString().trim();
        StringBuilder queryBuilder = new StringBuilder(typedQuery);

        for (String tag : selectedTags) {
            if (queryBuilder.length() > 0) queryBuilder.append(' ');
            queryBuilder.append(tag);
        }

        return queryBuilder.toString().trim();
    }

    private void openSearchResults() {
        String query = buildSearchQuery();

        if (query.isEmpty() && travelSettingTags.isEmpty()) {
            Toast.makeText(this, "검색어 또는 여행 설정을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selected_nav", R.id.nav_community);
        intent.putExtra(EXTRA_SEARCH_QUERY, query);
        intent.putStringArrayListExtra(EXTRA_TRAVEL_SETTINGS, new ArrayList<>(travelSettingTags));

        // 핵심: 인원 설정을 MainActivity에 정확하게 넘겨줍니다
        intent.putExtra(EXTRA_PEOPLE, currentSelectedPeople);

        startActivity(intent);
        finish();
    }

    private void openTravelSetting() {
        Intent intent = new Intent(this, PostTravelSettingActivity.class);
        intent.putExtra(EXTRA_SEARCH_QUERY, buildSearchQuery());
        intent.putStringArrayListExtra(EXTRA_TRAVEL_SETTINGS, new ArrayList<>(travelSettingTags));
        // 여행 설정창으로 갈 때도 현재 인원을 들고 갑니다.
        intent.putExtra(EXTRA_PEOPLE, currentSelectedPeople);
        travelSettingLauncher.launch(intent);
    }

    private void openMyPage() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_MY_PAGE, true);
        startActivity(intent);
        finish();
    }

    private void openMainTab(int navId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("selected_nav", navId);
        startActivity(intent);
        finish();
    }
}