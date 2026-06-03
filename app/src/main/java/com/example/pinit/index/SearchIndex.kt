package com.example.pinit

data class SearchIndex(
    val postId: String = "", // 이 부분 추가
    val title: String = "",
    val content: String = "",
    val mainTheme: String = "",
    val hashtags: List<String> = emptyList(),
    val country: String = "",
    val city: String = "",
    val travelerCount: Long = 0L,
    val startDate: String = "",
    val endDate: String = "",
    val postImageUrl: String = "",
    val createdAt: Long = 0L
)

