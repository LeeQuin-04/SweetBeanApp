package com.example.sweetbean.data.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

object UserRole {
    const val USER = "USER"
    const val ADMIN = "ADMIN"
}

object UserStatus {
    const val ACTIVE = "ACTIVE"
    const val RESTRICTED = "RESTRICTED"
    const val SUSPENDED = "SUSPENDED"
}

/**
 * Document users/{userId}
 * `uid` chính là ID của document (Firebase Authentication UID): Firestore điền khi đọc
 * và không ghi nó thành một field.
 */
data class UserProfile(
    @DocumentId val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val role: String = UserRole.USER,
    val status: String = UserStatus.ACTIVE,
    val followersCount: Long = 0,
    val followingCount: Long = 0,
    val postsCount: Long = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)