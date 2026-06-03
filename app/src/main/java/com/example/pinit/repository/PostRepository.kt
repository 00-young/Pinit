package com.example.pinit.repository

import android.net.Uri
import com.example.pinit.model.post.ContentBlock
import com.example.pinit.model.post.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class PostRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * 썸네일 이미지(Uri)를 Storage 에 올린 뒤, 그 URL 을 넣어 Post + contentBlocks 저장.
     */
    fun uploadPost(
        post: Post,
        thumbnailUri: Uri,
        contentBlocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child("posts/thumbnails/$fileName")

        imageRef.putFile(thumbnailUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val uploadPost = post.copy(thumbnailImageUrl = uri.toString())
                        // 썸네일 URL 채운 뒤 Post + blocks 저장 (완료까지 대기)
                        savePostWithBlocks(uploadPost, contentBlocks, onSuccess, onFailure)
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * 썸네일 업로드 없이 Post(이미 thumbnailImageUrl 채워짐) + contentBlocks 저장.
     * 지도 Static Map URL 을 썸네일로 쓸 때 사용.
     */
    fun uploadPostWithoutThumbnail(
        post: Post,
        blocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        savePostWithBlocks(post, blocks, onSuccess, onError)
    }

    // =====================================================
    // 수정(Edit)
    // =====================================================

    /**
     * 게시물 수정: 새 썸네일 이미지(Uri)를 Storage 에 올린 뒤 업데이트.
     * 기존 contentBlocks 를 모두 지우고 새로 쓴다.
     */
    fun updatePost(
        post: Post,
        thumbnailUri: Uri,
        blocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child("posts/thumbnails/$fileName")

        imageRef.putFile(thumbnailUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val updated = post.copy(thumbnailImageUrl = uri.toString())
                        replacePostWithBlocks(updated, blocks, onSuccess, onFailure)
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * 게시물 수정: 썸네일 재업로드 없이 (이미 thumbnailImageUrl 채워짐) 업데이트.
     * 기존 썸네일 URL 을 그대로 유지하거나, Static Map URL 을 쓸 때 사용.
     */
    fun updatePostWithoutThumbnail(
        post: Post,
        blocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        replacePostWithBlocks(post, blocks, onSuccess, onFailure)
    }

    // =====================================================
    // 내부 공통
    // =====================================================

    /**
     * Post 문서 + contentBlocks 하위 컬렉션을 batch 로 한 번에 저장(신규).
     */
    private fun savePostWithBlocks(
        post: Post,
        blocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val postRef = db.collection("posts").document(post.postId)
        val searchIndexRef = db.collection("searchIndexPosts").document(post.postId)
        val batch = db.batch()

        // Post 문서
        batch.set(postRef, post)

        val searchIndexData = hashMapOf(
            "postId" to post.postId,
            "title" to post.title,
            "thumbnailImageUrl" to post.thumbnailImageUrl,
            "postImageUrl" to post.thumbnailImageUrl,
            "hashtags" to post.hashtags,
            "userNickname" to post.userNickname,
            "content" to blocks.joinToString(" ") { it.toString() }
        )

        batch.set(searchIndexRef, searchIndexData)

        for (block in blocks) {
            val blockRef = postRef.collection("contentBlocks").document(block.blockId)
            batch.set(blockRef, block)
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * 수정용: 기존 contentBlocks 를 모두 삭제한 뒤, Post 의 변경 필드만 갱신하고
     * 새 blocks 를 batch 로 저장한다.
     * - 블록 개수가 줄어도 옛 블록이 남지 않음
     * - likeCount/scrapCount/createdAt 등 기존 통계 값은 건드리지 않음(제목·썸네일만 갱신)
     */
    private fun replacePostWithBlocks(
        post: Post,
        blocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val postRef = db.collection("posts").document(post.postId)

        // 1) 기존 블록 먼저 조회
        postRef.collection("contentBlocks").get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()

                // 2) 기존 블록 전부 삭제
                for (doc in snap.documents) {
                    batch.delete(doc.reference)
                }

                // 3) Post 문서는 변경 필드만 update (카운트/createdAt 보존)
                val updates = mapOf(
                    "title" to post.title,
                    "thumbnailImageUrl" to post.thumbnailImageUrl,
                    "postImageType" to post.postImageType,
                    "visibility" to post.visibility
                )
                batch.update(postRef, updates)

                // 4) 새 블록 쓰기
                for (block in blocks) {
                    val blockRef = postRef.collection("contentBlocks").document(block.blockId)
                    batch.set(blockRef, block)
                }

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
}