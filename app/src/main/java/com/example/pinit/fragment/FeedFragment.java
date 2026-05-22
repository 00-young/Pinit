package com.example.pinit.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.activity.PostSearchActivity;
import com.example.pinit.adapter.FeedAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FeedFragment extends Fragment {

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private EditText searchEditText;
    private HorizontalScrollView resultTagScroller;
    private ChipGroup resultTagContainer;

    // 기존 태그와 여행 설정 태그를 따로 관리합니다.
    private final Set<String> selectedTags = new LinkedHashSet<>();
    private final List<String> travelSettingTags = new ArrayList<>();

    private final String[] knownTags = {
            "#아이와 함께", "#부모님과 함께", "#친구와 함께",
            "#가족들과 함께", "#신혼여행 맞춤", "#커플 여행",
            "#혼자 놀아도 좋은", "#당일치기", "#1박 2일",
            "#2박 3일", "#3박 4일", "#4박 5일",
            "#5일 이상", "#10일 이상", "#한 달 살기",
            "#장기 여행", "#관광 명소", "#맛집 투어",
            "#카페 투어", "#귀여운 캐릭터를 찾아", "#자연 경관",
            "#야경"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        recyclerView = view.findViewById(R.id.feedRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FeedAdapter();
        recyclerView.setAdapter(adapter);

        String query = "";
        if (getArguments() != null) {
            // 1. 검색어 꺼내기
            query = getArguments().getString(PostSearchActivity.EXTRA_SEARCH_QUERY, "");

            //  2. MainActivity에서 넣어준 여행 설정 데이터 꺼내기
            ArrayList<String> settings = getArguments().getStringArrayList("travel_settings");
            if (settings != null) {
                travelSettingTags.addAll(settings);
            }
        }

        searchEditText = view.findViewById(R.id.searchEditText);
        resultTagScroller = view.findViewById(R.id.resultTagScroller);
        resultTagContainer = view.findViewById(R.id.resultTagContainer);

        searchEditText.setFocusable(false);
        setInitialQuery(query);
        applyFilter();

        searchEditText.setOnClickListener(v -> openSearchScreen());

        View btnOpenMyPage = view.findViewById(R.id.btnOpenMyPage);
        btnOpenMyPage.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new MyPageFragment())
                .addToBackStack(null)
                .commit());

        View fabWritePost = view.findViewById(R.id.fabWritePost);
        fabWritePost.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new CreatePostFragment())
                .addToBackStack(null)
                .commit());

        return view;
    }

    private void setInitialQuery(String incomingQuery) {
        String visibleQuery = incomingQuery == null ? "" : incomingQuery.trim();

        for (String tag : knownTags) {
            if (visibleQuery.contains(tag)) {
                selectedTags.add(tag);
                visibleQuery = visibleQuery.replace(tag, " ");
            }
        }

        visibleQuery = visibleQuery.replaceAll("\\s+", " ").trim();
        searchEditText.setText(visibleQuery);

        // 두 종류의 태그를 모두 그리는 함수로 이름 변경
        renderAllTags();
    }

    // 기존 태그(베이지)와 여행 설정 태그(흰색)를 함께 그려주는 핵심 로직
    private void renderAllTags() {
        resultTagContainer.removeAllViews();

        // 두 태그 리스트가 모두 비어있을 때만 스크롤 영역을 숨깁니다.
        boolean hasAnyTags = !selectedTags.isEmpty() || !travelSettingTags.isEmpty();
        resultTagScroller.setVisibility(hasAnyTags ? View.VISIBLE : View.GONE);

        // 1. 기존 해시태그 렌더링 (베이지색)
        for (String tag : selectedTags) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setTextColor(Color.rgb(34, 34, 34));
            chip.setTextSize(14);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.rgb(255, 248, 232)));
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.rgb(210, 180, 140)));
            chip.setChipStrokeWidth(1);
            chip.setSingleLine(true);
            chip.setCheckable(false);
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ColorStateList.valueOf(Color.rgb(120, 100, 70)));
            chip.setOnCloseIconClickListener(v -> {
                selectedTags.remove(tag);
                renderAllTags();
                applyFilter();
            });
            resultTagContainer.addView(chip);
        }

        // 2. 여행 설정 태그 렌더링 (하얀색 바탕, 얇은 회색 테두리)
        for (String tag : travelSettingTags) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setTextColor(Color.rgb(34, 34, 34));
            chip.setTextSize(14);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.WHITE));
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.rgb(221, 221, 221)));
            chip.setChipStrokeWidth(1);
            chip.setSingleLine(true);
            chip.setCheckable(false);
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ColorStateList.valueOf(Color.rgb(120, 100, 70)));
            chip.setOnCloseIconClickListener(v -> {
                travelSettingTags.remove(tag);
                renderAllTags();
                applyFilter();
            });
            resultTagContainer.addView(chip);
        }
    }

    private void applyFilter() {
        adapter.filterByQuery(buildSearchQuery());
    }

    // 어댑터(게시물 목록)가 필터링을 제대로 할 수 있도록, 여행 설정 태그도 검색어에 포함시켜 줍니다.
    private String buildSearchQuery() {
        String typedQuery = searchEditText.getText().toString().trim();
        StringBuilder queryBuilder = new StringBuilder(typedQuery);

        for (String tag : selectedTags) {
            if (queryBuilder.length() > 0) queryBuilder.append(' ');
            queryBuilder.append(tag);
        }
        for (String tag : travelSettingTags) {
            if (queryBuilder.length() > 0) queryBuilder.append(' ');
            queryBuilder.append(tag);
        }

        return queryBuilder.toString().trim();
    }

    private void openSearchScreen() {
        Intent intent = new Intent(requireContext(), PostSearchActivity.class);
        // 검색바를 눌러 돌아갈 때는 모든 조건을 하나로 뭉쳐서 가져갑니다.
        intent.putExtra(PostSearchActivity.EXTRA_SEARCH_QUERY, buildSearchQuery());
        startActivity(intent);
    }
}