package com.example.pinit.model.post

import com.google.firebase.Timestamp

data class Post(

    val postId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userNickname: String = "",
    val title: String = "",
    val postImageType: String = "image",
    val thumbnailImageUrl: String = "",
    val hashtags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val scrapCount: Int = 0,
    val viewCount: Int = 0,
    val isPinned: Boolean = false,
    // 공개 범위: "public"(전체공개) | "private"(나만보기). 기본은 전체공개.
    val visibility: String = VISIBILITY_PUBLIC,
    val createdAt: Timestamp? = null
) {
    companion object {
        const val VISIBILITY_PUBLIC = "public"
        const val VISIBILITY_PRIVATE = "private"
    }
}