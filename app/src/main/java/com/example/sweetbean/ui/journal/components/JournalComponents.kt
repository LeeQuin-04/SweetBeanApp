package com.example.sweetbean.ui.journal.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sweetbean.data.journal.Emotion
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import com.example.sweetbean.R
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.example.sweetbean.ui.common.rememberNightArtFilter

/** Mỗi cảm xúc một màu, đi từ nắng (vui) đến đêm (buồn). */
val Emotion.accent: Color
    get() = when (this) {
        Emotion.VERY_HAPPY -> Color(0xFFFFB347)
        Emotion.HAPPY -> Color(0xFF4FC9A0)
        Emotion.NEUTRAL -> Color(0xFFA593F5)
        Emotion.SAD -> Color(0xFF6C9BF2)
        Emotion.VERY_SAD -> Color(0xFF5B54C9)
    }

/** Gợi ý viết đổi theo cảm xúc đã chọn. */
val Emotion.prompt: String
    get() = when (this) {
        Emotion.VERY_HAPPY -> "Điều gì làm hôm nay tuyệt vời đến vậy?"
        Emotion.HAPPY -> "Có chuyện nhỏ nào làm bạn vui hôm nay?"
        Emotion.NEUTRAL -> "Hôm nay trôi qua thế nào?"
        Emotion.SAD -> "Có điều gì đang làm bạn nặng lòng?"
        Emotion.VERY_SAD -> "Cứ viết ra những gì bạn đang nghĩ."
    }

/** Chưa chọn cảm xúc thì dùng màu thương hiệu. */
@Composable
fun Emotion?.accentOrBrand(): Color = this?.accent ?: MaterialTheme.colorScheme.primary

@Composable
fun EmotionBadge(
    emotion: Emotion,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val accent = emotion.accent

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.40f), accent.copy(alpha = 0.12f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emotion.emoji, fontSize = (size.value * 0.52f).sp)
    }
}

/** Emoji lớn ở đầu form: nảy lên mỗi khi đổi cảm xúc. */
@Composable
fun MoodHero(
    emotion: Emotion?,
    modifier: Modifier = Modifier
) {
    val accent = emotion.accentOrBrand()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(emotion) {
        if (emotion != null) {
            scale.snapTo(0.6f)
            scale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 250f))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.45f), accent.copy(alpha = 0.10f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emotion?.emoji ?: "🙂",
                fontSize = 56.sp,
                modifier = Modifier.alpha(if (emotion == null) 0.45f else 1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = emotion?.label ?: "Hôm nay bạn thấy thế nào?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EmotionPicker(
    selected: Emotion?,
    onSelected: (Emotion) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Emotion.entries.forEach { emotion ->
            val isSelected = emotion == selected
            val accent = emotion.accent

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.22f else 1f,
                animationSpec = spring(dampingRatio = 0.42f, stiffness = 380f),
                label = "emotionScale"
            )
            val dim by animateFloatAsState(
                targetValue = if (selected == null || isSelected) 1f else 0.5f,
                label = "emotionDim"
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = dim
                    }
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (isSelected) 0.35f else 0.16f))
                    .then(
                        if (isSelected) Modifier.border(2.5.dp, accent, CircleShape)
                        else Modifier
                    )
                    .clickable(enabled = enabled) { onSelected(emotion) }
                    .semantics { contentDescription = emotion.label },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emotion.emoji, fontSize = 28.sp)
            }
        }
    }
}

/**
 * Ảnh thu nhỏ. [model] là URL (String), Uri hoặc null; video chỉ có biểu tượng phát đè lên.
 * Kích thước và bo góc do nơi gọi truyền qua [modifier].
 */
@Composable
fun MediaThumb(
    model: Any?,
    isVideo: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isVideo) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Nhún nhẹ khi chạm: phản hồi trực tiếp cho thao tác của người dùng. */
@Composable
fun Modifier.bouncyClickable(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "pressScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}


private const val TOP_ART = 0.40f
private const val BOTTOM_ART = 0.32f

enum class JournalScene(
    @DrawableRes val resId: Int,
    val moon: Offset,
    val lamp: Offset
) {
    List(R.drawable.journal_bg_list, moon = Offset(0.10f, 0.45f), lamp = Offset(0.20f, 0.50f)),
    Editor(R.drawable.journal_bg_editor, moon = Offset(0.86f, 0.30f), lamp = Offset(0.81f, 0.72f))
}

private val MoonColor = Color(0xFFBED4FF)   // trăng: xanh bạc
private val LampColor = Color(0xFFFFBE6E)   // đèn: hổ phách ấm

@Composable
fun JournalBackdrop(
    modifier: Modifier = Modifier,
    scene: JournalScene = JournalScene.List,
    content: @Composable BoxScope.() -> Unit
) {
    val base = MaterialTheme.colorScheme.background
    val isDark = base.luminance() < 0.5f
    val painter = painterResource(scene.resId)
    val ratio = painter.intrinsicSize.width / painter.intrinsicSize.height
    val colorFilter = rememberNightArtFilter()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(base)
    ) {
        // Chiều cao ảnh khi phủ hết bề ngang màn hình.
        val fullHeight = maxWidth / ratio

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            colorFilter = colorFilter,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(fullHeight * TOP_ART)
                .fadeEdge(fadeAtBottom = true)
        )

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.BottomCenter,
            colorFilter = colorFilter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(fullHeight * BOTTOM_ART)
                .fadeEdge(fadeAtBottom = false)
        )

        // Lớp mờ nhẹ phía trên để tiêu đề không chìm vào khung cửa sổ.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(base.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )

        // Ánh sáng le lói của đêm: chỉ ở chế độ tối.
        if (isDark) {
            val breathe = rememberInfiniteTransition(label = "glimmer")
            val lampPulse by breathe.animateFloat(
                initialValue = 0.80f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "lampPulse"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val topHeight = fullHeight.toPx() * TOP_ART
                        val bottomHeight = fullHeight.toPx() * BOTTOM_ART

                        // Ánh trăng qua cửa sổ.
                        drawRect(
                            brush = Brush.radialGradient(
                                0f to MoonColor.copy(alpha = 0.34f),
                                0.4f to MoonColor.copy(alpha = 0.12f),
                                1f to Color.Transparent,
                                center = Offset(size.width * scene.moon.x, topHeight * scene.moon.y),
                                radius = size.width * 0.75f
                            ),
                            blendMode = BlendMode.Screen
                        )

                        // Ánh ấm quanh cốc, thở nhẹ.
                        val strength = 0.40f * lampPulse
                        drawRect(
                            brush = Brush.radialGradient(
                                0f to LampColor.copy(alpha = strength),
                                0.4f to LampColor.copy(alpha = strength * 0.35f),
                                1f to Color.Transparent,
                                center = Offset(
                                    size.width * scene.lamp.x,
                                    size.height - bottomHeight + bottomHeight * scene.lamp.y
                                ),
                                radius = size.width * 0.50f
                            ),
                            blendMode = BlendMode.Screen
                        )
                    }
            )
        }

        content()
    }
}


private fun Modifier.fadeEdge(fadeAtBottom: Boolean, fraction: Float = 0.35f): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val stops = if (fadeAtBottom) {
                arrayOf(0f to Color.Black, (1f - fraction) to Color.Black, 1f to Color.Transparent)
            } else {
                arrayOf(0f to Color.Transparent, fraction to Color.Black, 1f to Color.Black)
            }
            drawRect(Brush.verticalGradient(*stops), blendMode = BlendMode.DstIn)
        }

