package com.example.pinit.fragment;

import android.graphics.Color;
import android.content.res.ColorStateList;
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
import com.example.pinit.model.post.Post;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MyScrapFragment extends Fragment {

    private ScrapPostAdapter adapter;
    private TextView emptyScrapText;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_scrap, container, false);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        recyclerView = view.findViewById(R.id.scrapRecyclerView);
        emptyScrapText = view.findViewById(R.id.emptyScrapText);

        adapter = new ScrapPostAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadScrappedPosts();

        return view;
    }
    private void loadScrappedPosts() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collectionGroup("scrap")
                .whereEqualTo("userId", currentUid) // 👈 안전한 필드 기반 쿼리 (크래시 해결 핵심)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> postIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // scrap 문서의 상위 게시글 ID 추출 (posts/{postId}/scrap/{UID})
                        // getParent()는 'scrap' 컬렉션, 그 부모의 getParent()는 특정 post 문서를 가리킴
                        if (doc.getReference().getParent() != null && doc.getReference().getParent().getParent() != null) {
                            String postId = doc.getReference().getParent().getParent().getId();
                            postIds.add(postId);
                        }
                    }

                    if (postIds.isEmpty()) {
                        updateUI(new ArrayList<>());
                        return;
                    }

                    fetchPostsByIds(postIds);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MyScrapFragment", "스크랩 목록 로드 실패", e);
                    updateUI(new ArrayList<>());
                });
    }

    private void fetchPostsByIds(List<String> postIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Post> postList = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(postIds.size());

        for (String id : postIds) {
            db.collection("posts").document(id).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Post post = documentSnapshot.toObject(Post.class);
                            if (post != null) postList.add(post);
                        }
                        if (count.decrementAndGet() == 0) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> updateUI(postList));
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (count.decrementAndGet() == 0) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> updateUI(postList));
                            }
                        }
                    });
        }
    }

    private void updateUI(List<Post> posts) {
        if (!isAdded()) return;
        adapter.updatePosts(posts);
        emptyScrapText.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(posts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static class ScrapPostAdapter extends RecyclerView.Adapter<ScrapPostAdapter.ViewHolder> {
        private final List<Post> posts = new ArrayList<>();

        ScrapPostAdapter(List<Post> posts) {
            updatePosts(posts);
        }

        void updatePosts(List<Post> newPosts) {
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
            Post post = posts.get(position);
            holder.userName.setText(post.getUserNickname());
            holder.postTitle.setText(post.getTitle());
            loadProfileImage(holder.profileImage, post.getUserEmail());

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
            ImageView profileImage;
            TextView userName;
            TextView postTitle;
            TextView tvCommentCount;
            TextView tvScrapCount;
            TextView tvPostDate;
            ChipGroup tagGroup;

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

        private void loadProfileImage(ImageView imageView, String email) {
            imageView.setImageResource(R.drawable.bg_profile_avatar);
            if (email == null || email.isEmpty()) return;

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(email)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String imageUrl = snapshot.getString("profileImageUrl");
                        if (imageUrl != null
                                && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                            Glide.with(imageView)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.bg_profile_avatar)
                                    .error(R.drawable.bg_profile_avatar)
                                    .into(imageView);
                        }
                    });
        }
    }
}
