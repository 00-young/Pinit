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

    fun uploadPost(
        post: Post,
        thumbnailUri: Uri,
        contentBlocks: List<ContentBlock>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        val fileName = "${UUID.randomUUID()}.jpg"

        val imageRef = storage.reference
            .child("posts/thumbnails/$fileName")

        imageRef.putFile(thumbnailUri)

            .addOnSuccessListener {

                imageRef.downloadUrl

                    .addOnSuccessListener { uri ->

                        val uploadPost = post.copy(
                            thumbnailImageUrl = uri.toString()
                        )

                        db.collection("posts")
                            .document(uploadPost.postId)
                            .set(uploadPost)

                            .addOnSuccessListener {

                                uploadContentBlocks(
                                    uploadPost.postId,
                                    contentBlocks
                                )

                                onSuccess()
                            }

                            .addOnFailureListener {
                                onFailure(it)
                            }
                    }
            }

            .addOnFailureListener {
                onFailure(it)
            }
    }

    private fun uploadContentBlocks(
        postId: String,
        contentBlocks: List<ContentBlock>
    ) {

        for (block in contentBlocks) {

            db.collection("posts")
                .document(postId)
                .collection("contentBlocks")
                .document(block.blockId)
                .set(block)
        }
    }
}