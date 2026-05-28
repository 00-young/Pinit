package com.example.pinit.fragment;

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
import com.example.pinit.manager.FirebaseManager;
import com.example.pinit.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
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
                        String userEmail = doc.getId();
                        db.collection("users").document(userEmail).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        if (user != null) {
                                            users.add(new FollowUser(user.getEmail(), user.getNickname(), user.getBio()));
                                        }
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
            notifyDataSetChanged(); // 실시간 리스너 갱신 시 완전히 도화지를 새로 그림
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
            holder.name.setText(user.name);
            holder.bio.setText(user.bio);
            holder.avatar.setImageDrawable(null);

            // 버튼 상태 초기화 보장
            FirebaseManager.getInstance().checkFollowing(user.userId, isFollowing -> {
                holder.actionButton.setText(isFollowing ? "언팔로우하기" : "팔로우하기");
            });

            View.OnClickListener openProfile = v -> {
                androidx.appcompat.app.AppCompatActivity activity =
                        (androidx.appcompat.app.AppCompatActivity) v.getContext();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, OtherMyPageFragment.newInstanceWithEmail(user.userId))
                        .addToBackStack(null)
                        .commit();
            };
            holder.avatar.setOnClickListener(openProfile);
            holder.name.setOnClickListener(openProfile);
            holder.bio.setOnClickListener(openProfile);

            holder.actionButton.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                // 클릭 연타 및 타임 지연 방지를 위해 버튼을 우선 비활성화
                holder.actionButton.setEnabled(false);

                FirebaseManager.getInstance().checkFollowing(user.userId, isFollowing -> {
                    if (isFollowing) {
                        FirebaseManager.getInstance().unfollowUser(user.userId, new FirebaseManager.OnActionListener() {
                            @Override
                            public void onSuccess() {
                                // 🛠️ 버그 완전 저격 수술 핵심 구역
                                // 팔로잉 탭이든 아니든 로컬에서 리스트를 수동으로 자르지 마세요!
                                // 서버 데이터가 지워지면 상단의 실시간 addSnapshotListener가 감지하여
                                // 알아서 부드럽게 UI를 새로 리로드 해줍니다. 버튼을 다시 활성화만 시킵니다.
                                holder.actionButton.setEnabled(true);
                            }
                            @Override
                            public void onFailure(Exception e) {
                                holder.actionButton.setEnabled(true);
                            }
                        });
                    } else {
                        FirebaseManager.getInstance().followUser(user.userId, new FirebaseManager.OnActionListener() {
                            @Override
                            public void onSuccess() {
                                holder.actionButton.setEnabled(true);
                                holder.actionButton.setText("언팔로우하기");
                            }
                            @Override
                            public void onFailure(Exception e) {
                                holder.actionButton.setEnabled(true);
                            }
                        });
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
            android.widget.ImageView avatar;

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

        FollowUser(String userId, String name, String bio) {
            this.userId = userId;
            this.name = name;
            this.bio = bio;
        }
    }
}