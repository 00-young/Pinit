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
import com.example.pinit.fragment.PostDetailFragment;
import com.example.pinit.model.post.Post;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private final List<Post> allPosts = new ArrayList<>();

    private final List<Post> visiblePosts = new ArrayList<>();

    public FeedAdapter(List<Post> postList) {

        allPosts.addAll(postList);

        visiblePosts.addAll(postList);
    }

    public void updatePosts(List<Post> newPosts) {

        allPosts.clear();
        visiblePosts.clear();

        allPosts.addAll(newPosts);
        visiblePosts.addAll(newPosts);

        notifyDataSetChanged();
    }

    public void filterByQuery(String query) {

        visiblePosts.clear();

        List<String> selectedTags = extractTags(query);

        if (selectedTags.isEmpty()) {

            visiblePosts.addAll(allPosts);

        } else {

            for (Post post : allPosts) {

                List<String> hashtags = post.getHashtags();

                if (hashtags == null) continue;

                List<String> normalizedTags = new ArrayList<>();

                for (String tag : hashtags) {
                    normalizedTags.add(tag.toLowerCase(Locale.KOREAN));
                }

                if (normalizedTags.containsAll(selectedTags)) {
                    visiblePosts.add(post);
                }
            }
        }

        notifyDataSetChanged();
    }

    public void sortPostsByLatest() {

        Collections.sort(visiblePosts, new Comparator<Post>() {
            @Override
            public int compare(Post p1, Post p2) {

                if (p1.getCreatedAt() == null || p2.getCreatedAt() == null) {
                    return 0;
                }

                return p2.getCreatedAt()
                        .compareTo(p1.getCreatedAt());
            }
        });

        notifyDataSetChanged();
    }

    public void sortPostsByScrap() {

        Collections.sort(visiblePosts, new Comparator<Post>() {
            @Override
            public int compare(Post p1, Post p2) {

                return Integer.compare(
                        p2.getScrapCount(),
                        p1.getScrapCount()
                );
            }
        });

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        Post post = visiblePosts.get(position);

        holder.userName.setText(post.getUserNickname());

        holder.postTitle.setText(post.getTitle());

        if (post.getCreatedAt() != null) {

            String formattedDate =
                    new SimpleDateFormat(
                            "yyyy. MM. dd",
                            Locale.KOREA
                    ).format(post.getCreatedAt().toDate());

            holder.tvPostDate.setText(formattedDate);
        }

        holder.tvScrapCount.setText(
                String.valueOf(post.getScrapCount())
        );

        holder.tvCommentCount.setText(
                String.valueOf(post.getCommentCount())
        );

        holder.tagGroup.removeAllViews();

        View.OnClickListener profileClickListener = v ->
                openOtherProfile(
                        v,
                        post.getUserNickname()
                );

        holder.profileImage.setOnClickListener(
                profileClickListener
        );

        holder.userName.setOnClickListener(
                profileClickListener
        );

        List<String> hashtags = post.getHashtags();

        if (hashtags != null) {

            for (String tag : hashtags) {

                Chip chip = new Chip(holder.itemView.getContext());

                chip.setText(tag);

                chip.setTextColor(
                        Color.rgb(34, 34, 34)
                );

                chip.setTextSize(14);

                chip.setChipBackgroundColor(
                        ColorStateList.valueOf(
                                Color.rgb(255, 248, 232)
                        )
                );

                chip.setChipStrokeColor(
                        ColorStateList.valueOf(
                                Color.rgb(210, 180, 140)
                        )
                );

                chip.setChipStrokeWidth(1);

                chip.setClickable(false);

                holder.tagGroup.addView(chip);
            }
        }

        holder.itemView.setOnClickListener(v -> {

            androidx.appcompat.app.AppCompatActivity activity =
                    (androidx.appcompat.app.AppCompatActivity)
                            v.getContext();

            PostDetailFragment fragment =
                    PostDetailFragment.newInstance(
                            post.getPostId()
                    );

            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragmentContainer,
                            fragment
                    )
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return visiblePosts.size();
    }

    private List<String> extractTags(String query) {

        List<String> tags = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return tags;
        }

        String[] parts = query.trim().split("\\s+#");

        for (String part : parts) {

            String tag = part.trim();

            if (tag.isEmpty()) continue;

            if (!tag.startsWith("#")) {
                tag = "#" + tag;
            }

            tags.add(tag.toLowerCase(Locale.KOREAN));
        }

        return tags;
    }

    private void openOtherProfile(
            View view,
            String userName
    ) {

        androidx.appcompat.app.AppCompatActivity activity =
                (androidx.appcompat.app.AppCompatActivity)
                        view.getContext();

        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        OtherMyPageFragment.newInstance(userName)
                )
                .addToBackStack(null)
                .commit();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        View profileImage;

        TextView userName;

        TextView postTitle;

        ChipGroup tagGroup;

        TextView tvCommentCount;

        TextView tvScrapCount;

        TextView tvPostDate;

        public ViewHolder(@NonNull View itemView) {

            super(itemView);

            profileImage =
                    itemView.findViewById(R.id.profileImage);

            userName =
                    itemView.findViewById(R.id.userName);

            postTitle =
                    itemView.findViewById(R.id.postTitle);

            tagGroup =
                    itemView.findViewById(R.id.postTagGroup);

            tvCommentCount =
                    itemView.findViewById(R.id.tvCommentCount);

            tvScrapCount =
                    itemView.findViewById(R.id.tvScrapCount);

            tvPostDate =
                    itemView.findViewById(R.id.tvPostDate);
        }
    }
}