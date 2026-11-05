package com.example.sweetbean.ui.auth.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.sweetbean.R
import com.example.sweetbean.ui.common.ArtPaper
import com.example.sweetbean.ui.common.rememberNightArtFilter
import kotlin.math.roundToInt

/** Bo góc dùng chung cho các dialog xác thực. */
val DialogShape = RoundedCornerShape(24.dp)

/**
 * Vẽ ảnh nền cho dialog theo kiểu Crop (phóng đều cho phủ kín, căn giữa, cắt phần thừa).
 * Dùng drawBehind để không ảnh hưởng tới kích thước của dialog.
 * Chế độ sáng phủ lớp trắng mờ; chế độ tối dùng ảnh đêm và phủ lớp màu bề mặt tối để chữ dễ đọc.
 */
@Composable
fun Modifier.dialogBackgroundImage(
    shape: Shape = DialogShape,
    overlayAlpha: Float = 0.45f
): Modifier {
    val image = ImageBitmap.imageResource(R.drawable.dialog_background)
    val colorFilter = rememberNightArtFilter(ArtPaper.Dialog)
    val overlay = if (colorFilter != null) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    } else {
        Color.White.copy(alpha = overlayAlpha)
    }

    return this
        .clip(shape)
        .drawBehind {
            val scale = maxOf(size.width / image.width, size.height / image.height)
            val dstWidth = (image.width * scale).roundToInt()
            val dstHeight = (image.height * scale).roundToInt()

            drawImage(
                image = image,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset(
                    x = ((size.width - dstWidth) / 2f).roundToInt(),
                    y = ((size.height - dstHeight) / 2f).roundToInt()
                ),
                dstSize = IntSize(dstWidth, dstHeight),
                colorFilter = colorFilter,
                filterQuality = FilterQuality.Medium
            )

            drawRect(overlay)
        }
}

/**
 * Dialog xác thực nay theo giao diện chung của app (sáng/tối).
 */
@Composable
fun AuthDialogTheme(content: @Composable () -> Unit) {
    content()
}