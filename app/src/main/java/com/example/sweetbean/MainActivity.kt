package com.example.sweetbean

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.sweetbean.di.AuthDependencies
import com.example.sweetbean.ui.auth.gate.AuthGateRoute
import com.example.sweetbean.ui.splash.SplashScreen
import com.example.sweetbean.ui.theme.SweetBeanTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import com.example.sweetbean.ui.main.MainScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

private enum class AppScreen {
    Splash,
    Auth,
    Home
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SweetBeanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // rememberSaveable: xoay màn hình không đưa app về Splash rồi chạy lại 1,5 giây.
                    var currentScreen by rememberSaveable {
                        mutableStateOf(AppScreen.Splash)
                    }

                    LaunchedEffect(Unit) {
                        if (currentScreen == AppScreen.Splash) {
                            delay(1500.milliseconds)
                            currentScreen = AppScreen.Auth
                        }
                    }

                    when (currentScreen) {
                        AppScreen.Splash -> {
                            SplashScreen()
                        }

                        AppScreen.Auth -> {
                            AuthGateRoute(
                                onNavigateToHome = {
                                    currentScreen = AppScreen.Home
                                }
                            )
                        }

                        AppScreen.Home -> {
                            MainScreen(
                                onLogout = {
                                    AuthDependencies.firebaseAuth.signOut()
                                    currentScreen = AppScreen.Auth
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
