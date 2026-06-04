package com.example.pinit.fragment;

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

import com.bumptech.glide.Glide;
import com.example.pinit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FollowListFragment extends Fragment {
    private static final String ARG_MODE = "mode";
    private static final String ARG_USER_EMAIL = "user_email";
    private static final String MODE_FOLLOWERS = "followers";
    private static final String MODE_FOLLOWING = "following";

    private FollowUserAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private String mode;
    private String userEmail;
    private ListenerRegistration followListener;

    public static FollowListFragment newFollowers() {
        String email = (FirebaseAuth.getInstance().getCurrentUser() != null) ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        return newInstance(MODE_FOLLOWERS, email);
    }

    public static FollowListFragment newFollowers(String email) {
        return newInstance(MODE_FOLLOWERS, email);
    }

    public static FollowListFragment newFollowing() {
        String email = (FirebaseAuth.getInstance().getCurrentUser() != null) ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        return newInstance(MODE_FOLLOWING, email);
    }

    public static FollowListFragment newFollowing(String email) {
        return newInstance(MODE_FOLLOWING, email);
    }

    private static FollowListFragment newInstance(String mode, String email) {
        FollowListFragment fragment = new FollowListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        args.putString(ARG_USER_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_list, container, false);
        Bundle args = getArguments();
        mode = (args != null) ? args.getString(ARG_MODE, MODE_FOLLOWERS) : MODE_FOLLOWERS;
        userEmail = (args != null) ? args.getString(ARG_USER_EMAIL) : null;

        if (userEmail == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }

        TextView title = view.findViewById(R.id.followListTitle);
        emptyText = view.findViewById(R.id.emptyFollowText);
        recyclerView = view.findViewById(R.id.followRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        title.setText(MODE_FOLLOWING.equals(mode) ? "팔로우" : "팔로워");
        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        adapter = new FollowUserAdapter(new ArrayList<>(), mode);
        recyclerView.setAdapter(adapter);
        setupFollowListener();

        return view;
    }

    private void setupFollowListener() {
        if (userEmail == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 파이어베이스 DB 구조에 맞게 "follows" 사용
        String subCollection = MODE_FOLLOWING.equals(mode) ? "follows" : "followers";

        followListener = db.collection("users").document(userEmail).collection(subCollection)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    if (snapshots.isEmpty()) {
                        updateUI(new ArrayList<>());
                        return;
                    }

                    List<FollowUser> users = new ArrayList<>();
                    java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(snapshots.size());

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String fetchedEmail = doc.getId();
                        db.collection("users").document(fetchedEmail).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        // 수정 부분: toObject()를 쓰지 않고 필요한 것만 직접 빼냅니다(튕김 방지)
                                        String nickname = userDoc.getString("nickname");
                                        String bio = userDoc.getString("bio");
                                        String profileImageUrl = userDoc.getString("profileImageUrl");

                                        users.add(new FollowUser(userDoc.getId(), nickname, bio, profileImageUrl));
                                    }
                                    if (count.decrementAndGet() == 0) {
                                        updateUI(users);
                                    }
                                })
                                .addOnFailureListener(err -> {
                                    if (count.decrementAndGet() == 0) {
                                        updateUI(users);
                                    }
                                });
                    }
                });
    }

    private void updateUI(List<FollowUser> users) {
        if (!isAdded()) return;
        adapter.updateUsers(users);
        emptyText.setText(MODE_FOLLOWING.equals(mode)
                ? "팔로우한 사람이 없습니다."
                : "팔로워가 없습니다.");
        emptyText.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(users.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (followListener != null) {
            followListener.remove();
        }
    }

    private static class FollowUserAdapter extends RecyclerView.Adapter<FollowUserAdapter.ViewHolder> {
        private final List<FollowUser> users;
        private final String mode;

        FollowUserAdapter(List<FollowUser> users, String mode) {
            this.users = users;
            this.mode = mode;
        }

        void updateUsers(List<FollowUser> newUsers) {
            users.clear();
            users.addAll(newUsers);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follow_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FollowUser user = users.get(position);
            holder.name.setText(user.name != null ? user.name : "알 수 없음");
            holder.bio.setText(user.bio != null ? user.bio : "");

            if (user.profileImageUrl != null && (user.profileImageUrl.startsWith("http://") || user.profileImageUrl.startsWith("https://"))) {
                Glide.with(holder.itemView.getContext())
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.bg_profile_avatar)
                        .error(R.drawable.bg_profile_avatar)
                        .into(holder.avatar);
            } else {
                holder.avatar.setImageResource(R.drawable.bg_profile_avatar);
            }

            View.OnClickListener openProfile = v -> {
                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) v.getContext();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, OtherMyPageFragment.newInstance(user.name)) // 닉네임 사용
                        .addToBackStack(null)
                        .commit();
            };
            holder.avatar.setOnClickListener(openProfile);
            holder.name.setOnClickListener(openProfile);
            holder.bio.setOnClickListener(openProfile);

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) return;
            String myEmail = auth.getCurrentUser().getEmail();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            DocumentReference myFollowingRef = db.collection("users").document(myEmail).collection("follows").document(user.userId);
            DocumentReference otherFollowerRef = db.collection("users").document(user.userId).collection("followers").document(myEmail);

            myFollowingRef.get().addOnSuccessListener(doc -> {
                boolean isFollowing = doc.exists();
                holder.actionButton.setText(isFollowing ? "언팔로우" : "팔로우");

                holder.actionButton.setOnClickListener(v -> {
                    holder.actionButton.setEnabled(false);

                    if (holder.actionButton.getText().toString().equals("언팔로우")) {
                        myFollowingRef.delete();
                        otherFollowerRef.delete();
                        db.collection("users").document(myEmail).update("followingCount", FieldValue.increment(-1));
                        db.collection("users").document(user.userId).update("followerCount", FieldValue.increment(-1));

                        holder.actionButton.setText("팔로우");
                        holder.actionButton.setEnabled(true);
                    } else {
                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        data.put("timestamp", com.google.firebase.Timestamp.now());

                        myFollowingRef.set(data);
                        otherFollowerRef.set(data);
                        db.collection("users").document(myEmail).update("followingCount", FieldValue.increment(1));
                        db.collection("users").document(user.userId).update("followerCount", FieldValue.increment(1));

                        holder.actionButton.setText("언팔로우");
                        holder.actionButton.setEnabled(true);
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView bio;
            TextView actionButton;
            ImageView avatar;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.followAvatar);
                name = itemView.findViewById(R.id.followName);
                bio = itemView.findViewById(R.id.followBio);
                actionButton = itemView.findViewById(R.id.followActionButton);
            }
        }
    }

    private static class FollowUser {
        String userId;
        String name;
        String bio;
        String profileImageUrl;

        FollowUser(String userId, String name, String bio, String profileImageUrl) {
            this.userId = userId;
            this.name = name;
            this.bio = bio;
            this.profileImageUrl = profileImageUrl;
        }
    }
}