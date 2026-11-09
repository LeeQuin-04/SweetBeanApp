package com.example.sweetbean.data.media

import android.util.Log
import com.example.sweetbean.data.journal.JournalMedia
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object MediaCleanupConfig {
    const val WORKER_BASE_URL = "https://sweetbean-moderation.lequyen22122004.workers.dev"
}

/**
 * Xóa media trên Cloudinary qua Worker (Cloudinary chỉ cho xóa khi có API secret,
 * không thể để trong app). Lỗi chỉ được ghi log, không làm hỏng thao tác chính.
 */
class MediaCleaner(
    private val firebaseAuth: FirebaseAuth
) {

    suspend fun delete(media: List<JournalMedia>) {
        val items = media.filter { it.publicId.isNotBlank() }
        if (items.isEmpty()) return

        try {
            val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token ?: return
            withContext(Dispatchers.IO) { post(token, items) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MediaCleaner", "Không xóa được media trên Cloudinary", e)
        }
    }

    private fun post(token: String, items: List<JournalMedia>) {
        val body = JSONObject().put(
            "items",
            JSONArray().apply {
                items.forEach {
                    put(JSONObject().put("publicId", it.publicId).put("type", it.type))
                }
            }
        )

        val connection = (URL("${MediaCleanupConfig.WORKER_BASE_URL}/media/delete")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }

        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Worker trả về mã $code")
        } finally {
            connection.disconnect()
        }
    }
}