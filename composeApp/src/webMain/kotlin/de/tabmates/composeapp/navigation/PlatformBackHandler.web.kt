package de.tabmates.composeapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * Bridges the browser Back button to the Nav3 back stack.
 *
 * On mount a sentinel history entry is pushed on top of the loaded page, so the first
 * Back press produces a `popstate` we can intercept instead of unloading the app. On
 * every `popstate` the sentinel is immediately re-pushed — this pins the app to the top
 * of the browser history, so Back can never navigate away from it (stay-in-app at the
 * root) — and then, when [enabled], [onBack] pops one screen off the Compose back stack.
 *
 * Re-pushing truncates any forward history, so the browser Forward button is intentionally
 * inert; in-app navigation does not sync the address bar (back-button integration only).
 */
@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnBack by rememberUpdatedState(onBack)

    DisposableEffect(Unit) {
        // Arm the sentinel: an extra history entry above the loaded page.
        window.history.pushState(null, "", window.location.href)

        val listener: (Event) -> Unit = {
            // Re-arm first so the app is never popped off the browser history.
            window.history.pushState(null, "", window.location.href)
            if (currentEnabled) {
                currentOnBack()
            }
        }
        window.addEventListener("popstate", listener)

        onDispose {
            window.removeEventListener("popstate", listener)
        }
    }
}
