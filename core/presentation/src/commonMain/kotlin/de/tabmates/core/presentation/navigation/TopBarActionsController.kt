package de.tabmates.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.presentation.util.UiText

/**
 * Lets a routed screen contribute to the host scaffold's single top bar at runtime — for parts that
 * need live callbacks/state that the declarative [ScreenWithTopBar] NavKey can't express.
 *
 * Two channels, both keyed by [NavKey] so the scaffold only ever shows the *current* destination's
 * (and predictive-back previews of other screens never clobber it):
 * - [TopBarActions]: trailing actions appended to the declarative top bar (e.g. Save).
 * - [OverrideTopBar]: a full top bar (title + nav icon + actions) that *replaces* the declarative
 *   one while present — used for in-screen sub-views like the split editor.
 */
class TopBarActionsController {
    private val actions = mutableStateMapOf<NavKey, @Composable () -> Unit>()
    private val overrides = mutableStateMapOf<NavKey, ScreenTopBarSpec>()

    fun publishActions(
        key: NavKey,
        content: @Composable () -> Unit,
    ) {
        actions[key] = content
    }

    fun clearActions(key: NavKey) {
        actions.remove(key)
    }

    fun actionsFor(key: NavKey?): (@Composable () -> Unit)? = key?.let { actions[it] }

    fun publishOverride(
        key: NavKey,
        spec: ScreenTopBarSpec,
    ) {
        overrides[key] = spec
    }

    fun clearOverride(key: NavKey) {
        overrides.remove(key)
    }

    fun overrideFor(key: NavKey?): ScreenTopBarSpec? = key?.let { overrides[it] }
}

/** A full top-bar specification a screen can install over the declarative one. */
class ScreenTopBarSpec(
    val title: UiText,
    val navigationAction: TopBarAction,
    val onNavigationClick: () -> Unit,
    val actions: @Composable () -> Unit,
)

val LocalTopBarActionsController = staticCompositionLocalOf<TopBarActionsController?> { null }

/**
 * Publishes [content] as [key]'s trailing top-bar actions while in composition, clearing on dispose.
 * Re-published every recomposition so captured state (e.g. a submit flag) stays current. [key] is
 * the screen's own NavKey/route.
 */
@Composable
fun TopBarActions(
    key: NavKey,
    content: @Composable () -> Unit,
) {
    val controller = LocalTopBarActionsController.current ?: return
    SideEffect { controller.publishActions(key, content) }
    DisposableEffect(controller, key) {
        onDispose { controller.clearActions(key) }
    }
}

/**
 * Replaces the declarative top bar for [key] with the given title/navigation/actions while in
 * composition, clearing on dispose. For in-screen sub-views (e.g. a split editor) that need their
 * own scaffold top bar without being a separate route.
 */
@Composable
fun OverrideTopBar(
    key: NavKey,
    title: UiText,
    navigationAction: TopBarAction,
    onNavigationClick: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    val controller = LocalTopBarActionsController.current ?: return
    SideEffect {
        controller.publishOverride(
            key,
            ScreenTopBarSpec(title, navigationAction, onNavigationClick, actions),
        )
    }
    DisposableEffect(controller, key) {
        onDispose { controller.clearOverride(key) }
    }
}
