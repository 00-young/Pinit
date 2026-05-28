package com.example.pinit.model.post

import com.google.firebase.Timestamp


data class Post(

    val postId: String = "",

    val ownerUserId: String = "",

    val title: String = "",

    val postImageType: String = "image",

    val thumbnailImageUrl: String = "",

    val hashtags: List<String> = emptyList(),

    val likeCount: Int = 0,

    val commentCount: Int = 0,

    val scrapCount: Int = 0,

    val viewCount: Int = 0,

    val isPinned: Boolean = false,

    val createdAt: Timestamp? = null
)