package com.example.pinit.repository

import android.net.Uri
import com.example.pinit.index.SearchIndex
import com.example.pinit.model.post.ContentBlock
import com.example.pinit.model.post.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class PostRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * 검색 인덱스(searchIndexPosts) 저장/갱신.
     * postId 를 문서 ID 로 써서 게시물과 1:1 매핑. 신규/수정 모두 set 으로 덮어쓰기.
     * (인덱스 내용은 CreatePostFragment 에서 완전한 SearchIndex 로 구성해 넘긴다)
     */
    fun saveSearchIndex(searchIndex: SearchIndex) {
        if (searchIndex.postId.isEmpty()) return
        db.collection("searchIndexPosts")
            .document(searchIndex.postId)
            .set(searchIndex)
    }

    /** 게시물 삭제 시 검색 인덱스도 함께 제거 */
    fun deleteSearchIndex(postId: String) {
        if (postId.isEmpty()) return
        db.collection("searchIndexPosts").document(postId).delete()
    }

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
     * 검색 인덱스는 호출부(CreatePostFragment)에서 saveSearchIndex 로 별도 저장한다.
     */
    private fun savePostWithBlocks(
        post: Post,
        blocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val postRef = db.collection("posts").document(post.postId)
        val batch = db.batch()

        batch.set(postRef, post)
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
     * - likeCount/scrapCount/createdAt 등 기존 통계 값은 보존(제목·썸네일·공개범위·닉네임만 갱신)
     */
    private fun replacePostWithBlocks(
        post: Post,
        blocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val postRef = db.collection("posts").document(post.postId)

        postRef.collection("contentBlocks").get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()

                for (doc in snap.documents) {
                    batch.delete(doc.reference)
                }

                val updates = mapOf(
                    "title" to post.title,
                    "thumbnailImageUrl" to post.thumbnailImageUrl,
                    "postImageType" to post.postImageType,
                    "visibility" to post.visibility,
                    "userNickname" to post.userNickname,
                    "hashtags" to post.hashtags
                )
                batch.update(postRef, updates)

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