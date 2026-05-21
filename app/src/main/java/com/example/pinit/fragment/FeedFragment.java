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
import com.example.pinit.pinit.FeedAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.LinkedHashSet;
import java.util.Set;

public class FeedFragment extends Fragment {

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private EditText searchEditText;
    private HorizontalScrollView resultTagScroller;
    private ChipGroup resultTagContainer;
    private final Set<String> selectedTags = new LinkedHashSet<>();

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
            query = getArguments().getString(PostSearchActivity.EXTRA_SEARCH_QUERY, "");
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

        fabWritePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new CreatePostFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
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
        renderSelectedTags();
    }

    private void renderSelectedTags() {
        resultTagContainer.removeAllViews();
        resultTagScroller.setVisibility(selectedTags.isEmpty() ? View.GONE : View.VISIBLE);

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
                renderSelectedTags();
                applyFilter();
            });
            resultTagContainer.addView(chip);
        }
    }

    private void applyFilter() {
        adapter.filterByQuery(buildSearchQuery());
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

    private void openSearchScreen() {
        Intent intent = new Intent(requireContext(), PostSearchActivity.class);
        intent.putExtra(PostSearchActivity.EXTRA_SEARCH_QUERY, buildSearchQuery());
        startActivity(intent);
    }
}
