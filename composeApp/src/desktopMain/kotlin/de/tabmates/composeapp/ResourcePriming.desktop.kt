package de.tabmates.composeapp

import androidx.compose.runtime.Composable

// Desktop resources are bundled on the classpath, no network fetch/cache race to guard against.
@Composable
actual fun rememberResourcesPrimed(): Boolean = true
