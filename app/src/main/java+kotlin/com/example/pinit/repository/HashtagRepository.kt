package com.example.pinit.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HashtagRepository {

    private val db = FirebaseFirestore.getInstance()
    // 인기 hashtag 조회
    fun getPopularHashtags(
        limit: Long = 10,
        onResult: (List<String>) -> Unit
    ) {
        db.collection("hashtags")
            .orderBy("postCount", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { result ->
                val hashtags = result.documents.map { document ->
                    document.id
                }

                Log.d("HashtagRepository", "인기 해시태그 조회 성공: $hashtags")
                onResult(hashtags)
            }
            .addOnFailureListener { e ->
                Log.e("HashtagRepository", "인기 해시태그 조회 실패", e)
                onResult(emptyList())
            }
    }
    // 테마 목록 조회
    fun getThemes(
        onResult: (List<String>) -> Unit
    ) {
        db.collection("themes")
            .get()
            .addOnSuccessListener { result ->
                val themes = result.documents.mapNotNull {
                    it.getString("themeName")
                }
                onResult(themes)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}