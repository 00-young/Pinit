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

import com.example.pinit.R;
import com.example.pinit.manager.FirebaseManager;
import com.example.pinit.model.User;
import com.example.pinit.model.post.Post;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OtherMyPageFragment extends Fragment {
    private static final String ARG_USER_NAME = "user_name";
    private static final String ARG_USER_EMAIL = "user_email";
    private static final String DEFAULT_USER_NAME = "털털한 복숭아";
    private static final String DEFAULT_BIO = "가벼운 일정으로 남기는 여행 기록";

    private TextView btnFollow;
    private TextView followerCount;
    private TextView followingCount;
    private TextView profileName;
    private TextView profileBio;
    private ImageView profileAvatar;
    private OtherPostAdapter otherPostAdapter;
    
    private String userEmail;
    private String userName;
    private User targetUser;
    private ListenerRegistration userListener;

    public static OtherMyPageFragment newInstance(String userName) {
        OtherMyPageFragment fragment = new OtherMyPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    public static OtherMyPageFragment newInstanceWithEmail(String email) {
        OtherMyPageFragment fragment = new OtherMyPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_page, container, false);

        profileAvatar = view.findViewById(R.id.profileAvatar);
        profileName = view.findViewById(R.id.profileName);
        profileBio = view.findViewById(R.id.profileBio);
        followingCount = view.findViewById(R.id.followingCount);
        followerCount = view.findViewById(R.id.followerCount);
        btnFollow = view.findViewById(R.id.btnEditProfile);
        TextView btnScrap = view.findViewById(R.id.btnMyScrap);
        
        // "내 스크랩" 버튼 비활성화 (상대방 페이지이므로)
        btnScrap.setVisibility(View.GONE);
        view.findViewById(R.id.btnLogout).setVisibility(View.GONE);

        Bundle args = getArguments();
        if (args != null) {
            userEmail = args.getString(ARG_USER_EMAIL);
            userName = args.getString(ARG_USER_NAME);
        }

        if (userEmail != null) {
            setupUserListener(userEmail);
        } else if (userName != null) {
            searchUserByName(userName);
        }

        RecyclerView recyclerView = view.findViewById(R.id.myPostRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // 실제 데이터 연동을 위해 어댑터 초기화 및 연결
        otherPostAdapter = new OtherPostAdapter(new ArrayList<>());
        recyclerView.setAdapter(otherPostAdapter);

        // 내비게이션 비활성화: 상대방의 팔로워/팔로잉 목록은 볼 수 없게 하고 숫자만 표시
        view.findViewById(R.id.btnFollowers).setOnClickListener(null);
        view.findViewById(R.id.btnFollowers).setClickable(false);
        view.findViewById(R.id.btnFollowers).setFocusable(false);
        
        view.findViewById(R.id.btnFollowing).setOnClickListener(null);
        view.findViewById(R.id.btnFollowing).setClickable(false);
        view.findViewById(R.id.btnFollowing).setFocusable(false);

        return view;
    }

    private void searchUserByName(String name) {
        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("nickname", name)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        userEmail = doc.getId();
                        setupUserListener(userEmail);
                    } else {
                        profileName.setText(name);
                        profileBio.setText("사용자를 찾을 수 없습니다.");
                    }
                });
    }

    private void setupUserListener(String email) {
        if (userListener != null) userListener.remove();
        
        userListener = FirebaseFirestore.getInstance().collection("users").document(email)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    targetUser = snapshot.toObject(User.class);
                    if (targetUser != null) {
                        renderUser();
                    }
                });
    }

    private void renderUser() {
        profileName.setText(targetUser.getNickname());
        profileBio.setText(targetUser.getBio());
        followerCount.setText(String.valueOf(targetUser.getFollowerCount()));
        followingCount.setText(String.valueOf(targetUser.getFollowingCount()));
        
        // 상대방이 쓴 실제 게시물 로드 시작
        loadOtherUserPosts(targetUser.getEmail());
        
        // 프로필 이미지 로드 (Glide 등이 없으므로 Placeholder 또는 URI 처리)
        if (targetUser.getProfileImageUrl() != null && !targetUser.getProfileImageUrl().isEmpty()) {
            try {
                profileAvatar.setImageURI(android.net.Uri.parse(targetUser.getProfileImageUrl()));
            } catch (Exception e) {
                profileAvatar.setImageResource(R.drawable.bg_profile_avatar);
            }
        } else {
            profileAvatar.setImageResource(R.drawable.bg_profile_avatar);
        }

        // 팔로우 버튼 상태 업데이트
        FirebaseManager.getInstance().checkFollowing(targetUser.getEmail(), isFollowing -> {
            btnFollow.setText(isFollowing ? "언팔로우하기" : "팔로우하기");
            btnFollow.setOnClickListener(v -> {
                if (isFollowing) {
                    FirebaseManager.getInstance().unfollowUser(targetUser.getEmail(), new FirebaseManager.OnActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "언팔로우했습니다.", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(getContext(), "실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    FirebaseManager.getInstance().followUser(targetUser.getEmail(), new FirebaseManager.OnActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "팔로우했습니다.", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(getContext(), "실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });
    }

    /**
     * 상대방이 쓴 게시물 로드
     */
    private void loadOtherUserPosts(String otherId) {
        if (otherId == null || otherId.isEmpty()) return;
        
        FirebaseFirestore.getInstance().collection("posts")
                .whereEqualTo("userId", otherId) // 식별자 기반 쿼리
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> postsList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        postsList.add(doc.toObject(Post.class));
                    }
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (otherPostAdapter != null) {
                                otherPostAdapter.updatePosts(postsList);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("OtherMyPage", "게시물 로드 실패", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) userListener.remove();
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
            
            if (post.getCreatedAt() != null) {
                String formattedDate = new java.text.SimpleDateFormat("yyyy. MM. dd", java.util.Locale.KOREA)
                        .format(post.getCreatedAt().toDate());
                holder.tvPostDate.setText(formattedDate);
            }
            
            holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));
            holder.tvScrapCount.setText(String.valueOf(post.getScrapCount()));

            holder.tagGroup.removeAllViews();
            for (String tag : post.getHashtags()) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(tag.startsWith("#") ? tag : "#" + tag);
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
                        .replace(R.id.fragmentContainer, PostDetailFragment.newInstance(post.getPostId()))
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
}
