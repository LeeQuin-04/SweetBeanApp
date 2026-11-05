package com.example.sweetbean.ui.common

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Chiều cao vùng bị thanh điều hướng nổi che ở đáy (gồm cả khoảng cách tới mép). */
val LocalFloatingBarHeight = compositionLocalOf<Dp> { 0.dp }