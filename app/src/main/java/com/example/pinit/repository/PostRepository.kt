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

    /**
     * Post 문서 + contentBlocks 하위 컬렉션을 batch 로 한 번에 저장.
     * 모두 성공해야 onSuccess, 하나라도 실패하면 onFailure.
     */
    private fun savePostWithBlocks(
        post: Post,
        blocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val postRef = db.collection("posts").document(post.postId)
        val batch = db.batch()

        // Post 문서
        batch.set(postRef, post)

        // contentBlocks 하위 컬렉션
        for (block in blocks) {
            val blockRef = postRef.collection("contentBlocks").document(block.blockId)
            batch.set(blockRef, block)
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}