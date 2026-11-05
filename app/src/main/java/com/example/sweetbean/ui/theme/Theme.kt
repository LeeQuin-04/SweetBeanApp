package com.example.sweetbean.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB9B4F5),
    onPrimary = Color(0xFF22275A),
    primaryContainer = Color(0xFF343C7A),
    onPrimaryContainer = Color(0xFFE4E6FF),
    secondary = Color(0xFFC3C9F2),
    tertiary = BeanCandyDark,
    background = BeanNight,
    onBackground = Color(0xFFE8ECFA),
    surface = BeanNightSurface,
    onSurface = Color(0xFFE8ECFA),
    surfaceVariant = Color(0xFF2B3456),
    onSurfaceVariant = Color(0xFFAEB7D6),
    outline = Color(0xFF6F7BA6),
    outlineVariant = Color(0xFF343E63)
)

private val LightColorScheme = lightColorScheme(
    primary = BeanViolet,
    onPrimary = Color.White,
    primaryContainer = BeanVioletContainer,
    onPrimaryContainer = Color(0xFF2B1B7A),
    secondary = Color(0xFF9B7BEA),
    tertiary = BeanCandy,
    tertiaryContainer = BeanCandyContainer,
    background = BeanMist,
    onBackground = BeanInk,
    surface = Color.White,
    onSurface = BeanInk,
    surfaceVariant = Color(0xFFEDE8FB),
    onSurfaceVariant = BeanInkSoft,
    outline = Color(0xFFB9B0E0),
    outlineVariant = Color(0xFFE0DAF5)
)

@Composable
fun SweetBeanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}