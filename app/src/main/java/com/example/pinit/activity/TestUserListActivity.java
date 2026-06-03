package com.example.pinit.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pinit.R;
import com.example.pinit.manager.FirebaseManager;
import com.example.pinit.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// TODO: 개발 완료 후 삭제 예정 (테스트용)
public class TestUserListActivity extends AppCompatActivity {

    private TestUserAdapter adapter;
    private final List<User> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_user_list);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.rvTestUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TestUserAdapter(userList);
        recyclerView.setAdapter(adapter);

        loadAllUsers();
    }

    private void loadAllUsers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (myEmail == null) return;

        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            userList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                User user = doc.toObject(User.class);
                if (user != null && !user.getEmail().equals(myEmail)) {
                    userList.add(user);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private static class TestUserAdapter extends RecyclerView.Adapter<TestUserAdapter.ViewHolder> {
        private final List<User> users;

        TestUserAdapter(List<User> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follow_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            holder.name.setText(user.getNickname());
            holder.bio.setText(user.getBio());

            // 초기 상태 설정
            FirebaseManager.getInstance().checkFollowing(user.getEmail(), isFollowing -> {
                holder.actionButton.setText(isFollowing ? "언팔로우하기" : "팔로우하기");
            });

            holder.actionButton.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                FirebaseManager.getInstance().checkFollowing(user.getEmail(), isFollowing -> {
                    if (isFollowing) {
                        FirebaseManager.getInstance().unfollowUser(user.getEmail(), new FirebaseManager.OnActionListener() {
                            @Override
                            public void onSuccess() {
                                // 최신 인덱스 재확인
                                int currentPos = holder.getBindingAdapterPosition();
                                if (currentPos != RecyclerView.NO_POSITION) {
                                    holder.actionButton.setText("팔로우하기");
                                }
                            }
                            @Override
                            public void onFailure(Exception e) {}
                        });
                    } else {
                        FirebaseManager.getInstance().followUser(user.getEmail(), new FirebaseManager.OnActionListener() {
                            @Override
                            public void onSuccess() {
                                int currentPos = holder.getBindingAdapterPosition();
                                if (currentPos != RecyclerView.NO_POSITION) {
                                    holder.actionButton.setText("언팔로우하기");
                                }
                            }
                            @Override
                            public void onFailure(Exception e) {}
                        });
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, bio, actionButton;
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
}
