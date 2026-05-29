package com.example.pinit.model.post

import com.google.firebase.Timestamp

data class Comment(

    val commentId: String = "",

    val userId: String = "",

    val nickname: String = "",

    val profileImageUrl: String = "",

    val parentCommentId: String? = null,

    val content: String = "",

    val createdAt: Timestamp? = null
)
