package com.example.sweetbean.ui.journal.emotion

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.sweetbean.R
import com.example.sweetbean.data.journal.Emotion
import androidx.core.content.edit

/**
 * Một bộ biểu tượng cảm xúc. [icons] = null nghĩa là dùng emoji của hệ thống.
 * Nhật ký chỉ lưu khóa cảm xúc nên đổi bộ thì nhật ký cũ cũng đổi theo.
 */
data class EmotionSet(
    val id: String,
    val label: String,
    val icons: Map<Emotion, Int>?
)

object EmotionSets {
    val Bean = EmotionSet(
        id = "bean",
        label = "Đậu nảy mầm",
        icons = mapOf(
            Emotion.VERY_HAPPY to R.drawable.emo_bean_very_happy,
            Emotion.HAPPY to R.drawable.emo_bean_happy,
            Emotion.NEUTRAL to R.drawable.emo_bean_neutral,
            Emotion.SAD to R.drawable.emo_bean_sad,
            Emotion.VERY_SAD to R.drawable.emo_bean_very_sad
        )
    )

    val Cloud = EmotionSet(
        id = "cloud",
        label = "Mây",
        icons = mapOf(
            Emotion.VERY_HAPPY to R.drawable.emo_cloud_very_happy,
            Emotion.HAPPY to R.drawable.emo_cloud_happy,
            Emotion.NEUTRAL to R.drawable.emo_cloud_neutral,
            Emotion.SAD to R.drawable.emo_cloud_sad,
            Emotion.VERY_SAD to R.drawable.emo_cloud_very_sad
        )
    )

    val Animal = EmotionSet(
        id = "animal",
        label = "Động vật",
        icons = mapOf(
            Emotion.VERY_HAPPY to R.drawable.emo_animal_very_happy,
            Emotion.HAPPY to R.drawable.emo_animal_happy,
            Emotion.NEUTRAL to R.drawable.emo_animal_neutral,
            Emotion.SAD to R.drawable.emo_animal_sad,
            Emotion.VERY_SAD to R.drawable.emo_animal_very_sad
        )
    )

    val System = EmotionSet(id = "system", label = "Emoji", icons = null)

    /**
     * Thứ tự hiển thị trong bộ chọn.
     */
    val all: List<EmotionSet> = listOf(Bean, Cloud, Animal, System)

    val default: EmotionSet = Bean

    fun byId(id: String?): EmotionSet = all.firstOrNull { it.id == id } ?: default
}

val LocalEmotionSet = compositionLocalOf { EmotionSets.default }
val LocalEmotionSetChanger = compositionLocalOf<(EmotionSet) -> Unit> { {} }

private const val PREFS = "sweetbean_prefs"
private const val KEY_EMOTION_SET = "emotion_set"

/** Cung cấp bộ biểu tượng đang chọn cho cả cây giao diện bên dưới và nhớ lựa chọn trên máy. */
@Composable
fun ProvideEmotionSet(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var current by remember {
        mutableStateOf(EmotionSets.byId(prefs.getString(KEY_EMOTION_SET, null)))
    }
    val change = remember(prefs) {
        { set: EmotionSet ->
            current = set
            prefs.edit { putString(KEY_EMOTION_SET, set.id) }
        }
    }

    CompositionLocalProvider(
        LocalEmotionSet provides current,
        LocalEmotionSetChanger provides change,
        content = content
    )
}