package de.tabmates.composeapp

import androidx.compose.runtime.Composable

/**
 * Reconciles the host platform's system bars (status + navigation) with the app's
 * active [darkTheme]. Android re-applies `enableEdgeToEdge` with a `SystemBarStyle` that
 * uses [darkTheme] for the `detectDarkMode` lambda, so the bar icons follow the in-app
 * `ThemeMode` override (Light / Dark / System) rather than mirroring the OS night-mode
 * flag. Other targets are no-ops because they don't expose a system bar concept here.
 */
@Composable
expect fun ApplySystemBars(darkTheme: Boolean)
