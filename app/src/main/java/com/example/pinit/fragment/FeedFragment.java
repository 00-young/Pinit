package com.example.pinit.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.SearchIndex;
import com.example.pinit.SearchRepository;
import com.example.pinit.activity.PostSearchActivity;
import com.example.pinit.adapter.FeedAdapter;
import com.example.pinit.model.post.Post;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath; // 💡 문서 ID 검색을 위한 import 추가됨!
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FeedFragment extends Fragment {

    private final List<Post> postList = new ArrayList<>();

    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private EditText searchEditText;
    private HorizontalScrollView resultTagScroller;
    private ChipGroup resultTagContainer;
    private Button btnSort;

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

        adapter = new FeedAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        String query = "";
        if (getArguments() != null) {
            query = getArguments().getString(PostSearchActivity.EXTRA_SEARCH_QUERY, "");
            ArrayList<String> settings = getArguments().getStringArrayList(PostSearchActivity.EXTRA_TRAVEL_SETTINGS);
            if (settings != null) {
                travelSettingTags.addAll(settings);
            }
        }

        searchEditText = view.findViewById(R.id.searchEditText);
        resultTagScroller = view.findViewById(R.id.resultTagScroller);
        resultTagContainer = view.findViewById(R.id.resultTagContainer);
        btnSort = view.findViewById(R.id.btnSort);

        btnSort.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(getContext(), btnSort);
            popup.getMenu().add("최신순");
            popup.getMenu().add("스크랩순");

            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                btnSort.setText(title + " ▼");

                if (title.equals("최신순")) {
                    adapter.sortPostsByLatest();
                } else if (title.equals("스크랩순")) {
                    adapter.sortPostsByScrap();
                }
                return true;
            });
            popup.show();
        });

        searchEditText.setFocusable(false);
        setInitialQuery(query);

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

        loadPosts();
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

        renderAllTags();
    }

    private void renderAllTags() {
        resultTagContainer.removeAllViews();

        boolean hasAnyTags = !selectedTags.isEmpty() || !travelSettingTags.isEmpty();
        resultTagScroller.setVisibility(hasAnyTags ? View.VISIBLE : View.GONE);

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
                loadPosts(); // 로컬 필터 대신 새로 DB를 찔러 검색합니다.
            });
            resultTagContainer.addView(chip);
        }

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
                loadPosts(); // 로컬 필터 대신 새로 DB를 찔러 검색합니다.
            });
            resultTagContainer.addView(chip);
        }
    }

    private String buildEditableSearchQuery() {
        String typedQuery = searchEditText.getText().toString().trim();
        StringBuilder queryBuilder = new StringBuilder(typedQuery);

        for (String tag : selectedTags) {
            if (queryBuilder.length() > 0) queryBuilder.append(' ');
            queryBuilder.append(tag);
        }

        return queryBuilder.toString().trim();
    }

    private void openSearchScreen() {
        Intent intent = new Intent(requireContext(), PostSearchActivity.class);
        intent.putExtra(PostSearchActivity.EXTRA_SEARCH_QUERY, buildEditableSearchQuery());
        intent.putStringArrayListExtra(PostSearchActivity.EXTRA_TRAVEL_SETTINGS, new ArrayList<>(travelSettingTags));
        startActivity(intent);
    }

    private void loadPosts() {
        String keyword = searchEditText.getText().toString().trim();
        boolean isSearchMode = !keyword.isEmpty() || !selectedTags.isEmpty() || !travelSettingTags.isEmpty();

        if (isSearchMode) {
            // [트랙 2] 검색 조건이 하나라도 있으면 전체 유저 대상으로 DB 검색!
            performSearch();
        } else {
            // [트랙 1] 아무 조건도 없으면 홈 피드 (임시 전체 조회로 변경됨)
            loadHomeFeed();
        }
    }

    // 🛣️ 트랙 1: 홈 피드 불러오기 (임시: 전체 게시물 최신순)
    private void loadHomeFeed() {
        FirebaseFirestore.getInstance().collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20) // 일단 최신 글 20개만 가져오기
                .get()
                .addOnSuccessListener(postSnapshots -> {
                    postList.clear();

                    // 💡 통째로 변환하지 않고, 하나씩 꺼내서 ID를 주입합니다!
                    for (DocumentSnapshot doc : postSnapshots.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            try {
                                // 🌟 Post.kt 수정 없이 강제로 postId를 주입하는 자바의 마법 (리플렉션)
                                java.lang.reflect.Field field = post.getClass().getDeclaredField("postId");
                                field.setAccessible(true);
                                field.set(post, doc.getId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            postList.add(post);
                        }
                    }

                    adapter.updatePosts(postList);
                    adapter.sortPostsByLatest();
                });
    }

    // 🛣️ 트랙 2: 검색 결과 불러오기 (전체 유저 대상)
    // 🛣️ 트랙 2: 검색 결과 불러오기 (전체 유저 대상)
    private void performSearch() {
        String keyword = searchEditText.getText().toString().trim();
        if (keyword.isEmpty()) keyword = null;

        Long travelerCount = null;
        String location = null;
        String filterStartDate = null;
        String filterEndDate = null;

        for (String tag : travelSettingTags) {
            if (tag.contains("명")) {
                try { travelerCount = Long.parseLong(tag.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            } else if (tag.contains("~")) {
                String[] parts = tag.split("~");
                filterStartDate = parts[0].trim().replace("/", "-");
                filterEndDate = parts[1].trim().replace("/", "-");
            } else {
                location = tag;
            }
        }

        SearchRepository repository = new SearchRepository();
        repository.search(
                keyword, travelerCount, location, null, null, filterStartDate, filterEndDate,
                new ArrayList<>(selectedTags),
                searchIndices -> {
                    List<String> postIds = new ArrayList<>();
                    for (SearchIndex index : searchIndices) {
                        if (index.getPostId() != null && !index.getPostId().isEmpty()) {
                            postIds.add(index.getPostId());
                        }
                    }

                    if (postIds.isEmpty()) {
                        postList.clear();
                        adapter.updatePosts(postList);
                        return kotlin.Unit.INSTANCE;
                    }

                    List<String> safePostIds = postIds.size() > 10 ? postIds.subList(0, 10) : postIds;

                    FirebaseFirestore.getInstance().collection("posts")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), safePostIds)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                postList.clear();

                                // 💡 여기서도 하나씩 꺼내서 ID를 주입합니다!
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    Post post = doc.toObject(Post.class);
                                    if (post != null) {
                                        try {
                                            // 🌟 Post.kt 수정 없이 강제로 postId를 주입하는 자바의 마법 (리플렉션)
                                            java.lang.reflect.Field field = post.getClass().getDeclaredField("postId");
                                            field.setAccessible(true);
                                            field.set(post, doc.getId());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        postList.add(post);
                                    }
                                }

                                adapter.updatePosts(postList);
                                adapter.sortPostsByLatest();
                            });
                    return kotlin.Unit.INSTANCE;
                }
        );
    }
}