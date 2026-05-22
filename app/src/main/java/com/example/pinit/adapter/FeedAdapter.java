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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> visiblePosts = new ArrayList<>();

    public FeedAdapter() {
        allPosts.add(new Post(
                "털털한 복숭아",
                "1박 2일 상하이 여행기",
                "#감성", "#우정 여행", "#1박 2일"
        ));
        allPosts.add(new Post(
                "냉동된 블루베리",
                "급.상하이 여행",
                "#혼자", "#맛집 탐방", "#2박 3일"
        ));

        visiblePosts.addAll(allPosts);
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
        holder.tagGroup.removeAllViews();

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
                // 어댑터 안에서 프래그먼트 전환을 하기 위해 현재 화면의 Activity 정보를 가져옵니다.
                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) v.getContext();

                // MainActivity의 fragmentContainer 영역에 상세 화면 프래그먼트를 갈아 끼웁니다!
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new com.example.pinit.fragment.PostDetailFragment())
                        .addToBackStack(null) // 뒤로 가기 누르면 다시 피드 목록으로 컴백!
                        .commit();
            }
        });
        // 임시 테스트 용도 더미 데이터
        holder.tvCommentCount.setText("12");
        holder.tvScrapCount.setText("5");
        /*  나중에 백엔드랑 연결해서 진짜 데이터를 넣을 때는 이렇게 바꿉니다.
        holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));
        holder.tvScrapCount.setText(String.valueOf(post.getScrapCount()));
        */
        // 임시 날짜 데이터 세팅 (나중에 서버 연동 시 post.getDate() 등으로 교체)
        holder.tvPostDate.setText("2026. 05. 21");
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView postTitle;
        ChipGroup tagGroup;

        // 🌟 1. 새로 추가한 텍스트뷰 변수 선언
        TextView tvCommentCount;
        TextView tvScrapCount;
        // 🌟 날짜를 보여줄 텍스트뷰 변수 추가
        TextView tvPostDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            postTitle = itemView.findViewById(R.id.postTitle);
            tagGroup = itemView.findViewById(R.id.postTagGroup);
            // 🌟 2. XML의 아이디와 연결해주기
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvScrapCount = itemView.findViewById(R.id.tvScrapCount);
            // 🌟 XML의 날짜 아이디와 연결
            tvPostDate = itemView.findViewById(R.id.tvPostDate);
        }
    }

    private static class Post {
        String userName;
        String title;
        List<String> tags;

        Post(String userName, String title, String... tags) {
            this.userName = userName;
            this.title = title;
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
