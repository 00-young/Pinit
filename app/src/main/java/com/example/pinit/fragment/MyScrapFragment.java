package com.example.pinit.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.data.MyScrap;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyScrapFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_scrap, container, false);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        RecyclerView recyclerView = view.findViewById(R.id.scrapRecyclerView);
        TextView emptyScrapText = view.findViewById(R.id.emptyScrapText);
        List<ScrapPost> posts = createScrapedPosts();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new ScrapPostAdapter(posts));
        emptyScrapText.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(posts.isEmpty() ? View.GONE : View.VISIBLE);

        return view;
    }

    private List<ScrapPost> createScrapedPosts() {
        List<ScrapPost> posts = new ArrayList<>();
        if (MyScrap.isScraped(requireContext(), MyScrap.POST_ID_SHANGHAI)) {
            posts.add(new ScrapPost(
                    "\uD138\uD138\uD55C \uBCF5\uC22D\uC544",
                    "1\uBC15 2\uC77C \uC0C1\uD558\uC774 \uC5EC\uD589\uAE30",
                    "2026. 05. 20",
                    12,
                    5,
                    "#\uAC10\uC131", "#\uC6B0\uC815 \uC5EC\uD589", "#1\uBC15 2\uC77C"
            ));
        }
        return posts;
    }

    private static class ScrapPostAdapter extends RecyclerView.Adapter<ScrapPostAdapter.ViewHolder> {
        private final List<ScrapPost> posts;

        ScrapPostAdapter(List<ScrapPost> posts) {
            this.posts = posts;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ScrapPost post = posts.get(position);
            holder.userName.setText(post.userName);
            holder.postTitle.setText(post.title);
            holder.tvPostDate.setText(post.date);
            holder.tvCommentCount.setText(String.valueOf(post.commentCount));
            holder.tvScrapCount.setText(String.valueOf(post.scrapCount));

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

            holder.itemView.setOnClickListener(v -> {
                androidx.appcompat.app.AppCompatActivity activity =
                        (androidx.appcompat.app.AppCompatActivity) v.getContext();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new PostDetailFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            TextView userName;
            TextView postTitle;
            TextView tvCommentCount;
            TextView tvScrapCount;
            TextView tvPostDate;
            ChipGroup tagGroup;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                userName = itemView.findViewById(R.id.userName);
                postTitle = itemView.findViewById(R.id.postTitle);
                tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
                tvScrapCount = itemView.findViewById(R.id.tvScrapCount);
                tvPostDate = itemView.findViewById(R.id.tvPostDate);
                tagGroup = itemView.findViewById(R.id.postTagGroup);
            }
        }
    }

    private static class ScrapPost {
        String userName;
        String title;
        String date;
        int commentCount;
        int scrapCount;
        List<String> tags;

        ScrapPost(String userName, String title, String date, int commentCount, int scrapCount, String... tags) {
            this.userName = userName;
            this.title = title;
            this.date = date;
            this.commentCount = commentCount;
            this.scrapCount = scrapCount;
            this.tags = Arrays.asList(tags);
        }
    }
}
