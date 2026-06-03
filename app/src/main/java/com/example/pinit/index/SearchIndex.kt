package com.example.pinit

data class SearchIndex(
    val postId: String = "",
    val title: String = "",
    val content: String = "",

    // 닉네임 검색용 추가
    val nickname: String = "",
    val writerNickname: String = "",

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