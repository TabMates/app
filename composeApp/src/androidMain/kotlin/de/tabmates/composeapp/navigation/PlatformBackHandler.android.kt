package de.tabmates.composeapp.navigation

import androidx.compose.runtime.Composable

// Android's system back is already handled by NavDisplay's built-in predictive-back support.
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
