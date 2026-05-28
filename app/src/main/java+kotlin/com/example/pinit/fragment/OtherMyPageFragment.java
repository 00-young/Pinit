package com.example.pinit.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.data.MyFollow;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OtherMyPageFragment extends Fragment {
    private static final String ARG_USER_NAME = "user_name";
    private static final String DEFAULT_USER_NAME = "\uD138\uD138\uD55C \uBCF5\uC22D\uC544";
    private static final String DEFAULT_BIO = "\uAC00\uBCBC\uC6B4 \uC77C\uC815\uC73C\uB85C \uB0A8\uAE30\uB294 \uC5EC\uD589 \uAE30\uB85D";

    private TextView btnFollow;
    private TextView followerCount;

    public static OtherMyPageFragment newInstance(String userName) {
        OtherMyPageFragment fragment = new OtherMyPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName == null || userName.trim().isEmpty() ? DEFAULT_USER_NAME : userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_page, container, false);

        String userName = getUserName();
        ImageView profileAvatar = view.findViewById(R.id.profileAvatar);
        TextView profileName = view.findViewById(R.id.profileName);
        TextView profileBio = view.findViewById(R.id.profileBio);
        TextView followingCount = view.findViewById(R.id.followingCount);
        btnFollow = view.findViewById(R.id.btnEditProfile);
        TextView btnNotification = view.findViewById(R.id.btnMyScrap);
        followerCount = view.findViewById(R.id.followerCount);

        profileAvatar.setImageDrawable(null);
        profileName.setText(userName);
        profileBio.setText(DEFAULT_BIO);
        followingCount.setText("0");

        btnFollow.setOnClickListener(v -> toggleFollow());
        btnNotification.setText("");
        btnNotification.setOnClickListener(null);
        btnNotification.setClickable(false);
        btnNotification.setFocusable(false);
        btnNotification.setEnabled(false);

        RecyclerView recyclerView = view.findViewById(R.id.myPostRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new OtherPostAdapter(createDummyPosts(userName)));

        renderFollowState();
        return view;
    }

    private String getUserName() {
        Bundle args = getArguments();
        if (args == null) return DEFAULT_USER_NAME;
        String userName = args.getString(ARG_USER_NAME, DEFAULT_USER_NAME);
        return userName == null || userName.trim().isEmpty() ? DEFAULT_USER_NAME : userName;
    }

    private void toggleFollow() {
        boolean nextFollowing = !MyFollow.isFollowing(requireContext(), MyFollow.USER_PEACH);
        MyFollow.setFollowing(requireContext(), MyFollow.USER_PEACH, nextFollowing);
        renderFollowState();
    }

    private void renderFollowState() {
        boolean following = MyFollow.isFollowing(requireContext(), MyFollow.USER_PEACH);
        btnFollow.setText(following ? "\uC5B8\uD314\uB85C\uC6B0\uD558\uAE30" : "\uD314\uB85C\uC6B0\uD558\uAE30");
        followerCount.setText(following ? "1" : "0");
    }

    private List<OtherPost> createDummyPosts(String userName) {
        List<OtherPost> posts = new ArrayList<>();
        posts.add(new OtherPost(
                userName,
                "1\uBC15 2\uC77C \uC0C1\uD558\uC774 \uC5EC\uD589\uAE30",
                "2026. 05. 20",
                12,
                5,
                "#\uAC10\uC131", "#\uC6B0\uC815 \uC5EC\uD589", "#1\uBC15 2\uC77C"
        ));
        posts.add(new OtherPost(
                userName,
                "\uAE09. \uC0C1\uD558\uC774 \uC5EC\uD589",
                "2026. 05. 18",
                8,
                4,
                "#\uC0C1\uD558\uC774", "#\uB9DB\uC9D1", "#2\uBC15 3\uC77C"
        ));
        return posts;
    }

    private static class OtherPostAdapter extends RecyclerView.Adapter<OtherPostAdapter.ViewHolder> {
        private final List<OtherPost> posts;

        OtherPostAdapter(List<OtherPost> posts) {
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
            OtherPost post = posts.get(position);
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

    private static class OtherPost {
        String userName;
        String title;
        String date;
        int commentCount;
        int scrapCount;
        List<String> tags;

        OtherPost(String userName, String title, String date, int commentCount, int scrapCount, String... tags) {
            this.userName = userName;
            this.title = title;
            this.date = date;
            this.commentCount = commentCount;
            this.scrapCount = scrapCount;
            this.tags = Arrays.asList(tags);
        }
    }
}
