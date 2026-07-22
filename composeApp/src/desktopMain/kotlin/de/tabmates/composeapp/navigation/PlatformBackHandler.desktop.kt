package de.tabmates.composeapp.navigation

import androidx.compose.runtime.Composable

// Desktop exposes no host-level back affordance to bridge here.
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
