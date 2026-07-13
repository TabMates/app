package de.tabmates.composeapp

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

@Composable
actual fun ApplySystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val activity = view.context as? ComponentActivity ?: return@SideEffect
        val scrim = Color.Transparent.toArgb()
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(scrim, scrim) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(scrim, scrim) { darkTheme },
        )
    }
}
