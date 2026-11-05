package com.example.sweetbean.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance

private const val NIGHT_KEEP = 0.10f   // giữ lại bao nhiêu màu gốc (0 = đơn sắc hoàn toàn)
private const val NIGHT_GLOW = 0.90f   // tăng để nét tranh sáng hơn, giảm để dịu hơn

// Đỏ, lục, lam của ánh sáng: nghiêng về xanh bạc.
private val GlowTint = floatArrayOf(0.90f, 1.00f, 1.15f)

/** Màu "giấy" (màu nền kem ở giữa) của từng ảnh gốc; bộ lọc đêm kéo màu này về màu nền tối. */
object ArtPaper {
    val Desk = Color(253, 245, 239)
    val Login = Color(253, 245, 243)
    val Dialog = Color(249, 245, 253)
}

/** Bộ lọc đêm khi máy bật chế độ tối; null ở chế độ sáng (ảnh giữ nguyên). */
@Composable
fun rememberNightArtFilter(paper: Color = ArtPaper.Desk): ColorFilter? {
    val base = MaterialTheme.colorScheme.background
    val isDark = base.luminance() < 0.5f

    return remember(base, isDark, paper) {
        if (isDark) nightArtFilter(base, paper) else null
    }
}

/**
 * Với mỗi kênh màu c:
 *   ra_c = nền_c + KEEP * (ảnh_c - giấy_c) + GLOW * tint_c * (độ sáng giấy - độ sáng ảnh)
 * Chi tiết sẫm hơn nền kem nên sáng lên trên nền đêm; chính nền kem trở thành màu nền tối.
 */
fun nightArtFilter(target: Color, paper: Color): ColorFilter {
    val wr = 0.2126f
    val wg = 0.7152f
    val wb = 0.0722f

    val p = floatArrayOf(paper.red * 255f, paper.green * 255f, paper.blue * 255f)
    val paperLuma = wr * p[0] + wg * p[1] + wb * p[2]
    val background = floatArrayOf(target.red * 255f, target.green * 255f, target.blue * 255f)

    val m = FloatArray(20)
    for (c in 0..2) {
        val glow = NIGHT_GLOW * GlowTint[c]
        m[c * 5 + 0] = -glow * wr + if (c == 0) NIGHT_KEEP else 0f
        m[c * 5 + 1] = -glow * wg + if (c == 1) NIGHT_KEEP else 0f
        m[c * 5 + 2] = -glow * wb + if (c == 2) NIGHT_KEEP else 0f
        m[c * 5 + 4] = background[c] + glow * paperLuma - NIGHT_KEEP * p[c]
    }
    m[18] = 1f // giữ nguyên kênh alpha

    return ColorFilter.colorMatrix(ColorMatrix(m))
}