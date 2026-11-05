package com.example.sweetbean.data.media

import android.content.ContentResolver
import android.net.Uri
import com.example.sweetbean.data.journal.JournalMedia
import com.example.sweetbean.data.journal.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object CloudinaryConfig {
    const val CLOUD_NAME = "deb7bnxp6"
    const val UPLOAD_PRESET = "sweetbean_media"
}

/** Upload ảnh/video lên Cloudinary bằng unsigned preset.*/
class CloudinaryMediaUploader(
    private val contentResolver: ContentResolver
) {

    fun isVideo(uri: Uri): Boolean =
        contentResolver.getType(uri)?.startsWith("video/") == true

    suspend fun upload(uri: Uri, folder: String): JournalMedia = withContext(Dispatchers.IO) {
        val boundary = "SweetBean${System.currentTimeMillis()}"
        val mime = contentResolver.getType(uri) ?: "application/octet-stream"
        val endpoint =
            "https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/auto/upload"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 120_000
            // Truyền trực tiếp từng khối, không nạp cả video vào RAM.
            setChunkedStreamingMode(0)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            connection.outputStream.buffered().use { out ->
                fun write(text: String) = out.write(text.toByteArray(Charsets.UTF_8))

                fun field(name: String, value: String) {
                    write("--$boundary\r\n")
                    write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    write("$value\r\n")
                }

                field("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                field("folder", folder)

                write("--$boundary\r\n")
                write("Content-Disposition: form-data; name=\"file\"; filename=\"file\"\r\n")
                write("Content-Type: $mime\r\n\r\n")
                val input = contentResolver.openInputStream(uri)
                    ?: throw IOException("Không đọc được tệp đã chọn.")
                input.use { it.copyTo(out) }
                write("\r\n--$boundary--\r\n")
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException("Cloudinary trả về mã $code: $body")
            }

            val json = JSONObject(body)
            JournalMedia(
                type = if (json.optString("resource_type") == "video") {
                    MediaType.VIDEO
                } else {
                    MediaType.IMAGE
                },
                url = json.getString("secure_url"),
                publicId = json.getString("public_id"),
                width = json.optInt("width").takeIf { it > 0 },
                height = json.optInt("height").takeIf { it > 0 },
                duration = json.optDouble("duration").takeIf { !it.isNaN() },
                format = json.optString("format").takeIf { it.isNotBlank() }
            )
        } finally {
            connection.disconnect()
        }
    }
}