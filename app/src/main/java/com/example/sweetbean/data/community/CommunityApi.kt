package com.example.sweetbean.data.community

import com.example.sweetbean.data.journal.JournalMedia
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object CommunityConfig {
    const val WORKER_URL = "https://sweetbean-moderation.lequyen22122004.workers.dev"
}

class CommunityApiException(val code: Int, val error: String) :
    IOException("HTTP $code: $error")

class CommunityApi(
    private val firebaseAuth: FirebaseAuth
) {

    suspend fun createPost(
        text: String,
        visibility: String,
        media: List<JournalMedia>
    ): CreatePostResult {
        val json = postJson(
            "/posts",
            JSONObject().apply {
                put("text", text)
                put("visibility", visibility)
                put("media", JSONArray().also { array -> media.forEach { array.put(it.toJson()) } })
            }
        )
        return CreatePostResult(
            postId = json.getString("postId"),
            distributionLevel = json.optString("distributionLevel", DistributionLevel.NORMAL),
            moderationReviewStatus = json.optString("moderationReviewStatus").ifBlank { null }        )
    }

    /** [parentCommentId] null = bình luận gốc, khác null = trả lời bình luận đó. */
    suspend fun createComment(postId: String, text: String, parentCommentId: String?) {
        postJson(
            "/comments",
            JSONObject().apply {
                put("postId", postId)
                put("text", text)
                if (parentCommentId != null) put("parentCommentId", parentCommentId)
            }
        )
    }
    suspend fun deletePost(postId: String) {
        postJson("/posts/delete", JSONObject().apply { put("postId", postId) })
    }
    suspend fun editPost(postId: String, text: String, visibility: String): CreatePostResult {
        val json = postJson(
            "/posts/edit",
            JSONObject().apply {
                put("postId", postId)
                put("text", text)
                put("visibility", visibility)
            }
        )
        return CreatePostResult(
            postId = json.optString("postId", postId),
            distributionLevel = json.optString("distributionLevel", DistributionLevel.NORMAL),
            moderationReviewStatus = json.optString("moderationReviewStatus").ifBlank { null }
        )
    }

    private suspend fun postJson(path: String, payload: JSONObject): JSONObject =
        withContext(Dispatchers.IO) {
            val user = firebaseAuth.currentUser ?: throw IOException("Chưa đăng nhập.")
            val idToken = user.getIdToken(false).await().token
                ?: throw IOException("Không lấy được phiên đăng nhập.")

            val connection = (URL("${CommunityConfig.WORKER_URL}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $idToken")
            }

            try {
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (code !in 200..299) {
                    val error = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
                    throw CommunityApiException(code, error)
                }
                JSONObject(body)
            } finally {
                connection.disconnect()
            }
        }
}

private fun JournalMedia.toJson(): JSONObject = JSONObject().apply {
    put("type", type)
    put("url", url)
    put("publicId", publicId)
    width?.let { put("width", it) }
    height?.let { put("height", it) }
    duration?.let { put("duration", it) }
    format?.let { put("format", it) }
}