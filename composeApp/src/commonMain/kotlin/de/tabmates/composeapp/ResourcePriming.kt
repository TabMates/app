package de.tabmates.composeapp

import androidx.compose.runtime.Composable

/**
 * Waits for compose-resources to be safe to use before the caller renders anything that depends
 * on them. Only the wasmJs target does real work here — see the `web` actual for why. Other
 * targets load resources locally (bundled in the APK/jar/app bundle) and return `true`
 * immediately, so this has no effect on them.
 */
@Composable
expect fun rememberResourcesPrimed(): Boolean
