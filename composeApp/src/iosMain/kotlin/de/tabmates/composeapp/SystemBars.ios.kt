package de.tabmates.composeapp

import androidx.compose.runtime.Composable

@Composable
actual fun ApplySystemBars(darkTheme: Boolean) {
    // iOS status bar style is controlled by the Swift/UIKit layer (Info.plist + UIViewController).
    // Not managed from the Compose side in this project.
}
