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
import com.example.pinit.data.MyFollow;

import java.util.ArrayList;
import java.util.List;

public class FollowListFragment extends Fragment {
    private static final String ARG_MODE = "mode";
    private static final String MODE_FOLLOWERS = "followers";
    private static final String MODE_FOLLOWING = "following";

    private FollowUserAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private String mode;

    public static FollowListFragment newFollowers() {
        return newInstance(MODE_FOLLOWERS);
    }

    public static FollowListFragment newFollowing() {
        return newInstance(MODE_FOLLOWING);
    }

    private static FollowListFragment newInstance(String mode) {
        FollowListFragment fragment = new FollowListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_list, container, false);
        mode = getArguments() == null ? MODE_FOLLOWERS : getArguments().getString(ARG_MODE, MODE_FOLLOWERS);

        TextView title = view.findViewById(R.id.followListTitle);
        emptyText = view.findViewById(R.id.emptyFollowText);
        recyclerView = view.findViewById(R.id.followRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        title.setText(MODE_FOLLOWING.equals(mode) ? "\uD314\uB85C\uC6B0" : "\uD314\uB85C\uC6CC");
        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        adapter = new FollowUserAdapter(new ArrayList<>(), () -> renderList());
        recyclerView.setAdapter(adapter);
        renderList();

        return view;
    }

    private void renderList() {
        List<FollowUser> users = createUsers();
        adapter.updateUsers(users);
        emptyText.setText(MODE_FOLLOWING.equals(mode)
                ? "\uD314\uB85C\uC6B0\uD55C \uC0AC\uB78C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4."
                : "\uD314\uB85C\uC6CC\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.");
        emptyText.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(users.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private List<FollowUser> createUsers() {
        List<FollowUser> allUsers = new ArrayList<>();
        allUsers.add(new FollowUser(
                MyFollow.USER_PEACH,
                "\uD138\uD138\uD55C \uBCF5\uC22D\uC544",
                "\uAC00\uBCBC\uC6B4 \uC77C\uC815\uC73C\uB85C \uB0A8\uAE30\uB294 \uC5EC\uD589 \uAE30\uB85D"
        ));
        allUsers.add(new FollowUser(
                MyFollow.USER_MINT,
                "\uC0C1\uD07C\uD55C \uBBFC\uD2B8",
                "\uC8FC\uB9D0\uB9C8\uB2E4 \uC791\uC740 \uB3C4\uC2DC\uB97C \uAC78\uC5B4\uBCF4\uB294 \uC911"
        ));

        if (MODE_FOLLOWERS.equals(mode)) {
            return allUsers;
        }

        List<FollowUser> followingUsers = new ArrayList<>();
        for (FollowUser user : allUsers) {
            if (MyFollow.isFollowing(requireContext(), user.userId)) {
                followingUsers.add(user);
            }
        }
        return followingUsers;
    }

    private static class FollowUserAdapter extends RecyclerView.Adapter<FollowUserAdapter.ViewHolder> {
        private final List<FollowUser> users;
        private final Runnable onFollowChanged;

        FollowUserAdapter(List<FollowUser> users, Runnable onFollowChanged) {
            this.users = users;
            this.onFollowChanged = onFollowChanged;
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
            holder.name.setText(user.name);
            holder.bio.setText(user.bio);
            holder.avatar.setImageDrawable(null);
            renderButton(holder.actionButton, user);

            View.OnClickListener openProfile = v -> {
                androidx.appcompat.app.AppCompatActivity activity =
                        (androidx.appcompat.app.AppCompatActivity) v.getContext();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, OtherMyPageFragment.newInstance(user.name))
                        .addToBackStack(null)
                        .commit();
            };
            holder.avatar.setOnClickListener(openProfile);
            holder.name.setOnClickListener(openProfile);
            holder.bio.setOnClickListener(openProfile);

            holder.actionButton.setOnClickListener(v -> {
                boolean nextFollowing = !MyFollow.isFollowing(v.getContext(), user.userId);
                MyFollow.setFollowing(v.getContext(), user.userId, nextFollowing);
                onFollowChanged.run();
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        private void renderButton(TextView button, FollowUser user) {
            boolean following = MyFollow.isFollowing(button.getContext(), user.userId);
            button.setText(following ? "\uC5B8\uD314\uB85C\uC6B0\uD558\uAE30" : "\uD314\uB85C\uC6B0\uD558\uAE30");
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
