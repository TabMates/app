package de.tabmates.composeapp

import androidx.compose.runtime.Composable

// iOS resources are bundled in the app bundle, no network fetch/cache race to guard against.
@Composable
actual fun rememberResourcesPrimed(): Boolean = true
