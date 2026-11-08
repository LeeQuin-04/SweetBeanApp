package com.example.sweetbean.ui.journal.tree

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import com.example.sweetbean.R
import androidx.core.graphics.createBitmap

/** Độ đậm và màu của một tháng. [colors] có nhiều màu khi các cảm xúc hòa nhau (mỗi màu một mảng). */
class MonthInk(
    val colors: List<Color>,
    val alpha: Float
)

/**
 * Lưới nhỏ 128x116 quyết định màu từng chỗ trên tán cây:
 *  - tree_labels: ô thuộc tháng nào (các vùng vô định hình, diện tích gần bằng nhau)
 *  - tree_noise: cách chia mảng khi một tháng có nhiều cảm xúc hòa nhau
 * Màu được tô vào [bitmap], sau đó khi vẽ sẽ phóng lên và cắt theo hình tán lá.
 */
class TreeField private constructor(
    private val labels: IntArray,   // 0 = ngoài tán, 1..12 = tháng
    private val noise: FloatArray,  // 0..1, phân hạng đều trong từng vùng
    val width: Int,
    val height: Int
) {
    val bitmap: Bitmap = createBitmap(width, height)

    private val cells = width * height
    private val a = FloatArray(cells)
    private val r = FloatArray(cells)
    private val g = FloatArray(cells)
    private val b = FloatArray(cells)
    private val tmp = FloatArray(cells)
    private val pixels = IntArray(cells)

    /** Tháng (1..12) tại điểm (nx, ny) tính theo tỉ lệ 0..1; null nếu chạm ngoài tán, thân hoặc cỏ. */
    fun monthAt(nx: Float, ny: Float): Int? {
        val x = (nx * width).toInt().coerceIn(0, width - 1)
        val y = (ny * height).toInt().coerceIn(0, height - 1)
        val month = labels[y * width + x]
        return if (month in 1..12) month else null
    }

    /** Tô lại toàn bộ lưới theo dữ liệu hiện tại; [inks] có đúng 12 phần tử (tháng 1 đến 12). */
    fun render(inks: List<MonthInk>) {
        for (i in 0 until cells) {
            val month = labels[i]
            val ink = if (month in 1..12) inks.getOrNull(month - 1) else null

            if (ink == null || ink.alpha <= 0f || ink.colors.isEmpty()) {
                a[i] = 0f; r[i] = 0f; g[i] = 0f; b[i] = 0f
                continue
            }

            val n = ink.colors.size
            val color = ink.colors[minOf((noise[i] * n).toInt(), n - 1)]
            val alpha = ink.alpha.coerceIn(0f, 1f)
            a[i] = alpha
            r[i] = color.red * alpha
            g[i] = color.green * alpha
            b[i] = color.blue * alpha
        }

        // Làm mềm ranh giới giữa các mảng, để màu loang vào nhau như màu nước.
        blur(2)
        blur(1)

        for (i in 0 until cells) {
            val alpha = a[i]
            if (alpha < 0.002f) {
                pixels[i] = 0
                continue
            }
            val inv = 1f / alpha
            val red = (r[i] * inv).coerceIn(0f, 1f)
            val green = (g[i] * inv).coerceIn(0f, 1f)
            val blue = (b[i] * inv).coerceIn(0f, 1f)
            pixels[i] = ((alpha * 255f + 0.5f).toInt() shl 24) or
                    ((red * 255f + 0.5f).toInt() shl 16) or
                    ((green * 255f + 0.5f).toInt() shl 8) or
                    (blue * 255f + 0.5f).toInt()
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun blur(radius: Int) {
        for (channel in arrayOf(a, r, g, b)) {
            boxHorizontal(channel, tmp, radius)
            boxVertical(tmp, channel, radius)
        }
    }

    private fun boxHorizontal(src: FloatArray, dst: FloatArray, radius: Int) {
        val window = (2 * radius + 1).toFloat()
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var sum = 0f
                for (k in -radius..radius) {
                    sum += src[row + (x + k).coerceIn(0, width - 1)]
                }
                dst[row + x] = sum / window
            }
        }
    }

    private fun boxVertical(src: FloatArray, dst: FloatArray, radius: Int) {
        val window = (2 * radius + 1).toFloat()
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                for (k in -radius..radius) {
                    sum += src[(y + k).coerceIn(0, height - 1) * width + x]
                }
                dst[y * width + x] = sum / window
            }
        }
    }

    companion object {
        /** Tỉ lệ rộng/cao của ảnh cây (640x580). */
        const val IMAGE_RATIO = 640f / 580f

        fun load(resources: Resources): TreeField {
            val (labelValues, w, h) = decodeGray(resources, R.drawable.tree_labels)
            val (noiseValues, _, _) = decodeGray(resources, R.drawable.tree_noise)

            // Nhãn lưu dưới dạng tháng x 20 để mã hoá ảnh không bị lệch giá trị.
            val labels = IntArray(w * h) { (labelValues[it] + 10) / 20 }
            val noise = FloatArray(w * h) { noiseValues[it] / 255f }
            return TreeField(labels, noise, w, h)
        }

        /** Đọc ảnh xám ở đúng kích thước thật. */
        private fun decodeGray(resources: Resources, id: Int): Triple<IntArray, Int, Int> {
            val options = BitmapFactory.Options().apply { inScaled = false }
            val bitmap = checkNotNull(BitmapFactory.decodeResource(resources, id, options))
            val w = bitmap.width
            val h = bitmap.height
            val argb = IntArray(w * h)
            bitmap.getPixels(argb, 0, w, 0, 0, w, h)
            bitmap.recycle()
            return Triple(IntArray(w * h) { argb[it] and 0xFF }, w, h)
        }
    }
}