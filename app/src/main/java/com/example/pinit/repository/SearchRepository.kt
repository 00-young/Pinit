package com.example.pinit

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class SearchRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getSearchPosts(onResult: (List<SearchIndex>) -> Unit) {
        db.collection("posts")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { doc ->
                    SearchIndex(
                        postId = doc.id,
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        mainTheme = doc.getString("mainTheme") ?: "",
                        hashtags = (doc.get("hashtags") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        country = doc.getString("country") ?: "",
                        city = doc.getString("city") ?: "",
                        // 💡 파이어베이스에서 실수로 글자/숫자가 잘못 들어가도 앱이 안 튕기게 방어!
                        travelerCount = try { doc.getLong("travelerCount") ?: 0L } catch (e: Exception) { 0L },
                        startDate = doc.getString("startDate") ?: "",
                        endDate = doc.getString("endDate") ?: "",
                        postImageUrl = doc.getString("postImageUrl") ?: "",
                        // 💡 핵심 수정: 검색 필터에서 안 쓰는 시간 데이터 때문에 앱이 튕기지 않도록 0으로 고정!
                        createdAt = 0L
                    )
                }
                onResult(list)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("SearchTest", "검색 데이터 불러오기 실패", e)
                onResult(emptyList())
            }
    }

    fun search(
        keyword: String? = null,
        travelerCount: Long? = null,
        location: String? = null,
        selectedDate: String? = null,
        selectedMonth: String? = null,
        filterStartDate: String? = null,
        filterEndDate: String? = null,
        selectedHashtags: List<String> = emptyList(),
        onResult: (List<SearchIndex>) -> Unit
    ) {
        getSearchPosts { posts ->
            val result = posts.filter { post ->

                val keywordMatch =
                    keyword.isNullOrBlank() ||
                            post.title.contains(keyword, ignoreCase = true) ||
                            post.content.contains(keyword, ignoreCase = true)

                val travelerMatch =
                    travelerCount == null ||
                            post.travelerCount == travelerCount

                val locationMatch =
                    location.isNullOrBlank() ||
                            post.country.contains(location, ignoreCase = true) ||
                            post.city.contains(location, ignoreCase = true)

                val dateMatch =
                    selectedDate.isNullOrBlank() ||
                            isDateInTrip(post.startDate, post.endDate, selectedDate)

                val monthMatch =
                    selectedMonth.isNullOrBlank() ||
                            isTripInMonth(post.startDate, post.endDate, selectedMonth)

                val rangeMatch =
                    filterStartDate.isNullOrBlank() ||
                            filterEndDate.isNullOrBlank() ||
                            isTripOverlapped(
                                post.startDate,
                                post.endDate,
                                filterStartDate,
                                filterEndDate
                            )

                val hashtagMatch =
                    selectedHashtags.isEmpty() ||
                            selectedHashtags.all { tag ->
                                post.hashtags.contains(tag)
                            }

                keywordMatch &&
                        travelerMatch &&
                        locationMatch &&
                        dateMatch &&
                        monthMatch &&
                        rangeMatch &&
                        hashtagMatch
            }

            onResult(result)
        }
    }

    private fun isDateInTrip(
        postStartDate: String,
        postEndDate: String,
        selectedDate: String
    ): Boolean {
        return postStartDate <= selectedDate &&
                postEndDate >= selectedDate
    }

    private fun isTripInMonth(
        postStartDate: String,
        postEndDate: String,
        selectedMonth: String
    ): Boolean {
        val monthStart = "$selectedMonth-01"
        val monthEnd = "$selectedMonth-31"

        return postStartDate <= monthEnd &&
                postEndDate >= monthStart
    }

    private fun isTripOverlapped(
        postStartDate: String,
        postEndDate: String,
        filterStartDate: String,
        filterEndDate: String
    ): Boolean {
        return postStartDate <= filterEndDate &&
                postEndDate >= filterStartDate
    }
}

// 검색 기능