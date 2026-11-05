package com.example.sweetbean.data.community

object CommentLimits {
    const val MAX_TEXT_LENGTH = 500
}

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val parentCommentId: String?,
    val text: String,
    val moderationLabel: String?,
    val likeCount: Long,
    val replyCount: Long,
    val createdAtMillis: Long
)