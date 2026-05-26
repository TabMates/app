package de.tabmates.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * Lets a routed screen publish trailing top-bar actions (Save, edit, delete, …) to the host
 * scaffold's top bar. Title and navigation icon stay declarative on the [ScreenWithTopBar] NavKey;
 * only the trailing actions — which need live callbacks and screen state — flow through here.
 *
 * Actions are stored per [NavKey] and the scaffold renders only the *current* destination's. This
 * survives predictive-back, where the previous screen is composed for the preview and publishes its
 * own actions before the gesture is committed: keying by NavKey keeps each screen's actions
 * independent, so the in-progress destination never overwrites the still-current screen's.
 */
class TopBarActionsController {
    private val entries = mutableStateMapOf<NavKey, @Composable () -> Unit>()

    fun publish(
        key: NavKey,
        content: @Composable () -> Unit,
    ) {
        entries[key] = content
    }

    fun clear(key: NavKey) {
        entries.remove(key)
    }

    fun contentFor(key: NavKey?): (@Composable () -> Unit)? = key?.let { entries[it] }
}

val LocalTopBarActionsController = staticCompositionLocalOf<TopBarActionsController?> { null }

/**
 * Publishes [content] as [key]'s trailing top-bar actions while this composable is in composition,
 * clearing it on dispose. Re-published on every recomposition so captured state (e.g. a submit
 * flag) stays current. [key] is the screen's own NavKey/route.
 */
@Composable
fun TopBarActions(
    key: NavKey,
    content: @Composable () -> Unit,
) {
    val controller = LocalTopBarActionsController.current ?: return
    SideEffect { controller.publish(key, content) }
    DisposableEffect(controller, key) {
        onDispose { controller.clear(key) }
    }
}
