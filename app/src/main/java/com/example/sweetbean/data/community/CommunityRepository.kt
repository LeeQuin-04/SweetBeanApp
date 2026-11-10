package com.example.sweetbean.data.community

import android.util.Log
import com.example.sweetbean.data.journal.JournalMedia
import com.example.sweetbean.data.journal.MediaType
import com.example.sweetbean.di.AuthDependencies
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

data class FeedPage(
    val posts: List<Post>,
    val cursor: DocumentSnapshot?,
    val endReached: Boolean
)

class CommunityRepository(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val POSTS = "posts"
        private const val USERS = "users"
        private const val FOLLOWS = "follows"
        private const val LIKES = "likes"
        const val PAGE_SIZE = 15
        private const val FOLLOWING_FEED_SIZE = 40
        private const val IN_CHUNK = 10
        private const val COMMENTS = "comments"
    }

    private val authorCache = ConcurrentHashMap<String, PostAuthor>()


    /**
     * Tab "Tất cả": Post PUBLIC, nhãn CLEAN (NORMAL), chưa xóa.
     * Cần chỉ mục: visibility, distributionLevel, deletedAt, createdAt DESC .
     */
    suspend fun loadAllPage(cursor: DocumentSnapshot?): FeedPage {
        var query = firestore.collection(POSTS)
            .whereEqualTo("visibility", PostVisibility.PUBLIC)
            .whereEqualTo("distributionLevel", DistributionLevel.NORMAL)
            .whereEqualTo("deletedAt", null)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())

        if (cursor != null) query = query.startAfter(cursor)

        val snapshot = query.get().await()
        return FeedPage(
            posts = snapshot.documents.mapNotNull { it.toPost() }.filter { !it.isHidden },
            cursor = snapshot.documents.lastOrNull(),
            endReached = snapshot.size() < PAGE_SIZE
        )
    }

    /**
     * Tab "Đang theo dõi": Post NORMAL hoặc LIMITED của người mình theo dõi (cả PUBLIC lẫn
     * FOLLOWERS vì mình là follower). Gộp theo nhóm 10 người rồi sắp xếp lại ở client.
     */
    suspend fun loadFollowingFeed(followingIds: Collection<String>): List<Post> {
        if (followingIds.isEmpty()) return emptyList()

        val posts = coroutineScope {
            followingIds.chunked(IN_CHUNK).map { chunk ->
                async {
                    firestore.collection(POSTS)
                        .whereIn("authorId", chunk)
                        .whereIn(
                            "distributionLevel",
                            listOf(DistributionLevel.NORMAL, DistributionLevel.LIMITED)
                        )
                        .whereEqualTo("deletedAt", null)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(20)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toPost() }
                }
            }.awaitAll().flatten()
        }

        return posts
            .filter { !it.isHidden }
            .sortedByDescending { it.createdAtMillis }
            .take(FOLLOWING_FEED_SIZE)
    }

    suspend fun getPost(postId: String): Post? {
        val doc = firestore.collection(POSTS).document(postId).get().await()
        return if (doc.exists()) doc.toPost() else null
    }

    suspend fun loadFollowingIds(uid: String): Set<String> =
        firestore.collection(FOLLOWS)
            .whereEqualTo("followerId", uid)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("followingId") }
            .toSet()


    suspend fun loadAuthors(ids: Collection<String>): Map<String, PostAuthor> {
        val missing = ids.distinct().filter {!authorCache.containsKey(it)}

        missing.chunked(IN_CHUNK).forEach { chunk ->
            val snapshot = firestore.collection(USERS)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
            snapshot.documents.forEach { doc ->
                authorCache[doc.id] = PostAuthor(
                    id = doc.id,
                    displayName = doc.getString("displayName").orEmpty(),
                    username = doc.getString("username").orEmpty(),
                    avatarUrl = doc.getString("avatarUrl")
                )
            }
        }

        return ids.distinct().mapNotNull { id -> authorCache[id]?.let { id to it } }.toMap()
    }

    suspend fun loadLikedPostIds(uid: String, postIds: Collection<String>): Set<String> {
        if (postIds.isEmpty()) return emptySet()

        val liked = mutableSetOf<String>()
        postIds.map { "${uid}_POST_$it" }.chunked(IN_CHUNK).forEach { chunk ->
            firestore.collection(LIKES)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
                .documents
                .forEach { doc -> doc.getString("targetId")?.let { liked.add(it) } }
        }
        return liked
    }

    fun observeFollowingIds(uid: String): Flow<Set<String>> = callbackFlow {
        val registration = firestore.collection(FOLLOWS)
            .whereEqualTo("followerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.getString("followingId") }.toSet())
                }
            }
        awaitClose { registration.remove() }
    }


    /** Trả về trạng thái Like sau khi chuyển. ID cố định {uid}_POST_{postId} chống Like trùng. */
    suspend fun toggleLike(uid: String, postId: String): Boolean {
        val likeRef = firestore.collection(LIKES).document("${uid}_POST_$postId")
        val postRef = firestore.collection(POSTS).document(postId)

        return firestore.runTransaction { tx ->
            if (tx.get(likeRef).exists()) {
                tx.delete(likeRef)
                tx.update(postRef, "likeCount", FieldValue.increment(-1))
                return@runTransaction false
            }
            tx.set(
                likeRef,
                mapOf(
                    "userId" to uid,
                    "targetType" to "POST",
                    "targetId" to postId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(postRef, "likeCount", FieldValue.increment(1))
            true
        }.await()
    }

    /** Follow một chiều, ID cố định {followerId}_{followingId}. */
    suspend fun toggleFollow(uid: String, targetId: String) {
        require(uid != targetId) { "Không thể tự theo dõi chính mình." }

        val followRef = firestore.collection(FOLLOWS).document("${uid}_$targetId")
        val meRef = firestore.collection(USERS).document(uid)
        val targetRef = firestore.collection(USERS).document(targetId)

        firestore.runTransaction { tx ->
            if (tx.get(followRef).exists()) {
                tx.delete(followRef)
                tx.update(meRef, "followingCount", FieldValue.increment(-1))
                tx.update(targetRef, "followersCount", FieldValue.increment(-1))
            } else {
                tx.set(
                    followRef,
                    mapOf(
                        "followerId" to uid,
                        "followingId" to targetId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(meRef, "followingCount", FieldValue.increment(1))
                tx.update(targetRef, "followersCount", FieldValue.increment(1))
            }
            Unit
        }.await()
    }

    suspend fun loadComments(postId: String, uid: String): List<Comment> = coroutineScope {
        val others = async {
            firestore.collection(COMMENTS)
                .whereEqualTo("postId", postId)
                .whereIn("moderationLabel", listOf("CLEAN", "OFFENSIVE"))
                .whereEqualTo("deletedAt", null)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(200)
                .get()
                .await()
                .documents
                .mapNotNull { it.toComment() }
        }
        val mine = async {
            firestore.collection(COMMENTS)
                .whereEqualTo("postId", postId)
                .whereEqualTo("authorId", uid)
                .whereEqualTo("deletedAt", null)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(100)
                .get()
                .await()
                .documents
                .mapNotNull { it.toComment() }
        }
        (others.await() + mine.await()).distinctBy { it.id }.sortedBy { it.createdAtMillis }
    }

    suspend fun loadLikedCommentIds(uid: String, commentIds: Collection<String>): Set<String> {
        if (commentIds.isEmpty()) return emptySet()

        val liked = mutableSetOf<String>()
        commentIds.map { "${uid}_COMMENT_$it" }.chunked(IN_CHUNK).forEach { chunk ->
            firestore.collection(LIKES)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
                .documents
                .forEach { doc -> doc.getString("targetId")?.let { liked.add(it) } }
        }
        return liked
    }

    suspend fun toggleCommentLike(uid: String, commentId: String): Boolean {
        val likeRef = firestore.collection(LIKES).document("${uid}_COMMENT_$commentId")
        val commentRef = firestore.collection(COMMENTS).document(commentId)

        return firestore.runTransaction { tx ->
            if (tx.get(likeRef).exists()) {
                tx.delete(likeRef)
                tx.update(commentRef, "likeCount", FieldValue.increment(-1))
                return@runTransaction false
            }
            tx.set(
                likeRef,
                mapOf(
                    "userId" to uid,
                    "targetType" to "COMMENT",
                    "targetId" to commentId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(commentRef, "likeCount", FieldValue.increment(1))
            true
        }.await()
    }

    suspend fun searchUsers(query: String, limit: Int = 20): List<PostAuthor> {
        val q = query.trim().removePrefix("@").lowercase()
        if (q.length < 2) return emptyList()

        return firestore.collection(USERS)
            .orderBy("usernameLower")
            .startAt(q)
            .endAt(q + "\uf8ff")
            .limit(limit.toLong())
            .get()
            .await()
            .documents
            .map { doc ->
                PostAuthor(
                    id = doc.id,
                    displayName = doc.getString("displayName").orEmpty(),
                    username = doc.getString("username").orEmpty(),
                    avatarUrl = doc.getString("avatarUrl")
                )
            }
    }
    suspend fun getPublicProfile(userId: String): PublicProfile? {
        val doc = firestore.collection(USERS).document(userId).get().await()
        if (!doc.exists()) return null

        return PublicProfile(
            id = doc.id,
            displayName = doc.getString("displayName").orEmpty(),
            username = doc.getString("username").orEmpty(),
            bio = doc.getString("bio").orEmpty(),
            avatarUrl = doc.getString("avatarUrl"),
            followersCount = doc.getLong("followersCount") ?: 0L,
            followingCount = doc.getLong("followingCount") ?: 0L,
            postsCount = doc.getLong("postsCount") ?: 0L
        )
    }

    suspend fun isFollowing(uid: String, targetId: String): Boolean =
        firestore.collection(FOLLOWS).document("${uid}_$targetId").get().await().exists()

    /**
     * Bài của một người, theo quyền của người xem:
     * chính mình = mọi bài; người đang theo dõi = NORMAL + LIMITED (cả "Người theo dõi");
     * người chưa theo dõi = chỉ PUBLIC + NORMAL.
     */
    suspend fun loadUserPostsPage(
        viewerId: String,
        ownerId: String,
        viewerFollows: Boolean,
        cursor: DocumentSnapshot?
    ): FeedPage {
        var query: Query = firestore.collection(POSTS).whereEqualTo("authorId", ownerId)

        query = when {
            viewerId == ownerId -> query
            viewerFollows -> query.whereIn(
                "distributionLevel",
                listOf(DistributionLevel.NORMAL, DistributionLevel.LIMITED)
            )
            else -> query
                .whereEqualTo("visibility", PostVisibility.PUBLIC)
                .whereEqualTo("distributionLevel", DistributionLevel.NORMAL)
        }

        query = query
            .whereEqualTo("deletedAt", null)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())

        if (cursor != null) query = query.startAfter(cursor)

        val snapshot = query.get().await()
        return FeedPage(
            posts = snapshot.documents.mapNotNull { it.toPost() }.filter { !it.isHidden },
            cursor = snapshot.documents.lastOrNull(),
            endReached = snapshot.size() < PAGE_SIZE
        )
    }
}

private fun DocumentSnapshot.toPost(): Post? {
    return try {
        val createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: return null
        val authorId = getString("authorId") ?: return null

        Post(
            id = id,
            authorId = authorId,
            text = getString("text").orEmpty(),
            visibility = getString("visibility") ?: PostVisibility.PUBLIC,
            media = (get("media") as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.toMedia() },
            distributionLevel = getString("distributionLevel") ?: DistributionLevel.NORMAL,
            isHidden = getBoolean("isHidden") == true,
            likeCount = getLong("likeCount") ?: 0L,
            commentCount = getLong("commentCount") ?: 0L,
            repostCount = getLong("repostCount") ?: 0L,
            createdAtMillis = createdAt.toDate().time,
            moderationLabel = getString("moderationLabel")
        )
    } catch (e: RuntimeException) {
        Log.w("CommunityRepository", "Bỏ qua post $id vì dữ liệu không hợp lệ", e)
        null
    }
}
private fun DocumentSnapshot.toComment(): Comment? {
    return try {
        val createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: return null

        Comment(
            id = id,
            postId = getString("postId") ?: return null,
            authorId = getString("authorId") ?: return null,
            parentCommentId = getString("parentCommentId"),
            text = getString("text").orEmpty(),
            moderationLabel = getString("moderationLabel"),
            likeCount = getLong("likeCount") ?: 0L,
            replyCount = getLong("replyCount") ?: 0L,
            createdAtMillis = createdAt.toDate().time
        )
    } catch (e: RuntimeException) {
        Log.w("CommunityRepository", "Bỏ qua comment $id vì dữ liệu không hợp lệ", e)
        null
    }
}

private fun Map<*, *>.toMedia(): JournalMedia? {
    val url = this["url"] as? String ?: return null
    return JournalMedia(
        type = this["type"] as? String ?: MediaType.IMAGE,
        url = url,
        publicId = this["publicId"] as? String ?: "",
        width = (this["width"] as? Number)?.toInt(),
        height = (this["height"] as? Number)?.toInt(),
        duration = (this["duration"] as? Number)?.toDouble(),
        format = this["format"] as? String
    )
}
