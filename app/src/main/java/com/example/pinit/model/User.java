package com.example.pinit.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String bio;
    private int followerCount;
    private int followingCount;
    private int postCount;
    private int scrapCount;
    private boolean isPrivate;
    
    @ServerTimestamp
    private Date createdAt;
    
    @ServerTimestamp
    private Date updatedAt;

    // Required empty constructor for Firestore
    public User() {}

    // Full constructor
    public User(String email, String nickname, String profileImageUrl, String bio, 
                int followerCount, int followingCount, int postCount, int scrapCount, 
                boolean isPrivate, Date createdAt, Date updatedAt) {
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.postCount = postCount;
        this.scrapCount = scrapCount;
        this.isPrivate = isPrivate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public int getPostCount() { return postCount; }
    public void setPostCount(int postCount) { this.postCount = postCount; }

    public int getScrapCount() { return scrapCount; }
    public void setScrapCount(int scrapCount) { this.scrapCount = scrapCount; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
