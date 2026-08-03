package de.tabmates.core.presentation.format

import androidx.compose.runtime.staticCompositionLocalOf

private val deviceNumberSymbols by lazy { platformNumberSymbols() }

/**
 * The [NumberSymbols] composables format with. It reads the device locale directly rather than
 * being provided from the graph, so previews and screenshots need no wiring — `CorePresentationModule`
 * hands ViewModels the same reading, and both are resolved once at startup from the same source.
 * Override it to force a locale, e.g. in a preview.
 */
val LocalNumberSymbols = staticCompositionLocalOf { deviceNumberSymbols }
