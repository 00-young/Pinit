package com.example.pinit.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

public class MyPageFragment extends Fragment {

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_BIO = "bio";
    private static final String KEY_AVATAR_URI = "avatar_uri";
    private static final String DEFAULT_NICKNAME = "User_1234567";
    private static final String DEFAULT_BIO = "Pinit is good";
    private static final String LEGACY_DEFAULT_NICKNAME = "\uB0C9\uB3D9\uB41C \uBE14\uB8E8\uBCA0\uB9AC";
    private static final String LEGACY_DEFAULT_BIO = "\uC5EC\uD589 \uAE30\uB85D\uC744 \uCC28\uACE1\uCC28\uACE1 \uBAA8\uC73C\uB294 \uC911";

    private ImageView profileAvatar;
    private TextView profileName;
    private TextView profileBio;
    private TextView followerCount;
    private TextView followingCount;
    private Uri pendingAvatarUri;
    private ImageView dialogAvatarPreview;
    private ActivityResultLauncher<String[]> profileImageLauncher;
    private MyPostAdapter myPostAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null || getContext() == null) return;
                    requireContext().getContentResolver().takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    pendingAvatarUri = uri;
                    if (dialogAvatarPreview != null) {
                        dialogAvatarPreview.setImageURI(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_page, container, false);

        profileAvatar = view.findViewById(R.id.profileAvatar);
        profileName = view.findViewById(R.id.profileName);
        profileBio = view.findViewById(R.id.profileBio);
        followerCount = view.findViewById(R.id.followerCount);
        followingCount = view.findViewById(R.id.followingCount);

        myPostAdapter = new MyPostAdapter(createDummyPosts(getNickname()));
        RecyclerView recyclerView = view.findViewById(R.id.myPostRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(myPostAdapter);

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.btnFollowers).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, FollowListFragment.newFollowers())
                        .addToBackStack(null)
                        .commit());
        view.findViewById(R.id.btnFollowing).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, FollowListFragment.newFollowing())
                        .addToBackStack(null)
                        .commit());
        view.findViewById(R.id.btnMyScrap).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new MyScrapFragment())
                        .addToBackStack(null)
                        .commit());
        renderProfile();

        return view;
    }

    private List<MyPost> createDummyPosts(String userName) {
        List<MyPost> posts = new ArrayList<>();
        posts.add(new MyPost(
                userName,
                "\uC544\uC774\uC640 \uD568\uAED8\uD55C \uC81C\uC8FC \uCE74\uD398 \uD22C\uC5B4",
                "2026. 05. 22",
                12,
                5,
                "#\uAC00\uC871", "#\uC81C\uC8FC", "#\uCE74\uD398"
        ));
        posts.add(new MyPost(
                userName,
                "1\uBC15 2\uC77C \uC0C1\uD558\uC774 \uC5EC\uD589\uAE30",
                "2026. 05. 20",
                8,
                11,
                "#\uC0C1\uD558\uC774", "#1\uBC152\uC77C", "#\uB3C4\uC2DC\uC5EC\uD589"
        ));
        posts.add(new MyPost(
                userName,
                "\uBD80\uBAA8\uB2D8\uACFC \uD568\uAED8\uD55C \uACBD\uC8FC \uC5EC\uD589",
                "2026. 05. 14",
                4,
                3,
                "#\uACBD\uC8FC", "#\uBD80\uBAA8\uB2D8", "#\uC5ED\uC0AC\uC5EC\uD589"
        ));
        posts.add(new MyPost(
                userName,
                "\uD63C\uC790 \uB2E4\uB140\uC628 \uBD80\uC0B0 \uB9DB\uC9D1 \uAE30\uB85D",
                "2026. 05. 08",
                6,
                7,
                "#\uBD80\uC0B0", "#\uB9DB\uC9D1", "#\uD63C\uC790\uC5EC\uD589"
        ));
        return posts;
    }

    private void renderProfile() {
        profileName.setText(getNickname());
        profileBio.setText(getBio());

        String avatarUri = getPrefs().getString(KEY_AVATAR_URI, "");
        if (avatarUri == null || avatarUri.isEmpty()) {
            profileAvatar.setImageDrawable(null);
        } else {
            profileAvatar.setImageURI(Uri.parse(avatarUri));
        }

        if (myPostAdapter != null) {
            myPostAdapter.updatePosts(createDummyPosts(getNickname()));
        }
        followerCount.setText(String.valueOf(MyFollow.getFollowerCount()));
        followingCount.setText(String.valueOf(MyFollow.getFollowingCount(requireContext())));
    }

    private String getNickname() {
        return getProfileValue(KEY_NICKNAME, DEFAULT_NICKNAME, LEGACY_DEFAULT_NICKNAME);
    }

    private String getBio() {
        return getProfileValue(KEY_BIO, DEFAULT_BIO, LEGACY_DEFAULT_BIO);
    }

    private String getProfileValue(String key, String defaultValue, String legacyDefaultValue) {
        String value = getPrefs().getString(key, defaultValue);
        if (value == null || value.trim().isEmpty()
                || legacyDefaultValue.equals(value)
                || value.startsWith("\uC5EC\uD589 \uAE30\uB85D\uC744 \uCC28\uACE1\uCC28\uACE1")) {
            getPrefs().edit().putString(key, defaultValue).apply();
            return defaultValue;
        }
        return value;
    }

    private SharedPreferences getPrefs() {
        return requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null, false);
        dialogAvatarPreview = dialogView.findViewById(R.id.editProfileAvatar);
        EditText editName = dialogView.findViewById(R.id.editProfileName);
        EditText editBio = dialogView.findViewById(R.id.editProfileBio);
        Button chooseImage = dialogView.findViewById(R.id.btnChooseProfileImage);

        SharedPreferences prefs = getPrefs();
        editName.setText(getNickname());
        editBio.setText(getBio());

        String savedAvatar = prefs.getString(KEY_AVATAR_URI, "");
        pendingAvatarUri = savedAvatar == null || savedAvatar.isEmpty() ? null : Uri.parse(savedAvatar);
        if (pendingAvatarUri != null) {
            dialogAvatarPreview.setImageURI(pendingAvatarUri);
        }

        chooseImage.setOnClickListener(v -> profileImageLauncher.launch(new String[]{"image/*"}));

        new AlertDialog.Builder(requireContext())
                .setTitle("\uD504\uB85C\uD544 \uC218\uC815")
                .setView(dialogView)
                .setNegativeButton("\uCDE8\uC18C", (dialog, which) -> clearDialogState())
                .setPositiveButton("\uC800\uC7A5", (dialog, which) -> {
                    String nickname = editName.getText().toString().trim();
                    String bio = editBio.getText().toString().trim();
                    if (nickname.isEmpty()) nickname = DEFAULT_NICKNAME;
                    if (bio.isEmpty()) bio = DEFAULT_BIO;

                    SharedPreferences.Editor editor = prefs.edit()
                            .putString(KEY_NICKNAME, nickname)
                            .putString(KEY_BIO, bio);
                    if (pendingAvatarUri != null) {
                        editor.putString(KEY_AVATAR_URI, pendingAvatarUri.toString());
                    }
                    editor.apply();

                    clearDialogState();
                    renderProfile();
                })
                .show();
    }

    private void clearDialogState() {
        pendingAvatarUri = null;
        dialogAvatarPreview = null;
    }

    private static class MyPostAdapter extends RecyclerView.Adapter<MyPostAdapter.ViewHolder> {
        private final List<MyPost> posts = new ArrayList<>();

        MyPostAdapter(List<MyPost> posts) {
            updatePosts(posts);
        }

        void updatePosts(List<MyPost> newPosts) {
            posts.clear();
            posts.addAll(newPosts);
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
            MyPost post = posts.get(position);
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

    private static class MyPost {
        String userName;
        String title;
        String date;
        int commentCount;
        int scrapCount;
        List<String> tags;

        MyPost(String userName, String title, String date, int commentCount, int scrapCount, String... tags) {
            this.userName = userName;
            this.title = title;
            this.date = date;
            this.commentCount = commentCount;
            this.scrapCount = scrapCount;
            this.tags = Arrays.asList(tags);
        }
    }
}
