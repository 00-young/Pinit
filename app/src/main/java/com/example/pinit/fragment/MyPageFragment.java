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
import com.example.pinit.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyPageFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private User currentUser;

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
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

        myPostAdapter = new MyPostAdapter(new ArrayList<>());
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

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            mAuth.signOut();
            androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) requireActivity();
            activity.finish();
            // Assuming LoginActivity will be started by the system or next restart
        });

        // TODO: 개발 완료 후 삭제 예정 (테스트용)
        view.findViewById(R.id.btnTestUsers).setOnClickListener(v -> {
            startActivity(new android.content.Intent(getContext(), com.example.pinit.activity.TestUserListActivity.class));
        });

        setupUserListener();

        return view;
    }

    private void setupUserListener() {
        if (mAuth.getCurrentUser() == null) return;

        String email = mAuth.getCurrentUser().getEmail();
        if (email == null) return;

        userListener = db.collection("users").document(email)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        currentUser = snapshot.toObject(User.class);
                        renderProfile();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
        }
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
        if (currentUser == null) return;

        profileName.setText(currentUser.getNickname());
        profileBio.setText(currentUser.getBio());

        String avatarUrl = currentUser.getProfileImageUrl();
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            // Using a placeholder or existing drawable if available
            profileAvatar.setImageResource(R.drawable.bg_profile_avatar);
        } else {
            try {
                profileAvatar.setImageURI(Uri.parse(avatarUrl));
            } catch (Exception e) {
                // Handle image loading error
            }
        }

        if (myPostAdapter != null) {
            myPostAdapter.updatePosts(createDummyPosts(currentUser.getNickname()));
        }
        followerCount.setText(String.valueOf(currentUser.getFollowerCount()));
        followingCount.setText(String.valueOf(currentUser.getFollowingCount()));
    }

    private void showEditProfileDialog() {
        if (currentUser == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null, false);
        dialogAvatarPreview = dialogView.findViewById(R.id.editProfileAvatar);
        EditText editName = dialogView.findViewById(R.id.editProfileName);
        EditText editBio = dialogView.findViewById(R.id.editProfileBio);
        Button chooseImage = dialogView.findViewById(R.id.btnChooseProfileImage);

        editName.setText(currentUser.getNickname());
        editBio.setText(currentUser.getBio());

        String currentAvatar = currentUser.getProfileImageUrl();
        pendingAvatarUri = (currentAvatar == null || currentAvatar.isEmpty()) ? null : Uri.parse(currentAvatar);
        if (pendingAvatarUri != null) {
            dialogAvatarPreview.setImageURI(pendingAvatarUri);
        }

        chooseImage.setOnClickListener(v -> profileImageLauncher.launch(new String[]{"image/*"}));

        new AlertDialog.Builder(requireContext())
                .setTitle("프로필 수정")
                .setView(dialogView)
                .setNegativeButton("취소", (dialog, which) -> clearDialogState())
                .setPositiveButton("저장", (dialog, which) -> {
                    String newNickname = editName.getText().toString().trim();
                    String newBio = editBio.getText().toString().trim();

                    if (newNickname.isEmpty()) {
                        Toast.makeText(getContext(), "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentReference userRef = db.collection("users").document(currentUser.getEmail());
                    
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("nickname", newNickname);
                    updates.put("bio", newBio);
                    updates.put("updatedAt", FieldValue.serverTimestamp());
                    
                    if (pendingAvatarUri != null) {
                        updates.put("profileImageUrl", pendingAvatarUri.toString());
                    }

                    userRef.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "프로필이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                                clearDialogState();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                clearDialogState();
                            });
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
