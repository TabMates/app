package de.tabmates.core.presentation.navigation

/**
 * Marks a nav entry as one half of a list/detail pair so a `SceneStrategy` can render two back-stack
 * entries side by side on wide windows instead of stacking them.
 *
 * Attached declaratively through `entry(metadata = PaneRole.list)`, because a `NavEntry` hides its
 * typed key — metadata is the only thing a strategy can read back off an entry.
 */
object PaneRole {
    const val KEY: String = "de.tabmates.navigation.paneRole"
    const val LIST: String = "list"
    const val DETAIL: String = "detail"

    /** Metadata for the entry that owns the left pane. */
    val list: Map<String, Any> = mapOf(KEY to LIST)

    /** Metadata for entries that may fill the right pane above a [list] entry. */
    val detail: Map<String, Any> = mapOf(KEY to DETAIL)
}
