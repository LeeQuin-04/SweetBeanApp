package com.example.sweetbean.data.community

data class PublicProfile(
    val id: String,
    val displayName: String,
    val username: String,
    val bio: String,
    val avatarUrl: String?,
    val followersCount: Long,
    val followingCount: Long,
    val postsCount: Long
)