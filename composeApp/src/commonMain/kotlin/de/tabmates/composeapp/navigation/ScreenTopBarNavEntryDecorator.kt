package de.tabmates.composeapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import de.tabmates.core.presentation.navigation.LocalTopBarActionsController
import de.tabmates.core.presentation.navigation.ScreenWithTopBar

/**
 * Renders each entry's [ScreenTopBar] as part of the entry's own content so the bar animates with
 * the screen inside NavDisplay, instead of living in a host-Scaffold slot that snaps in/out and
 * shifts everything below it.
 *
 * Reads the decorated entry's own key rather than the back stack's last key: during a crossfade or
 * predictive-back gesture two entries are visible at once, and each must show its own bar.
 *
 * Also paints an opaque [MaterialTheme.colorScheme.background] behind every entry: NavEntry
 * content is otherwise transparent, so when two entries render at once (crossfade, predictive
 * back) the outgoing screen showed through as a ghost instead of a solid page.
 */
@Composable
fun rememberScreenTopBarNavEntryDecorator(backStack: NavBackStack<NavKey>): NavEntryDecorator<NavKey> =
    remember(backStack) {
        NavEntryDecorator { entry ->
            // NavEntry.key is private; re-wrapping hands the typed key back through the content
            // lambda — the same mechanism nav3's own decorateEntry uses to fold decorators.
            val wrapped: NavEntry<NavKey> = NavEntry(navEntry = entry) { key ->
                val controller = LocalTopBarActionsController.current
                val override = controller?.overrideFor(key)
                val declared = key as? ScreenWithTopBar
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (override != null || declared != null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (override != null) {
                                ScreenTopBar(
                                    title = override.title,
                                    navigationAction = override.navigationAction,
                                    onNavigationClick = override.onNavigationClick,
                                    actions = override.actions,
                                )
                            } else if (declared != null) {
                                ScreenTopBar(
                                    title = declared.topBarTitle,
                                    navigationAction = declared.topBarAction,
                                    onNavigationClick = { backStack.removeLastOrNull() },
                                    actions = { controller?.actionsFor(key)?.invoke() },
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                entry.Content()
                            }
                        }
                    } else {
                        entry.Content()
                    }
                }
            }
            wrapped.Content()
        }
    }
