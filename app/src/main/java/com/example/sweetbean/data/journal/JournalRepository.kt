package com.example.sweetbean.data.journal

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class JournalRepository(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val JOURNALS_COLLECTION = "journals"
    }

    /**
     * Theo dõi nhật ký của một tháng, mới nhất trước. [month] tính từ 1 đến 12.
     * Truy vấn cần chỉ mục userId ASC + date DESC.
     */
    fun observeMonth(uid: String, year: Int, month: Int): Flow<List<Journal>> = callbackFlow {
        val start = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }

        val registration = firestore
            .collection(JOURNALS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereGreaterThanOrEqualTo("date", Timestamp(start.time))
            .whereLessThan("date", Timestamp(end.time))
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toJournal() })
                }
            }

        awaitClose { registration.remove() }
    }
    /** Toàn bộ nhật ký của một năm (đọc một lần), dùng cho Cây cảm xúc. */
    suspend fun loadYear(uid: String, year: Int): List<Journal> {
        val start = Calendar.getInstance().apply {
            clear()
            set(year, 0, 1)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 1) }

        val snapshot = firestore
            .collection(JOURNALS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereGreaterThanOrEqualTo("date", Timestamp(start.time))
            .whereLessThan("date", Timestamp(end.time))
            .orderBy("date", Query.Direction.DESCENDING) // cùng chỉ mục với truy vấn theo tháng
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toJournal() }
    }

    /** Nhật ký của hôm nay (nếu đã có). Mỗi ngày chỉ một nhật ký nhờ ID cố định theo ngày. */
    suspend fun findToday(uid: String): Journal? {
        val snapshot = firestore
            .collection(JOURNALS_COLLECTION)
            .document(todayId(uid))
            .get()
            .await()

        return if (snapshot.exists()) snapshot.toJournal() else null
    }

    /**
     * Tạo nhật ký của hôm nay với ID {uid}_{yyyyMMdd}. Nếu hôm nay đã có thì set() trở thành
     * một lần update và Security Rules từ chối (date/createdAt bị đổi), nên không ghi đè được.
     */
    suspend fun create(uid: String, draft: JournalDraft) {
        require(uid.isNotBlank()) { "UID không được để trống." }

        firestore.collection(JOURNALS_COLLECTION)
            .document(todayId(uid))
            .set(
                mapOf(
                    "userId" to uid,
                    "date" to FieldValue.serverTimestamp(),
                    "title" to draft.title,
                    "text" to draft.text,
                    "emotion" to draft.emotion.name,
                    "tags" to draft.tags,
                    "media" to draft.media.map { it.toMap() },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    /** Chỉ cập nhật nội dung; userId, date, createdAt giữ nguyên. */
    suspend fun update(journalId: String, draft: JournalDraft) {
        require(journalId.isNotBlank()) { "journalId không được để trống." }

        firestore.collection(JOURNALS_COLLECTION)
            .document(journalId)
            .update(
                mapOf(
                    "title" to draft.title,
                    "text" to draft.text,
                    "emotion" to draft.emotion.name,
                    "tags" to draft.tags,
                    "media" to draft.media.map { it.toMap() },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    suspend fun delete(journalId: String) {
        require(journalId.isNotBlank()) { "journalId không được để trống." }

        firestore.collection(JOURNALS_COLLECTION)
            .document(journalId)
            .delete()
            .await()
    }

    private fun todayId(uid: String): String =
        "${uid}_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}"
}

private fun DocumentSnapshot.toJournal(): Journal? = try {
    val date = getTimestamp("date", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    val emotion = Emotion.fromKey(getString("emotion"))

    if (date == null || emotion == null) {
        null
    } else {
        Journal(
            id = id,
            userId = getString("userId").orEmpty(),
            dateMillis = date.toDate().time,
            title = getString("title").orEmpty(),
            text = getString("text").orEmpty(),
            emotion = emotion,
            tags = (get("tags") as? List<*>).orEmpty().filterIsInstance<String>(),
            media = (get("media") as? List<*>).orEmpty()
                .mapNotNull { (it as? Map<*, *>)?.toJournalMedia() }
        )
    }
} catch (e: RuntimeException) {
    // Dữ liệu sai kiểu (ví dụ date là string do sửa tay trên Console): bỏ qua, không làm sập app.
    Log.w("JournalRepository", "Bỏ qua journal $id vì dữ liệu không hợp lệ", e)
    null
}

private fun Map<*, *>.toJournalMedia(): JournalMedia? {
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

private fun JournalMedia.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
        "type" to type,
        "url" to url,
        "publicId" to publicId
    )
    width?.let { map["width"] = it }
    height?.let { map["height"] = it }
    duration?.let { map["duration"] = it }
    format?.let { map["format"] = it }
    return map
}