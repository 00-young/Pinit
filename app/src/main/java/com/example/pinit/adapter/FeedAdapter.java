package com.example.pinit.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.fragment.OtherMyPageFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections; // 정렬을 위해 추가
import java.util.Comparator; // 정렬 기준 설정을 위해 추가
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> visiblePosts = new ArrayList<>();

    public FeedAdapter() {
        // 더미 데이터에 임의의 날짜와 스크랩 수를 추가했습니다.
        allPosts.add(new Post(
                "털털한 복숭아",
                "1박 2일 상하이 여행기",
                "2026. 05. 21", // 날짜 추가
                5, // 스크랩 수 추가
                "#감성", "#우정 여행", "#1박 2일"
        ));
        allPosts.add(new Post(
                "냉동된 블루베리",
                "급.상하이 여행",
                "2026. 05. 23", // 날짜 추가
                12, // 스크랩 수 추가
                "#혼자", "#맛집 탐방", "#2박 3일"
        ));

        visiblePosts.addAll(allPosts);
        // 기본적으로 최신순으로 정렬되도록 초기화합니다.
        sortPostsByLatest();
    }

    public void filterByQuery(String query) {
        visiblePosts.clear();
        List<String> selectedTags = extractTags(query);

        if (selectedTags.isEmpty()) {
            visiblePosts.addAll(allPosts);
        } else {
            for (Post post : allPosts) {
                if (post.hasAllTags(selectedTags)) {
                    visiblePosts.add(post);
                }
            }
        }

        notifyDataSetChanged();
    }

    // 1. 최신순 정렬 메서드
    public void sortPostsByLatest() {
        Collections.sort(visiblePosts, new Comparator<Post>() {
            @Override
            public int compare(Post p1, Post p2) {
                // 날짜 문자열 비교 (내림차순)
                return p2.date.compareTo(p1.date);
            }
        });
        notifyDataSetChanged();
    }

    // 2. 스크랩순 정렬 메서드
    public void sortPostsByScrap() {
        Collections.sort(visiblePosts, new Comparator<Post>() {
            @Override
            public int compare(Post p1, Post p2) {
                // 스크랩 수 비교 (내림차순)
                return Integer.compare(p2.scrapCount, p1.scrapCount);
            }
        });
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = visiblePosts.get(position);
        holder.userName.setText(post.userName);
        holder.postTitle.setText(post.title);

        // 🌟 Post 객체의 실제 데이터를 사용하도록 변경
        holder.tvPostDate.setText(post.date);
        holder.tvScrapCount.setText(String.valueOf(post.scrapCount));

        // 댓글 수는 일단 고정값으로 둡니다. 나중에 백엔드와 연결 시 수정하세요.
        holder.tvCommentCount.setText("12");

        holder.tagGroup.removeAllViews();
        View.OnClickListener profileClickListener = v -> openOtherProfile(v, post.userName);
        holder.profileImage.setOnClickListener(profileClickListener);
        holder.userName.setOnClickListener(profileClickListener);

        for (String tag : post.tags) {
            Chip chip = new Chip(holder.itemView.getContext());
            chip.setText(tag);
            chip.setTextColor(Color.rgb(34, 34, 34));
            chip.setTextSize(14);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.rgb(255, 248, 232)));
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.rgb(210, 180, 140)));
            chip.setChipStrokeWidth(1);
            chip.setClickable(false);
            holder.tagGroup.addView(chip);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) v.getContext();

                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new com.example.pinit.fragment.PostDetailFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override
    public int getItemCount() {
        return visiblePosts.size();
    }

    private List<String> extractTags(String query) {
        List<String> tags = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return tags;

        String[] parts = query.trim().split("\\s+#");
        for (String part : parts) {
            String tag = part.trim();
            if (tag.isEmpty()) continue;
            if (!tag.startsWith("#")) tag = "#" + tag;
            tags.add(tag.toLowerCase(Locale.KOREAN));
        }
        return tags;
    }

    private void openOtherProfile(View view, String userName) {
        androidx.appcompat.app.AppCompatActivity activity =
                (androidx.appcompat.app.AppCompatActivity) view.getContext();
        
        // 닉네임으로 검색하여 이메일을 찾은 뒤 이동하는 로직이 OtherMyPageFragment 내부에 있으므로
        // 닉네임만 넘겨주는 기존 방식(newInstance)을 유지하되, 내부에서 이메일을 찾아 리스너를 붙이도록 설계했습니다.
        activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, OtherMyPageFragment.newInstance(userName))
                .addToBackStack(null)
                .commit();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View profileImage;
        TextView userName;
        TextView postTitle;
        ChipGroup tagGroup;
        TextView tvCommentCount;
        TextView tvScrapCount;
        TextView tvPostDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            userName = itemView.findViewById(R.id.userName);
            postTitle = itemView.findViewById(R.id.postTitle);
            tagGroup = itemView.findViewById(R.id.postTagGroup);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvScrapCount = itemView.findViewById(R.id.tvScrapCount);
            tvPostDate = itemView.findViewById(R.id.tvPostDate);
        }
    }

    // Post 클래스에 날짜와 스크랩 수 추가
    private static class Post {
        String userName;
        String title;
        String date; // 추가
        int scrapCount; // 추가
        List<String> tags;

        Post(String userName, String title, String date, int scrapCount, String... tags) {
            this.userName = userName;
            this.title = title;
            this.date = date;
            this.scrapCount = scrapCount;
            this.tags = Arrays.asList(tags);
        }

        boolean hasAllTags(List<String> selectedTags) {
            List<String> normalizedTags = new ArrayList<>();
            for (String tag : tags) {
                normalizedTags.add(tag.toLowerCase(Locale.KOREAN));
            }
            return normalizedTags.containsAll(selectedTags);
        }
    }
}