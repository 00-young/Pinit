package com.example.pinit.manager;

import com.example.pinit.model.User;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final FirebaseFirestore db;

    private FirebaseManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public interface OnActionListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnCheckListener {
        void onResult(boolean isFollowing);
    }

    public void createNewUser(User user, OnActionListener listener) {
        if (user == null || user.getEmail() == null) {
            if (listener != null) listener.onFailure(new Exception("Invalid user data"));
            return;
        }

        // Set default nickname if empty
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            String email = user.getEmail();
            int atIndex = email.indexOf("@");
            if (atIndex > 0) {
                user.setNickname(email.substring(0, atIndex));
            } else {
                user.setNickname(email);
            }
        }

        // Set default values for new user
        user.setBio("안녕하세요!");
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        user.setPostCount(0);
        user.setScrapCount(0);
        user.setPrivate(false);

        // Convert to Map to inject server timestamps
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", user.getEmail());
        userMap.put("nickname", user.getNickname());
        userMap.put("profileImageUrl", user.getProfileImageUrl());
        userMap.put("bio", user.getBio());
        userMap.put("followerCount", user.getFollowerCount());
        userMap.put("followingCount", user.getFollowingCount());
        userMap.put("postCount", user.getPostCount());
        userMap.put("scrapCount", user.getScrapCount());
        userMap.put("isPrivate", user.isPrivate());
        userMap.put("createdAt", FieldValue.serverTimestamp());
        userMap.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("users").document(user.getEmail())
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e);
                });
    }

    public void followUser(String targetEmail, OnActionListener listener) {
        String myEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (myEmail == null || targetEmail == null || myEmail.equals(targetEmail)) {
            if (listener != null) listener.onFailure(new Exception("Invalid email"));
            return;
        }

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.set(db.collection("users").document(myEmail).collection("follows").document(targetEmail), new HashMap<>());
        batch.set(db.collection("users").document(targetEmail).collection("followers").document(myEmail), new HashMap<>());
        batch.update(db.collection("users").document(myEmail), "followingCount", FieldValue.increment(1));
        batch.update(db.collection("users").document(targetEmail), "followerCount", FieldValue.increment(1));

        batch.commit()
                .addOnSuccessListener(aVoid -> { if (listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e); });
    }

    public void unfollowUser(String targetEmail, OnActionListener listener) {
        String myEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (myEmail == null || targetEmail == null) {
            if (listener != null) listener.onFailure(new Exception("Invalid email"));
            return;
        }

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(myEmail).collection("follows").document(targetEmail));
        batch.delete(db.collection("users").document(targetEmail).collection("followers").document(myEmail));
        batch.update(db.collection("users").document(myEmail), "followingCount", FieldValue.increment(-1));
        batch.update(db.collection("users").document(targetEmail), "followerCount", FieldValue.increment(-1));

        batch.commit()
                .addOnSuccessListener(aVoid -> { if (listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e); });
    }

    public void checkFollowing(String targetEmail, OnCheckListener listener) {
        String myEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (myEmail == null || targetEmail == null) {
            if (listener != null) listener.onResult(false);
            return;
        }

        db.collection("users").document(myEmail).collection("follows").document(targetEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (listener != null) listener.onResult(documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onResult(false);
                });
    }
}
