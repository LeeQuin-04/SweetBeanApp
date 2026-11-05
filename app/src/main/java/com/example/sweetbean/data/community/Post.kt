package com.example.sweetbean.data.community

import com.example.sweetbean.data.journal.JournalMedia

object PostVisibility {
    const val PUBLIC = "PUBLIC"
    const val FOLLOWERS = "FOLLOWERS"
}

object DistributionLevel {
    const val NORMAL = "NORMAL"
    const val LIMITED = "LIMITED"
    const val AUTHOR_ONLY = "AUTHOR_ONLY"
}

object PostLimits {
    const val MAX_TEXT_LENGTH = 1000
    const val MAX_MEDIA = 10
}

data class PostAuthor(
    val id: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?
)

data class Post(
    val id: String,
    val authorId: String,
    val text: String,
    val visibility: String,
    val media: List<JournalMedia>,
    val distributionLevel: String,
    val isHidden: Boolean,
    val likeCount: Long,
    val commentCount: Long,
    val repostCount: Long,
    val createdAtMillis: Long,
    val editedAtMillis: Long? = null,
    val moderationLabel: String? = null
)

data class CreatePostResult(
    val postId: String,
    val distributionLevel: String,
    val moderationReviewStatus: String? = null
)

object ReviewStatus {
    const val PENDING_AI_UNAVAILABLE = "pending_ai_unavailable"
    const val PENDING_ADMIN = "pending_admin"
}