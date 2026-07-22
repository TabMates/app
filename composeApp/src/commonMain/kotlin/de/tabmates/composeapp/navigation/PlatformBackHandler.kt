package de.tabmates.composeapp.navigation

import androidx.compose.runtime.Composable

/**
 * Bridges the host platform's "back" affordance to the app's Nav3 back stack.
 *
 * Only the **web** target provides a real implementation: it wires the browser's Back
 * button (History API `popstate`) to [onBack], so navigating back in the browser pops
 * the Compose [androidx.navigation3.runtime.NavBackStack] — mirroring how Android's
 * system back drives `NavDisplay`. When [enabled] is `false` (the back stack is at its
 * root) the web implementation traps the Back press and stays in the app instead of
 * unloading it.
 *
 * Android, iOS and desktop are no-ops: Android's system back is already consumed by
 * `NavDisplay`'s predictive-back support, and the other targets expose no equivalent
 * host-level back affordance here.
 *
 * @param enabled whether a back press should pop the stack. Pass `backStack.size > 1`
 *   so the root screen keeps the user in the app.
 * @param onBack invoked on a back press when [enabled]; typically `backStack.removeLastOrNull()`.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
