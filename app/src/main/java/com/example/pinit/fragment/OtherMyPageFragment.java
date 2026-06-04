package com.example.pinit.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pinit.R;
import com.example.pinit.model.User;
import com.example.pinit.model.post.Post;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OtherMyPageFragment extends Fragment {

    private static final String ARG_USER_NICKNAME = "user_nickname";

    private String targetNickname;
    private String targetEmail;
    private User otherUser;
    private boolean isFollowing = false;

    private ImageView profileAvatar;
    private TextView profileName;
    private TextView profileBio;
    private TextView followerCount;
    private TextView followingCount;
    private TextView btnFollowToggle;

    private OtherPostAdapter postAdapter;

    public static OtherMyPageFragment newInstance(String nickname) {
        OtherMyPageFragment fragment = new OtherMyPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NICKNAME, nickname);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_other_my_page, container, false);

        profileAvatar = view.findViewById(R.id.otherProfileAvatar);
        profileName = view.findViewById(R.id.otherProfileName);
        profileBio = view.findViewById(R.id.otherProfileBio);
        followerCount = view.findViewById(R.id.otherFollowerCount);
        followingCount = view.findViewById(R.id.otherFollowingCount);
        btnFollowToggle = view.findViewById(R.id.btnFollowToggle);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnFollowToggle.setOnClickListener(v -> toggleFollowStatus());

        RecyclerView recyclerView = view.findViewById(R.id.otherPostRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new OtherPostAdapter(new ArrayList<>());
        recyclerView.setAdapter(postAdapter);

        if (getArguments() != null) {
            targetNickname = getArguments().getString(ARG_USER_NICKNAME);
            if (targetNickname != null && !targetNickname.isEmpty()) {
                findUserByNickname(targetNickname);
            }
        }
        return view;
    }

    private void findUserByNickname(String nickname) {
        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("nickname", nickname)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                        targetEmail = doc.getId();

                        otherUser = new User();
                        otherUser.setNickname(doc.getString("nickname"));
                        otherUser.setBio(doc.getString("bio"));
                        otherUser.setProfileImageUrl(doc.getString("profileImageUrl"));

                        Long followers = doc.getLong("followerCount");
                        otherUser.setFollowerCount(followers != null ? followers.intValue() : 0);

                        Long followings = doc.getLong("followingCount");
                        otherUser.setFollowingCount(followings != null ? followings.intValue() : 0);

                        renderUser();
                    } else {
                        showUserNotFound();
                    }
                })
                .addOnFailureListener(e -> showUserNotFound());
    }

    private void showUserNotFound() {
        profileName.setText(targetNickname != null ? targetNickname : "알 수 없음");
        profileBio.setText("사용자를 찾을 수 없거나 탈퇴한 회원입니다.");
        btnFollowToggle.setVisibility(View.GONE);
    }

    private void renderUser() {
        if (otherUser == null || targetEmail == null) return;

        profileName.setText(otherUser.getNickname());
        profileBio.setText(otherUser.getBio() != null ? otherUser.getBio() : "");
        followerCount.setText(String.valueOf(otherUser.getFollowerCount()));
        followingCount.setText(String.valueOf(otherUser.getFollowingCount()));

        String avatarUrl = otherUser.getProfileImageUrl();
        if (avatarUrl != null && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))) {
            Glide.with(requireContext()).load(avatarUrl).placeholder(R.drawable.bg_profile_avatar).into(profileAvatar);
        }

        btnFollowToggle.setVisibility(View.VISIBLE);

        loadOtherUserPosts();
        checkFollowStatus();
    }

    // 핵심 업데이트: 색인(Index) 에러 우회 및 불량 데이터 방어
    private void loadOtherUserPosts() {
        if (targetNickname == null) return;

        FirebaseFirestore.getInstance().collection("posts")
                .whereEqualTo("userNickname", targetNickname)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Post> postsList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            // 날짜(createdAt)가 숫자로 잘못 들어간 게시물이 있으면 튕기지 않고 무시합니다.
                            postsList.add(doc.toObject(Post.class));
                        } catch (Exception e) {
                            android.util.Log.e("OtherMyPage", "불량 게시물 무시됨: " + doc.getId());
                        }
                    }

                    // 파이어베이스 대신 안드로이드(Java)에서 직접 최신순으로 정렬해 줍니다.
                    Collections.sort(postsList, new Comparator<Post>() {
                        @Override
                        public int compare(Post p1, Post p2) {
                            if (p1.getCreatedAt() == null || p2.getCreatedAt() == null) return 0;
                            return p2.getCreatedAt().compareTo(p1.getCreatedAt()); // 내림차순(최신순)
                        }
                    });

                    postAdapter.updatePosts(postsList);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "게시물을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkFollowStatus() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || targetEmail == null) return;

        String myEmail = auth.getCurrentUser().getEmail();
        if (myEmail == null || myEmail.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("users").document(myEmail)
                .collection("follows").document(targetEmail)
                .get()
                .addOnSuccessListener(doc -> {
                    isFollowing = doc.exists();
                    updateFollowBtnUI();
                });
    }

    private void updateFollowBtnUI() {
        if (isFollowing) {
            btnFollowToggle.setText("언팔로우");
            btnFollowToggle.setBackgroundColor(Color.parseColor("#E0E0E0"));
        } else {
            btnFollowToggle.setText("팔로우");
            btnFollowToggle.setBackgroundResource(R.drawable.bg_profile_button);
        }
    }

    private void toggleFollowStatus() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || targetEmail == null || otherUser == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String myEmail = auth.getCurrentUser().getEmail();
        if (myEmail == null || myEmail.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference myFollowingRef = db.collection("users").document(myEmail).collection("follows").document(targetEmail);
        DocumentReference otherFollowerRef = db.collection("users").document(targetEmail).collection("followers").document(myEmail);

        DocumentReference myUserDoc = db.collection("users").document(myEmail);
        DocumentReference otherUserDoc = db.collection("users").document(targetEmail);

        if (isFollowing) {
            myFollowingRef.delete();
            otherFollowerRef.delete();
            isFollowing = false;

            int newCount = Math.max(0, otherUser.getFollowerCount() - 1);
            otherUser.setFollowerCount(newCount);
            followerCount.setText(String.valueOf(newCount));

            myUserDoc.update("followingCount", FieldValue.increment(-1));
            otherUserDoc.update("followerCount", FieldValue.increment(-1));
        } else {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("timestamp", com.google.firebase.Timestamp.now());

            myFollowingRef.set(data);
            otherFollowerRef.set(data);
            isFollowing = true;

            int newCount = otherUser.getFollowerCount() + 1;
            otherUser.setFollowerCount(newCount);
            followerCount.setText(String.valueOf(newCount));

            myUserDoc.update("followingCount", FieldValue.increment(1));
            otherUserDoc.update("followerCount", FieldValue.increment(1));
        }
        updateFollowBtnUI();
    }

    private static class OtherPostAdapter extends RecyclerView.Adapter<OtherPostAdapter.ViewHolder> {
        private final List<Post> posts;

        OtherPostAdapter(List<Post> posts) {
            this.posts = posts;
        }

        void updatePosts(List<Post> newPosts) {
            this.posts.clear();
            this.posts.addAll(newPosts);
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
            Post post = posts.get(position);
            holder.userName.setText(post.getUserNickname());
            holder.postTitle.setText(post.getTitle());

            holder.profileImage.setImageResource(R.drawable.bg_profile_avatar);
            FirebaseFirestore.getInstance().collection("users").whereEqualTo("nickname", post.getUserNickname()).limit(1).get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.isEmpty()) {
                            String img = snap.getDocuments().get(0).getString("profileImageUrl");
                            if (img != null && (img.startsWith("http"))) {
                                Glide.with(holder.profileImage).load(img).into(holder.profileImage);
                            }
                        }
                    });

            if (post.getCreatedAt() != null) {
                String date = new java.text.SimpleDateFormat("yyyy. MM. dd", java.util.Locale.KOREA).format(post.getCreatedAt().toDate());
                holder.tvPostDate.setText(date);
            }

            holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            holder.tvScrapCount.setText(String.valueOf(post.getScrapCount()));
            holder.tagGroup.removeAllViews();

            if (post.getHashtags() != null) {
                for (String tag : post.getHashtags()) {
                    Chip chip = new Chip(holder.itemView.getContext());
                    chip.setText(tag.startsWith("#") ? tag : "#" + tag);
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.rgb(255, 248, 232)));
                    holder.tagGroup.addView(chip);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) v.getContext();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, PostDetailFragment.newInstance(post.getPostId()))
                        .addToBackStack(null).commit();
            });
        }

        @Override
        public int getItemCount() { return posts.size(); }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImage; TextView userName; TextView postTitle;
            TextView tvCommentCount; TextView tvScrapCount; TextView tvPostDate; ChipGroup tagGroup;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profileImage);
                userName = itemView.findViewById(R.id.userName);
                postTitle = itemView.findViewById(R.id.postTitle);
                tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
                tvScrapCount = itemView.findViewById(R.id.tvScrapCount);
                tvPostDate = itemView.findViewById(R.id.tvPostDate);
                tagGroup = itemView.findViewById(R.id.postTagGroup);
            }
        }
    }
}