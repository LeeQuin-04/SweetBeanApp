package com.example.sweetbean.ui.journal

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

enum class JournalViewMode { CALENDAR, TIMELINE }

private const val PREFS = "sweetbean_prefs"
private const val KEY_VIEW_MODE = "journal_view_mode"

/** Chế độ xem đang chọn (mặc định Lịch) và hàm đổi; lựa chọn được nhớ trên máy. */
@Composable
fun rememberJournalViewMode(): Pair<JournalViewMode, (JournalViewMode) -> Unit> {
    val context = LocalContext.current.applicationContext
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var mode by remember {
        mutableStateOf(
            runCatching { JournalViewMode.valueOf(prefs.getString(KEY_VIEW_MODE, "") ?: "") }
                .getOrDefault(JournalViewMode.CALENDAR)
        )
    }
    val change = remember(prefs) {
        { newMode: JournalViewMode ->
            mode = newMode
            prefs.edit { putString(KEY_VIEW_MODE, newMode.name) }
        }
    }
    return mode to change
}

@Composable
fun ViewModeToggle(
    mode: JournalViewMode,
    onChange: (JournalViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(3.dp)
    ) {
        ViewModeSegment(
            icon = Icons.Default.DateRange,
            description = "Xem theo lịch",
            selected = mode == JournalViewMode.CALENDAR,
            onClick = { onChange(JournalViewMode.CALENDAR) }
        )
        ViewModeSegment(
            icon = Icons.AutoMirrored.Filled.List,
            description = "Xem theo dòng thời gian",
            selected = mode == JournalViewMode.TIMELINE,
            onClick = { onChange(JournalViewMode.TIMELINE) }
        )
    }
}

@Composable
private fun ViewModeSegment(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 32.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) colors.onPrimary else colors.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}