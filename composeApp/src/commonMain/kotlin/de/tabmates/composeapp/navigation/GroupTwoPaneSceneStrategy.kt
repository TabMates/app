package de.tabmates.composeapp.navigation

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import de.tabmates.core.presentation.navigation.PaneRole
import de.tabmates.features.tabgroup.presentation.navigation.groupoverview.GroupTwoPane

/**
 * Renders the group list and the entry stacked on top of it as two panes of a single scene on wide
 * windows, so the back stack alone decides what the detail pane shows: `[Group]` is the list plus an
 * empty state, `[Group, GroupDetail]` fills the pane with that group, and `[Group, GroupDetail,
 * SettleUp]` swaps the pane to settle-up. Back pops one entry and the pane follows.
 *
 * Returns null on compact windows and for every other stack shape, which drops NavDisplay back to
 * its single-pane scene — so Add Entry, Entry Detail and Group Settings still take the whole window
 * even on a tablet.
 */
@Composable
fun rememberGroupTwoPaneSceneStrategy(): SceneStrategy<NavKey> {
    val isExpanded =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
            WIDTH_DP_MEDIUM_LOWER_BOUND,
        )
    return remember(isExpanded) { GroupTwoPaneSceneStrategy(isExpanded) }
}

private class GroupTwoPaneSceneStrategy(private val isExpanded: Boolean) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        if (!isExpanded) return null
        val listIndex = entries.indexOfLast { it.paneRole == PaneRole.LIST }
        if (listIndex < 0) return null
        val above = entries.subList(listIndex + 1, entries.size)
        // Anything that isn't pane content covers both panes instead of splitting them.
        if (above.any { it.paneRole != PaneRole.DETAIL }) return null
        val listEntry = entries[listIndex]
        return GroupTwoPaneScene(
            key = above.lastOrNull()?.contentKey ?: listEntry.contentKey,
            listEntry = listEntry,
            detailEntries = above.toList(),
            previousEntries = entries.dropLast(1),
        )
    }
}

private val NavEntry<NavKey>.paneRole: Any?
    get() = metadata[PaneRole.KEY]

private data class GroupTwoPaneScene(
    override val key: Any,
    val listEntry: NavEntry<NavKey>,
    val detailEntries: List<NavEntry<NavKey>>,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    override val entries: List<NavEntry<NavKey>> = listOf(listEntry) + detailEntries

    override val content: @Composable () -> Unit = {
        GroupTwoPane(
            listPane = { listEntry.Content() },
            // Every detail entry stays composed, stacked with the newest on top — each entry paints
            // an opaque background of its own. Dropping the covered ones instead would throw away
            // their saved state, so backing out of settle-up would lose the group's selected tab.
            detailPane =
                if (detailEntries.isEmpty()) {
                    null
                } else {
                    { detailEntries.forEach { entry -> entry.Content() } }
                },
        )
    }
}
