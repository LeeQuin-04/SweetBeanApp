package com.example.sweetbean.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sweetbean.ui.common.LocalFloatingBarHeight
import com.example.sweetbean.ui.home.HomeScreen
import com.example.sweetbean.ui.journal.JournalRoute
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import com.example.sweetbean.ui.community.CommunityRoute
import androidx.compose.foundation.layout.consumeWindowInsets
import com.example.sweetbean.ui.journal.emotion.ProvideEmotionSet
import com.example.sweetbean.ui.journal.emotion.EmotionSetButton

private enum class MainTab(val label: String, val icon: ImageVector) {
    Home("Trang chủ", Icons.Default.Home),
    Journal("Nhật ký", Icons.Default.Edit),
    Community("Cộng đồng", Icons.Default.Face),
    Profile("Cá nhân", Icons.Default.Person)
}

private val FloatingBarHeight = 64.dp
private val FloatingBarMargin = 12.dp

@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    val reservedBottom = FloatingBarHeight + FloatingBarMargin

    ProvideEmotionSet {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                CompositionLocalProvider(LocalFloatingBarHeight provides reservedBottom) {
                    when (currentTab) {
                        MainTab.Journal -> JournalRoute()

                        MainTab.Home -> Box(
                            Modifier
                                .fillMaxSize()
                                .padding(bottom = reservedBottom)
                        ) {
                            HomeScreen(onLogout = onLogout)
                        }

                        MainTab.Community -> CommunityRoute()

                        else -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = reservedBottom),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${currentTab.label} (sắp có)")
                        }
                    }
                }

                FloatingNavBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = FloatingBarMargin)
                )
            }
        }
    }
}


@Composable
private fun FloatingNavBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(FloatingBarHeight),
        shape = CircleShape,
        color = colors.surface.copy(alpha = 0.30f),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == currentTab

                val background by animateColorAsState(
                    targetValue = if (selected) colors.primary
                    else Color.Transparent,
                    animationSpec = tween(200),
                    label = "tabBackground"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (selected) colors.onPrimary.copy(alpha = 0.5f)
                    else colors.onSurface.copy(alpha = 0.80f),
                    animationSpec = tween(200),
                    label = "tabContent"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(background)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(tab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = contentColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        onLogout = {}
    )
}
