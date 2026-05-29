package com.example.pinit.manager;

import com.example.pinit.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import android.util.Log;
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

    /**
     * 회원가입 시 FCM 토큰을 함께 안전하게 저장합니다. (실패해도 빈 값으로 가입 처리 진행)
     */
    public void createNewUser(User user, OnActionListener listener) {
        if (user == null || user.getEmail() == null) {
            if (listener != null) listener.onFailure(new Exception("Invalid user data"));
            return;
        }

        // 닉네임 기본값 설정
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            String email = user.getEmail();
            int atIndex = email.indexOf("@");
            if (atIndex > 0) {
                user.setNickname(email.substring(0, atIndex));
            } else {
                user.setNickname(email);
            }
        }

        // 기본값 세팅
        user.setBio("안녕하세요!");
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        user.setPostCount(0);
        user.setScrapCount(0);
        user.setPrivate(false);

        // 💡 FCM 토큰을 먼저 비동기로 가져옵니다.
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String fcmToken = "";
            if (task.isSuccessful() && task.getResult() != null) {
                fcmToken = task.getResult();
                Log.d("FirebaseManager", "FCM 토큰 획득 성공: " + fcmToken);
            } else {
                // 구글 서비스가 먹통이어도 에러를 로그로만 찍고 빈 값으로 계속 진행 (무한 로딩 방지)
                Log.w("FirebaseManager", "FCM 토큰 획득 실패 (빈 값으로 대체)", task.getException());
            }

            // Map 데이터 구성
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
            userMap.put("fcmToken", fcmToken); // 💡 여기에 토큰 주입 (실패 시 "")

            userMap.put("budget_type", user.getBudget_type());
            userMap.put("age_group", user.getAge_group());
            userMap.put("companion", user.getCompanion());
            userMap.put("theme", user.getTheme());

            userMap.put("createdAt", FieldValue.serverTimestamp());
            userMap.put("updatedAt", FieldValue.serverTimestamp());

            // Firestore에 한 번에 저장
            db.collection("users").document(user.getEmail())
                    .set(userMap)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("FirebaseManager", "유저 회원가입 문서 생성 성공");
                        if (listener != null) listener.onSuccess(); // 💡 무한 로딩 해결 (성공 리스너 트리거)
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirebaseManager", "유저 회원가입 문서 생성 실패", e);
                        if (listener != null) listener.onFailure(e);
                    });
        });
    }

    /**
     * 로그인 상태에서 수동으로 FCM 토큰을 갱신할 때만 사용합니다.
     */
    public void updateFcmToken() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email == null) return;

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FirebaseManager", "FCM 토큰 발행 실패", task.getException());
                return;
            }

            String token = task.getResult();
            db.collection("users").document(email)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d("FirebaseManager", "FCM 토큰 DB 업데이트 성공"))
                    .addOnFailureListener(e -> Log.e("FirebaseManager", "FCM 토큰 DB 업데이트 실패", e));
        });
    }

    public void followUser(String targetEmail, OnActionListener listener) {
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (myEmail == null || targetEmail == null || myEmail.equals(targetEmail)) {
            if (listener != null) listener.onFailure(new Exception("Invalid email"));
            return;
        }

        // 💡 팔로우/팔로워 문서 생성 시 createdAt 필드 추가
        Map<String, Object> followData = new HashMap<>();
        followData.put("createdAt", FieldValue.serverTimestamp());

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.set(db.collection("users").document(myEmail).collection("follows").document(targetEmail), followData);
        batch.set(db.collection("users").document(targetEmail).collection("followers").document(myEmail), followData);
        batch.update(db.collection("users").document(myEmail), "followingCount", FieldValue.increment(1));
        batch.update(db.collection("users").document(targetEmail), "followerCount", FieldValue.increment(1));

        batch.commit()
                .addOnSuccessListener(aVoid -> { if (listener != null) listener.onSuccess(); })
                .addOnFailureListener(e -> { if (listener != null) listener.onFailure(e); });
    }

    public void unfollowUser(String targetEmail, OnActionListener listener) {
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
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
        String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
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
    /**
     * 사용자의 여행 성향 및 필터를 수정합니다.
     */
    public void updateUserSurvey(String email, String budgetType, String ageGroup, String companion, String theme, OnActionListener listener) {
        if (email == null) return;

        // 업데이트할 필드들만 Map으로 구성
        Map<String, Object> updates = new HashMap<>();
        updates.put("budget_type", budgetType);
        updates.put("age_group", ageGroup);
        updates.put("companion", companion);
        updates.put("theme", theme);
        updates.put("updatedAt", FieldValue.serverTimestamp()); // 수정 시간 갱신

        // .update() 메서드로 해당 필드만 덮어쓰기
        db.collection("users").document(email)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseManager", "성향 정보 업데이트 성공");
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseManager", "성향 정보 업데이트 실패", e);
                    if (listener != null) listener.onFailure(e);
                });
    }
}