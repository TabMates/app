package de.tabmates.composeapp

import androidx.compose.runtime.Composable

// Android resources are bundled in the APK, no network fetch/cache race to guard against.
@Composable
actual fun rememberResourcesPrimed(): Boolean = true
